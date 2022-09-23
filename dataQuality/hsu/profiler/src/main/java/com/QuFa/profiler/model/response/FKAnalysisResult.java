package com.QuFa.profiler.model.response;

import java.util.List;
import lombok.Builder;

@Builder
public class FKAnalysisResult {
    String foreign_key;
    String referenced_table;
    String referenced_column;
    Boolean is_valid;
    List<String> invalid_values;

    @Override
    public String toString(){
        System.out.println("<FKAnalysisResult>");
        System.out.println("foreign_key = " + foreign_key);
        System.out.println("referenced_table = " + referenced_table);
        System.out.println("referenced_column = " + referenced_column);
        System.out.println("is_valid = " + is_valid);
        System.out.println("invalid_values = " + invalid_values);
        return null;
    }
}
