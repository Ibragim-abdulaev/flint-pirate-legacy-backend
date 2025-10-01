package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.QuestChain;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestChainRepository extends JpaRepository<QuestChain, Long> {
    // Жадная загрузка квестов для избежания N+1 запросов в JournalService
    @Override
    @EntityGraph(attributePaths = {"quests"})
    List<QuestChain> findAll();
}