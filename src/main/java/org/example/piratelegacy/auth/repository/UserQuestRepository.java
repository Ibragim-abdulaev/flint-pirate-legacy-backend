package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserQuest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

        boolean existsByUserIdAndQuestId(Long userId, Long questId);

    @EntityGraph(attributePaths = {"quest.questChain"})
    List<UserQuest> findByUserIdAndIsCompletedTrue(Long userId);

    long countByUserId(Long userId);
    Optional<UserQuest> findByUserIdAndIsCompletedFalse(Long userId);
    long countByUserIdAndIsCompletedTrue(Long userId);
    boolean existsByUserIdAndIsCompletedFalse(Long userId);
    List<UserQuest> findByUserIdOrderByStartedAtDesc(Long userId);
}