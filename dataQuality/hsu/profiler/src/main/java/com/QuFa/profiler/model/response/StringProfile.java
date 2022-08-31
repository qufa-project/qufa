package com.QuFa.profiler.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StringProfile {
    int blank_cnt;
    int min_len;
    int max_len;
    double avg_len;
}
