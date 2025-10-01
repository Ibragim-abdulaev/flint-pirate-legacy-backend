package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Quest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    @EntityGraph(attributePaths = {"itemRewards.item"})
    Optional<Quest> findByQuestKey(String questKey);
}