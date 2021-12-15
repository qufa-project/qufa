package com.example.datafairnessmodule.controller;

import com.example.datafairnessmodule.algorithm.ImproveExecutor;
import com.example.datafairnessmodule.algorithm.Permutation;
import com.example.datafairnessmodule.algorithm.ResultChartCreator;
import com.example.datafairnessmodule.property.FileStorageProperties;
import com.example.datafairnessmodule.repository.MainRepository;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static com.example.datafairnessmodule.config.MainConfig.*;

@Controller
@RequestMapping(value = "/fairness")
public class ModuleController {

    @Autowired
    private MainRepository mainRepository;

    @Autowired
    private FileStorageProperties fileStorageProperties;

    private static final Logger logger = LoggerFactory.getLogger(ModuleController.class);

    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    @GetMapping("/module1")
    public String module1(HttpServletRequest request) throws Exception {
        return "module1.html";
    }

    /*
     * ********************************************************************************
     * Module 2
     * ********************************************************************************
     */

    @GetMapping({"/module", "/module2"})
    public String module2(HttpServletRequest request) throws Exception {
        return "module2.html";
    }

    /*
     * ********************************************************************************
     * Step 2.
     * ********************************************************************************
     */

    @PostMapping("/module2/step2")
    @ResponseBody
    public Map<String, Object> v2Step2(@RequestBody Map<String, Object> requestMap) throws Exception {
        long lap0 = System.currentTimeMillis();
        List<String> headerList = (List<String>) requestMap.get("header");
        String tablename = (String) requestMap.get("tablename");
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("tablename", tablename);
        resultMap.put("order0", requestMap.get("order0"));
        Map<String, Object> stdDevMap = v2Step2StdDevColumn(tablename, headerList);
        resultMap.put("stddev", stdDevMap);
        Map<String, Object> sortedMap = v2Step2SortedColumn(stdDevMap, (String) requestMap.get("order0"));
        resultMap.put("order_column", sortedMap.get("order_column"));
        Map<String, Object> nonNumericMap = v2Step2SortedNonNumeric(tablename, sortedMap);
        resultMap.putAll(nonNumericMap);
//        List<String> sortedColumnList = (List<String>) sortedMap.get("order_column");
        List<String> sortedColumnList = (List<String>) nonNumericMap.get("order_column");
        Map<String, Object> dbParamMap = new HashMap<>();
        String query1 = "tablename, ";
        String query2 = "'" + tablename + "', ";
        String query3 = "";
        for (int i = 0; i < sortedColumnList.size(); i++) {
            if (i > EndStdDevNumber) {
                break;
            }
            query1 += "order" + i + ", stddev" + i + ", ";
            query2 += "'" + sortedColumnList.get(i) + "', '" + stdDevMap.get(sortedColumnList.get(i)) + "', ";
            query3 += "order" + i + " = '" + sortedColumnList.get(i) + "', stddev" + i + " = '" + stdDevMap.get(sortedColumnList.get(i)) + "', ";
        }
        query1 = query1.substring(0, query1.length() - 2);
        query2 = query2.substring(0, query2.length() - 2);
        query3 = query3.substring(0, query3.length() - 2);
        String insertQuery = "INSERT INTO " + MetaTableName + " (" + query1 + ") VALUES (" + query2 + ") " +
                "ON DUPLICATE KEY UPDATE " + query3;
        dbParamMap.put("query", insertQuery);
        mainRepository.v2Step2InsertDataMeta(dbParamMap);
        resultMap.putAll(v2Step2OrderColumn(tablename));
        dbParamMap.put("tablename", tablename);
//        DataStructureCreator dataStructureCreator = new DataStructureCreator(tablename, (ArrayList<String>) resultMap.get("order_column"), mainRepository);
//        dataStructureCreator.create();
        long lap9 = System.currentTimeMillis();
        Map<String, Object> dbResultMap = mainRepository.v2Step2SelectRows(dbParamMap);
        resultMap.put("rows", dbResultMap.get("count"));
        resultMap.put("elapsed(sec)", (lap9 - lap0) / 1000.0);
        return resultMap;
    }

    private Map<String, Object> v2Step2StdDevColumn(String tablename, List<String> columns) throws Exception {
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

    private Map<String, Object> v2Step2SortedColumn(Map<String, Object> paramMap, String order0) throws Exception {
        Map<String, Double> map1 = new HashMap<>();
        for (String key : paramMap.keySet()) {
            map1.put(key, (Double) paramMap.get(key));
        }
        List<Map.Entry<String, Double>> entryList = new LinkedList<>(map1.entrySet());
        try {
//            Collections.sort(entryList, ((o1, o2) -> o1.getValue().compareTo(o2.getValue()))); // 표준편차오름차순
            Collections.sort(entryList, ((o1, o2) -> o2.getValue().compareTo(o1.getValue()))); // 표준편차내림차순
        } catch (Exception e) {
        }
        List<String> sortedColumn = new ArrayList<>();
        sortedColumn.add(order0);
        for (Map.Entry<String, Double> entry : entryList) {
            if (!entry.getKey().equals(order0)) {
                sortedColumn.add(entry.getKey());
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("order_column", sortedColumn);
        return resultMap;
    }

    private Map<String, Object> v2Step2SortedNonNumeric(String tablename, Map<String, Object> paramMap) throws Exception {
        List<String> columnList = (List<String>) paramMap.get("order_column");
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("tablename", tablename);
        queryMap.put("limit", LimitRowsTestNonNumeric);
        List<Map<String, Object>> testList = mainRepository.v2Step2TestNonNumericColumn(queryMap);
        List<String> nonNumericColumnList = new ArrayList<>();
        for (int i = 0; i < columnList.size(); i++) {
            for (int j = 0; j < testList.size(); j++) {
//                logger.info(j + ":" + columnList.get(i) + ": " + testList.get(j).get(columnList.get(i)));
                String testStr = (String) testList.get(j).get(columnList.get(i));
                if (!isNumeric(testStr)) {
                    nonNumericColumnList.add(columnList.get(i));
                    break;
                }
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("order_column", nonNumericColumnList);
        return resultMap;
    }

    public Map<String, Object> v2Step2OrderColumn(String tablename) throws Exception {
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
        resultMap.put("column_data", columnDataMap);
        return resultMap;
    }

    private boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        return pattern.matcher(str).matches();
    }

    @PostMapping({"/module2/step2Chart2Data", "/module2/step2Chart3Data", "/module2/step4Chart2Data", "/module2/step4Chart3Data"})
    @ResponseBody
    public Map<String, Object> v2Step2Chart2Data(@RequestBody Map<String, Object> requestMap) throws Exception {
        ResultChartCreator resultChartCreator = new ResultChartCreator();
        List<Map<String, Object>> dataList = resultChartCreator.creator(requestMap, mainRepository);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("name", "root");
        resultMap.put("children", dataList);
        return resultMap;
    }

    /*
     * ********************************************************************************
     * Step 3.
     * ********************************************************************************
     */

    @PostMapping("/module2/step3")
    @ResponseBody
    public Map<String, Object> v2Step3(@RequestBody Map<String, Object> requestMap) throws Exception {
        String tableName = (String) requestMap.get("tablename");
        ArrayList<String> columnList = (ArrayList<String>) requestMap.get("order_column");
        int permutationCount = Integer.parseInt((String) requestMap.get("permutation_count"));
        int avgLoop = Integer.parseInt((String) requestMap.get("avg_loop_limit"));
        long lap0 = System.currentTimeMillis();
        // Permutation
        ArrayList<String> nonBinaryColumnList = new ArrayList<>();
        for (int i = 1; i < columnList.size(); i++) {
            nonBinaryColumnList.add(columnList.get(i));
        }
        if (TurnOnConsoleLog) {
            logger.info("nonBinaryColumnList: " + nonBinaryColumnList);
        }
        int[] arr = new int[nonBinaryColumnList.size()];
        for (int i = 0; i < nonBinaryColumnList.size(); i++) {
            arr[i] = i;
        }
        Permutation perm = new Permutation(arr.length, permutationCount);
        perm.permutation(arr, 0);
        ArrayList<ArrayList<Integer>> resultList = perm.getResult();
        // Debug
//        for (int i = 0; i < resultList.size(); i++) {
//            debugLog = "[";
//            for (int j = 0; j < resultList.get(i).size(); j++) {
//                debugLog += resultList.get(i).get(j) + ", ";
//            }
//            debugLog = debugLog.substring(0, debugLog.length() - 2);
//            debugLog += "] ";
//            for (int j = 0; j < resultList.get(i).size(); j++) {
//                debugLog += nonBinaryColumnList.get(resultList.get(i).get(j)) + " ";
//            }
//            logger.info(debugLog);
//        }
        // Thread
        // https://pjh3749.tistory.com/280
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("columnIndex", 0);
        paramMap.put("whereClause", "");
        paramMap.put("previousMap", null);
        paramMap.put("tableName", tableName);
        paramMap.put("columnList", requestMap.get("order_column"));
        paramMap.put("binaryColumn", columnList.get(0));
        paramMap.put("nonBinaryColumnList", nonBinaryColumnList);
        paramMap.put("avgLoopLimit", avgLoop);
        paramMap.put("header", requestMap.get("header"));
        ImproveExecutor improveExecutor = new ImproveExecutor(resultList, paramMap, mainRepository);
        List<Map<String, Object>> improveList = improveExecutor.getList();
        // Thread
        Map<String, Object> dbParamMap = new HashMap<>();
        String query = "SELECT tablename AS tableName, COUNT(*) AS count, SUM(count) AS sum, AVG(count) AS avg, STDDEV(count) AS sd " +
                "FROM %s WHERE tablename LIKE '%s%%' GROUP BY tablename ORDER BY sd";
        dbParamMap.put("query", String.format(query, StructureTableName, tableName.replace(PrefixSourceTableName, PrefixResultTableName)));
        List<Map<String, Object>> statsList = mainRepository.v2Step2SelectQuery(dbParamMap);
        // Permutation
        for (Map<String, Object> map : improveList) {
            Map<String, Object> csv = (Map<String, Object>) map.get("csv");
            String csvFile = (String) csv.get("fileName");
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(csvFile).toUriString();
            csv.put("downloadUri", fileDownloadUri);
        }
        long lap9 = System.currentTimeMillis();
        if (TurnOnConsoleLog) {
            logger.info("Result: " + resultList.size());
            logger.info("Time Elapsed: " + (lap9 - lap0) + " ms (thread pool: " + ThreadPoolSize + ")");
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("case", resultList.size());
        resultMap.put("elapsed(sec)", (lap9 - lap0) / 1000.0);
        resultMap.put("tables", improveList);
        resultMap.put("stats", statsList);
        return resultMap;
    }

    private void v2Step3CategoryRecursion(Map<String, Object> paramMap) throws Exception {
        int columnIdx = (int) paramMap.get("column_index");
        int columnListLimit = (int) paramMap.get("column_list_limit");
        int avgLoopLimit = (int) paramMap.get("avg_loop_limit");
        String tableName = (String) paramMap.get("table_name");
        String whereClause = (String) paramMap.get("where_clause");
        ArrayList<String> columnList = (ArrayList<String>) paramMap.get("column_list");
        Map<String, Object> prevMap = (Map<String, Object>) paramMap.get("previous_map");
        if ((columnListLimit > 0 && columnIdx >= columnListLimit) || columnIdx >= columnList.size()) {
            v2Step3SaveResult(paramMap);
            return;
        }
        String prefix = v2Step3LogPrefix(columnIdx);
        String queryBase = "SELECT CONCAT('%s') AS column_name, `%s` AS column_value, count(*) AS actual_count " +
                "FROM %s WHERE 1=1 %s GROUP BY `%s`";
        String columnName = columnList.get(columnIdx);
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
                logger.info("\t" + prefix + columnIdx + "." + i + ": " + dbResultMap.get(i));
            }
            String columnValue = (String) dbResultMap.get(i).get("column_value");
            String nextWhere = whereClause + "AND `" + columnName + "` = '" + columnValue + "' ";
            paramMap.put("where_clause", nextWhere);
            paramMap.put("column_index", columnIdx + 1);
            paramMap.put("previous_map", dbResultMap.get(i));
            v2Step3CategoryRecursion(paramMap);
        }
    }

    private void v2Step3SaveResult(Map<String, Object> paramMap) throws Exception {
        boolean saveResult;
        String prefix = v2Step3LogPrefix((int) paramMap.get("column_index"));
        try {
            Map<String, Object> dbParamMap = new HashMap<>();
            String sourceTableName = (String) paramMap.get("table_name");
            String tid = sourceTableName.replaceAll("[^0-9]", "");
            String resultTableName = PrefixResultTableName + tid;
            String whereClause = (String) paramMap.get("where_clause");
            Map<String, Object> previousMap = (Map<String, Object>) paramMap.get("previous_map");
            String limitClause = Integer.toString((int) previousMap.get("result_count"));
            String query1 = "INSERT INTO " + resultTableName +
                    " (SELECT * FROM " + sourceTableName + " WHERE 1=1 " + whereClause + " LIMIT " + limitClause + ")";
            if (TurnOnConsoleLog) {
                logger.info(prefix + query1);
            }
            dbParamMap.put("query", query1);
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

    private String v2Step3LogPrefix(int i) {
        String p = "";
        for (int j = 0; j < i; j++) {
            p += "\t";
        }
        return p;
    }

    /*
     * ********************************************************************************
     * Step 4.
     * ********************************************************************************
     */

    @PostMapping("/module2/step4")
    @ResponseBody
    public Map<String, Object> v2Step4(@RequestBody Map<String, Object> requestMap) throws Exception {
        String sourceFileName = (String) requestMap.get("filename");
        String tableName = (String) requestMap.get("tablename");
        ArrayList<String> headerList = (ArrayList<String>) requestMap.get("header");
        String resultFileName = v2Step4GetResultFileName(sourceFileName);
        Path path = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path resultFilePath = path.resolve(resultFileName).normalize();
        String exportQuery = "SELECT * FROM " + tableName;
        Map<String, Object> dbParamMap = new HashMap<>();
        dbParamMap.put("query", exportQuery);
        List<Map<String, Object>> dbResultMap = mainRepository.v2Step3ExportResult(dbParamMap);
        Map<String, Object> headerMap = dbResultMap.get(0);
        String[] header = new String[headerList.size()];
        for (int i = 0; i < headerList.size(); i++) {
            header[i] = headerList.get(i);
        }
        CSVWriter csvWriter = new CSVWriter(new FileWriter(resultFilePath.toString()));
        csvWriter.writeNext(header);
        String[] row = new String[headerList.size()];
        for (Map<String, Object> map : dbResultMap) {
            for (int i = 0; i < header.length; i++) {
                row[i] = (String) map.get(header[i]);
            }
            csvWriter.writeNext(row);
        }
        csvWriter.flush();
        String resultFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(resultFileName).toUriString();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("filename", resultFileName);
        resultMap.put("fileurl", resultFileDownloadUri);
        return resultMap;
    }

    private String v2Step4GetResultFileName(String sourceFileName) throws Exception {
//        String prefixFileName = String.valueOf(System.currentTimeMillis());
        String prefixFileName = "result";
        int pos = sourceFileName.lastIndexOf(".");
        String nameFileName = sourceFileName.substring(0, pos);
        String extFileName = sourceFileName.substring(pos + 1);
        return nameFileName + "_" + prefixFileName + "." + extFileName;
    }

}
