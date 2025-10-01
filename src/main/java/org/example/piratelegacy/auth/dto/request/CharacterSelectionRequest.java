package org.example.piratelegacy.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharacterSelectionRequest implements Serializable {
    @NotBlank(message = "Имя персонажа обязательно")
    @Size(min = 2, max = 50, message = "Имя персонажа должно быть от 2 до 50 символов")
    private String name;

    @NotBlank(message = "Тип персонажа обязателен")
    private String characterType;
}