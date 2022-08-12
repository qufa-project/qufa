package com.QuFa.profiler.controller.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    /* 400 BAD_REQUEST : 잘못된 요청 */
    BAD_JSON_REQUEST(HttpStatus.BAD_REQUEST,"400"),

    /* 404 NOT_FOUND : Resource 를 찾을 수 없음 */
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "404"),

    /* 411 LENGTH_REQUIRED : Request Data Malformed */
    REQUEST_DATA_MALFORMED(HttpStatus.LENGTH_REQUIRED,"411"),

    /* 500 SERVER_ERROR */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"500");

    private final HttpStatus httpStatus;
    private final String detail;

}
