package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.datafairnessmodule.config.MainConfig.StructurePathDelimiter;
import static com.example.datafairnessmodule.config.MainConfig.StructureTableName;

public class DataStructureCreator {
    private static final Logger logger = LoggerFactory.getLogger(DataStructureCreator.class);
    private String resultTableName;
    private ArrayList<String> executeColumnList;
    private MainRepository mainRepository;

    public DataStructureCreator(String resultTableName, ArrayList<String> executeColumnList, MainRepository mainRepository) {
        this.resultTableName = resultTableName;
        this.executeColumnList = executeColumnList;
        this.mainRepository = mainRepository;
    }

    public void create() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("tableName", resultTableName);
        dataMap.put("columnList", executeColumnList);
        dataMap.put("columnIndex", 0);
        dataMap.put("whereClause", "");
        dataMap.put("dataPath", "");
        dataMap.put("parentId", 0L);
        try {
            save(dataMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save(Map<String, Object> dataMap) throws Exception {
        String tableName = (String) dataMap.get("tableName");
        ArrayList<String> columnList = (ArrayList<String>) dataMap.get("columnList");
        int columnIndex = (int) dataMap.get("columnIndex");
        String whereClause = (String) dataMap.get("whereClause");
        String dataPath = (String) dataMap.get("dataPath");
        int parentId = (int) (long) dataMap.get("parentId");
        int columnListSize = columnList.size();
        String queryBase = "SELECT `%s` AS name, COUNT(*) AS size FROM %s WHERE 1=1 %s GROUP BY `%s`";
        Map<String, Object> dbParamMap1 = new HashMap<>();
        if (columnIndex + 1 >= columnListSize) {
            dbParamMap1.put("query", String.format(queryBase, columnList.get(columnIndex), tableName, whereClause, columnList.get(columnIndex)));
            List<Map<String, Object>> dbResultMap = mainRepository.v2Step2SelectQuery(dbParamMap1);
            for (int i = 0; i < dbResultMap.size(); i++) {
                String nextPath = dataPath + columnList.get(columnIndex) + "=" + dbResultMap.get(i).get("name");
                String query = "INSERT INTO %s (%s) VALUES (%s)";
                String querySub1 = "tablename, dataPath, count";
                String querySub2 = "'" + tableName + "', '" + nextPath + "', '" + dbResultMap.get(i).get("size") + "'";
                Map<String, Object> dbRequestMap = new HashMap<>();
                dbRequestMap.put("query", String.format(query, StructureTableName, querySub1, querySub2));
                mainRepository.v2Step2InsertQuery(dbRequestMap);
            }
            return;
        }
        dbParamMap1.put("query", String.format(queryBase, columnList.get(columnIndex), tableName, whereClause, columnList.get(columnIndex)));
        List<Map<String, Object>> dbResultMap = mainRepository.v2Step2SelectQuery(dbParamMap1);
        for (int i = 0; i < dbResultMap.size(); i++) {
            String newWhere = whereClause + " AND `" + columnList.get(columnIndex) + "` = '" + dbResultMap.get(i).get("name") + "'";
            String nextPath = dataPath + columnList.get(columnIndex) + "=" + dbResultMap.get(i).get("name") + StructurePathDelimiter;
            dataMap.put("columnIndex", columnIndex + 1);
            dataMap.put("whereClause", newWhere);
            dataMap.put("dataPath", nextPath);
            dataMap.put("parentId", (parentId > 0 ? dbResultMap.get(i).get("id") : 0L));
            save(dataMap);
        }
    }

}
