package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    List<Quest> findAllByIsActiveTrueOrderByQuestOrder();
    Optional<Quest> findByQuestOrderAndIsActiveTrue(Integer order);
}
