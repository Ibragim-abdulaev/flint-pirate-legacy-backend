package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BattleLocationDto {
    private String locationImageId;  // идентификатор изображения локации
    private List<BattlePirateDto> pirates;
}
