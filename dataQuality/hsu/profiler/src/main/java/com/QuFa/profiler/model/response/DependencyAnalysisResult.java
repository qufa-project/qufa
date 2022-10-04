package com.QuFa.profiler.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
    @JsonInclude(Include.NON_EMPTY)
    List<String> invalid_values;
}
