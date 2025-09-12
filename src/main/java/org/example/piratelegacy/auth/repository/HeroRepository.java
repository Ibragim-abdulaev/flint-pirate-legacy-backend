package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Pirate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PirateRepository extends JpaRepository<Pirate, Long> {
    List<Pirate> findByOwnerId(Long ownerId);

    // Найти пиратов выше определённого уровня
    List<Pirate> findByOwnerIdAndLevelGreaterThan(Long ownerId, int level);

}