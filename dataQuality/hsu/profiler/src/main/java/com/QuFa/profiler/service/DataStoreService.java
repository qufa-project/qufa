package com.QuFa.profiler.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.configuration.DataCleanerConfigurationImpl;
import org.datacleaner.connection.CsvDatastore;
import org.datacleaner.connection.Datastore;
import org.datacleaner.connection.DatastoreCatalogImpl;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * 프로파일링을 위한 기본 설정 작업을 수행하는 클래스 <br> Configruation 파일 파싱, Datasource 연결, 프로파일 분석 작업 설정 등을 수행한다
 */
@Component
public class DataStoreService {

    public char quote_char = '\"';
    public char separator_char = ',';
    public String csvEncoding = "UTF-8";
    public boolean fail_on_inconsistencies = true;
    public int header_line_number = 1;
    public final String DEFAULT_DB = "local";
    public final String PROD_DB = "prod";
    public final String TEST_DB = "test";
    private DataCleanerConfiguration configuration;
    private AnalysisJobBuilder builder;
    private Datastore dataStore;
    String targetFolderPath;

    public void storeUrlFile(String url, Boolean isHeader) throws IOException {
        String[] split = url.split("/");
        String fileName = split[split.length - 1].split("\\.")[0];

        // 운영체제별로 targetfiles 다르게 설정
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            targetFolderPath = "C://Temp/targetfiles/";
            System.out.println("targetFolderPath = " + targetFolderPath);
        } else if (os.contains("linux")) {
            targetFolderPath = "~/tmp";
        }

        File f = new File(targetFolderPath + fileName + ".csv");
        FileUtils.copyURLToFile(new URL(url), f);


        if (!isHeader) {
            String path = targetFolderPath + fileName + ".csv";
            CSVReader csvReader = new CSVReader((new FileReader(path)));
            List<String> header = new ArrayList<>();

            // 컬럼 수에 따라 헤더 설정
            int recordsCount = csvReader.readNext().length;
            for (int i = 1; i < recordsCount + 1; i++) {
                header.add(fileName + "_" + i);
            }
            csvReader.close();

            File originFile = new File(path); // 원본 파일
            csvReader = new CSVReader((new FileReader(path)));
            List<Object> originData = new ArrayList<>();
            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                originData.add(nextLine); // 원본 데이터 읽기
            }

            // 헤더가 없을 경우 targetfiles에 변형 파일 저장
            CSVWriter csvWriter = new CSVWriter(new FileWriter(originFile));
            csvWriter.writeNext(header.toArray(String[]::new)); // 헤더 작성

            // 헤더 아랫줄부터 원본 데이터 쓰기
            for (Object data : originData) {
                csvWriter.writeNext((String[]) data);
            }

            csvWriter.close();
        }

    }

    /**
     * Configuration파일을 파싱하여 DB를 연결하는 메소드
     */
    public  void createDataStore(String fileName) {
        try {

            //JdbcDatastore datastore = new JdbcDatastore(dbName, url, driver, username, password, false);
            CsvDatastore datastore = new CsvDatastore("CSVDS",
                    "C:\\develop\\profiler\\src\\main\\resources\\targetfiles\\" + fileName
                            + ".csv", quote_char, separator_char, csvEncoding,
                    fail_on_inconsistencies, header_line_number);

            DatastoreCatalogImpl catalog = new DatastoreCatalogImpl(datastore);

            configuration = new DataCleanerConfigurationImpl(null, null, catalog, null);

            setDefault("CSVDS");
        } finally {
            System.gc();
        }
    }

    //TODO: fileName 파라미터 추가
    public  void createLocalDataStore(String path) {
        try {

            //JdbcDatastore datastore = new JdbcDatastore(dbName, url, driver, username, password, false);
            CsvDatastore datastore = new CsvDatastore("CSVDS", path, quote_char, separator_char,
                    csvEncoding, fail_on_inconsistencies, header_line_number);

            DatastoreCatalogImpl catalog = new DatastoreCatalogImpl(datastore);

            configuration = new DataCleanerConfigurationImpl(null, null, catalog, null);

            setDefault("CSVDS");
        } finally {
            System.gc();
        }
    }

    public Datastore getDataStore() {
        return dataStore;
    }

    public AnalysisJobBuilder setDataStore(String csvName) {
        System.out.println("System.out for dataStore : " + configuration);

        dataStore = configuration.getDatastoreCatalog().getDatastore(csvName);

        System.out.println("dataStore name: " + dataStore); //.toString()

        return builder.setDatastore(dataStore);
    }

    public DataCleanerConfiguration getConfiguration() {
        return configuration;
    }

    public AnalysisJobBuilder getBuilder() {
        return builder;
    }

    public void setDefault(String csvName) {
        try {
            builder = new AnalysisJobBuilder(configuration);
            if (configuration != null) {
                setDataStore(csvName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.gc();
    }

    public void setQuote_char(char quote_char) {
        quote_char = quote_char;
    }

    public void setSeparator_char(char separator_char) {
        separator_char = separator_char;
    }

    public void setCsvEncoding(String encoding) {
        csvEncoding = encoding;
    }

    public void setFail_on_inconsistencies(boolean fail_on_inconsistencies) {
        fail_on_inconsistencies = fail_on_inconsistencies;
    }

    public void setHeader_line_number(int header_line_number) {
        header_line_number = header_line_number;
    }


}