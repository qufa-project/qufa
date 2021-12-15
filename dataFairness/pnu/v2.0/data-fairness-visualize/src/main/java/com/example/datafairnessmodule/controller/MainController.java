package com.example.datafairnessmodule.controller;

import com.example.datafairnessmodule.config.MainConfig;
import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private MainConfig mainConfig = new MainConfig();

    @Autowired
    private MainRepository mainRepository;

    @RequestMapping(value = "/getNow", method = RequestMethod.GET)
    @ResponseBody
    public String getNow(HttpServletRequest request) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time = new Date();
        String time1 = format.format(time);
        return time1;
    }

    @RequestMapping(value = "/getNow2", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getNow2(HttpServletRequest request) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        String getNow = mainRepository.getNow2(paramMap);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("now", getNow);
        return resultMap;
    }

}
