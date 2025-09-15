package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.Hero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeroRepository extends JpaRepository<Hero, Long> {
    List<Hero> findByOwnerId(Long ownerId);
    List<Hero> findByOwnerIdAndLevelGreaterThan(Long ownerId, int level);
}