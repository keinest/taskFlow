/* ═══════════════════════════════════════════════════════
   TASKFLOW — app.js
   ═══════════════════════════════════════════════════════ */

// === État global ========================================
let draggedTask = null;
let selectedProjColor = '#6366f1';

// === Dark Mode Toggle ====================================
function toggleTheme() {
  const html = document.documentElement;
  const current = html.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  localStorage.setItem('taskflow-theme', next);
  const btn = document.getElementById('themeToggle');
  if (btn) btn.textContent = next === 'dark' ? '☀️' : '🌙';
}

// Init theme on page load
(function initTheme() {
  const saved = localStorage.getItem('taskflow-theme') || 'light';
  document.documentElement.setAttribute('data-theme', saved);
  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('themeToggle');
    if (btn) btn.textContent = saved === 'dark' ? '☀️' : '🌙';
  });
})();


// === Sidebar ============================================
function toggleSidebar() {
  const s = document.querySelector('.sidebar');
  const o = document.getElementById('sidebarOverlay');
  if (s) {
    s.classList.toggle('open');
    if (o) o.classList.toggle('show');
  }
}

function closeSidebar() {
  const s = document.querySelector('.sidebar');
  const o = document.getElementById('sidebarOverlay');
  if (s) s.classList.remove('open');
  if (o) o.classList.remove('show');
}

// === Notifications =======================================
function toggleNotifPanel() {
  const p = document.getElementById('notifPanel');
  if (p) {
    p.classList.toggle('hidden');
    if (!p.classList.contains('hidden')) loadNotifications();
  }
}

function loadNotifications() {
  // BUG CORRIGÉ : l'API retourne directement un tableau, pas { notifications: [...] }
  fetch('/api/notifications')
    .then(r => r.json())
    .then(data => {
      const list = document.getElementById('notifList');
      if (!list) return;
      const notifications = Array.isArray(data) ? data : (data.notifications || []);
      if (notifications.length === 0) {
        list.innerHTML = '<div class="notif-empty">Aucune notification 🎉</div>';
        return;
      }
      list.innerHTML = notifications.map(n => `
        <div class="notif-item${n.read ? '' : ' unread'}" data-id="${n.id}">
          <p>${escHtml(n.message)}</p>
          <small>${formatDate(n.createdAt)}</small>
        </div>
      `).join('');

      // Badge
      const badge = document.querySelector('.notif-badge');
      if (badge) {
        const unread = notifications.filter(n => !n.read).length;
        if (unread > 0) {
          badge.textContent = unread;
          badge.style.display = '';
        } else {
          badge.style.display = 'none';
        }
      }
    })
    .catch(() => {});
}

function markAllRead() {
  fetch('/api/notifications/read-all', { method: 'POST' })
    .then(() => loadNotifications())
    .catch(() => {});
}

// Fermer notif panel en cliquant dehors
document.addEventListener('click', e => {
  const panel = document.getElementById('notifPanel');
  const wrapper = document.querySelector('.notif-wrapper');
  if (panel && wrapper && !wrapper.contains(e.target)) {
    panel.classList.add('hidden');
  }
});

// === Modals ==============================================
function openModal(id) {
  const m = document.getElementById(id);
  if (m) {
    m.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }
}

function closeModal(id) {
  const m = document.getElementById(id);
  if (m) {
    m.classList.add('hidden');
    document.body.style.overflow = '';
  }
}

// Fermer modal via backdrop
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.add('hidden');
    document.body.style.overflow = '';
  }
});

// Fermer modal via Echap
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-overlay:not(.hidden)').forEach(m => {
      m.classList.add('hidden');
    });
    document.body.style.overflow = '';
  }
});

// === TÂCHES ==============================================

function openNewTaskModal(status) {
  resetTaskModal();
  const statusEl = document.getElementById('task-status');
  if (statusEl && status) statusEl.value = status;
  document.getElementById('taskModalTitle').textContent = 'Nouvelle tâche';
  document.getElementById('taskSaveBtn').textContent = 'Créer la tâche';
  document.getElementById('task-id').value = '';
  openModal('taskModal');
}

function resetTaskModal() {
  ['task-title','task-desc','task-due'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  ['task-project','task-assignee'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  const st = document.getElementById('task-status');
  if (st) st.value = 'TODO';
  const pr = document.getElementById('task-priority');
  if (pr) pr.value = 'MEDIUM';
  document.getElementById('task-id').value = '';
}

function editTask(taskId) {
  fetch(`/api/tasks/${taskId}`)
    .then(r => r.json())
    .then(t => {
      document.getElementById('task-id').value = t.id;
      document.getElementById('task-title').value = t.title || '';
      document.getElementById('task-desc').value = t.description || '';
      document.getElementById('task-status').value = t.status || 'TODO';
      document.getElementById('task-priority').value = t.priority || 'MEDIUM';
      document.getElementById('task-due').value = t.dueDate ? t.dueDate.substring(0, 10) : '';

      const proj = document.getElementById('task-project');
      if (proj) proj.value = t.projectId || '';
      const ass = document.getElementById('task-assignee');
      if (ass) ass.value = t.assigneeId || '';

      document.getElementById('taskModalTitle').textContent = 'Modifier la tâche';
      document.getElementById('taskSaveBtn').textContent = 'Enregistrer';
      openModal('taskModal');
    })
    .catch(() => showToast('Erreur lors du chargement', 'error'));
}

function saveTask() {
  const idEl = document.getElementById('task-id');
  const taskId = idEl ? idEl.value : '';
  const title = document.getElementById('task-title').value.trim();

  if (!title) {
    showToast('Le titre est requis', 'error');
    document.getElementById('task-title').focus();
    return;
  }

  const payload = {
    title,
    description:  document.getElementById('task-desc')?.value.trim() || null,
    status:       document.getElementById('task-status')?.value || 'TODO',
    priority:     document.getElementById('task-priority')?.value || 'MEDIUM',
    dueDate:      document.getElementById('task-due')?.value || null,
    projectId:    Number(document.getElementById('task-project')?.value) || null,
    assignedToId: Number(document.getElementById('task-assignee')?.value) || null,
  };

  const isNew = !taskId;
  const url    = isNew ? '/api/tasks' : `/api/tasks/${taskId}`;
  const method = isNew ? 'POST' : 'PUT';

  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(r => r.json())
  .then(data => {
    if (data.error) { showToast(data.error, 'error'); return; }
    closeModal('taskModal');
    showToast(isNew ? 'Tâche créée ✅' : 'Tâche mise à jour ✅', 'success');
    setTimeout(() => location.reload(), 700);
  })
  .catch(() => showToast('Erreur réseau', 'error'));
}

function deleteTask(taskId) {
  if (!confirm('Supprimer cette tâche ? Cette action est irréversible.')) return;
  fetch(`/api/tasks/${taskId}`, { method: 'DELETE' })
    .then(r => r.json())
    .then(data => {
      if (data.error) { showToast(data.error, 'error'); return; }
      showToast('Tâche supprimée', 'success');
      setTimeout(() => location.reload(), 700);
    })
    .catch(() => showToast('Erreur réseau', 'error'));
}

// === Détail Tâche =========================================
function openTaskDetail(taskId) {
  openModal('taskDetailModal');
  const body = document.getElementById('taskDetailBody');
  if (body) body.innerHTML = '<div class="loading">Chargement...</div>';

  fetch(`/api/tasks/${taskId}`)
    .then(r => r.json())
    .then(t => {
      if (t.error) { body.innerHTML = '<p style="padding:20px;color:red">Erreur : ' + t.error + '</p>'; return; }
      const title = document.getElementById('detail-title');
      if (title) title.textContent = t.title;

      // Bouton éditer
      const editBtn = document.getElementById('detail-edit-btn');
      if (editBtn) editBtn.onclick = () => { closeModal('taskDetailModal'); editTask(t.id); };

      const comments = (t.comments || []).map(c => `
        <div class="comment-item">
          <div class="avatar avatar-xs" style="background:${c.authorColor}">${c.authorInitials}</div>
          <div class="comment-content">
            <div class="comment-author">${escHtml(c.authorName)}
              <button style="background:none;border:none;cursor:pointer;color:var(--text-4);margin-left:auto" onclick="deleteComment(${c.id})" title="Supprimer">✕</button>
            </div>
            <div class="comment-text">${escHtml(c.content)}</div>
            <div class="comment-date">${formatDate(c.createdAt)}</div>
          </div>
        </div>
      `).join('') || '<p style="color:var(--text-3);font-size:13px">Aucun commentaire.</p>';

      body.innerHTML = `
        <div class="task-detail-body">
          <div class="task-detail-main">
            <div class="detail-meta-row">
              <div class="detail-meta-label">Description</div>
              <div style="font-size:14px;color:var(--text-2);line-height:1.6;margin-top:4px">${escHtml(t.description || 'Aucune description.')}</div>
            </div>
            <div class="comments-section">
              <div class="detail-meta-label">Commentaires (${(t.comments || []).length})</div>
              <div class="comment-list" id="commentList">${comments}</div>
              <div class="comment-form">
                <input type="text" id="commentInput" class="form-input comment-input"
                       placeholder="Ajouter un commentaire..." onkeydown="if(event.key==='Enter') submitComment(${t.id})"/>
                <button class="btn btn-primary btn-sm" onclick="submitComment(${t.id})">Envoyer</button>
              </div>
            </div>
          </div>
          <div class="task-detail-meta-panel">
            <div class="detail-meta-row">
              <div class="detail-meta-label">Statut</div>
              <span class="badge ${t.statusBadgeClass}">${t.statusLabel}</span>
            </div>
            <div class="detail-meta-row">
              <div class="detail-meta-label">Priorité</div>
              <span class="badge ${t.priorityBadgeClass}">${t.priorityLabel}</span>
            </div>
            ${t.projectName ? `
            <div class="detail-meta-row">
              <div class="detail-meta-label">Projet</div>
              <span class="project-tag" style="background:${t.projectColor}22;color:${t.projectColor}">${escHtml(t.projectName)}</span>
            </div>` : ''}
            <div class="detail-meta-row">
              <div class="detail-meta-label">Assigné à</div>
              <div class="detail-meta-value">
                ${t.assigneeName ? `
                  <div style="display:flex;align-items:center;gap:8px">
                    <div class="avatar avatar-xs" style="background:${t.assigneeColor}">${t.assigneeInitials}</div>
                    <span>${escHtml(t.assigneeName)}</span>
                  </div>` : '<span style="color:var(--text-3)">Non assigné</span>'}
              </div>
            </div>
            ${t.dueDate ? `
            <div class="detail-meta-row">
              <div class="detail-meta-label">Échéance</div>
              <div class="detail-meta-value ${t.overdue ? 'text-danger' : t.dueSoon ? 'text-warning' : ''}">
                📅 ${t.dueDate}
                ${t.overdue ? '<span class="badge badge-critical" style="margin-left:6px">En retard</span>' :
                  t.dueSoon ? '<span class="badge badge-high" style="margin-left:6px">Bientôt</span>' : ''}
              </div>
            </div>` : ''}
            <div style="display:flex;flex-direction:column;gap:8px;padding-top:12px">
              <button class="btn btn-ghost btn-sm" onclick="changeTaskStatus(${t.id},'TODO')" style="${t.status==='TODO'?'display:none':''}">→ À faire</button>
              <button class="btn btn-ghost btn-sm" onclick="changeTaskStatus(${t.id},'IN_PROGRESS')" style="${t.status==='IN_PROGRESS'?'display:none':''}">→ En cours</button>
              <button class="btn btn-primary btn-sm" onclick="changeTaskStatus(${t.id},'DONE')" style="${t.status==='DONE'?'display:none':''}">✅ Marquer terminée</button>
              <button class="btn btn-ghost btn-sm" onclick="deleteTask(${t.id})" style="color:var(--danger)">🗑 Supprimer</button>
            </div>
          </div>
        </div>
      `;
    })
    .catch(() => {
      if (body) body.innerHTML = '<div class="loading">Erreur de chargement</div>';
    });
}

function changeTaskStatus(id, status) {
  fetch(`/api/tasks/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  })
  .then(() => {
    showToast('Statut mis à jour ✅', 'success');
    setTimeout(() => location.reload(), 700);
  });
}

// === Commentaires =========================================
function submitComment(taskId) {
  const input = document.getElementById('commentInput');
  if (!input || !input.value.trim()) return;

  fetch(`/api/tasks/${taskId}/comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content: input.value.trim() })
  })
  .then(r => r.json())
  .then(c => {
    if (c.error) { showToast(c.error, 'error'); return; }
    const list = document.getElementById('commentList');
    const el = document.createElement('div');
    el.className = 'comment-item';
    el.innerHTML = `
      <div class="avatar avatar-xs" style="background:${c.authorColor}">${c.authorInitials}</div>
      <div class="comment-content">
        <div class="comment-author">${escHtml(c.authorName)}
          <button style="background:none;border:none;cursor:pointer;color:var(--text-4);margin-left:auto" onclick="deleteComment(${c.id})" title="Supprimer">✕</button>
        </div>
        <div class="comment-text">${escHtml(c.content)}</div>
        <div class="comment-date">À l'instant</div>
      </div>
    `;
    if (list) {
      const empty = list.querySelector('p');
      if (empty) empty.remove();
      list.appendChild(el);
      list.scrollTop = list.scrollHeight;
    }
    input.value = '';
    showToast('Commentaire ajouté', 'success');
  })
  .catch(() => showToast('Erreur', 'error'));
}

function deleteComment(commentId) {
  if (!confirm('Supprimer ce commentaire ?')) return;
  fetch(`/api/comments/${commentId}`, { method: 'DELETE' })
    .then(r => r.json())
    .then(d => {
      if (d.error) { showToast(d.error, 'error'); return; }
      showToast('Commentaire supprimé', 'success');
      // Retirer de l'UI
      const btn = document.querySelector(`[onclick="deleteComment(${commentId})"]`);
      if (btn) btn.closest('.comment-item').remove();
    })
    .catch(() => showToast('Erreur', 'error'));
}

// === Drag & Drop ==========================================
function dragStart(e) {
  draggedTask = e.currentTarget;
  draggedTask.classList.add('dragging');
  e.dataTransfer.effectAllowed = 'move';
  e.dataTransfer.setData('text/plain', draggedTask.dataset.id);
}

function dragOver(e) {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'move';
  const col = e.currentTarget;
  col.classList.add('drag-over');
}

function drop(e) {
  e.preventDefault();
  const col = e.currentTarget;
  col.classList.remove('drag-over');

  if (!draggedTask) return;
  const newStatus = col.dataset.status;
  const taskId = draggedTask.dataset.id;

  // Déplacer visuellement
  const cardsContainer = col.querySelector('.kanban-cards');
  if (cardsContainer) {
    cardsContainer.appendChild(draggedTask);
    draggedTask.dataset.status = newStatus;
  }

  // Mise à jour compteurs
  updateColumnCounts();

  // API call
  fetch(`/api/tasks/${taskId}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: newStatus })
  })
  .then(r => r.json())
  .then(d => {
    if (!d.error) showToast('Statut mis à jour', 'success');
  })
  .catch(() => showToast('Erreur lors de la mise à jour', 'error'));

  draggedTask.classList.remove('dragging');
  draggedTask = null;
}

document.addEventListener('dragend', () => {
  document.querySelectorAll('.kanban-col').forEach(c => c.classList.remove('drag-over'));
  if (draggedTask) draggedTask.classList.remove('dragging');
});

function updateColumnCounts() {
  ['TODO','IN_PROGRESS','DONE'].forEach(s => {
    const count = document.querySelectorAll(`#cards-${s} .task-card`).length;
    const el = document.getElementById(`count-${s}`);
    if (el) el.textContent = count;
  });
}

// === Vue switch ===========================================
function switchView(view) {
  const kanban = document.getElementById('kanbanView');
  const list   = document.getElementById('listView');
  const kTab   = document.getElementById('kanbanTab');
  const lTab   = document.getElementById('listTab');

  if (view === 'kanban') {
    kanban?.classList.remove('hidden');
    list?.classList.add('hidden');
    kTab?.classList.add('active');
    lTab?.classList.remove('active');
    localStorage.setItem('taskView', 'kanban');
  } else {
    list?.classList.remove('hidden');
    kanban?.classList.add('hidden');
    lTab?.classList.add('active');
    kTab?.classList.remove('active');
    localStorage.setItem('taskView', 'list');
  }
}

// Restaurer la vue préférée
window.addEventListener('DOMContentLoaded', () => {
  const saved = localStorage.getItem('taskView');
  if (saved === 'list') switchView('list');

  // Debounce recherche
  const searchInput = document.getElementById('searchInput');
  if (searchInput) {
    let timer;
    searchInput.addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(() => document.getElementById('filterForm')?.submit(), 500);
    });
  }
});

// === Projets ==============================================
function selectProjColor(color, el) {
  selectedProjColor = color;
  document.getElementById('proj-color').value = color;
  document.querySelectorAll('.color-swatch').forEach(s => s.classList.remove('active'));
  el.classList.add('active');
}

function saveProject() {
  const idEl = document.getElementById('proj-id');
  const projId = idEl ? idEl.value : '';
  const name = document.getElementById('proj-name')?.value.trim();

  if (!name) {
    showToast('Le nom du projet est requis', 'error');
    return;
  }

  const payload = {
    name,
    description: document.getElementById('proj-desc')?.value.trim() || null,
    color: document.getElementById('proj-color')?.value || '#6366f1'
  };

  const isNew = !projId;
  const url = isNew ? '/api/projects' : `/api/projects/${projId}`;
  const method = isNew ? 'POST' : 'PUT';

  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(r => r.json())
  .then(data => {
    if (data.error) { showToast(data.error, 'error'); return; }
    closeModal('projectModal');
    showToast(isNew ? 'Projet créé ✅' : 'Projet mis à jour ✅', 'success');
    setTimeout(() => location.reload(), 700);
  })
  .catch(() => showToast('Erreur réseau', 'error'));
}

function editProject(id, name, desc, color) {
  document.getElementById('proj-id').value = id;
  document.getElementById('proj-name').value = name;
  document.getElementById('proj-desc').value = desc;
  document.getElementById('proj-color').value = color;

  // Sélectionner la couleur
  document.querySelectorAll('.color-swatch').forEach(s => {
    s.classList.toggle('active', s.style.background === color);
  });

  document.getElementById('projModalTitle').textContent = 'Modifier le projet';
  document.getElementById('projSaveBtn').textContent = 'Enregistrer';
  openModal('projectModal');
}

function deleteProject(id) {
  if (!confirm('Supprimer ce projet ? Toutes ses tâches seront supprimées.')) return;
  fetch(`/api/projects/${id}`, { method: 'DELETE' })
    .then(r => r.json())
    .then(d => {
      if (d.error) { showToast(d.error, 'error'); return; }
      showToast('Projet supprimé', 'success');
      setTimeout(() => location.reload(), 700);
    })
    .catch(() => showToast('Erreur réseau', 'error'));
}

// === Charts ===============================================
function initCharts(data) {
  const statusCtx = document.getElementById('statusChart');
  if (statusCtx) {
    new Chart(statusCtx, {
      type: 'doughnut',
      data: {
        labels: ['À faire', 'En cours', 'Terminées'],
        datasets: [{
          data: [data.status.todo, data.status.inProgress, data.status.done],
          backgroundColor: ['#94a3b8', '#3b82f6', '#10b981'],
          borderWidth: 0,
          hoverOffset: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { font: { family: 'Inter', size: 12 } } }
        },
        cutout: '65%'
      }
    });
  }

  const priorityCtx = document.getElementById('priorityChart');
  if (priorityCtx) {
    new Chart(priorityCtx, {
      type: 'bar',
      data: {
        labels: ['Critique', 'Haute', 'Moyenne', 'Basse'],
        datasets: [{
          label: 'Tâches',
          data: [data.priority.critical, data.priority.high, data.priority.medium, data.priority.low],
          backgroundColor: ['#ef4444', '#f97316', '#eab308', '#22c55e'],
          borderRadius: 6,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            beginAtZero: true,
            grid: { color: '#f1f5f9' },
            ticks: { font: { family: 'Inter', size: 11 }, stepSize: 1 }
          },
          x: {
            grid: { display: false },
            ticks: { font: { family: 'Inter', size: 12 } }
          }
        }
      }
    });
  }
}

// === Utilitaires =========================================
function escHtml(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#039;');
}

function formatDate(str) {
  if (!str) return '';
  try {
    const d = new Date(str);
    return d.toLocaleDateString('fr-FR', { day:'2-digit', month:'2-digit' })
           + ' à ' + d.toLocaleTimeString('fr-FR', { hour:'2-digit', minute:'2-digit' });
  } catch { return str; }
}

function stopProp(e) { e.stopPropagation(); }

// === Toast notifications ==================================
function showToast(message, type = 'info') {
  const existing = document.getElementById('toast-container');
  if (!existing) {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:8px;';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  const colors = { success: '#10b981', error: '#ef4444', info: '#6366f1', warning: '#f59e0b' };
  toast.style.cssText = `
    background:${colors[type] || colors.info};
    color:#fff;
    padding:12px 18px;
    border-radius:10px;
    font-size:13.5px;
    font-weight:600;
    box-shadow:0 4px 16px rgba(0,0,0,.15);
    animation:slideInRight .25s ease;
    max-width:300px;
  `;
  toast.textContent = message;

  const style = document.createElement('style');
  style.textContent = '@keyframes slideInRight{from{transform:translateX(100%);opacity:0}to{transform:none;opacity:1}}';
  if (!document.getElementById('toast-style')) {
    style.id = 'toast-style';
    document.head.appendChild(style);
  }

  document.getElementById('toast-container').appendChild(toast);
  setTimeout(() => {
    toast.style.transition = 'opacity .3s, transform .3s';
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}
