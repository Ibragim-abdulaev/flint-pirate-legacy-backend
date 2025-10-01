package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DialogueLineDto implements Serializable {
    private String characterName; // Имя персонажа (Боцман, Шкипер)
    private String characterImageUrl; // URL его аватара
    private String line;          // Текст реплики
}