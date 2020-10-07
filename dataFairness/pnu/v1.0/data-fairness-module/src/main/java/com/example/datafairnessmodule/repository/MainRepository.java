package com.example.datafairnessmodule.repository;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Mapper
@Repository
public interface MainRepository {
    String getNow2(Map map);
}
