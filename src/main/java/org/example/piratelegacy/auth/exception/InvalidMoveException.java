package org.example.piratelegacy.auth.exception; // Укажи свой пакет

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST) // Эта аннотация говорит Spring, что при выбросе этого исключения нужно возвращать статус 400
public class InvalidMoveException extends RuntimeException {
    public InvalidMoveException(String message) {
        super(message);
    }
}