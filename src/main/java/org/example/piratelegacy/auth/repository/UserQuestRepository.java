package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {
    List<UserQuest> findByUserIdOrderByStartedAtDesc(Long userId);
    Optional<UserQuest> findByUserIdAndQuestId(Long userId, Long questId);
    Optional<UserQuest> findByUserIdAndIsCompletedFalse(Long userId);
    long countByUserIdAndIsCompletedTrue(Long userId);
}
