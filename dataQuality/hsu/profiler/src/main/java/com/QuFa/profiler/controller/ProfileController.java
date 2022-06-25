package com.QuFa.profiler.controller;

import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.profile.ProfileTableResult;
import com.QuFa.profiler.service.DataStoreService;
import com.QuFa.profiler.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ProfileTableResult> localProfile(
            @RequestBody Local local) {
        System.out.println("Local Profiling");
        System.out.println("===================");
        System.out.println("===================");


        System.out.println(local.getSource().getType());
        System.out.println(local.getSource().getPath());
        System.out.println(local.getSource().getUrl());
        System.out.println(local.isHeader());

        String url = local.getSource().getUrl();
        String type = url.substring(0, 4);
        if (type.equals("file")) {
            local.getSource().setType("path");
            local.getSource().setPath( url.substring(8).replace('/','\\'));
        }

        System.out.println(local.getSource().getType());
        System.out.println(local.getSource().getPath());
        System.out.println(local.getSource().getUrl());

        return new ResponseEntity<>(profileService.profileLocalCSV(local), HttpStatus.OK);
    }
}