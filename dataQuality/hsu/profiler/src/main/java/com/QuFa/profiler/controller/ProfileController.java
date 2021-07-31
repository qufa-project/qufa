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

//    @PostMapping("/server")
//    public ResponseEntity<ProfileTableResult> serverProfile(
//            @RequestParam("targetfile") MultipartFile file,
//            @RequestParam(value = "profiling", defaultValue = "all") String profiling,
//            @RequestParam(value = "header", defaultValue = "no") String header,
//            @RequestParam(value = "coltype", defaultValue = "default") String coltype) {
//        System.out.println("Server Profiling");
//        return new ResponseEntity<>(profileService.profileCSV(file), HttpStatus.OK);
//    }

    @PostMapping("/local")
    public ResponseEntity<ProfileTableResult> localProfile(
            @RequestBody Local local) {
        System.out.println("Local Profiling");
        System.out.println("===================");
        System.out.println("===================");

        System.out.println(local.getPath());
        System.out.println(local.isHeader());
        System.out.println(local.getColtype());
        System.out.println(local.getProfiling());

        return new ResponseEntity<>(profileService.profileLocalCSV(local.getPath()), HttpStatus.OK);
    }
}