package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultChartCreator {
    private static final Logger logger = LoggerFactory.getLogger(ResultChartCreator.class);

    public List<Map<String, Object>> creator(Map<String, Object> requestMap, MainRepository mainRepository) throws Exception {
        String tableName = (String) requestMap.get("tablename");
        ArrayList<String> columnList = (ArrayList<String>) requestMap.get("columnlist");
        Map<String, Object> dbParamMap = new HashMap<>();
        String queryBase = "SELECT `%s` AS name, COUNT(*) AS size FROM " + tableName + " WHERE 1=1 %s GROUP BY `%s`";
        // Column0
        List<Map<String, Object>> dataList0 = new ArrayList<>();
        if (columnList.size() > 0) {
            String sub0a = columnList.get(0);
            String sub0b = "";
            dbParamMap.put("query", String.format(queryBase, sub0a, sub0b, sub0a));
            List<Map<String, Object>> dbResultMap0 = mainRepository.v2Step2SelectChartData(dbParamMap);
            for (int i0 = 0; i0 < dbResultMap0.size(); i0++) {
                Map<String, Object> dataMap0 = new HashMap<>();
                Map<String, Object> map0 = dbResultMap0.get(i0);
                dataMap0.put("name", map0.get("name"));
                // Column1
                List<Map<String, Object>> dataList1 = new ArrayList<>();
                if (columnList.size() > 1) {
                    String sub1a = columnList.get(1);
                    String sub1b = sub0b + "AND `" + columnList.get(0) + "` = '" + map0.get("name") + "' ";
                    dbParamMap.put("query", String.format(queryBase, sub1a, sub1b, sub1a));
                    List<Map<String, Object>> dbResultMap1 = mainRepository.v2Step2SelectChartData(dbParamMap);
                    for (int i1 = 0; i1 < dbResultMap1.size(); i1++) {
                        Map<String, Object> dataMap1 = new HashMap<>();
                        Map<String, Object> map1 = dbResultMap1.get(i1);
                        dataMap1.put("name", map1.get("name"));
                        // Column2
                        List<Map<String, Object>> dataList2 = new ArrayList<>();
                        if (columnList.size() > 2) {
                            String sub2a = columnList.get(2);
                            String sub2b = sub1b + "AND `" + columnList.get(1) + "` = '" + map1.get("name") + "' ";
                            dbParamMap.put("query", String.format(queryBase, sub2a, sub2b, sub2a));
                            List<Map<String, Object>> dbResultMap2 = mainRepository.v2Step2SelectChartData(dbParamMap);
                            for (int i2 = 0; i2 < dbResultMap2.size(); i2++) {
                                Map<String, Object> dataMap2 = new HashMap<>();
                                Map<String, Object> map2 = dbResultMap2.get(i2);
                                dataMap2.put("name", map2.get("name"));
                                // Column3
                                List<Map<String, Object>> dataList3 = new ArrayList<>();
                                if (columnList.size() > 3) {
                                    String sub3a = columnList.get(3);
                                    String sub3b = sub2b + "AND `" + columnList.get(2) + "` = '" + map2.get("name") + "' ";
                                    dbParamMap.put("query", String.format(queryBase, sub3a, sub3b, sub3a));
                                    List<Map<String, Object>> dbResultMap3 = mainRepository.v2Step2SelectChartData(dbParamMap);
                                    for (int i3 = 0; i3 < dbResultMap3.size(); i3++) {
                                        Map<String, Object> dataMap3 = new HashMap<>();
                                        Map<String, Object> map3 = dbResultMap3.get(i3);
                                        dataMap3.put("name", map3.get("name"));
                                        // Column4
                                        List<Map<String, Object>> dataList4 = new ArrayList<>();
                                        if (columnList.size() > 4) {
                                            String sub4a = columnList.get(4);
                                            String sub4b = sub3b + "AND `" + columnList.get(3) + "` = '" + map3.get("name") + "' ";
                                            dbParamMap.put("query", String.format(queryBase, sub4a, sub4b, sub4a));
                                            List<Map<String, Object>> dbResultMap4 = mainRepository.v2Step2SelectChartData(dbParamMap);
                                            for (int i4 = 0; i4 < dbResultMap4.size(); i4++) {
                                                Map<String, Object> dataMap4 = new HashMap<>();
                                                Map<String, Object> map4 = dbResultMap4.get(i4);
                                                dataMap4.put("name", map4.get("name"));
                                                // Column5
//                                                List<Map<String, Object>> dataList5 = new ArrayList<>();
//                                                if (columnList.size() > 5) {
//                                                    String sub5a = columnList.get(5);
//                                                    String sub5b = sub4b + "AND `" + columnList.get(4) + "` = '" + map4.get("name") + "' ";
//                                                    dbParamMap.put("query", String.format(queryBase, sub5a, sub5b, sub5a));
//                                                    logger.info("query5: " + dbParamMap.get("query"));
//                                                    List<Map<String, Object>> dbResultMap5 = mainRepository.v2Step2SelectChartData(dbParamMap);
//                                                    for (int i5 = 0; i5 < dbResultMap3.size(); i5++) {
//                                                        Map<String, Object> dataMap5 = new HashMap<>();
//                                                        Map<String, Object> map5 = dbResultMap5.get(i5);
//                                                        dataMap5.put("name", map5.get("name"));
//                                                        // Column...
//                                                        List<Map<String, Object>> dataList6 = new ArrayList<>();
//                                                        if (columnList.size() > 6) {
//                                                            //
//                                                        } else {
//                                                            dataMap5.put("size", map5.get("size"));
//                                                        } // Column...
//                                                        dataList5.add(dataMap5);
//                                                    }
//                                                    dataMap4.put("children", dataList5);
//                                                } else {
//                                                    dataMap4.put("size", map4.get("size"));
//                                                } // Column5
                                                dataMap4.put("size", map4.get("size"));
                                                dataList4.add(dataMap4);
                                            }
                                            dataMap3.put("children", dataList4);
                                        } else {
                                            dataMap3.put("size", map3.get("size"));
                                        } // Column4
                                        dataList3.add(dataMap3);
                                    }
                                    dataMap2.put("children", dataList3);
                                } else {
                                    dataMap2.put("size", map2.get("size"));
                                } // Column3
                                dataList2.add(dataMap2);
                            }
                            dataMap1.put("children", dataList2);
                        } else {
                            dataMap1.put("size", map1.get("size"));
                        } // Column2
                        dataList1.add(dataMap1);
                    }
                    dataMap0.put("children", dataList1);
                } else {
                    dataMap0.put("size", map0.get("size"));
                } // Column1
                dataList0.add(dataMap0);
            }
        }
        return dataList0;
    }
}
