package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.datafairnessmodule.config.MainConfig.*;

public class Improvement {
    private static final Logger logger = LoggerFactory.getLogger(Improvement.class);
    private int caseIndex;
    private ArrayList<Integer> list;
    private Map<String, Object> paramMap;
    private Map<String, Object> resultMap;
    private MainRepository mainRepository;

    public Improvement(int caseIndex, ArrayList<Integer> list, Map<String, Object> paramMap, MainRepository mainRepository) {
        this.caseIndex = caseIndex;
        this.list = list;
        this.paramMap = paramMap;
        this.mainRepository = mainRepository;
        resultMap = new HashMap<>();
    }

    public Map<String, Object> getMap() {
        return save();
    }

    public ArrayList<Integer> getList() {
        return list;
    }

    private Map<String, Object> save() {
        ArrayList<String> nonBinaryColumnList = (ArrayList<String>) paramMap.get("nonBinaryColumnList");
        ArrayList<String> executeColumnList = new ArrayList<>();
        executeColumnList.add((String) paramMap.get("binaryColumn"));
        for (int i = 0; i < list.size(); i++) {
            executeColumnList.add(nonBinaryColumnList.get(list.get(i)));
        }
        if (TurnOnConsoleLog) {
            logger.info(ImproveLogPrefix(caseIndex, 0) + list + " " + executeColumnList + "\t(" + Thread.currentThread().getName() + ")");
        }
        ResultTableCreator resultTableCreator = new ResultTableCreator(caseIndex, list, paramMap, mainRepository);
        String resultTableName = resultTableCreator.create();
        ResultDataCreator resultDataCreator = new ResultDataCreator(caseIndex, list, paramMap, mainRepository);
        resultDataCreator.create();
        CsvModule csvModule = new CsvModule();
        Map<String, Object> csvResult = csvModule.export(resultTableName, (ArrayList<String>) paramMap.get("header"), mainRepository);
        resultMap.put("caseIndex", caseIndex);
        resultMap.put("columnList", paramMap.get("columnList"));
        resultMap.put("executeColumnList", executeColumnList);
        resultMap.put("tableName", resultTableName);
        resultMap.put("csv", csvResult);
        /*
        delay();
         */
        return resultMap;
    }

    private static void delay() {
        int delay = 1000;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> sdColumn(String tablename, List<String> columns) throws Exception {
        String selectQuery = "SELECT * FROM (";
        for (String column : columns) {
            selectQuery += "( " +
                    "SELECT CONCAT('" + column + "') AS col, STDDEV(cnt) AS stddev FROM " +
                    "(SELECT `" + column + "` AS col, COUNT(*) AS cnt FROM " + tablename + " GROUP BY `" + column + "`) AS A " +
                    ") " +
                    "UNION ";
        }
        selectQuery = selectQuery.substring(0, selectQuery.length() - 6);
        selectQuery += ") AS B ";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("query", selectQuery);
        List<Map<String, Object>> resultList = mainRepository.v2Step2OrderByStdDev(paramMap);
        Map<String, Object> resultMap = new HashMap<>();
        for (Map<String, Object> map1 : resultList) {
            resultMap.put((String) map1.get("col"), map1.get("stddev"));
        }
        return resultMap;
    }

    public Map<String, Object> orderColumn(String tablename) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("tablename", tablename);
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> metaMap = mainRepository.v2Step2OrderStdDev(paramMap);
        Map<String, Object> map1 = metaMap.get(0);
        Map<String, Object> columnDataMap = new HashMap<>();
        for (int i = 0; i <= EndStdDevNumber; i++) {
            String column = (String) map1.get("order" + i);
            if (column == null || column.equalsIgnoreCase("null") || column.equals("")) {
                continue;
            }
            String query = "SELECT `" + column + "` AS column_value, COUNT(*) AS count " +
                    "FROM " + tablename + " GROUP BY `" + column + "` ORDER BY count ASC";
            paramMap.put("query", query);
            List<Map<String, Object>> orderMap = mainRepository.v2Step2OrderColumn(paramMap);
            columnDataMap.put(column, orderMap);
        }
        resultMap.put(MetaTableName, metaMap);
        resultMap.put("columnData", columnDataMap);
        return resultMap;
    }

}
