package com.QuFa.profiler.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProfileTableResult {
    String dataset_name;
    String dataset_type;
    int dataset_size;
    int dataset_column_cnt;
    int dataset_row_cnt;

    List<ProfileColumnResult> single_column_results;

    @JsonInclude(Include.NON_EMPTY)
    List<Object> key_analysis_results;

    @JsonInclude(Include.NON_EMPTY)
    List<DependencyAnalysisResult> dependency_analysis_results;

    @JsonInclude(Include.NON_EMPTY)
    List<FKAnalysisResult> fk_analysis_results;
}
