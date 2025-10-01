package org.example.piratelegacy.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}