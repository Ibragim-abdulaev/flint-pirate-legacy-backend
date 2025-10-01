package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.QuestChain;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.exception.ResourceNotFoundException;
import org.example.piratelegacy.auth.repository.QuestRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "quests", key = "#questKey")
    public Quest getQuestByKey(String questKey) {
        return questRepository.findByQuestKey(questKey)
                .orElseThrow(() -> new ResourceNotFoundException("Квест с ключом не найден: " + questKey));
    }

    /**
     * Находит следующий по порядку квест в рамках ОДНОЙ цепочки.
     * @param user Пользователь
     * @param chain Цепочка, в которой ищем квест
     * @param nextOrder Порядковый номер следующего квеста (например, если выполнено 2, то ищем 3)
     * @return Сущность квеста или null, если квест не найден
     */
    @Transactional(readOnly = true)
    public Quest findNextQuestInChain(User user, QuestChain chain, int nextOrder) {
        // Мы ищем следующий квест в уже загруженном списке квестов цепочки,
        // это эффективно и не требует дополнительных запросов к БД.
        return chain.getQuests().stream()
                .filter(q -> q.getQuestOrder().equals(nextOrder))
                .findFirst()
                .orElse(null);
    }
}