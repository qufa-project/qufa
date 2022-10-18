package com.QuFa.profiler.service;

import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.configuration.DataCleanerConfigurationImpl;
import org.datacleaner.connection.CsvDatastore;
import org.datacleaner.connection.Datastore;
import org.datacleaner.connection.DatastoreCatalogImpl;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.springframework.stereotype.Component;

//FKAnalysis 위한 기본 설정 작업을 수행하는 클래스.
//Configuration 파일 파싱, Datasource 연결 설정 등을 수행한다.
@Component
public class FKDataStoreService {

    public char quote_char = '\"';
    public char separator_char = ',';
    public String csvEncoding = "UTF-8";
    public boolean fail_on_inconsistencies = true;
    public int header_line_number = 1;
    private DataCleanerConfiguration configuration;
    private AnalysisJobBuilder builder;
    private Datastore dataStore;

    public void createLocalDataStore(String name, String path) {
        try {
            CsvDatastore datastore = new CsvDatastore(name, path, quote_char, separator_char,
                    csvEncoding, fail_on_inconsistencies, header_line_number);

            DatastoreCatalogImpl catalog = new DatastoreCatalogImpl(datastore);

            configuration = new DataCleanerConfigurationImpl(null, null, catalog, null);
            dataStore = configuration.getDatastoreCatalog().getDatastore(name);
        } finally {
            System.gc();
        }
    }

    public Datastore getDataStore() {
        System.out.println("FKDataStoreService.createLocalDataStore");
        System.out.println("datastore = " + dataStore);
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
}
