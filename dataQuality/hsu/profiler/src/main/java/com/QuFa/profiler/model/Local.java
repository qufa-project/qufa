package com.QuFa.profiler.model;

import com.QuFa.profiler.model.request.Profiles;
import lombok.Getter;

@Getter
public class Local {

    private Source source;
    private boolean header = true;
    private Profiles profiles;

    //private boolean header = false;
    //private List<String> profiling;
    //private Map<String, String> coltype;
}