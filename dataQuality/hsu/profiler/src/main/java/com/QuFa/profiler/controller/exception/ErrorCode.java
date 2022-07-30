package com.QuFa.profiler.controller.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    /* 400 BAD_REQUEST : 잘못된 요청 */
    COLUMN_NAME_BAD_REQUEST(HttpStatus.BAD_REQUEST,"header가 true인 경우, 컬럼 이름(String)으로 요청 해야 합니다."),
    COLUMN_NUMBER_BAD_REQUEST(HttpStatus.BAD_REQUEST,"header가 false인 경우, 컬럼 번호(Integer)로 요청 해야 합니다."),

    /* 401 UNAUTHORIZED : 잘못된 요청 */

    /* 404 NOT_FOUND : Resource 를 찾을 수 없음 */
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "지정된 파일을 찾을 수 없습니다");

    /* 500 SERVER_ERROR */


    private final HttpStatus httpStatus;
    private final String detail;

}
