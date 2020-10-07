package com.example.datafairnessmodule.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = "/module")
public class ModuleController {

    @GetMapping("/compensation")
    public String fairness(HttpServletRequest request) throws Exception {
        return "compensation.html";
    }

}
