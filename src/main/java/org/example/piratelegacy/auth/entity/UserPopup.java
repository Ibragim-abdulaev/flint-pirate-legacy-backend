package org.example.piratelegacy.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_popups")
public class UserPopup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "popup_type", nullable = false)
    private String popupType; // "WELCOME", "FIRST_QUEST", и т.д.

    @Column(name = "is_shown", nullable = false)
    private boolean isShown = false;

    @Column(name = "shown_at")
    private LocalDateTime shownAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}