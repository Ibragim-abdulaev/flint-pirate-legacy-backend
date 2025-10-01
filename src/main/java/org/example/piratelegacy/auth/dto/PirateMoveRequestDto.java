package org.example.piratelegacy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PirateMoveRequestDto implements Serializable {
    private String pirateId;
    private int targetQ;
    private int targetR;
}