package kr.co.promptech.profiler.service;

import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.configuration.JaxbConfigurationReader;
import org.datacleaner.connection.Datastore;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 프로파일링을 위한 기본 설정 작업을 수행하는 클래스 <br>
 * Configruation 파일 파싱, Datasource 연결, 프로파일 분석 작업 설정 등을 수행한다
 */
@Component
public class DataStoreService {

    public static final String DEFAULT_DB = "dataStore";
    //    public static final String DEFAULT_DB = "test";
    public static final String CONFIG_FILE_PATH = "config/datastore_config.xml";


    private static DataCleanerConfiguration configuration;
    private static AnalysisJobBuilder builder;
    private static Datastore dataStore;

    /**
     * Configuration파일을 파싱하여 DB를 연결하는 메소드
     */
    public static void createDataStore() {
        InputStream dbInputStream;

        try {
            dbInputStream = new ClassPathResource(CONFIG_FILE_PATH).getInputStream();

            JaxbConfigurationReader dbConfigurationReader = new JaxbConfigurationReader();

            configuration = dbConfigurationReader.read(dbInputStream);

            setDefault(DEFAULT_DB);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            dbInputStream = null;
            System.gc();
        }
    }

    public static void createDataStore(String dbName) {
        InputStream dbInputStream;

        try {
            dbInputStream = new ClassPathResource(CONFIG_FILE_PATH).getInputStream();

            JaxbConfigurationReader dbConfigurationReader = new JaxbConfigurationReader();

            configuration = dbConfigurationReader.read(dbInputStream);

            setDefault(dbName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            dbInputStream = null;
            System.gc();
        }
    }

    public static Datastore getDataStore() {
        return dataStore;
    }

    public static AnalysisJobBuilder setDataStore(String DBName) {
        System.out.println("System.out for dataStore : " + configuration);
        dataStore = configuration.getDatastoreCatalog().getDatastore(DBName);

        System.out.println("dataStore name: " + dataStore.toString());

        return builder.setDatastore(dataStore);
    }

    public static DataCleanerConfiguration getConfiguration() {
        return configuration;
    }

    public static AnalysisJobBuilder getBuilder() {
        return builder;
    }

    public static void setDefault(String DBName) {
        try {
            builder = new AnalysisJobBuilder(configuration);
            if (configuration != null) {
                setDataStore(DBName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.gc();
    }
}
