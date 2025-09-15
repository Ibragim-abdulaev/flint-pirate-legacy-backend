package org.example.piratelegacy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CharacterSelectionRequest {
    @NotBlank(message = "Имя персонажа обязательно")
    @Size(min = 2, max = 50, message = "Имя персонажа должно быть от 2 до 50 символов")
    private String name;

    @NotBlank(message = "Тип персонажа обязателен")
    private String characterType; // BARBARIAN, ARCHER, VALKYRIE, и т.д.
}
