package com.QuFa.profiler.service;


import static com.QuFa.profiler.controller.exception.ErrorCode.BAD_JSON_REQUEST;
import static com.QuFa.profiler.controller.exception.ErrorCode.FILE_NOT_FOUND;
import static com.QuFa.profiler.controller.exception.ErrorCode.INTERNAL_ERROR;

import com.QuFa.profiler.config.ActiveProfileProperty;
import com.QuFa.profiler.controller.exception.CustomException;
import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.request.DependencyAnalysis;
import com.QuFa.profiler.model.request.Profiles;
import com.QuFa.profiler.model.response.BasicProfile;
import com.QuFa.profiler.model.response.DateProfile;
import com.QuFa.profiler.model.response.DependencyAnalysisResult;
import com.QuFa.profiler.model.response.NumberProfile;
import com.QuFa.profiler.model.response.ProfileColumnResult;
import com.QuFa.profiler.model.response.ProfileTableResult;
import com.QuFa.profiler.model.response.StringProfile;
import com.QuFa.profiler.model.response.VdModel;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.datacleaner.api.AnalyzerResult;
import org.datacleaner.api.InputColumn;
import org.datacleaner.beans.DateAndTimeAnalyzer;
import org.datacleaner.beans.DateAndTimeAnalyzerResult;
import org.datacleaner.beans.NumberAnalyzer;
import org.datacleaner.beans.NumberAnalyzerResult;
import org.datacleaner.beans.StringAnalyzer;
import org.datacleaner.beans.StringAnalyzerResult;
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
import org.datacleaner.result.AnalysisResult;
import org.datacleaner.result.CrosstabDimension;
import org.datacleaner.result.CrosstabResult;
import org.datacleaner.result.ValueFrequency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 데이터 프로파일 작업을 수행하는 서비스
 */
@RequiredArgsConstructor
@Service
public class ProfileService {

    /**
     *
     */
    private final ActiveProfileProperty activeProfileProperty;
    private ProfileTableResult profileTableResult = new ProfileTableResult();
    private ProfileColumnResult profileColumnResult = new ProfileColumnResult();

    private List<DependencyAnalysis> dependencyAnalyses = null;

    /* 컬럼별 프로파일 */
    private Profiles profiles = null;
    Map<String, List<Object>> profileTypes = null;
    private Map<Object, List<String>> requestColumnAndType;
    private HashSet<Object> requestColumnSet;
    private boolean headerExist;

    //TODO: dataStoreService 객체 생성해서 사용하기
    @Autowired
    private final DataStoreService dataStoreService;

    private List<String> header;

    private long t = 0;
    private long totalDetact = 0; // 총 판단하는 데이터 개수
    private int cntDetactType = 0; // row당 판단하는 최대 데이터 개수
    private int detactingtime = 10000; // 전체 타입 판단 시간 설정 (1000 -> 1초)
    private boolean key_analysis; // 후보키를 찾을지 말지 판단

    private ArrayList<Object> key_analysis_results; // 후보키 컬럼을 담는 배열


    String targetFolderPath;
    String filePath;

    /**
     * <현 문제점> columnNames 에 대해 for 문 돌면서 호출됨. 해당 컬럼의 타입이 무엇인지 판단 근데 typecheck 는 모든 레코드에 대해서 하는데
     * (5000개~) 아래 컬럼 타입 max 구할땐 100개만 함. ?
     * <p>
     * <해결 방법> - file의 크기에 따라 예를들어 100개 이하면 전수조사, - 100개 이상이면 샘플링해서 판단.
     * <p>
     * - 예를들어 200개 -> 50%인 100개, 300개 -> 33%인 100개 ... - 목표는 모든 csv파일에 대해 동일한 시간이 나오도록 - 샘플링은 random
     * - 시간이 많이 걸리는 부분이 어딘지 한번 체크해볼것!
     */
    public String typeDetection(String path, String columnName) throws IOException {

        long beforeTime1 = System.currentTimeMillis();
        long beforeTime = System.currentTimeMillis();

        CSVReader csvReader = new CSVReader(new FileReader(path));
        String[] nextLine;
        List<String> rowValues = new ArrayList<>();
        Map<String, String> rowType = new HashMap<>();
        while ((nextLine = csvReader.readNext()) != null) {
            if (nextLine.length == header.size()) {
                rowValues.add(nextLine[header.indexOf(columnName)]);
            }
        }
        long afterTime1 = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
        long secDiffTime1 = (afterTime1 - beforeTime1); //두 시간에 차 계산
        System.out.println("시간차이(m) : "+secDiffTime1);

        if (cntDetactType == 0){
            cntDetactType = (int)(detactingtime - (rowValues.size()*0.005* profileTableResult.getDataset_column_cnt()))/ profileTableResult.getDataset_column_cnt() * 20;
            if (cntDetactType<1000) {
                cntDetactType = 1000;
            }
        }
        System.out.println("타입추출제한개수 : "+cntDetactType);
        System.out.println("row size : "+rowValues.size());

        if (rowValues.size() > cntDetactType) {
            Collections.shuffle(rowValues);
            rowValues = rowValues.subList(0, cntDetactType);
        }
        totalDetact += rowValues.size()-1;

        int i = 0;

        for (String rowVal : rowValues) {
            if (rowVal.trim().equals("")) {
                continue;
            }
            List<DateFormat> dfs = new ArrayList<>();
            SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sd.setLenient(false);
            dfs.add(sd);

            sd = new SimpleDateFormat("yyyy-MM-dd");
            sd.setLenient(false);
            dfs.add(sd);

            sd = new SimpleDateFormat("MM/dd/yyyy");
            sd.setLenient(false);
            dfs.add(sd);

            sd = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            sd.setLenient(false);
            dfs.add(sd);

            sd = new SimpleDateFormat("dd/MM/yy HH:mm");
            sd.setLenient(false);
            dfs.add(sd);

            Date date;
            for (DateFormat df : dfs) {
                try {
                    date = df.parse(rowVal);
                    rowType.put(rowVal, "date");
                    break;
                } catch (ParseException e) {
                    try {
                        Double.parseDouble(rowVal);
                        rowType.put(rowVal, "number");
                    } catch (NumberFormatException ex) {
                        rowType.put(rowVal, "string");
                    }

                }
            }
        }

        /*
         * 해당 컬럼의 100개의 값에 대한 type 을 검사해서
         * 그중 가장 많이 나온 값으로 리턴
         * date, number, number, string -> type: number
         *
         * ->> 고쳐야 할 부분f
         */
        i = 0;
        int n;

        String typeValue;

        boolean dateType;
        boolean numberType;
        dateType = true;
        numberType = true;
        for (String t : rowType.values()) {
            if (t.equals("number")) {
                dateType = false;
            } else if (t.equals("date")) {
                numberType= false;
            } else if (t.equals("string")) {
                dateType = false;
                numberType= false;
            }
        }

        if(dateType) {
            typeValue = "date";
        } else if (numberType) {
            typeValue = "number";
        } else {
            typeValue = "string";
        }

        long afterTime = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
        long secDiffTime = (afterTime - beforeTime); //두 시간에 차 계산
        System.out.println("시간차이(m) : "+secDiffTime);
        t = t+secDiffTime;
        System.out.println("타입구분시간 : "+t);
        System.out.println("타입 : "+typeValue);


        return typeValue;
    }

    public ProfileTableResult profileLocalCSV(Local local) {
        /* 컬럼별 프로파일  */
        profiles = local.getProfiles();
        if (profiles != null)  {
            if (profiles.getTypes() != null) profileTypes = profiles.getTypes();
            if (profiles.getDependencied_analysis() != null ) dependencyAnalyses = profiles.getDependencied_analysis();
        }

        requestColumnAndType = new HashMap<>();
        requestColumnSet = new HashSet<>();
        key_analysis_results = new ArrayList<>();
        key_analysis = false;

        /* 후보키 요청이 왔는지 판단 */
        assert profiles != null;
        if (profiles.isKey_analysis()) {
            key_analysis = true;
        }

        t = 0;
        cntDetactType = 0;
        totalDetact = 0;

        // 운영체제별로 targetfiles 다르게 설정
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            targetFolderPath = "C://Temp/targetfiles/";
            System.out.println("targetFolderPath = " + targetFolderPath);
        } else if (os.contains("linux")) {
            targetFolderPath = "~/tmp";
        }

        if (local.getSource().getType().equals("path")) {
            String fileName = getFileName(local.getSource().getType(), local.getSource().getPath());
            String path = local.getSource().getPath();
            headerExist = local.isHeader();
            try {
                // header는 처음에 한번만 구하고, ProfileService 객체에 필드로 정의.
                header = getHeader(path, local.isHeader(), fileName);

            } catch (Exception e) {
                e.printStackTrace();
//                throw new CustomException(INTERNAL_ERROR);
            }
            try {
                profileTableResult = profileLocalColumns("path", path, header, local.isHeader());
            } catch (Exception e) {
                e.printStackTrace();
//                if (profileTypes != null){
//                    throw new CustomException(BAD_JSON_REQUEST);
//                }
            }
            System.out.println("파일 크기 : "+profileTableResult.getDataset_size());
            System.out.println("타입 판단 제한 개수 : "+cntDetactType);
            System.out.println("설정한 타입구분시간 : "+detactingtime);
            System.out.println("실제 타입구분시간 : "+t);
            System.out.println("총 타입판단개수 : "+totalDetact);
            System.out.println("총 데이터 수 : "+profileTableResult.getDataset_column_cnt()*profileTableResult.getDataset_row_cnt());
            System.out.println("데이터당 걸린 시간 : "+(double)t/(double)totalDetact);

        } else if (local.getSource().getType().equals("url")) {
            String fileName = getFileName(local.getSource().getType(), local.getSource().getUrl());
            String url = local.getSource().getUrl();
            try {
                dataStoreService.storeUrlFile(url, local.isHeader()); // url의 파일을 로컬에 복사
                URL file = new URL(url);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.openStream()));

                // header는 처음에 한번만 구하고, ProfileService 객체에 필드로 정의.
                if (local.isHeader()) {
                    header = Arrays.asList(reader.readLine().split(","));
                }
                else {
                    int recordsCount = Arrays.asList(reader.readLine().split(",")).size();
                    header = new ArrayList<>();
                    for (int i = 1; i < recordsCount + 1; i++) {
                        header.add(fileName + "_" + i);
                    }
                    System.out.println("!!!!!" + header);
                }

                profileTableResult = profileLocalColumns("url", url, header, local.isHeader());

                reader.close();

            } catch (Exception e) {
                e.printStackTrace();
//                if (profileTypes != null){
//                    throw new CustomException(BAD_JSON_REQUEST);
//                } else{
//                    System.out.println("profileLocalCSV() returned: " + profileTableResult);
//                    throw new CustomException(FILE_NOT_FOUND);
//                }
            }

            System.out.println("파일 크기 : "+profileTableResult.getDataset_size());
            System.out.println("타입 판단 제한 개수 : "+cntDetactType);
            System.out.println("설정한 타입구분시간 : "+detactingtime);
            System.out.println("실제 타입구분시간 : "+t);
            System.out.println("총 타입판단개수 : "+totalDetact);
            System.out.println("데이터당 걸린 시간 : "+(double)t/(double)totalDetact);
        } else {

        }

        /* dependency analysis result */
        if (dependencyAnalyses != null) {
            System.out.println("dependencyAnalyses = " + dependencyAnalyses.toString());
            List<DependencyAnalysisResult> dependencyAnalysisResults = new ArrayList<>();
            dependencyAnalyses.forEach(dependencyAnalysis -> {
                try {
                    dependencyAnalysisResults.add(dependencyAnalysis(dependencyAnalysis));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            profileTableResult.setDependency_analysis_results(dependencyAnalysisResults);
        }
        return profileTableResult;
    }

    public ProfileTableResult profileLocalColumns(String type, String path, List<String> columnNames,
            Boolean isHeader) {
//        profileTableResult = new ProfileTableResult();
        System.out.println(path);

        String filename = getFileName(type, path);
        System.out.println("filename:" + filename);

        //CsvDatastore
        filePath = path; // 헤더가 있으면 original path
        if (type.equals("url") || !isHeader) { // url이거나, 헤더가 없으면 targetfiles~
            filePath = targetFolderPath + filename + ".csv";
        }
        //TODO: fileName 파라미터 추가
        dataStoreService.createLocalDataStore(filePath);

        profileTableResult.setDataset_name(filename);
        profileTableResult.setDataset_type("csv");

        File file = new File(path);
        profileTableResult.setDataset_size((int) file.length());
        file = null;

        profileTableResult.setDataset_column_cnt(columnNames.size());


        /* 컬럼별 프로파일  */
        if (profileTypes != null) { // profiles가 있으면 칼럼 타입 판단 X
            profileTypes.forEach((key, valueList) -> {
                valueList.forEach(value -> {
                    if (headerExist) { // "header": true
                        if (value.getClass().getName().equals("java.lang.String")) {
                            for (String columnName : columnNames) {
                                if (Objects.equals(String.valueOf(value), columnName)) {
                                    value = columnNames.indexOf(String.valueOf(value)) + 1;
                                }
                            }
                        } else {
                            throw new CustomException(BAD_JSON_REQUEST);
                        }
                    } else { // "header": false
                        if (!value.getClass().getName().equals("java.lang.Integer")) {
                            throw new CustomException(BAD_JSON_REQUEST);
                        }
                    }
                    List<String> keyList;
                    if (requestColumnAndType.containsKey(value)) {
                        keyList = requestColumnAndType.get(value);
                    } else {
                        keyList = new ArrayList<>();
                    }

                    keyList.add(key);
                    requestColumnAndType.put(value, keyList);
                    requestColumnSet.add(value);
                });
            });

            System.out.println("requestColumnAndType= "
                                       + requestColumnAndType); // {1=[basic, number], 2=[basic, string]}
            System.out.println("requestColumnSet= " + requestColumnSet); // [1, 2]

            for (String columnName : columnNames) {
                if (columnName.isEmpty()) continue;

                int index = columnNames.indexOf(columnName) + 1;
                if (requestColumnSet.contains(index)) {
                    profileColumnResult = new ProfileColumnResult();
                    profileColumnResult.setColumn_id(columnNames.indexOf(columnName) + 1);
                    profileColumnResult.setColumn_name(columnName);

                    List<String> typeList = requestColumnAndType.get(index);
                    String valueType = "string";
                    for (String x : typeList) {
                        if (x != "basic") {
                            valueType = x;
                        }
                    }
                    profileColumnResult.setColumn_type(valueType);

                    this.profileSingleColumn(filename, columnName);
                    profileTableResult.getSingle_column_results().add(profileColumnResult);
                }
            }
        } else { // profiles가 없으면 칼럼 타입 판단 O
            for (String columnName : columnNames) {
                if (columnName.isEmpty()) continue;

                profileColumnResult = new ProfileColumnResult();
                profileColumnResult.setColumn_id(columnNames.indexOf(columnName) + 1);
                profileColumnResult.setColumn_name(columnName);
                try {
                    profileColumnResult.setColumn_type(typeDetection(path, columnName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.profileSingleColumn(filename, columnName);
                profileTableResult.getSingle_column_results().add(profileColumnResult);
            }
        }
        return profileTableResult;
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

        //TODO: dataStoreService 객체 생성해서 사용하기
        dataStoreService.setDataStore("CSVDS");
        //TODO: dataStoreService 객체 생성해서 사용하기
        AnalysisJobBuilder builder = dataStoreService.getBuilder();
        builder.addSourceColumns(columnName);
        InputColumn<?> targetInputColumn = builder.getSourceColumnByName(columnName);
        InputColumn<?> dateTargetInputColumn = null;

        //Convert column data type
        if (type.equals("number")) {
            TransformerComponentBuilder<ConvertToNumberTransformer> ctn = builder
                    .addTransformer(ConvertToNumberTransformer.class);
            ctn.addInputColumns(targetInputColumn);
            targetInputColumn = ctn.getOutput()[0];

            AnalyzerComponentBuilder<ValueMatchAnalyzer> vma = builder
                    .addAnalyzer(ValueMatchAnalyzer.class);
            vma.addInputColumn(targetInputColumn);
            String[] expected_values = {"0"};
            vma.setConfiguredProperty("Expected values", expected_values);

        } else if (type.equals("date")) {
            TransformerComponentBuilder<ConvertToDateTransformer> ctd = builder
                    .addTransformer(ConvertToDateTransformer.class);
            ctd.setConfiguredProperty("Time zone", "Asia/Seoul");
            ctd.addInputColumns(targetInputColumn);
            dateTargetInputColumn = targetInputColumn;
            targetInputColumn = ctd.getOutput()[0];
        }
        System.out.println(targetInputColumn); // null로 나옴

        // analyzer config
        // val
        AnalyzerComponentBuilder<ValueDistributionAnalyzer> valDistAnalyzer = builder
                .addAnalyzer(ValueDistributionAnalyzer.class);
        if (type.equals("date")) {
            valDistAnalyzer.addInputColumns(dateTargetInputColumn);
        } else {
            valDistAnalyzer.addInputColumns(targetInputColumn);
        }
        valDistAnalyzer
                .setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_UNIQUE_VALUES,
                        true);
        valDistAnalyzer.setConfiguredProperty(
                ValueDistributionAnalyzer.PROPERTY_RECORD_DRILL_DOWN_INFORMATION, true);

        //Column data type과 매핑되는 analyzer config
        if (type.equals("string")) {
            AnalyzerComponentBuilder<StringAnalyzer> stringAnalyzer = builder
                    .addAnalyzer(StringAnalyzer.class);
            stringAnalyzer.addInputColumn(targetInputColumn);
        } else if (type.equals("number")) {
            AnalyzerComponentBuilder<NumberAnalyzer> numberAnalyzer = builder
                    .addAnalyzer(NumberAnalyzer.class);
            numberAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            numberAnalyzer.addInputColumn(targetInputColumn);
        } else if (type.equals("date")) {
            AnalyzerComponentBuilder<DateAndTimeAnalyzer> dateAnalyzer = builder
                    .addAnalyzer(DateAndTimeAnalyzer.class);
            dateAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            dateAnalyzer.addInputColumn(targetInputColumn);

            AnalyzerComponentBuilder<MonthDistributionAnalyzer> monthDistAnalyzer = builder
                    .addAnalyzer(MonthDistributionAnalyzer.class);
            monthDistAnalyzer.addInputColumns(targetInputColumn);

            AnalyzerComponentBuilder<YearDistributionAnalyzer> yearDistAnalyzer = builder
                    .addAnalyzer(YearDistributionAnalyzer.class);
            yearDistAnalyzer.addInputColumns(targetInputColumn);
        }

        // job build run & result
        AnalysisJob analysisJob = builder.toAnalysisJob();
        //TODO: dataStoreService 객체 생성해서 사용하기
        AnalysisRunner runner = new AnalysisRunnerImpl(dataStoreService.getConfiguration());
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

    private String SerialNumberToDate(long num) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(num * 86400000L));
    }

    private Map<Object, Object> getTop100(Map<Object, Object> map) {
        Map<Object, Object> top100 = new HashMap<>();

        return top100;
    }

    private Map<Object, Object> valueSortByDesc(Map<Object, Object> map) {
        List<Map.Entry<Object, Object>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<Integer>) ((Map.Entry<Integer, Integer>) (o2)).getValue())
                        .compareTo(((Map.Entry<Integer, Integer>) (o1)).getValue());
            }
        });
        Map<Object, Object> resultMap = new LinkedHashMap<>();
        for (Iterator<Map.Entry<Object, Object>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
            resultMap.put(entry.getKey(), entry.getValue());
        }

        return resultMap;
    }

    private List<Map<Object, Object>> monthSort(Map<Object, Object> map) {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August",
                "September", "October", "November", "December"};
        List<Map<Object, Object>> resultList = new ArrayList<>();

        for (String month : months) {
            resultList.add(Map.of(month, map.get(month)));
        }

        return resultList;
    }

    private Map<Object, Object> numberKeySortByAsc(Map<Object, Object> map, String type) {
        Object[] keyArray = map.keySet().toArray();
        Map<Object, Object> resultMap = new LinkedHashMap<>();

        if (type.equals("int")) {
            int[] intArray = new int[keyArray.length];
            for (int i = 0; i < keyArray.length; i++) {
                intArray[i] = Integer.parseInt(keyArray[i].toString());
            }

            Arrays.sort(intArray);

            for (int i : intArray) {
                resultMap.put(i, map.get(Integer.toString(i)));
            }
        } else if (type.equals("double")) {
            double[] doubleArray = new double[keyArray.length];
            for (int i = 0; i < keyArray.length; i++) {
                doubleArray[i] = Double.parseDouble(keyArray[i].toString());
            }

            Arrays.sort(doubleArray);

            for (double d : doubleArray) {
                resultMap.put(Double.toString(d), map.get(Double.toString(d)));
            }
        }

        return resultMap;
    }

    private List<Map<Object, Object>> getRange(Map<Object, Object> vfModelList) {
        List<Map<Object, Object>> range = new ArrayList<>();
        Map<Object, Object> LastMapInRange = new HashMap<>();
        Map<Object, Object> ascList;
        Object[] keyArray = vfModelList.keySet().toArray();

        Map<Object, Object> vfModelList_copy = new LinkedHashMap<>(vfModelList);

        for (Object o : vfModelList.keySet()) {
            double d = Double.parseDouble(o.toString());
            if (!((Double.toString(d)).equals(o.toString()))) {
                vfModelList_copy.put(Double.toString(d), vfModelList_copy.remove(o));
            }
        }

        boolean isDouble = false;
        for (int i = 0; i < 100; i++) {
            if (keyArray[i].toString().contains(".")) {
                isDouble = true;
                break;
            }
        }

        if (isDouble) {
            ascList = numberKeySortByAsc(vfModelList_copy, "double");
        } else {
            ascList = numberKeySortByAsc(vfModelList, "int");
        }

        Object[] objectKeyArray = ascList.keySet().toArray();
        String[] stringKeyArray = new String[objectKeyArray.length];
        for (int i = 0; i < stringKeyArray.length; i++) {
            stringKeyArray[i] = objectKeyArray[i].toString();
        }
        //String[] stringKeyArray = Arrays.copyOf(objectKeyArray,objectKeyArray.length,String[].class);

        int valueSum = 0;

        if (isDouble) { //실수
            BigDecimal BDmin = new BigDecimal(stringKeyArray[0]);
            BigDecimal BDmax = new BigDecimal(stringKeyArray[stringKeyArray.length - 1]);
            BigDecimal BigDist = (BDmax.subtract(BDmin))
                    .divide(BigDecimal.valueOf(10), 100, RoundingMode.HALF_EVEN);
            BigDist = BigDecimal.valueOf(BigDist.doubleValue());

            for (String s : stringKeyArray) {
                BigDecimal b;

                try {
                    b = new BigDecimal(s);
                } catch (Exception e) {
                    System.out.println("[BigDecimal except]:" + s);
                    e.printStackTrace();
                    continue;
                }

                if ((b.compareTo(BDmin.add(BigDist))) < 0) {
                    try {
                        valueSum += Integer.parseInt(ascList.get(s).toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (stringKeyArray[stringKeyArray.length - 1].equals(s)) {
                    valueSum += Integer.parseInt(ascList.get(s).toString());
                    range.add(Map.of(BDmin.toString(), valueSum));
                } else {
                    range.add(Map.of(BDmin.toString(), valueSum));
                    BDmin = BDmin.add(BigDist);
                    valueSum = Integer.parseInt(ascList.get(s).toString());
                }
            }

            LastMapInRange.put(BDmax.toString(), null);
            range.add(LastMapInRange);
        } else { //정수
            int Imin = Integer.parseInt(stringKeyArray[0]);
            int Imax = Integer.parseInt(stringKeyArray[stringKeyArray.length - 1]);
            int dist = (Imax - Imin) / 10;
            int remains = (Imax - Imin) % 10;
            int isRemain;

            for (String s : stringKeyArray) {
                int i;

                try {
                    i = Integer.parseInt(s);
                } catch (Exception e) {
                    System.out.println("[integer parse except]:" + s);
                    e.printStackTrace();
                    continue;
                }

                if (remains > 0) {
                    isRemain = 1;
                } else {
                    isRemain = 0;
                }

                if (i < (Imin + dist + isRemain)) {
                    valueSum += Integer.parseInt(vfModelList.get(s).toString());
                } else if (stringKeyArray[stringKeyArray.length - 1].equals(s)) {
                    valueSum += Integer.parseInt(vfModelList.get(s).toString());
                    range.add(Map.of(Integer.toString(Imin), valueSum));
                } else {
                    range.add(Map.of(Integer.toString(Imin), valueSum));
                    Imin += dist;
                    valueSum = Integer.parseInt(vfModelList.get(s).toString());
                    remains--;
                }
            }

            LastMapInRange.put(Imax, null);
            range.add(LastMapInRange);
        }

        return range;
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
        VdModel vdModel = new VdModel();
        List<Map<Object, Object>> vdValueList = new ArrayList<>();
        List<Map<Object, Object>> vdMonthList = new ArrayList<>();
        List<Map<Object, Object>> vdYearList = new ArrayList<>();

        basicProfile.setNull_cnt(0);
        stringProfile.setBlank_cnt(0);

        for (AnalyzerResult result : results) {
            //System.out.println(result.getClass());
            if (result instanceof ValueDistributionAnalyzerResult) {
                if (((ValueDistributionAnalyzerResult) result).getNullCount() > 0) {
                    basicProfile
                            .setNull_cnt(((ValueDistributionAnalyzerResult) result).getNullCount());
                }

                int distinct_cnt = ((ValueDistributionAnalyzerResult) result).getDistinctCount();
                int row_cnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();

                profileTableResult.setDataset_row_cnt(row_cnt);
                //TODO: if(header=yes) Table row cnt=row_cnt+1

                basicProfile.setDistinct_cnt(distinct_cnt);
                basicProfile.setRow_cnt(row_cnt);
                basicProfile
                        .setUnique_cnt(((ValueDistributionAnalyzerResult) result).getUniqueCount());
                basicProfile.setDistinctness((double) distinct_cnt / row_cnt);

                /* 후보키 : 널값이 존재하지 않고  Distinct Count/Row Count 가 1인 경우*/
                if (key_analysis) {
                    if(basicProfile.getNull_cnt() == 0 && basicProfile.getDistinctness() == 1) {
                        key_analysis_results.add(profileColumnResult.getColumn_id());
                    }
                }

                Collection<ValueFrequency> vfList = ((ValueDistributionAnalyzerResult) result)
                        .getValueCounts();
                for (ValueFrequency vf : vfList) {
                    if (vf.getChildren() != null) {
                        Collection<ValueFrequency> vfChildren = vf.getChildren();
                        for (ValueFrequency vfChild : vfChildren) {
                            if (vfChild.getValue() != null) {
                                vfModelList.put(vfChild.getValue(), vfChild.getCount());
                            }
                        }
                    } else {
                        if (vf.getValue() != null) {
                            vfModelList.put(vf.getValue(), vf.getCount());
                        }
                    }
                }

                vfModelList = valueSortByDesc(vfModelList);

                if (distinct_cnt > 100) {
                    vdModel.setType("top100");

                    Object[] keys = vfModelList.keySet().toArray();
                    for (int i = 0; i < 100; i++) {
                        vdValueList.add(Map.of(
                                keys[i], vfModelList.get(keys[i])
                        ));
                    }
                    vdModel.setValue(vdValueList);

                    if (profileColumnResult.getColumn_type().equals("number")) {
                        List<Map<Object, Object>> range = getRange(vfModelList);
                        vdModel.setRange(range);
                    } else {
                        vdModel.setRange("-");
                    }

                } else {
                    vdModel.setType("all");

                    Object[] keys = vfModelList.keySet().toArray();
                    for (Object key : keys) {
                        vdValueList.add(Map.of(
                                key, vfModelList.get(key)
                        ));
                    }
                    vdModel.setValue(vdValueList);

                    vdModel.setRange("-");
                }

                basicProfile.setValue_distribution(vdModel);
                totalCnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();
            }
            if (profileColumnResult.getColumn_type().equals("date")
                        && result instanceof CrosstabResult &&
                        !(result instanceof DateAndTimeAnalyzerResult)) {
                CrosstabDimension ctr = ((CrosstabResult) result).getCrosstab().getDimension(1);
                String dimension = "";

                if (ctr.getName().equals("Month")) {
                    dimension = "Month";
                } else {
                    dimension = "Year";
                }

                for (String category : ctr.getCategories()) {
                    Object value = ((CrosstabResult) result).getCrosstab().where("Column",
                                    targetInputColumn.getName()).where(dimension, category)
                            .safeGet(null);
                    if (value == null) {
                        value = 0;
                    }

                    if (dimension.equals("Month")) {
                        monthList.put(category, value);
                    } else {
                        yearList.put(category, value);
                    }
                }

                vdMonthList = monthSort(monthList);
                dateProfile.setMonth_distribution(vdMonthList);

                yearList = numberKeySortByAsc(yearList, "int");
                for (Object year : yearList.keySet().toArray()) {
                    vdYearList.add(Map.of(year, yearList.get(year)));
                }
                dateProfile.setYear_distribution(vdYearList);
            }
        }

        DecimalFormat form = new DecimalFormat("#.###");

        for (AnalyzerResult result : results) {
            if (result instanceof StringAnalyzerResult) {
                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) > 0
                            && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(
                            ((StringAnalyzerResult) result).getNullCount(targetInputColumn));
                }

                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) < totalCnt) {
                    stringProfile.setAvg_len(Double.parseDouble(form.format(
                            ((StringAnalyzerResult) result).getAvgChars(targetInputColumn))));
                    stringProfile.setMax_len(
                            ((StringAnalyzerResult) result).getMaxChars(targetInputColumn));
                    stringProfile.setMin_len(
                            ((StringAnalyzerResult) result).getMinChars(targetInputColumn));
                }
                stringProfile.setBlank_cnt(
                        ((StringAnalyzerResult) result).getBlankCount(targetInputColumn));
            }

            if (result instanceof NumberAnalyzerResult) {
                if (((NumberAnalyzerResult) result).getNullCount(targetInputColumn).intValue() > 0
                            && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(
                            ((NumberAnalyzerResult) result).getNullCount(targetInputColumn)
                                    .intValue());
                }

                if (((NumberAnalyzerResult) result).getLowestValue(targetInputColumn) != null) {
                    numberProfile.setMin((Double) ((NumberAnalyzerResult) result)
                            .getLowestValue(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getHighestValue(targetInputColumn) != null) {
                    numberProfile.setMax((Double) ((NumberAnalyzerResult) result)
                            .getHighestValue(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getSum(targetInputColumn) != null) {
                    numberProfile.setSum(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result).getSum(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getMean(targetInputColumn) != null) {
                    numberProfile.setMean(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result).getMean(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getMedian(targetInputColumn) != null) {
                    numberProfile.setMedian(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result).getMedian(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getStandardDeviation(targetInputColumn)
                            != null) {
                    numberProfile.setSd(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result)
                                    .getStandardDeviation(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getVariance(targetInputColumn) != null) {
                    numberProfile.setVariance(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result).getVariance(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getPercentile25(targetInputColumn) != null) {
                    numberProfile.setPercentile_25th(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result).getPercentile25(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getPercentile75(targetInputColumn) != null) {
                    numberProfile.setPercentile_75th(Double.parseDouble(form.format(
                            ((NumberAnalyzerResult) result).getPercentile75(targetInputColumn))));
                }
            }

            if (result instanceof ValueMatchAnalyzerResult) {
                if (((ValueMatchAnalyzerResult) result).getCount("0") != null) {
                    numberProfile.setZero_cnt(((ValueMatchAnalyzerResult) result).getCount("0"));
                }
            }

            if (result instanceof DateAndTimeAnalyzerResult) {
                Object value;

                if (((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn) > 0
                            && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(
                            ((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn));
                }

                value = ((CrosstabResult) result).getCrosstab()
                        .where("Column", targetInputColumn.getName())
                        .where("Measure", "Highest date")
                        .safeGet(null);
                if (value != null) {
                    dateProfile.setHighest_date((String) value);
                } else {
                    dateProfile.setHighest_date("-");
                }

                value = ((CrosstabResult) result).getCrosstab()
                        .where("Column", targetInputColumn.getName())
                        .where("Measure", "Lowest date")
                        .safeGet(null);
                if (value != null) {
                    dateProfile.setLowest_date((String) value);
                } else {
                    dateProfile.setLowest_date("-");
                }

                value = ((CrosstabResult) result).getCrosstab()
                        .where("Column", targetInputColumn.getName()).where("Measure", "Mean")
                        .safeGet(null);
                if (value != null) {
                    dateProfile.setMean_date((String) value);
                } else {
                    dateProfile.setMean_date("-");
                }

                value = ((CrosstabResult) result).getCrosstab()
                        .where("Column", targetInputColumn.getName()).where("Measure", "Median")
                        .safeGet(null);
                if (value != null) {
                    dateProfile.setMedian_date((String) value);
                } else {
                    dateProfile.setMedian_date("-");
                }

                value = ((CrosstabResult) result).getCrosstab()
                        .where("Column", targetInputColumn.getName())
                        .where("Measure", "25th percentile")
                        .safeGet(null);
                if (value != null) {
                    dateProfile.setPercentile_25th((String) value);
                } else {
                    dateProfile.setPercentile_25th("-");
                }

                value = ((CrosstabResult) result).getCrosstab()
                        .where("Column", targetInputColumn.getName())
                        .where("Measure", "75th percentile")
                        .safeGet(null);
                if (value != null) {
                    dateProfile.setPercentile_75th((String) value);
                } else {
                    dateProfile.setPercentile_75th("-");
                }

                //basicProfile.setValue_distribution(vfModelList);
            }
            if (key_analysis) {
                profileTableResult.setKey_analysis_results(key_analysis_results);
            }
        }

        /* 컬럼별 프로파일  */
        if (profileTypes != null) {
            int index = header.indexOf(columnName) + 1;
            List<String> typeList = requestColumnAndType.get(index);

            if (typeList.contains("basic")) {
                profileColumnResult.getProfiles().put("basic_profile", basicProfile);
            }
            if (typeList.contains("number")) {
                if (profileColumnResult.getColumn_type().equals("number")) {
                    profileColumnResult.getProfiles().put("number_profile", numberProfile);
                }
            } else if (typeList.contains("string")) {
                if (profileColumnResult.getColumn_type().equals("string")) {
                    profileColumnResult.getProfiles().put("string_profile", stringProfile);
                }
            } else if (typeList.contains("date")) {
                if (profileColumnResult.getColumn_type().equals("date")) {
                    profileColumnResult.getProfiles().put("date_profile", dateProfile);
                }
            }
        } else {
            profileColumnResult.getProfiles().put("basic_profile", basicProfile);
            if (profileColumnResult.getColumn_type().equals("number")) {
                profileColumnResult.getProfiles().put("number_profile", numberProfile);
            }
            if (profileColumnResult.getColumn_type().equals("string")) {
                profileColumnResult.getProfiles().put("string_profile", stringProfile);
            }
            if (profileColumnResult.getColumn_type().equals("date")) {
                profileColumnResult.getProfiles().put("date_profile", dateProfile);
            }
        }


    }

    public String getFileName(String type, String path) {
        String[] split = null;
        if (type.equals("path")) {
            split = path.split("\\\\");
        } else if (type.equals("url")) {
            split = path.split("/");
        }
        return split[split.length - 1].split("\\.")[0];
    }

    public List<String> getHeader(String path, Boolean isHeader, String fileName)
            throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(path));
        List<String> header = new ArrayList<>();

        // 헤더가 있을 경우
        if (isHeader) {
            header = Arrays.asList(csvReader.readNext().clone());
            csvReader.close();
        } else { // 헤더가 없을 경우
            csvReader = new CSVReader((new FileReader(path)));

            // 컬럼 수에 따라 헤더 설정
            int recordsCount = csvReader.readNext().length;
            for (int i = 1; i < recordsCount + 1; i++) {
                header.add(fileName + "_" + i);
            }
            csvReader.close();

            csvReader = new CSVReader((new FileReader(path)));
            List<Object> originData = new ArrayList<>();
            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                originData.add(nextLine); // 원본 데이터 읽기
            }

            // 헤더가 없을 경우 targetfiles에 변형 파일 저장
            boolean directoryCreated = new File(targetFolderPath).mkdir(); // 폴더 생성
            String newFilePath = targetFolderPath + fileName + ".csv";
            File newFile = new File(newFilePath);
            CSVWriter csvWriter = new CSVWriter(new FileWriter(newFile));
            csvWriter.writeNext(header.toArray(String[]::new)); // 헤더 작성

            // 헤더 아랫줄부터 원본 데이터 쓰기
            for (Object data : originData) {
                csvWriter.writeNext((String[]) data);
            }

            csvWriter.close();
        }

        return header;
    }

    public DependencyAnalysisResult dependencyAnalysis(DependencyAnalysis dependencyAnalysis)
            throws IOException {

        // request
        int determinantIdx;
        int dependencyIdx;
        Map<String, Set<String>> dependencySet = new HashMap<>();

        // result
        DependencyAnalysisResult dependencyAnalysisResult = new DependencyAnalysisResult();
        boolean isValid = true;
        List<String> inValidValues = new ArrayList<>();

        /* column 은 인덱스 번호이거나 column 이름이므로 이를 구분 */

        // request : column name
        if (dependencyAnalysis.getDeterminant().getClass().getName().equals("java.lang.String") && dependencyAnalysis.getDependency().getClass().getName().equals("java.lang.String")) {
            determinantIdx = header.indexOf((String) dependencyAnalysis.getDeterminant());
            dependencyIdx = header.indexOf((String) dependencyAnalysis.getDependency());
        }
        // request : column index
        else if (dependencyAnalysis.getDeterminant().getClass().getName().equals("java.lang.Integer") && dependencyAnalysis.getDependency().getClass().getName().equals("java.lang.Integer")) {
            determinantIdx = (int) dependencyAnalysis.getDeterminant() - 1;
            dependencyIdx = (int) dependencyAnalysis.getDependency() - 1;
        } else {
            throw new CustomException(BAD_JSON_REQUEST);
        }

        /* 각 column values 읽는 작업 */
        CSVReader csvReader = new CSVReader(new FileReader(filePath));
        String[] nextLine;
        while ((nextLine = csvReader.readNext()) != null) {
            if (nextLine.length == header.size()) {
                if (!dependencySet.containsKey(nextLine[determinantIdx])) {
                    Set<String> set = new HashSet<>();
                    set.add(nextLine[dependencyIdx]);
                    dependencySet.put(nextLine[determinantIdx], set);
                } else {
                    dependencySet.get(nextLine[determinantIdx]).add(nextLine[dependencyIdx]);
                }
            }
        }

        dependencySet.forEach((key, value) -> {
            if (value.size() != 1) {
                inValidValues.add(key);
            }
        });

        dependencyAnalysisResult.setDeterminant(dependencyAnalysis.getDeterminant());
        dependencyAnalysisResult.setDependency(dependencyAnalysis.getDependency());
        if (inValidValues.size() == 0) {
            dependencyAnalysisResult.setIs_valid(true);
        } else {
            dependencyAnalysisResult.setIs_valid(false);
        }
        dependencyAnalysisResult.setInvalid_values(inValidValues);
        return dependencyAnalysisResult;
    }
}