package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserResources;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserResourcesRepository extends JpaRepository<UserResources, Long> {
    Optional<UserResources> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserResources ur SET " +
            "ur.gold = ur.gold + :gold, " +
            "ur.wood = ur.wood + :wood, " +
            "ur.stone = ur.stone + :stone, " +
            "ur.crystals = ur.crystals + :crystals " +
            "WHERE ur.user.id = :userId")
    void addResources(
            @Param("userId") Long userId,
            @Param("gold") long gold,
            @Param("wood") long wood,
            @Param("stone") long stone,
            @Param("crystals") long crystals
    );
}