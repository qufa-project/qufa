package kr.co.promptech.profiler.controller;

import kr.co.promptech.profiler.model.profile.ProfileTarget;
import kr.co.promptech.profiler.service.ProfileService;
import kr.co.promptech.profiler.service.meta.MetaColumnService;
import kr.co.promptech.profiler.service.profile.ProfileTargetService;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 개인 테스트용 REST API 컨트롤러
 */
@RestController
@RequestMapping(value = "/api")
public class TestController {

    private final MetaColumnService metaColumnService;
    private final ProfileService profileService;
    private final ProfileTargetService profileTargetService;

    @Builder
    @Autowired
    TestController(ProfileTargetService profileTargetService,
                   MetaColumnService metaColumnService,
                   ProfileService profileService) {

        this.profileService = profileService;
        this.metaColumnService = metaColumnService;
        this.profileTargetService = profileTargetService;
    }

    @GetMapping(value = "/test")
    public ResponseEntity<?> findAll() {

        List<?> testList = new ArrayList<>();
        try {
            // TODO : do something

            if (testList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(testList, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/test/{table-name}")
    public ResponseEntity<ProfileTarget> findAllResult(@PathVariable("table-name") String tableName) {

        try {
            // TODO : do something

            ProfileTarget profileTarget = null;
            return new ResponseEntity<>(profileTarget, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
