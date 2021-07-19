package com.QuFa.profiler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인 테스트용 REST API 컨트롤러
 */
@RestController
@RequestMapping(value = "/")
public class HealthCheckController {

    @GetMapping(value = "/hc")
    public ResponseEntity findAll() {

        try {
            return new ResponseEntity<>("ok", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
