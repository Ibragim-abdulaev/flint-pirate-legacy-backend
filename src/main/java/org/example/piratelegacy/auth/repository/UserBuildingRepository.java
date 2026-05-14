package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserBuilding;
import org.example.piratelegacy.auth.entity.enums.BuildingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBuildingRepository extends JpaRepository<UserBuilding, Long> {

    List<UserBuilding> findByOwnerId(Long ownerId);

    Optional<UserBuilding> findByOwnerIdAndBuildingType(Long ownerId, BuildingType buildingType);

    boolean existsByOwnerIdAndBuildingType(Long ownerId, BuildingType buildingType);
}
