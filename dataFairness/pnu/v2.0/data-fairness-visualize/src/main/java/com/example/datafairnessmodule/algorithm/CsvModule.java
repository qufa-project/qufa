package com.example.datafairnessmodule.algorithm;

import com.example.datafairnessmodule.repository.MainRepository;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvModule {
    private static final Logger logger = LoggerFactory.getLogger(CsvModule.class);

    private static String uploadDir;

    @Value("${file.upload-dir}")
    public void setUploadDir(String dir) {
        uploadDir = dir;
    }

    public Map<String, Object> export(String tableName, ArrayList<String> headerList, MainRepository mainRepository) {
        CSVWriter csvWriter = null;
        Map<String, Object> resultMap = new HashMap<>();
        try {
            String resultFileName = tableName + ".csv";
            Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
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
            csvWriter = new CSVWriter(new FileWriter(resultFilePath.toString()));
            csvWriter.writeNext(header);
            String[] row = new String[headerList.size()];
            for (Map<String, Object> map : dbResultMap) {
                for (int i = 0; i < header.length; i++) {
                    row[i] = (String) map.get(header[i]);
                }
                csvWriter.writeNext(row);
            }
            csvWriter.flush();
            resultMap.put("fileName", resultFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}
