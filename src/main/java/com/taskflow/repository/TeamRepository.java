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

    
    @Query("SELECT t FROM Team t WHERE t.isPublic = true ORDER BY t.createdAt DESC")
    List<Team> findPublicTeamsForSuggestions(org.springframework.data.domain.Pageable pageable);
}
