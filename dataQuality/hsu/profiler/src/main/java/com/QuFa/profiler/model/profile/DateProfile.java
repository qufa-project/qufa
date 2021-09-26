package com.QuFa.profiler.model.profile;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DateProfile {
    String highest_date;
    String lowest_date;
    String mean_date;
    String median_date;
    String percentile_25th;
    String percentile_75th;
    List<Map<Object, Object>> month_distribution;
    List<Map<Object, Object>> year_distribution;
}