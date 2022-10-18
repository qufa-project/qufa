package com.QuFa.profiler.controller.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;

@Getter
@Setter
@Builder
public class ErrorResponse {

    private final String Code;
    private final String Content;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .Code(errorCode.getDetail())
                        .Content(errorCode.name())
                        .build()
                );
    }
}