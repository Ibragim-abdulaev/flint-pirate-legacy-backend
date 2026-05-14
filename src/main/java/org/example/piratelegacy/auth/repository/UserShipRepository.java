package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserShip;
import org.example.piratelegacy.auth.entity.enums.ShipMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserShipRepository extends JpaRepository<UserShip, Long> {

    List<UserShip> findByOwnerId(Long ownerId);

    Optional<UserShip> findByIdAndOwnerId(Long shipId, Long ownerId);

    Optional<UserShip> findByOwnerIdAndMode(Long ownerId, ShipMode mode);

    int countByOwnerId(Long ownerId);
}