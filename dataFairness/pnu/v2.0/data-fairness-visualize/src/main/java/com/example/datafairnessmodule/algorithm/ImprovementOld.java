package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.example.datafairnessmodule.config.MainConfig.PrefixResultTableName;
import static com.example.datafairnessmodule.config.MainConfig.TailDelimiter;

// https://howtodoinjava.com/java/multi-threading/callable-future-example/
public class ImprovementOld implements Callable<Map<String, Object>> {
    @Autowired
    private MainRepository mainRepository;

    private static final Logger logger = LoggerFactory.getLogger(ImprovementOld.class);

    private int index;
    private ArrayList<Integer> list;
    private Map<String, Object> paramMap;
    private Map<String, Object> resultMap;

    public ImprovementOld(int index, ArrayList<Integer> list, Map<String, Object> paramMap) {
        this.index = index;
        this.list = list;
        this.paramMap = paramMap;
        this.resultMap = new HashMap<>();
    }

    @Override
    public Map<String, Object> call() throws Exception {
        ArrayList<String> columnList = (ArrayList<String>) paramMap.get("newColumnList");
        ArrayList<String> algorithmColumn = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            algorithmColumn.add(columnList.get(list.get(i)));
        }
        logger.info((index + 1) + "\t" + list + " " + algorithmColumn + "\t(" + Thread.currentThread().getName() + ")");

        String sourceTableName = (String) paramMap.get("tableName");
        String tid = sourceTableName.replaceAll("[^0-9]", "");
        String resultTableName = PrefixResultTableName + tid + TailDelimiter + (index + 1);
        Map<String, Object> dbParamMap = new HashMap<>();
        String query = "CREATE TABLE IF NOT EXISTS " + resultTableName + " LIKE " + sourceTableName;
        dbParamMap.put("query", query);
        mainRepository.v2Step3SaveResult(dbParamMap);
//        query = "TRUNCATE TABLE " + resultTableName;
//        dbParamMap.put("query", query);
//        mainRepository.v2Step3SaveResult(dbParamMap);

        resultMap.put("index", index);
        resultMap.put("algorithmColumn", algorithmColumn);
        resultMap.put("tableName", resultTableName);
        return resultMap;
    }

}
