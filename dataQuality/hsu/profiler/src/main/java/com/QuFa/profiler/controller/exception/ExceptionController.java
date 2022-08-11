package com.QuFa.profiler.controller.exception;

import static com.QuFa.profiler.controller.exception.ErrorCode.FILE_NOT_FOUND;

import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(value = {FileNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleFileNotFoundException() {
        log.error("handleFileNotFoundException throw FileNotFoundException : {}",
                FILE_NOT_FOUND);
        return ErrorResponse.toResponseEntity(FILE_NOT_FOUND);
    }


    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException throw CustomException : {}", e.getErrorCode());
        return ErrorResponse.toResponseEntity(e.getErrorCode());
    }

}
