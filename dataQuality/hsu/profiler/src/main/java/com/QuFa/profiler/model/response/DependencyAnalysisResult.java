package com.QuFa.profiler.model.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class DependencyAnalysisResult {
    Object determinant;
    Object dependency;
    Boolean is_valid;
    List<String> invalid_values;
}
