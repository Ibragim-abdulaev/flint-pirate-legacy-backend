package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Unit;
import org.example.piratelegacy.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    /**
     * Находит всех юнитов, принадлежащих указанному пользователю.
     * @param ownerId ID пользователя-владельца
     * @return Список юнитов
     */
    List<Unit> findByOwnerId(Long ownerId);

    /**
     * Находит конкретного юнита по его ID, дополнительно проверяя,
     * что он принадлежит указанному пользователю.
     * @param unitId ID юнита
     * @param ownerId ID пользователя-владельца
     * @return Optional с юнитом, если найден
     */
    Optional<Unit> findByIdAndOwnerId(Long unitId, Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndIsMainHeroTrue(Long ownerId);

    Optional<Unit> findByOwnerIdAndIsMainHeroTrue(Long ownerId);
}