package com.QuFa.profiler.service;


import com.QuFa.profiler.config.ActiveProfileProperty;
import com.QuFa.profiler.model.profile.*;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.datacleaner.api.AnalyzerResult;
import org.datacleaner.api.InputColumn;
import org.datacleaner.beans.*;
import org.datacleaner.beans.valuedist.ValueDistributionAnalyzer;
import org.datacleaner.beans.valuedist.ValueDistributionAnalyzerResult;
import org.datacleaner.job.AnalysisJob;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.datacleaner.job.builder.AnalyzerComponentBuilder;
import org.datacleaner.job.runner.AnalysisResultFuture;
import org.datacleaner.job.runner.AnalysisRunner;
import org.datacleaner.job.runner.AnalysisRunnerImpl;
import org.datacleaner.result.AnalysisResult;
import org.datacleaner.result.ValueFrequency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileReader;
import java.util.*;

/**
 * 데이터 프로파일 작업을 수행하는 서비스
 */
@RequiredArgsConstructor
@Component
public class ProfileService {
    /**
     *
     */
    private final ActiveProfileProperty activeProfileProperty;
    private ProfileTableResult profileTableResult = new ProfileTableResult();
    private ProfileColumnResult profileColumnResult = new ProfileColumnResult();

    @Autowired
    private final DataStoreService dataStoreService;

    public ProfileTableResult profileCSV(MultipartFile file){
        ProfileTableResult profileTableResult = new ProfileTableResult();
        try {
            dataStoreService.storeFile(file);
            CSVReader csvReader = new CSVReader(new FileReader("./src/main/resources/targetfiles/" + file.getOriginalFilename()));

            List<String> header = Arrays.asList(csvReader.readNext().clone());

            profileTableResult = profileColumns(file,header);

        } catch(Exception e){
            e.printStackTrace();
        }
        return profileTableResult;
    }

    public ProfileTableResult profileLocalCSV(String path){
        //ProfileTableResult profileTableResult = new ProfileTableResult();
        try {
            CSVReader csvReader = new CSVReader(new FileReader(path));

            List<String> header = Arrays.asList(csvReader.readNext().clone());

            profileLocalColumns(path,header);

            csvReader.close();

        } catch(Exception e){
            e.printStackTrace();
        }
        return profileTableResult;
    }

    //    /**
//     * 타겟 테이블의 모든 컬럼에 대해 프로파일을 수행하는 메소드
//     *
//     * @param tableName   타겟 테이블명
//     * @param columnNames 타겟 테이블의 컬럼리스트
//     */
    public ProfileTableResult profileColumns(MultipartFile file, List<String> columnNames) { //

        //파일이름만 분리
        String filename = file.getOriginalFilename().split("\\.")[0];

        //CsvDatastore
        DataStoreService.createDataStore(filename);

        profileTableResult.setDataset_name(filename);
        profileTableResult.setDataset_column_cnt(20);
        profileTableResult.setDataset_type("csv");

        for (String columnName : columnNames) {
            profileColumnResult = new ProfileColumnResult();
            profileColumnResult.setColumn_id(columnNames.indexOf(columnName) + 1);
            profileColumnResult.setColumn_name(columnName);
            profileColumnResult.setColumn_type("STRING");
            this.profileSingleColumn(filename, columnName);
            profileTableResult.getResults().add(profileColumnResult);
        }

        return profileTableResult;
    }

    public void profileLocalColumns(String path, List<String> columnNames) {
        profileTableResult = new ProfileTableResult();
        System.out.println(path);
        //파일이름만 분리
        String[] split = path.split("\\\\");
        String filename = split[split.length-1].split("\\.")[0];
        System.out.println("filename:"+filename);

        //CsvDatastore
        DataStoreService.createLocalDataStore(path);

        profileTableResult.setDataset_name(filename);
        profileTableResult.setDataset_column_cnt(20);
        profileTableResult.setDataset_type("csv");

        for (String columnName : columnNames) {
            profileColumnResult = new ProfileColumnResult();
            profileColumnResult.setColumn_id(columnNames.indexOf(columnName) + 1);
            profileColumnResult.setColumn_name(columnName);
            profileColumnResult.setColumn_type("STRING");
            this.profileSingleColumn(filename, columnName);
            profileTableResult.getResults().add(profileColumnResult);
        }
    }

    /**
     * 타겟 테이블의 한 컬럼별 프로파일을 수행하는 메소드
     *
     * @param tableName  타겟 테이블명
     * @param columnName 타겟 테이블의 1개 컬럼
     */
    private void profileSingleColumn(String tableName, String columnName) {

        String inputColumnName = tableName + "." + columnName;
        System.out.println("inputColumnName : " + inputColumnName);

        //DataStoreService.setDataStore2(activeProfileProperty.getActive());
        DataStoreService.setDataStore("CSVDS");
        AnalysisJobBuilder builder = DataStoreService.getBuilder();
        builder.addSourceColumns(columnName);
        InputColumn<?> targetInputColumn = builder.getSourceColumnByName(columnName);
        System.out.println(targetInputColumn); // null로 나옴


        // analyzer config
        // val
        AnalyzerComponentBuilder<ValueDistributionAnalyzer> valDistAnalyzer = builder.addAnalyzer(ValueDistributionAnalyzer.class);
        valDistAnalyzer.addInputColumns(targetInputColumn);
        valDistAnalyzer.setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_UNIQUE_VALUES, true);
        valDistAnalyzer.setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_DRILL_DOWN_INFORMATION, true);

        // str
        if (targetInputColumn.getDataType().getTypeName().equals("java.lang.String")) {
            AnalyzerComponentBuilder<StringAnalyzer> stringAnalyzer = builder.addAnalyzer(StringAnalyzer.class);
            stringAnalyzer.addInputColumn(targetInputColumn);

            // stringAnalyzer.setConfiguredProperty("Descriptive statistics", false);
        } else if (targetInputColumn.getDataType().getTypeName().equals("java.util.Date") ||
                targetInputColumn.getDataType().getTypeName().equals("java.time.Instant")) {
            AnalyzerComponentBuilder<DateAndTimeAnalyzer> numberAnalyzer = builder.addAnalyzer(DateAndTimeAnalyzer.class);
            numberAnalyzer.addInputColumn(targetInputColumn);
        } else if (targetInputColumn.getDataType().getTypeName().equals("java.lang.Boolean")) {
            // do something
        } else if (targetInputColumn.getDataType().getTypeName().equals("java.lang.Number") ||
                targetInputColumn.getDataType().getTypeName().equals("java.lang.Integer") ||
                targetInputColumn.getDataType().getTypeName().equals("java.lang.Doulbe") ||
                targetInputColumn.getDataType().getTypeName().equals("java.lang.Float") ||
                targetInputColumn.getDataType().getTypeName().equals("java.lang.BigInteger")) {
            AnalyzerComponentBuilder<NumberAnalyzer> numberAnalyzer = builder.addAnalyzer(NumberAnalyzer.class);
            numberAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            numberAnalyzer.addInputColumn(targetInputColumn);
        }

        // job build run & result
        AnalysisJob analysisJob = builder.toAnalysisJob();
        AnalysisRunner runner = new AnalysisRunnerImpl(DataStoreService.getConfiguration());
        AnalysisResultFuture resultFuture = runner.run(analysisJob);

        resultFuture.await();

        System.out.println("==========run===========");
        // 에러 발생 혹은 취소시,
        if (resultFuture.isCancelled() || resultFuture.isErrornous()) {
            System.out.println("profiling error !");
            List<Throwable> errors = resultFuture.getErrors();
            System.out.println(errors.toString());

            resultFuture.cancel();
            resultFuture = null;
        } else {
            // 성공시 결과 저장.
            AnalysisResult analysisResult = resultFuture;
            List<AnalyzerResult> results = analysisResult.getResults();
            System.out.println(" ============  RESULTS ============= ");
            System.out.println(results.toString());
            System.out.println("");

            System.out.println("====== extract Results ========");
            this.extractResult(results, targetInputColumn, columnName, tableName);

            // 빌더 리셋 후 재시작
            builder.reset();
        }
    }

    /**
     * 프로파일 결과를 추출하여 DB에 저장하는 메소드
     *
     * @param results           프로파일 결과에 대한 Resultset
     * @param targetInputColumn 현재 프로파일 작업을 수행한 컬럼
     * @param columnName        현재 프로파일 작업을 수행한 컬럼명
     * @param tableName         현재 프로파일 작업을 수행한 테이블명
     */
    private void extractResult(List<AnalyzerResult> results,
                               InputColumn<?> targetInputColumn,
                               String columnName,
                               String tableName) {
        BasicProfile basicProfile = new BasicProfile();
        NumberProfile numberProfile = new NumberProfile();
        StringProfile stringProfile = new StringProfile();
        DateProfile dateProfile = new DateProfile();
        int totalCnt = 0;

        Map<Object, Object> vfModelList = new HashMap<>();

        basicProfile.setNull_cnt(0);
        stringProfile.setBlank_cnt(0);

        //TODO: COLUMN COMMENT 가져오기
//        resultModel.setColumnDesc(columnInfo.getColumnDesc());

        for (AnalyzerResult result : results) {
            if (result instanceof ValueDistributionAnalyzerResult) {
                if (((ValueDistributionAnalyzerResult) result).getNullCount() > 0) {
                    basicProfile.setNull_cnt(((ValueDistributionAnalyzerResult) result).getNullCount());
                }

                basicProfile.setDistinct_cnt(((ValueDistributionAnalyzerResult) result).getDistinctCount());
                basicProfile.setRow_cnt(((ValueDistributionAnalyzerResult) result).getTotalCount());

                Collection<ValueFrequency> vfList = ((ValueDistributionAnalyzerResult) result).getValueCounts();
                for (ValueFrequency vf : vfList) {
                    if (vf.getChildren() != null) {
                        Collection<ValueFrequency> vfChildren = vf.getChildren();
                        for (ValueFrequency vfChild : vfChildren) {
                            //TODO: COLUMN COMMENT 가져오기
//                            resultVFModel.setColumnDesc(columnInfo.getColumnDesc());

                            vfModelList.put(vfChild.getValue(),vfChild.getCount());
                        }
                    } else {
                        //TODO: COLUMN COMMENT 가져오기
//                        resultVFModel.setColumnDesc(columnInfo.getColumnDesc());
                        vfModelList.put(vf.getValue(),vf.getCount());
                    }
                }

                totalCnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();
            }
        }
        //basicProfile.setValue_distribution(vfModelList);

        for (AnalyzerResult result : results) {
            if (result instanceof StringAnalyzerResult) {
                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(((StringAnalyzerResult) result).getNullCount(targetInputColumn));
                }

                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) < totalCnt) {
                    stringProfile.setAvg_len(((StringAnalyzerResult) result).getAvgChars(targetInputColumn));
                    stringProfile.setMax_len(((StringAnalyzerResult) result).getMaxChars(targetInputColumn));
                    stringProfile.setMin_len(((StringAnalyzerResult) result).getMinChars(targetInputColumn));
                }
                stringProfile.setBlank_cnt(((StringAnalyzerResult) result).getBlankCount(targetInputColumn));
            }

            if (result instanceof NumberAnalyzerResult) {
                if (((NumberAnalyzerResult) result).getNullCount(targetInputColumn).intValue() > 0 && basicProfile.getNull_cnt() == 0)
                    basicProfile.setNull_cnt(((NumberAnalyzerResult) result).getNullCount(targetInputColumn).intValue());

                if (((NumberAnalyzerResult) result).getLowestValue(targetInputColumn) != null) {
                    numberProfile.setMin((Double) ((NumberAnalyzerResult) result).getLowestValue(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getHighestValue(targetInputColumn) != null) {
                    numberProfile.setMax((Double) ((NumberAnalyzerResult) result).getHighestValue(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getSum(targetInputColumn) != null) {
                    numberProfile.setSum((Double) ((NumberAnalyzerResult) result).getSum(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getMean(targetInputColumn) != null) {
                    numberProfile.setMean((Double) ((NumberAnalyzerResult) result).getMean(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getMedian(targetInputColumn) != null) {
                    numberProfile.setMedian((Double) ((NumberAnalyzerResult) result).getMedian(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getStandardDeviation(targetInputColumn) != null) {
                    numberProfile.setSd((Double) ((NumberAnalyzerResult) result).getStandardDeviation(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getVariance(targetInputColumn) != null) {
                    numberProfile.setVariance((Double) ((NumberAnalyzerResult) result).getVariance(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getPercentile25(targetInputColumn) != null) {
                    numberProfile.setPercentile_25th((Double) ((NumberAnalyzerResult) result).getPercentile25(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getPercentile75(targetInputColumn) != null) {
                    numberProfile.setPercentile_75th((Double) ((NumberAnalyzerResult) result).getPercentile75(targetInputColumn));
                }
                //if (((NumberAnalyzerResult) result).getStandardDeviation(targetInputColumn) != null) {
                numberProfile.setZero_cnt(0);
                //}
            }

            if (result instanceof DateAndTimeAnalyzer) {
                if (((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn));
                }
            }
        }

//        basicProfile.setDuplicateCnt(totalCnt - basicProfile.getDistinctCnt());
//        basicProfile.setFrquentMaxVal(findFrequentMaxVal(vfModelList));
//        basicProfile.setFrquentMinVal(findFrequentMinVal(vfModelList));

        // add Results to ProfileTarget instance
//        this.profileTargetService.addProfileDetail(basicProfile, tableName);
//        this.profileTargetService.addProfileValue(vfModelList, tableName);

        profileColumnResult.getProfiles().put("basic_profile", basicProfile);
        profileColumnResult.getProfiles().put("number_profile", numberProfile);
        profileColumnResult.getProfiles().put("string_profile", stringProfile);
//        profileColumnResult.getProfiles().put("date_profile", dateProfile);
    }

//    /**
//     * Value Frequency 분석값 중 최다 빈도값을 찾는 메소드
//     *
//     * @param profileValues Value Frequency 분석에 대한 Resultset
//     * @return 최다 빈도수 데이터 반환
//     */
//    private String findFrequentMaxVal(List<ProfileValue> profileValues) {
//        int max = 0;
//        int index = -1;
//
//        if (profileValues.size() == 0 || (profileValues.size() == 1 && StringUtils.isBlank(profileValues.get(0).getColumnGroupVal())))
//            return null;
//
//        for (int i = 0; i < profileValues.size(); i++) {
//            String val = profileValues.get(i).getColumnGroupVal();
//            if (StringUtils.isBlank(val)) {
//                continue;
//            }
//
//            if (max < profileValues.get(i).getColumnGroupCount()) {
//                max = profileValues.get(i).getColumnGroupCount();
//                index = i;
//            }
//        }
//
//        return profileValues.get(index).getColumnGroupVal();
//    }
//
//    /**
//     * Value Frequency 분석값 중 최소 빈도값을 찾는 메소드
//     *
//     * @param profileValues Value Frequency 분석에 대한 Resultset
//     * @return 최다 빈도수 데이터 반환
//     */
//    private String findFrequentMinVal(List<ProfileValue> profileValues) {
//        int min = Integer.MAX_VALUE;
//        int index = -1;
//
//        if (profileValues.size() == 0 || (profileValues.size() == 1 && StringUtils.isBlank(profileValues.get(0).getColumnGroupVal())))
//            return null;
//
//        for (int i = 0; i < profileValues.size(); i++) {
//            String val = profileValues.get(i).getColumnGroupVal();
//            if (StringUtils.isBlank(val)) {
//                continue;
//            }
//
//            if (min > profileValues.get(i).getColumnGroupCount()) {
//                min = profileValues.get(i).getColumnGroupCount();
//                index = i;
//            }
//        }
//        return profileValues.get(index).getColumnGroupVal();
//    }

//    private boolean isRemovalColumn(String tableName, String columnName, Map<String, Set<String>> removals) {
//        if (!removals.containsKey(tableName)) {
//            return false;
//        }
//
//        Set<String> removalColumns = removals.get(tableName);
//        return removalColumns.contains(columnName);
//    }
}