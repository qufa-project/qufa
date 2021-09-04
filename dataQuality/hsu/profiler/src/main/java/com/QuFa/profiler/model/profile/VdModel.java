package com.QuFa.profiler.model.profile;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class VdModel {
    String type;
    Map<String, Object> value;
    Map<String, Object> range;
}
