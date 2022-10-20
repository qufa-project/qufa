package com.QuFa.profiler.controller;

import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.response.ProfileTableResult;
import com.QuFa.profiler.service.DataStoreService;
import com.QuFa.profiler.service.FileService;
import com.QuFa.profiler.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final FileService fileService;

    @PostMapping("/local")
    public ResponseEntity<ProfileTableResult> localProfile(@RequestBody Local local) {

        System.out.println("Local Profiling");
        System.out.println("===================");

        // url이 로컬파일의 url인지 외부 url인지 구분
        String url = local.getSource().getUrl();
        String type = url.substring(0, 4);
        if (type.equals("file")) {
            local.getSource().setType("path");
            String path = fileService.seperate_file(url);
            local.getSource().setPath(path);
        } else {
            local.getSource().setType("url");
        }

        System.out.println("type: " + local.getSource().getType());
        System.out.println("url: " + local.getSource().getUrl());
        System.out.println("header: " + local.isHeader());
        System.out.println("===================");

        return new ResponseEntity<>(profileService.profileLocalCSV(local), HttpStatus.OK);

    }
}