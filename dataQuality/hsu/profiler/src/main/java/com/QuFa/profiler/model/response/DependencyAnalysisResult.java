package com.QuFa.profiler.model.response;

import java.util.List;
import lombok.Setter;

@Setter
public class DependencyAnalysisResult {
    Object determinant;
    Object dependency;
    Boolean is_valid;
    List<String> invalid_values;
}
