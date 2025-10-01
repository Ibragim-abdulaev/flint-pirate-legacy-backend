package org.example.piratelegacy.auth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.entity.User;
import org.example.piratelegacy.auth.entity.UserPopup;
import org.example.piratelegacy.auth.repository.UserPopupRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j // <-- ДОБАВЛЕНО для логирования
@Service
@RequiredArgsConstructor
public class PopupService {

    private final UserPopupRepository userPopupRepository;

    @Getter
    public enum PopupType {
        WELCOME("WELCOME"),
        FIRST_QUEST("FIRST_QUEST");

        private final String value;

        PopupType(String value) {
            this.value = value;
        }

    }

    @Transactional(readOnly = true)
    @Cacheable(value = "popups", key = "#user.id + ':' + #popupType.name()")
    public boolean shouldShowPopup(User user, PopupType popupType) {
        log.info("ПРОВЕРКА СТАТУСА: Пользователь ID [{}], тип попапа [{}].", user.getId(), popupType.getValue());
        boolean shown = userPopupRepository.existsByUserIdAndPopupTypeAndIsShownTrue(user.getId(), popupType.getValue());
        log.info("--> РЕЗУЛЬТАТ: Попап уже был показан? {}. Значит, нужно ли показать сейчас? {}", shown, !shown);
        return !shown;
    }

    @Transactional
    @CacheEvict(value = "popups", key = "#user.id + ':' + #popupType.name()")
    public void markPopupAsShown(User user, PopupType popupType) {
        log.info("ПОПЫТКА ОТМЕТИТЬ ПОКАЗАННЫМ: Пользователь ID [{}], тип попапа [{}].", user.getId(), popupType.getValue());

        Optional<UserPopup> existingPopup = userPopupRepository.findByUserIdAndPopupType(user.getId(), popupType.getValue());

        if (existingPopup.isPresent()) {
            UserPopup popup = existingPopup.get();
            if (popup.isShown()) {
                log.warn("--> ВНИМАНИЕ: Попап [{}] для пользователя ID [{}] уже был отмечен как показанный. Новых изменений не будет.", popupType.getValue(), user.getId());
                return;
            }
            log.info("--> Найден существующий попап. Обновляем isShown на true.");
            popup.setShown(true);
            popup.setShownAt(LocalDateTime.now());
            userPopupRepository.save(popup);
            log.info("--> УСПЕШНО СОХРАНЕНО: Попап [{}] для пользователя ID [{}] отмечен как показанный.", popupType.getValue(), user.getId());
        } else {
            log.warn("--> ВНИМАНИЕ: Для пользователя ID [{}] не найдена запись о попапе [{}]. Создаем новую запись и сразу отмечаем ее показанной.", user.getId(), popupType.getValue());
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

    @Transactional
    public void initializePopupsForUser(User user) {
        log.info("Инициализация попапов для нового пользователя ID [{}].", user.getId());
        for (PopupType popupType : PopupType.values()) {
            boolean exists = userPopupRepository.findByUserIdAndPopupType(user.getId(), popupType.getValue()).isPresent();
            if (!exists) {
                UserPopup popup = UserPopup.builder()
                        .user(user)
                        .popupType(popupType.getValue())
                        .isShown(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                userPopupRepository.save(popup);
                log.info("--> Создана запись для попапа [{}] для пользователя ID [{}].", popupType.getValue(), user.getId());
            }
        }
    }
}