package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnitSummaryDto implements Serializable {
    private Long id;
    private String name;
    private int level;
    private String portraitImageUrl;
    private boolean isAlive;
    // Только для героя, для юнитов будет null
    private LocalDateTime recoveryEndsAt;
}