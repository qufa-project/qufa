package com.QuFa.profiler.controller;

import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.response.ProfileTableResult;
import com.QuFa.profiler.service.DataStoreService;
import com.QuFa.profiler.service.ProfileService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final DataStoreService dataStoreService;

    @Autowired
    public ProfileController(ProfileService profileService, DataStoreService dataStoreService) {
        this.profileService = profileService;
        this.dataStoreService = dataStoreService;
    }

    @PostMapping("/local")
    public ResponseEntity<ProfileTableResult> localProfile(@RequestBody Local local) {

        System.out.println("Local Profiling");
        System.out.println("===================");

        String url = local.getSource().getUrl();
        String type = url.substring(0, 4);
        if (type.equals("file")) {
            local.getSource().setType("path");
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win"))
                local.getSource().setPath( url.substring(8).replace('/','\\'));
            else
                local.getSource().setPath( url.substring(7));
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