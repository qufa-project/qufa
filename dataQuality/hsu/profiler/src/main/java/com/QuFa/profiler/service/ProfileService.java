package com.QuFa.profiler.service;


import com.QuFa.profiler.config.ActiveProfileProperty;
import com.QuFa.profiler.model.profile.*;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.datacleaner.api.AnalyzerResult;
import org.datacleaner.api.InputColumn;
import org.datacleaner.beans.*;
import org.datacleaner.beans.valuedist.MonthDistributionAnalyzer;
import org.datacleaner.beans.valuedist.ValueDistributionAnalyzer;
import org.datacleaner.beans.valuedist.ValueDistributionAnalyzerResult;
import org.datacleaner.beans.valuedist.YearDistributionAnalyzer;
import org.datacleaner.beans.valuematch.ValueMatchAnalyzer;
import org.datacleaner.beans.valuematch.ValueMatchAnalyzerResult;
import org.datacleaner.components.convert.ConvertToDateTransformer;
import org.datacleaner.components.convert.ConvertToNumberTransformer;
import org.datacleaner.job.AnalysisJob;
import org.datacleaner.job.builder.AnalysisJobBuilder;
import org.datacleaner.job.builder.AnalyzerComponentBuilder;
import org.datacleaner.job.builder.TransformerComponentBuilder;
import org.datacleaner.job.runner.AnalysisResultFuture;
import org.datacleaner.job.runner.AnalysisRunner;
import org.datacleaner.job.runner.AnalysisRunnerImpl;
import org.datacleaner.result.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    public String typeDetection(String filename, String columnName) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader("./src/main/resources/targetfiles/" + filename + ".csv"));
        List<String> header = Arrays.asList(csvReader.readNext().clone());
        String[] nextLine;
        List<String> rowValues = new ArrayList<>();
        Map<String, String> rowType = new HashMap<>();
        while ((nextLine = csvReader.readNext()) != null) {
            if(nextLine.length == header.size())
                rowValues.add(nextLine[header.indexOf(columnName)]);
        }

        int i = 0;

        for(String rowVal : rowValues) {
            if (rowVal.trim().equals("")) continue;
            List<DateFormat> dfs = new ArrayList<>();
            dfs.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            dfs.add(new SimpleDateFormat("yyyy-MM-dd"));
            dfs.add(new SimpleDateFormat("MM/dd/yyyy"));
            dfs.add(new SimpleDateFormat("HH:mm:ss"));
            Date date;
            for(DateFormat df : dfs) {
                try {
                    date = df.parse(rowVal);
                    rowType.put(rowVal, "DATE");
                    break;
                } catch (ParseException e) {
                    try {
                        Double.parseDouble(rowVal);
                        rowType.put(rowVal, "NUMBER");
                    } catch (NumberFormatException ex) {
                        rowType.put(rowVal, "STRING");
                    }

                }
            }
            if(i<=99)
                System.out.println(rowVal + " : " + rowType.get(rowVal));
            i++;
        }
        i = 0;
        int n;
        Map<String, Integer> vdTypes = new HashMap<>();
        vdTypes.put("STRING", 0);
        vdTypes.put("NUMBER", 0);
        vdTypes.put("DATE", 0);
        for(String t : rowType.values()){
            if(i >= 99) break;
            n = vdTypes.get(t) + 1;
            vdTypes.put(t, n);
            i++;
        }

        int maxVal =  Collections.max(vdTypes.values());

        for(String key : vdTypes.keySet()){
            if(vdTypes.get(key).equals(maxVal))
                return key;
        }

        return "STRING";
    }

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
            try {
                profileColumnResult.setColumn_type(typeDetection(filename, columnName));
            } catch(IOException e){
                e.printStackTrace();
            }
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
        String type = profileColumnResult.getColumn_type();
        //DataStoreService.setDataStore2(activeProfileProperty.getActive());
        DataStoreService.setDataStore("CSVDS");
        AnalysisJobBuilder builder = DataStoreService.getBuilder();
        builder.addSourceColumns(columnName);
        InputColumn<?> targetInputColumn = builder.getSourceColumnByName(columnName);

        //Convert column data type
        if (type.equals("NUMBER")){
            TransformerComponentBuilder<ConvertToNumberTransformer> ctn = builder.addTransformer(ConvertToNumberTransformer.class);
            ctn.addInputColumns(targetInputColumn);
            targetInputColumn = ctn.getOutput()[0];

            AnalyzerComponentBuilder<ValueMatchAnalyzer> vma = builder.addAnalyzer(ValueMatchAnalyzer.class);
            vma.addInputColumn(targetInputColumn);
            String[] expected_values = { "0" };
            vma.setConfiguredProperty("Expected values", expected_values);

        } else if (type.equals("DATE")){
            TransformerComponentBuilder<ConvertToDateTransformer> ctd = builder.addTransformer(ConvertToDateTransformer.class);
            ctd.setConfiguredProperty("Time zone","Asia/Seoul");
//            Date DateNullReplacement = new Date();
//            ctd.setConfiguredProperty("Null replacement", DateNullReplacement);
            ctd.addInputColumns(targetInputColumn);
            targetInputColumn = ctd.getOutput()[0];
        }
        System.out.println(targetInputColumn); // null로 나옴


        // analyzer config
        // val
        AnalyzerComponentBuilder<ValueDistributionAnalyzer> valDistAnalyzer = builder.addAnalyzer(ValueDistributionAnalyzer.class);
        valDistAnalyzer.addInputColumns(targetInputColumn);
        valDistAnalyzer.setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_UNIQUE_VALUES, true);
        valDistAnalyzer.setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_DRILL_DOWN_INFORMATION, true);


        //Column data type과 매핑되는 analyzer config
        if (type.equals("STRING")) {
            AnalyzerComponentBuilder<StringAnalyzer> stringAnalyzer = builder.addAnalyzer(StringAnalyzer.class);
            stringAnalyzer.addInputColumn(targetInputColumn);
        } else if (type.equals("NUMBER")) {
            AnalyzerComponentBuilder<NumberAnalyzer> numberAnalyzer = builder.addAnalyzer(NumberAnalyzer.class);
            numberAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            numberAnalyzer.addInputColumn(targetInputColumn);
        } else if (type.equals("DATE")) {
            AnalyzerComponentBuilder<DateAndTimeAnalyzer> dateAnalyzer = builder.addAnalyzer(DateAndTimeAnalyzer.class);
            dateAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            dateAnalyzer.addInputColumn(targetInputColumn);

            AnalyzerComponentBuilder<MonthDistributionAnalyzer> monthDistAnalyzer = builder.addAnalyzer(MonthDistributionAnalyzer.class);
            monthDistAnalyzer.addInputColumns(targetInputColumn);

            AnalyzerComponentBuilder<YearDistributionAnalyzer> yearDistAnalyzer = builder.addAnalyzer(YearDistributionAnalyzer.class);
            yearDistAnalyzer.addInputColumns(targetInputColumn);
        }
//        // str
//        if (targetInputColumn.getDataType().getTypeName().equals("java.lang.String")) {
//            AnalyzerComponentBuilder<StringAnalyzer> stringAnalyzer = builder.addAnalyzer(StringAnalyzer.class);
//            stringAnalyzer.addInputColumn(targetInputColumn);
//
//            // stringAnalyzer.setConfiguredProperty("Descriptive statistics", false);
//        } else if (targetInputColumn.getDataType().getTypeName().equals("java.util.Date") ||
//                targetInputColumn.getDataType().getTypeName().equals("java.time.Instant")) {
//            AnalyzerComponentBuilder<DateAndTimeAnalyzer> numberAnalyzer = builder.addAnalyzer(DateAndTimeAnalyzer.class);
//            numberAnalyzer.addInputColumn(targetInputColumn);
//        } else if (targetInputColumn.getDataType().getTypeName().equals("java.lang.Boolean")) {
//            // do something
//        } else if (targetInputColumn.getDataType().getTypeName().equals("java.lang.Number") ||
//                targetInputColumn.getDataType().getTypeName().equals("java.lang.Integer") ||
//                targetInputColumn.getDataType().getTypeName().equals("java.lang.Doulbe") ||
//                targetInputColumn.getDataType().getTypeName().equals("java.lang.Float") ||
//                targetInputColumn.getDataType().getTypeName().equals("java.lang.BigInteger")) {
//            AnalyzerComponentBuilder<NumberAnalyzer> numberAnalyzer = builder.addAnalyzer(NumberAnalyzer.class);
//            numberAnalyzer.setConfiguredProperty("Descriptive statistics", true);
//            numberAnalyzer.addInputColumn(targetInputColumn);
//        }

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

    private String SerialNumberToDate(Number num){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date((Integer)num * 86400000L));
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
        Map<Object, Object> monthList = new HashMap<>();
        Map<Object, Object> yearList = new HashMap<>();

        basicProfile.setNull_cnt(0);
        stringProfile.setBlank_cnt(0);

        for (AnalyzerResult result : results) {
            //System.out.println(result.getClass());
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
                            if(vfChild.getValue() != null)
                                vfModelList.put(vfChild.getValue(),vfChild.getCount());
                        }
                    } else {
                        if(vf.getValue() != null)
                            vfModelList.put(vf.getValue(),vf.getCount());
                    }
                }

                totalCnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();
            }
            if (profileColumnResult.getColumn_type().equals("DATE") && result instanceof CrosstabResult &&
                    !(result instanceof DateAndTimeAnalyzerResult)){
                CrosstabDimension ctr = ((CrosstabResult) result).getCrosstab().getDimension(1);
                String dimension = "";

                if(ctr.getName().equals("Month"))
                    dimension = "Month";
                else
                    dimension = "Year";

                for(String category : ctr.getCategories()){
                    Object value = ((CrosstabResult) result).getCrosstab().where("Column",
                            targetInputColumn.getName()).where(dimension, category)
                            .safeGet(null);
                    if(value == null)
                        value = 0;

                    if(dimension.equals("Month"))
                        monthList.put(category, value);
                    else
                        yearList.put(category, value);
                }
            }
        }
        //basicProfile.setValue_distribution(vfModelList);
        if(profileColumnResult.getColumn_type().equals("DATE")) {
            dateProfile.setMonth_distribution(monthList);
            dateProfile.setYear_distribution(yearList);
        }

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
            }

            if (result instanceof ValueMatchAnalyzerResult) {
                if (((ValueMatchAnalyzerResult) result).getCount("0") != null) {
                    numberProfile.setZero_cnt(((ValueMatchAnalyzerResult) result).getCount("0"));
                }
            }

            if (result instanceof DateAndTimeAnalyzerResult) {
                if (((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn));
                }
                if (((DateAndTimeAnalyzerResult) result).getHighestDate(targetInputColumn) != null) {
                    dateProfile.setHighest_date(SerialNumberToDate(((DateAndTimeAnalyzerResult) result).getHighestDate(targetInputColumn)));
                }
                if (((DateAndTimeAnalyzerResult) result).getLowestDate(targetInputColumn) != null) {
                    dateProfile.setLowest_date(SerialNumberToDate(((DateAndTimeAnalyzerResult) result).getLowestDate(targetInputColumn)));
                }
                if (((DateAndTimeAnalyzerResult) result).getMean(targetInputColumn) != null) {
                    dateProfile.setMean_date(SerialNumberToDate(((DateAndTimeAnalyzerResult) result).getMean(targetInputColumn)));
                }
                if (((DateAndTimeAnalyzerResult) result).getMedian(targetInputColumn) != null) {
                    dateProfile.setMedian_date(SerialNumberToDate(((DateAndTimeAnalyzerResult) result).getMedian(targetInputColumn)));
                }
                if (((DateAndTimeAnalyzerResult) result).getPercentile25(targetInputColumn) != null) {
                    dateProfile.setPercentile_25th(SerialNumberToDate(((DateAndTimeAnalyzerResult) result).getPercentile25(targetInputColumn)));
                }
                if (((DateAndTimeAnalyzerResult) result).getPercentile75(targetInputColumn) != null) {
                    dateProfile.setPercentile_75th(SerialNumberToDate(((DateAndTimeAnalyzerResult) result).getPercentile75(targetInputColumn)));
                }
                basicProfile.setValue_distribution(vfModelList);
            }
        }

//        basicProfile.setDuplicateCnt(totalCnt - basicProfile.getDistinctCnt());
//        basicProfile.setFrquentMaxVal(findFrequentMaxVal(vfModelList));
//        basicProfile.setFrquentMinVal(findFrequentMinVal(vfModelList));

        // add Results to ProfileTarget instance
//        this.profileTargetService.addProfileDetail(basicProfile, tableName);
//        this.profileTargetService.addProfileValue(vfModelList, tableName);

        profileColumnResult.getProfiles().put("basic_profile", basicProfile);
        if(profileColumnResult.getColumn_type().equals("NUMBER"))
            profileColumnResult.getProfiles().put("number_profile", numberProfile);
        if(profileColumnResult.getColumn_type().equals("STRING"))
            profileColumnResult.getProfiles().put("string_profile", stringProfile);
        if(profileColumnResult.getColumn_type().equals("DATE"))
            profileColumnResult.getProfiles().put("date_profile", dateProfile);
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