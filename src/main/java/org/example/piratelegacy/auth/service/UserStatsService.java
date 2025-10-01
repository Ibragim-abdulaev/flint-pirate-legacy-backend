package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UnitRepository unitRepository;

    /**
     * Добавляет опыт главному герою пользователя.
     *
     * @param user            Пользователь, которому начисляется опыт.
     * @param experienceToAdd Количество начисляемого опыта.
     */
    @Transactional
    public void addExperienceToMainHero(User user, Long experienceToAdd) {
        if (experienceToAdd == null || experienceToAdd <= 0) {
            return;
        }

        unitRepository.findByOwnerIdAndIsMainHeroTrue(user.getId()).ifPresent(hero -> {
            long currentExp = hero.getExperience();
            long newExp = currentExp + experienceToAdd;
            hero.setExperience(newExp);

            log.info("Added {} experience to hero '{}' (User ID {}). Total experience: {}",
                    experienceToAdd, hero.getName(), user.getId(), newExp);

            // TODO: Реализовать логику повышения уровня (level up)
            // checkLevelUp(hero);

            unitRepository.save(hero);
        });
    }
}