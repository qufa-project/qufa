package com.QuFa.profiler.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class Local {
    private Source source;
    private boolean header = false;
    private Map<String, List<Integer>> profiles;


    //private boolean header = false;
    //private List<String> profiling;
    //private Map<String, String> coltype;
}