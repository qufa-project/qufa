package com.QuFa.profiler.model.request;

import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class Profiles {

    private Map<String, List<Object>> types;
    private boolean key_analysis;
    private List<DependencyAnalysis> dependency_analysis;
    private List<FKAnalysis> FK_analysis;
}
