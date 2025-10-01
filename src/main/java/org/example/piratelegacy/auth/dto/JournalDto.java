package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor // <-- УБЕДИТЕСЬ, ЧТО ЭТА АННОТАЦИЯ ЕСТЬ
@NoArgsConstructor
@Builder
public class JournalDto implements Serializable {
    private List<QuestChainStatusDto> storylineChains;
    private List<QuestChainStatusDto> adventureChains;
}