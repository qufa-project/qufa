package com.example.datafairnessmodule.repository;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface MainRepository {
    String getNow2(Map map);

    int v2CreateTable(Map map);

    int v2InsertCsv(Map map);

    List<String> v2SelectTempTable(Map map);

    int v2DropTempTable(Map map);

    int v2DeleteDataMeta(Map map);

    int v2TrimColumnData(Map map);

    List<Map<String, Object>> v2Step2OrderByStdDev(Map map);

    int v2Step2InsertDataMeta(Map map);

    List<Map<String, Object>> v2Step2OrderStdDev(Map map);

    List<Map<String, Object>> v2Step2OrderColumn(Map map);

    List<Map<String, Object>> v2Step2TestNonNumericColumn(Map map);

    List<Map<String, Object>> v2Step3ColumnCategory(Map map);

    int v2Step3SaveResult(Map map);

    List<Map<String, Object>> v2Step3ExportResult(Map map);

    Map<String, Object> v2Step2SelectRows(Map map);

    Map<String, Object> v2Step1ColumnCategoryCount(Map map);

    List<Map<String, Object>> v2Step2SelectChartData(Map map);

    List<Map<String, Object>> v2Step2SelectQuery(Map map);

    int v2Step2InsertQuery(Map map);

}
