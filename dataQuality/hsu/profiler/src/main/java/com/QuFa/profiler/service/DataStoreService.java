package com.QuFa.profiler.service;

import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.configuration.DataCleanerConfigurationImpl;
import org.datacleaner.connection.CsvDatastore;
import org.datacleaner.connection.Datastore;
import org.datacleaner.connection.DatastoreCatalogImpl;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.springframework.stereotype.Component;

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

    /**
     * Configuration파일을 파싱하여 DB를 연결하는 메소드
     */
    public void createDataStore(String fileName) {
        try {
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

    public void createLocalDataStore(String path) {
        try {
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