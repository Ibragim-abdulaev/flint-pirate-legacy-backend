// org.example.piratelegacy.auth.service.UserStatsService.java
// ✅ ФИНАЛЬНАЯ, РАБОЧАЯ ВЕРСИЯ ✅

package org.example.piratelegacy.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserStatsService {

    private final UnitRepository unitRepository;
    private final UserStatsService self; // Для вызова проксированного метода

    // Используем @Lazy и инъекцию через конструктор, чтобы избежать циклической зависимости
    @Autowired
    public UserStatsService(UnitRepository unitRepository, @Lazy UserStatsService self) {
        this.unitRepository = unitRepository;
        this.self = self;
    }

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

            // TODO: Реализовать логику повышения уровня
            // checkLevelUp(hero);

            Unit savedHero = unitRepository.save(hero);

            // Вызываем публичный метод с аннотациями через прокси 'self'
            self.clearUnitCaches(user.getId(), savedHero.getId());
        });
    }

    /**
     * Этот метод существует только для того, чтобы носить на себе аннотации
     * и быть вызванным через прокси для корректной работы очистки кэша.
     * @param ownerId ID владельца для очистки кэша команды.
     * @param unitId ID юнита для очистки кэша его профиля.
     */
    @Caching(evict = {
            @CacheEvict(value = "units", key = "#unitId"),
            @CacheEvict(value = "units", key = "'team_summary:' + #ownerId")
    })
    public void clearUnitCaches(Long ownerId, Long unitId) {
        log.info("Evicting caches for ownerId={} and unitId={}", ownerId, unitId);
    }
}