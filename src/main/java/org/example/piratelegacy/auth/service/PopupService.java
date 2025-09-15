package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserPopup;
import org.example.piratelegacy.auth.repository.UserPopupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PopupService {

    private final UserPopupRepository userPopupRepository;

    public enum PopupType {
        WELCOME("WELCOME"),           // Приветственный попап со Старым Флинтом
        FIRST_QUEST("FIRST_QUEST");   // Первый квест (отбить трактирщика)

        private final String value;

        PopupType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Проверяет, нужно ли показать указанный попап пользователю
     */
    @Transactional(readOnly = true)
    public boolean shouldShowPopup(User user, PopupType popupType) {
        return !userPopupRepository.existsByUserIdAndPopupTypeAndIsShownTrue(user.getId(), popupType.getValue());
    }

    /**
     * Отмечает попап как показанный
     */
    @Transactional
    public void markPopupAsShown(User user, PopupType popupType) {
        Optional<UserPopup> existingPopup = userPopupRepository.findByUserIdAndPopupType(user.getId(), popupType.getValue());

        if (existingPopup.isPresent()) {
            UserPopup popup = existingPopup.get();
            popup.setShown(true);
            popup.setShownAt(LocalDateTime.now());
            userPopupRepository.save(popup);
        } else {
            UserPopup newPopup = UserPopup.builder()
                    .user(user)
                    .popupType(popupType.getValue())
                    .isShown(true)
                    .shownAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            userPopupRepository.save(newPopup);
        }
    }

    /**
     * Инициализирует записи попапов для нового пользователя (не показанными)
     */
    @Transactional
    public void initializePopupsForUser(User user) {
        for (PopupType popupType : PopupType.values()) {
            Optional<UserPopup> existingPopup = userPopupRepository.findByUserIdAndPopupType(user.getId(), popupType.getValue());

            if (!existingPopup.isPresent()) {
                UserPopup popup = UserPopup.builder()
                        .user(user)
                        .popupType(popupType.getValue())
                        .isShown(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                userPopupRepository.save(popup);
            }
        }
    }
}

