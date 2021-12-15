package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.example.datafairnessmodule.config.MainConfig.*;

public class ResultDataCreator {
    private static final Logger logger = LoggerFactory.getLogger(ResultDataCreator.class);
    private int caseIndex;
    private ArrayList<Integer> list;
    private Map<String, Object> paramMap;
    private MainRepository mainRepository;

    public ResultDataCreator(int caseIndex, ArrayList<Integer> list, Map<String, Object> paramMap, MainRepository mainRepository) {
        this.caseIndex = caseIndex;
        this.list = list;
        this.paramMap = paramMap;
        this.mainRepository = mainRepository;
    }

    public void create() {
        Map<String, Object> dataMap = new HashMap<>();
        for (String key : paramMap.keySet()) {
            Object object = paramMap.get(key);
            dataMap.put(key, object);
        }
        ArrayList<String> nonBinaryColumnList = (ArrayList<String>) paramMap.get("nonBinaryColumnList");
        ArrayList<String> executeColumnList = new ArrayList<>();
        executeColumnList.add((String) paramMap.get("binaryColumn"));
        for (int i = 0; i < list.size(); i++) {
            executeColumnList.add(nonBinaryColumnList.get(list.get(i)));
        }
        dataMap.put("executeColumnList", executeColumnList);
        if (TurnOnConsoleLog) {
            logger.info(ImproveLogPrefix(caseIndex, 0) + "caseIndex: " + caseIndex);
            logger.info(ImproveLogPrefix(caseIndex, 0) + "list: " + list);
            logger.info(ImproveLogPrefix(caseIndex, 0) + "executeColumnList: " + executeColumnList);
        }
        try {
            collector(dataMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void collector(Map<String, Object> dataMap) throws Exception {
        int columnIdx = (int) dataMap.get("columnIndex");
        int avgLoopLimit = (int) dataMap.get("avgLoopLimit");
        String tableName = (String) dataMap.get("tableName");
        String whereClause = (String) dataMap.get("whereClause");
        String dataPath = (dataMap.get("dataPath") == null ? "" : (String) dataMap.get("dataPath"));
        ArrayList<String> executeColumnList = (ArrayList<String>) dataMap.get("executeColumnList");
        Map<String, Object> prevMap = (Map<String, Object>) dataMap.get("previousMap");
        if (columnIdx >= executeColumnList.size()) {
            save(dataMap);
            return;
        }
        String prefix = ImproveLogPrefix(caseIndex, columnIdx);
        String queryBase = "SELECT CONCAT('%s') AS column_name, `%s` AS column_value, count(*) AS actual_count " +
                "FROM %s WHERE 1=1 %s GROUP BY `%s`";
        String columnName = executeColumnList.get(columnIdx);
        Map<String, Object> dbParamMap = new HashMap<>();
        dbParamMap.put("query", String.format(queryBase, columnName, columnName, tableName, whereClause, columnName));
        List<Map<String, Object>> dbResultMap = mainRepository.v2Step3ColumnCategory(dbParamMap);
        // 실제
        int[] actualCountArr = new int[dbResultMap.size()];
        int actualCountSum = 0;
        for (int i = 0; i < dbResultMap.size(); i++) {
            actualCountArr[i] = (int) (long) dbResultMap.get(i).get("actual_count");
            actualCountSum += actualCountArr[i];
        }
        int actualCountAvg = actualCountSum > 0 ? Math.round(1.0f * actualCountSum / actualCountArr.length) : 0;
        // 비율보정
        int[] resizeCountArr = new int[actualCountArr.length];
        int resizeCountSum = 0;
        for (int i = 0; i < actualCountArr.length; i++) {
            resizeCountArr[i] = prevMap == null ? actualCountArr[i] :
                    Math.round(1.0f * actualCountArr[i] * (int) prevMap.get("result_count") / (int) (long) prevMap.get("actual_count"));
            resizeCountSum += resizeCountArr[i];
            dbResultMap.get(i).put("resize_count", resizeCountArr[i]);
        }
        int resizeCountAvg = resizeCountSum > 0 ? Math.round(1.0f * resizeCountSum / resizeCountArr.length) : 0;
        // 평균보정
        int[] resultCountArr = new int[resizeCountArr.length];
        int resultCountSum = 0;
        int resultCountAvg = 0;
        for (int i = 0; i < resizeCountArr.length; i++) {
            resultCountArr[i] = resizeCountArr[i];
            resultCountSum += resizeCountArr[i];
            dbResultMap.get(i).put("result_count", resultCountArr[i]);
        }
        resultCountAvg = resultCountSum > 0 ? Math.round(1.0f * resultCountSum / resultCountArr.length) : 0;
        for (int i = 0; i < avgLoopLimit; i++) {
            String loopLog = "평균보정." + i + ": " + Arrays.toString(resultCountArr) + " -> ";
            resultCountSum = 0;
            for (int j = 0; j < resultCountArr.length; j++) {
                resultCountArr[j] = Math.min(resultCountArr[j], resultCountAvg);
                resultCountSum += resultCountArr[j];
                dbResultMap.get(j).put("result_count", resultCountArr[j]);
            }
            resultCountAvg = resultCountSum > 0 ? Math.round(1.0f * resultCountSum / resultCountArr.length) : 0;
            loopLog += Arrays.toString(resultCountArr);
            if (TurnOnConsoleLog) {
                logger.info(prefix + loopLog);
            }
        }
        if (TurnOnConsoleLog) {
            logger.info(prefix + dbParamMap.get("query"));
            logger.info(prefix + "실제(actualCountArr): " + Arrays.toString(actualCountArr));
            logger.info(prefix + "실제(actualCountSum): " + actualCountSum);
            logger.info(prefix + "실제(actualCountAvg): " + actualCountAvg);
            logger.info(prefix + "비율보정(resizeCountArr): " + Arrays.toString(resizeCountArr));
            logger.info(prefix + "비율보정(resizeCountSum): " + resizeCountSum);
            logger.info(prefix + "비율보정(resizeCountAvg): " + resizeCountAvg);
            logger.info(prefix + "평균보정(resultCountArr): " + Arrays.toString(resultCountArr));
            logger.info(prefix + "평균보정(resultCountSum): " + resultCountSum);
            logger.info(prefix + "평균보정(resultCountAvg): " + resultCountAvg);
        }
        for (int i = 0; i < dbResultMap.size(); i++) {
            if (TurnOnConsoleLog) {
                logger.info(prefix + "\t" + columnIdx + "." + i + ": " + dbResultMap.get(i));
            }
            String columnValue = (String) dbResultMap.get(i).get("column_value");
            String nextWhere = whereClause + "AND `" + columnName + "` = '" + columnValue + "' ";
            String nextPath = dataPath + columnName + "=" + columnValue + StructurePathDelimiter;
            dataMap.put("whereClause", nextWhere);
            dataMap.put("dataPath", nextPath);
            dataMap.put("columnIndex", columnIdx + 1);
            dataMap.put("previousMap", dbResultMap.get(i));
            collector(dataMap);
        }
    }

    private void save(Map<String, Object> dataMap) throws Exception {
        boolean saveResult;
        String prefix = ImproveLogPrefix(caseIndex, (int) dataMap.get("columnIndex"));
        try {
            Map<String, Object> dbParamMap = new HashMap<>();
            String sourceTableName = (String) dataMap.get("tableName");
            String resultTableName = getResultTableName(sourceTableName);
            String whereClause = (String) dataMap.get("whereClause");
            String dataPath = (String) dataMap.get("dataPath");
            ArrayList<String> executeColumnList = (ArrayList<String>) dataMap.get("executeColumnList");
            Map<String, Object> previousMap = (Map<String, Object>) dataMap.get("previousMap");
            String limitClause = Integer.toString((int) previousMap.get("result_count"));
            String query1 = "INSERT INTO " + resultTableName +
                    " (SELECT * FROM " + sourceTableName + " WHERE 1=1 " + whereClause + " LIMIT " + limitClause + ")";
            if (TurnOnConsoleLog) {
                logger.info(prefix + query1);
            }
            dbParamMap.put("query", query1);
            mainRepository.v2Step3SaveResult(dbParamMap);
            String query2 = "INSERT INTO %s (%s) VALUES (%s)";
            String query2Sub1 = "tablename, columns, dataPath, count";
            String query2Sub2 = "'" + resultTableName + "', '" + String.join(StructurePathDelimiter, executeColumnList) + "', '" + dataPath.substring(0, dataPath.length() - 1) + "', '" + limitClause + "'";
            dbParamMap.put("query", String.format(query2, StructureTableName, query2Sub1, query2Sub2));
            mainRepository.v2Step3SaveResult(dbParamMap);
            saveResult = true;
        } catch (Exception e) {
            e.printStackTrace();
            saveResult = false;
        }
        if (TurnOnConsoleLog) {
            logger.info(prefix + "Result: " + (saveResult ? "Success" : "Fail"));
        }
    }

    private String getResultTableName(String source) {
        String tid = source.replaceAll("[^0-9]", "");
        return PrefixResultTableName + tid + TailDelimiter + (caseIndex + 1);
    }

}
