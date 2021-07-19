package com.QuFa.profiler.model.profile;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ProfileColumnResult {
    int column_id;
    String column_name;
    String column_type;
    Map<String,Object> profiles;

    public ProfileColumnResult() { profiles = new HashMap<>(); }
}
