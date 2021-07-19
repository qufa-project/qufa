package com.QuFa.profiler.model.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NumberProfile {
    double min;
    double max;
    double sum;
    double mean;
    double median;
    double sd;
    double variance;
    double percentile_25th;
    double percentile_75th;
    int zero_cnt;
}
