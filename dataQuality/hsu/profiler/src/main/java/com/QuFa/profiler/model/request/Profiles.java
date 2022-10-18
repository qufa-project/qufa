package com.QuFa.profiler.model.request;

import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class Profiles {

    private Map<String, List<Object>> column_analysis;
    private boolean key_analysis;
    private List<DependencyAnalysis> dependencied_analysis;
    private List<FKAnalysis> fk_analysis;

}
