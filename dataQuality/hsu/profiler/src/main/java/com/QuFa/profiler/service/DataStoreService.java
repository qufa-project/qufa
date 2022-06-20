package com.QuFa.profiler.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.configuration.DataCleanerConfigurationImpl;
import org.datacleaner.configuration.JaxbConfigurationReader;
import org.datacleaner.connection.CsvDatastore;
import org.datacleaner.connection.Datastore;
import org.datacleaner.connection.DatastoreCatalogImpl;
import org.datacleaner.connection.JdbcDatastore;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 프로파일링을 위한 기본 설정 작업을 수행하는 클래스 <br> Configruation 파일 파싱, Datasource 연결, 프로파일 분석 작업 설정 등을 수행한다
 */
@Component
public class DataStoreService {

    public static char quote_char = '\"';
    public static char separator_char = ',';
    public static String csvEncoding = "UTF-8";
    public static boolean fail_on_inconsistencies = true;
    public static int header_line_number = 1;


    public static final String DEFAULT_DB = "local";
    public static final String PROD_DB = "prod";
    public static final String TEST_DB = "test";

    private static DataCleanerConfiguration configuration;
    private static AnalysisJobBuilder builder;
    private static Datastore dataStore;

    public void storeUrlFile(String url, Boolean isHeader) throws IOException {
        String[] split = url.split("/");
        String fileName = split[split.length - 1].split("\\.")[0];
        String folderName = "./src/main/resources/targetfiles/";

        File f = new File(folderName + fileName + ".csv");
        FileUtils.copyURLToFile(new URL(url), f);

        if (!isHeader) {
            String path = folderName + fileName + ".csv";
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
    public static void createDataStore(String fileName) {
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

    public static void createLocalDataStore(String path) {
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

    public static Datastore getDataStore() {
        return dataStore;
    }

    public static AnalysisJobBuilder setDataStore(String csvName) {
        System.out.println("System.out for dataStore : " + configuration);

        dataStore = configuration.getDatastoreCatalog().getDatastore(csvName);

        System.out.println("dataStore name: " + dataStore); //.toString()

        return builder.setDatastore(dataStore);
    }

    public static DataCleanerConfiguration getConfiguration() {
        return configuration;
    }

    public static AnalysisJobBuilder getBuilder() {
        return builder;
    }

    public static void setDefault(String csvName) {
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

    public static void setQuote_char(char quote_char) {
        DataStoreService.quote_char = quote_char;
    }

    public static void setSeparator_char(char separator_char) {
        DataStoreService.separator_char = separator_char;
    }

    public static void setCsvEncoding(String encoding) {
        DataStoreService.csvEncoding = encoding;
    }

    public static void setFail_on_inconsistencies(boolean fail_on_inconsistencies) {
        DataStoreService.fail_on_inconsistencies = fail_on_inconsistencies;
    }

    public static void setHeader_line_number(int header_line_number) {
        DataStoreService.header_line_number = header_line_number;
    }


}