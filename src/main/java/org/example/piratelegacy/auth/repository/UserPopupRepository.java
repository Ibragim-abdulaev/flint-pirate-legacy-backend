package org.example.piratelegacy.auth.repository;

import org.example.piratelegacy.auth.entity.UserPopup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPopupRepository extends JpaRepository<UserPopup, Long> {
    Optional<UserPopup> findByUserIdAndPopupType(Long userId, String popupType);
    boolean existsByUserIdAndPopupTypeAndIsShownTrue(Long userId, String popupType);
}