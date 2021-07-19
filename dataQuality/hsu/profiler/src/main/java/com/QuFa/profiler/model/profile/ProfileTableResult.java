package com.QuFa.profiler.model.profile;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProfileTableResult {
    String dataset_name;
    String dataset_type;
    int dataset_size;
    int dataset_column_cnt;
    int dataset_row_cnt;
    List<ProfileColumnResult> results;

    public ProfileTableResult(){ results = new ArrayList<>();
    }
}
