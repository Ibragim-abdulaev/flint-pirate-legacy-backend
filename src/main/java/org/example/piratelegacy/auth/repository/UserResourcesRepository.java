package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserResources;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserResourcesRepository extends JpaRepository<UserResources, Long> {
    Optional<UserResources> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserResources ur SET ur.gold = ur.gold + :gold, ur.wood = ur.wood + :wood, ur.stone = ur.stone + :stone WHERE ur.user.id = :userId")
    void addResources(Long userId, Long gold, Long wood, Long stone);

}