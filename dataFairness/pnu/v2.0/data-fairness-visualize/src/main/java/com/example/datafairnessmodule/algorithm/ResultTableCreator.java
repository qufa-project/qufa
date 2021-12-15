package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.example.datafairnessmodule.config.MainConfig.*;

public class ResultTableCreator {
    private static final Logger logger = LoggerFactory.getLogger(ResultTableCreator.class);
    private int caseIndex;
    private ArrayList<Integer> list;
    private Map<String, Object> paramMap;
    private MainRepository mainRepository;

    public ResultTableCreator(int caseIndex, ArrayList<Integer> list, Map<String, Object> paramMap, MainRepository mainRepository) {
        this.caseIndex = caseIndex;
        this.list = list;
        this.paramMap = paramMap;
        this.mainRepository = mainRepository;
    }

    public String create() {
        try {
            ArrayList<String> nonBinaryColumnList = (ArrayList<String>) paramMap.get("nonBinaryColumnList");
            ArrayList<String> executeColumnList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                executeColumnList.add(nonBinaryColumnList.get(list.get(i)));
            }
            String sourceTableName = (String) paramMap.get("tableName");
            String resultTableName = getResultTableName(sourceTableName);
            Map<String, Object> dbParamMap = new HashMap<>();
            String query = "CREATE TABLE IF NOT EXISTS " + resultTableName + " LIKE " + sourceTableName;
            dbParamMap.put("query", query);
            mainRepository.v2Step3SaveResult(dbParamMap);
            query = "TRUNCATE " + resultTableName;
            dbParamMap.put("query", query);
            mainRepository.v2Step3SaveResult(dbParamMap);
            query = "DELETE FROM " + StructureTableName + " WHERE tablename = '" + resultTableName + "'";
            dbParamMap.put("query", query);
            mainRepository.v2Step3SaveResult(dbParamMap);
            return resultTableName;
        } catch (Exception e) {
            return null;
        }
    }

    private String getResultTableName(String source) {
        String tid = source.replaceAll("[^0-9]", "");
        return PrefixResultTableName + tid + TailDelimiter + (caseIndex + 1);
    }

}
