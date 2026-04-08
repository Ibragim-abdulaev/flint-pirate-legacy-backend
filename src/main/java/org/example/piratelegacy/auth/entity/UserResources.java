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
@Table(name = "user_resources")
public class UserResources implements Serializable {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Long gold = 0L;

    @Column(nullable = false)
    private Long wood = 0L;

    @Column(nullable = false)
    private Long stone = 0L;

    @Column(nullable = false)
    private Long crystals = 0L;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime updatedAt;
}
