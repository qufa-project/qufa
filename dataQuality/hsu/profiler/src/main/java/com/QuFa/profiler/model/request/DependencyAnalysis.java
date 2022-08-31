package com.QuFa.profiler.model.request;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DependencyAnalysis {
    private Object determinant;
    private Object dependency;
}
