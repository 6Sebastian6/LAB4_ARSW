package edu.eci.arsw.blueprints.dto;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> of(HttpStatus status, T data) {
        return new ApiResponse<>(status.value(), status.getReasonPhrase(), data);
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), message, null);
    }
}
