package com.QuFa.profiler.model.response;

import java.util.List;

public class FKAnalysisResult {
    List<Object> foreign_key;
    String referenced_table;
    String referenced_column;
    Boolean is_valid;
    List<Integer> invalid_values;
}
