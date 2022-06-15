package com.QuFa.profiler.model;

import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class Local {

    private Source source;
    private boolean header = true;
    private Map<String, List<Integer>> profiles;

    //private boolean header = false;
    //private List<String> profiling;
    //private Map<String, String> coltype;
}