package com.QuFa.profiler.controller;

import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.profile.ProfileTableResult;
import com.QuFa.profiler.service.DataStoreService;
import com.QuFa.profiler.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final DataStoreService dataStoreService;

    @Autowired
    public ProfileController(ProfileService profileService, DataStoreService dataStoreService){
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
        for(String key : local.getProfiles().keySet())
            System.out.println(key + local.getProfiles().get(key));

        return new ResponseEntity<>(profileService.profileLocalCSV(local), HttpStatus.OK);
    }
}