package org.example.piratelegacy.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.piratelegacy.auth.entity.User;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class UserProfileResponse implements Serializable {
    private final Long id;
    private final String username;
    private final String email;
    private final boolean hasCharacter;

    public UserProfileResponse(User user, boolean hasCharacter) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.hasCharacter = hasCharacter;
    }
}