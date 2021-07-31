package com.QuFa.profiler.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class Local {
    private String path;
    private boolean header = false;
    private List<String> profiling;
    private Map<String, String> coltype;
}