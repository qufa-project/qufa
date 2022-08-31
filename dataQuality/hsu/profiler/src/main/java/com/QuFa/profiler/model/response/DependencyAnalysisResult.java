package com.QuFa.profiler.model.response;

import java.util.List;

public class DependencyAnalysisResult {
    Object determinant;
    Object dependency;
    Boolean is_valid;
    List<Integer> invalid_values;
}
