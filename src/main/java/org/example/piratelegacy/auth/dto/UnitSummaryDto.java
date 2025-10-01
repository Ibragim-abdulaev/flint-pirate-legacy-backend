package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor // <-- УБЕДИТЕСЬ, ЧТО ЭТА АННОТАЦИЯ ЕСТЬ
@NoArgsConstructor
@Builder
public class UnitSummaryDto implements Serializable {
    private Long id;
    private String name;
    private int level;
    private String portraitImageUrl;
}