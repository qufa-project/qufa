package com.QuFa.profiler.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FKAnalysis {
    private Object foreign_key;
    private String referenced_file;
    // private Object referenced_db; // 연결 db 정보
    private Object referenced_column;
}