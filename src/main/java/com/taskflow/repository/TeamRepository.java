package com.taskflow.repository;

import com.taskflow.entity.Team;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findAllByOrderByCreatedAtDesc();

    List<Team> findByIsPublicTrue();

    List<Team> findByCreatorId(Long creatorId);

    @Query("SELECT t FROM Team t WHERE t.isPublic = true ORDER BY SIZE(t.members) DESC")
    List<Team> findPublicTeamsOrderByPopularity();

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.id = :userId OR t.creator.id = :userId ORDER BY t.createdAt DESC")
    List<Team> findTeamsByUserId(@Param("userId") Long userId);

    /**
     * BUG CORRIGÉ #2 : RANDOM() et LIMIT ne sont pas du JPQL standard.
     * RANDOM() est une fonction SQL dialecte-spécifique (PostgreSQL/H2),
     * et LIMIT n'existe pas en JPQL → HibernateException au runtime.
     * Correction : on passe par un Pageable pour limiter les résultats,
     * et on trie par createdAt DESC (stable, reproductible en tests).
     * Pour un vrai "aléatoire", le mélange se fait en Java dans TeamService.
     */
    @Query("SELECT t FROM Team t WHERE t.isPublic = true ORDER BY t.createdAt DESC")
    List<Team> findPublicTeamsForSuggestions(org.springframework.data.domain.Pageable pageable);
}
