package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByOwnerId(Long ownerId);

    Optional<Unit> findByIdAndOwnerId(Long unitId, Long ownerId);

    List<Unit> findByOwnerIdAndIdIn(Long ownerId, Collection<Long> ids);

    boolean existsByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndIsMainHeroTrue(Long ownerId);

    Optional<Unit> findByOwnerIdAndIsMainHeroTrue(Long ownerId);

    List<Unit> findByShipId(Long shipId);

    long countByShipId(Long shipId);
}