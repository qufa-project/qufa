package com.QuFa.profiler.service;


import static com.QuFa.profiler.controller.exception.ErrorCode.BAD_JSON_REQUEST;

import com.QuFa.profiler.controller.exception.CustomException;
import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.request.DependencyAnalysis;
import com.QuFa.profiler.model.request.FKAnalysis;
import com.QuFa.profiler.model.request.Profiles;
import com.QuFa.profiler.model.response.BasicProfile;
import com.QuFa.profiler.model.response.DateProfile;
import com.QuFa.profiler.model.response.DependencyAnalysisResult;
import com.QuFa.profiler.model.response.FKAnalysisResult;
import com.QuFa.profiler.model.response.NumberProfile;
import com.QuFa.profiler.model.response.ProfileColumnResult;
import com.QuFa.profiler.model.response.ProfileTableResult;
import com.QuFa.profiler.model.response.StringProfile;
import com.QuFa.profiler.model.response.VdModel;
import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.datacleaner.api.AnalyzerResult;
import org.datacleaner.api.InputColumn;
import org.datacleaner.api.InputRow;
import org.datacleaner.beans.DateAndTimeAnalyzer;
import org.datacleaner.beans.DateAndTimeAnalyzerResult;
import org.datacleaner.beans.NumberAnalyzer;
import org.datacleaner.beans.NumberAnalyzerResult;
import org.datacleaner.beans.StringAnalyzer;
import org.datacleaner.beans.StringAnalyzerResult;
import org.datacleaner.beans.referentialintegrity.ReferentialIntegrityAnalyzer;
import org.datacleaner.beans.referentialintegrity.ReferentialIntegrityAnalyzerResult;
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

    private ProfileTableResult profileTableResult;
    private ProfileColumnResult profileColumnResult;

    private Profiles profiles = null;
    private Map<String, List<Object>> column_analysis;
    private Map<Object, List<String>> requestColumns;

    private List<DependencyAnalysis> dependencyAnalyses;
//    private ArrayList<Object> key_analysis_results; // 후보키 컬럼을 담는 배열
    private List<FKAnalysis> fkAnalyses;

    @Autowired
    private final DataStoreService dataStoreService;
    private final FileService fileService = new FileService();

    private final CandidateKeyService candidateKeyService = new CandidateKeyService();

    private AnalysisJobBuilder builder;

    private long t = 0;
    private long totalDetact = 0; // 총 판단하는 데이터 개수
    private int cntDetactType = 0; // row당 판단하는 최대 데이터 개수
    private int detactingtime = 10000; // 전체 타입 판단 시간 설정 (1000 -> 1초)
//    private boolean key_analysis; // 후보키를 찾을지 말지 판단

    private List<String> header;
    private String filePath;
    private String fileName;
    private String fileType;
    private boolean isHeader;

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
        System.out.println("시간차이(m) : " + secDiffTime1);

        if (cntDetactType == 0) {
            cntDetactType = (int) ((Math.log(rowValues.size() * profileTableResult.getDataset_column_cnt())/Math.log(1.0001))/
                    profileTableResult.getDataset_column_cnt());
            if (cntDetactType < 1000) {
                cntDetactType = 1000;
            }
        }
        System.out.println("타입추출제한개수 : " + cntDetactType);
        System.out.println("row size : " + rowValues.size());

        if (rowValues.size() > cntDetactType) {
            Collections.shuffle(rowValues);
            rowValues = rowValues.subList(0, cntDetactType);
        }
        totalDetact += rowValues.size() - 1;

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

        i = 0;
        int n;

        /*
          컬럼의 샘플링 데이터 중
          모든 type이 number면 컬럼의 type은 number
          모든 type이 date면 컬럼의 type은 date
           그렇지 않으면 컬럼의 type은 string
         */
        String typeValue;

        boolean dateType;
        boolean numberType;
        dateType = true;
        numberType = true;
        for (String t : rowType.values()) {
            if (t.equals("number")) {
                dateType = false;
            } else if (t.equals("date")) {
                numberType = false;
            } else if (t.equals("string")) {
                dateType = false;
                numberType = false;
            }
        }

        if (dateType) {
            typeValue = "date";
        } else if (numberType) {
            typeValue = "number";
        } else {
            typeValue = "string";
        }

        long afterTime = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
        long secDiffTime = (afterTime - beforeTime); //두 시간에 차 계산
        System.out.println("시간차이(m) : " + secDiffTime);
        t = t + secDiffTime;
        System.out.println("타입구분시간 : " + t);
        System.out.println("타입 : " + typeValue);

        return typeValue;
    }

    public ProfileTableResult profileLocalCSV(Local local) {

        /* 파일 정보 설정 */
        fileType = local.getSource().getType();
        filePath =
                (fileType.equals("url")) ? local.getSource().getUrl() : local.getSource().getPath();
        fileName = fileService.getFileName(fileType, filePath);
        isHeader = local.isHeader();

        try {

            // url의 파일을 로컬에 복사
            if (fileType.equals("url")) {
                filePath = fileService.storeUrlFile(filePath);
            }

            // 헤더 새로 쓰기
            if (!isHeader) {
                filePath = fileService.writeHeader(fileName, filePath);
            }

            header = fileService.getHeader(filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }

        profiles = local.getProfiles();


        if (profiles != null) {

            if (profiles.getColumn_analysis() != null) {
                column_analysis = profiles.getColumn_analysis();
            } else {
                column_analysis = null;
            }

            candidateKeyService.setKey_analysis(profiles.isKey_analysis());

            if (profiles.getDependencied_analysis() != null) {
                dependencyAnalyses = profiles.getDependencied_analysis();
            } else {
                dependencyAnalyses = null;
            }

            if (profiles.getFk_analysis() != null) {
                fkAnalyses = profiles.getFk_analysis();
            } else {
                fkAnalyses = null;
            }

        }

        t = 0;
        cntDetactType = 0;
        totalDetact = 0;

        /* DataStoreService 생성 */
        dataStoreService.createLocalDataStore(filePath);
        dataStoreService.setDataStore("CSVDS");
        builder = dataStoreService.getBuilder();

        /* profileTableResult 설정 */
        profileTableResult =
                ProfileTableResult.builder()
                        .dataset_name(fileName)
                        .dataset_type("csv")
                        .dataset_size(fileService.getFileLength(filePath))
                        .dataset_column_cnt(header.size())
                        .single_column_results(new ArrayList<>())
                        .build();

        /**
         * single column results 생성
         */
        profileTableResult = profileLocalColumns(filePath, header);

        System.out.println("파일 크기 : " + profileTableResult.getDataset_size());
        System.out.println("타입 판단 제한 개수 : " + cntDetactType);
        System.out.println("설정한 타입구분시간 : " + detactingtime);
        System.out.println("실제 타입구분시간 : " + t);
        System.out.println("총 타입판단개수 : " + totalDetact);
        System.out.println("총 데이터 수 : " + profileTableResult.getDataset_column_cnt()
                                                  * profileTableResult.getDataset_row_cnt());
        System.out.println("데이터당 걸린 시간 : " + (double) t / (double) totalDetact);

        /* dependency analysis result 생성*/
        if (dependencyAnalyses != null) {
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

        /* fk analysis results 생성 */
        if (fkAnalyses != null) {
            List<FKAnalysisResult> referentialIntegrityAnalyzerResults = new ArrayList<>();
            for (FKAnalysis fkAnalysis : fkAnalyses) {
                try {
                    referentialIntegrityAnalyzerResults.add(referentialIntegrity(fkAnalysis));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            profileTableResult.setFk_analysis_results(referentialIntegrityAnalyzerResults);
        }

        return profileTableResult;

    }

    /**
     * single column results 생성
     */
    public ProfileTableResult profileLocalColumns(String path, List<String> columnNames) {

        // column_analysis 요청이 오면 requestColumns 생성
        requestColumns = new HashMap<>();
        if (column_analysis != null) {
            /**
             *  column_analysis 예시
             *  key: "basic"
             *  valueList: : [1, 2, 3, 4, 5, 6]
             */
            column_analysis.forEach((key, valueList) -> {
                valueList.forEach(value -> {
                    if (value.getClass().getName().equals("java.lang.String")) {
                        value = columnNames.indexOf((String) value);
                    }

                    List<String> keyList;
                    keyList = (requestColumns.get(value) == null) ? new ArrayList<>()
                                      : requestColumns.get(value);
                    keyList.add(key);
                    requestColumns.put(value, keyList);
                });
            });

            System.out.println("column_analysis = " + requestColumns);

        }

        for (String columnName : columnNames) {

            if (columnName.isEmpty()) {
                continue;
            }

            String valueType = "string";
            int index = columnNames.indexOf(columnName) + 1;

            if (column_analysis != null) {
                if (requestColumns.get(index) == null) {
                    continue;
                }
                valueType = requestColumns.get(index).stream()
                        .filter(s -> !s.equals("basic"))
                        .findFirst()
                        .orElse("string");
            } else {
                try {
                    valueType = typeDetection(path, columnName); // 컬럼 타입 판단
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            profileColumnResult = new ProfileColumnResult();
            profileColumnResult.setColumn_id(index);
            profileColumnResult.setColumn_name(columnName);
            profileColumnResult.setColumn_type(valueType);

            profileSingleColumn(fileName, columnName);
            profileTableResult.getSingle_column_results().add(profileColumnResult);

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
        System.out.println("ProfileService.profileSingleColumn");

        String inputColumnName = tableName + "." + columnName;
        String type = profileColumnResult.getColumn_type();
        System.out.println("columnName = " + columnName);
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
        /* DataStoreService 설정 */
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
        for (int i = 0; i < keyArray.length; i++) {
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
                /* 데이터 클리너가 csv파일의 ""을 NULL값으로 판별하지 못함 */
//                if (((ValueDistributionAnalyzerResult) result).getNullCount() > 0) {
//                    basicProfile
//                            .setNull_cnt(((ValueDistributionAnalyzerResult) result).getNullCount());
//                }

                int distinct_cnt = ((ValueDistributionAnalyzerResult) result).getDistinctCount();
                int row_cnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();

                profileTableResult.setDataset_row_cnt(row_cnt);
                //TODO: if(header=yes) Table row cnt=row_cnt+1

                basicProfile.setDistinct_cnt(distinct_cnt);
                basicProfile.setRow_cnt(row_cnt);
                basicProfile
                        .setUnique_cnt(((ValueDistributionAnalyzerResult) result).getUniqueCount());
                basicProfile.setDistinctness((double) distinct_cnt / row_cnt);


                Collection<ValueFrequency> vfList = ((ValueDistributionAnalyzerResult) result)
                        .getValueCounts();
                for (ValueFrequency vf : vfList) {
                    /* NULL 값 처리 */
                    if (vf.getValue() != null) {
                        if (vf.getValue().isBlank()) {
                            candidateKeyService.addNullCnt(vf.getCount());
                        }
                    }


                    if (vf.getChildren() != null) {
                        Collection<ValueFrequency> vfChildren = vf.getChildren();
                        for (ValueFrequency vfChild : vfChildren) {
                            if (vfChild.getValue() != null) {
                                /* NULL 값 처리 */
                                if (vfChild.getValue().isBlank()) {
                                    candidateKeyService.addNullCnt(vfChild.getCount());
                                }

                                vfModelList.put(vfChild.getValue(), vfChild.getCount());
                            }
                        }
                    } else {
                        if (vf.getValue() != null) {
                            vfModelList.put(vf.getValue(), vf.getCount());
                        }
                    }
                }

                /* NULL값 처리 */
                if (candidateKeyService.getNullCnt() > 0) {
                    basicProfile
                           .setNull_cnt(candidateKeyService.getNullCnt());
                }

                /* key analysis result */
                if (candidateKeyService.isKey_analysis()) {
                    candidateKeyService.CheckCandidateKey(basicProfile.getDistinctness(), profileColumnResult.getColumn_id());
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
            if (candidateKeyService.isKey_analysis()) {
                profileTableResult.setKey_analysis_results(candidateKeyService.getKey_analysis_results());
            }
        }

        /* 컬럼별 프로파일 결과 */
        if (column_analysis != null) {
            int index = header.indexOf(columnName) + 1;
            List<String> typeList = requestColumns.get(index);

            if (typeList.contains("basic")) {
                profileColumnResult.getProfiles().put("basic_profile", basicProfile);
            }
            if (typeList.contains("number")) {
                profileColumnResult.getProfiles().put("number_profile", numberProfile);
            }
            if (typeList.contains("string")) {
                profileColumnResult.getProfiles().put("string_profile", stringProfile);
            }
            if (typeList.contains("date")) {
                profileColumnResult.getProfiles().put("date_profile", dateProfile);
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

    /**
     * 함수적 종속(functiuonal dependency)을 검사
     */
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
        if (dependencyAnalysis.getDeterminant().getClass().getName().equals("java.lang.String")
                    && dependencyAnalysis.getDependency().getClass().getName()
                .equals("java.lang.String")) {
            determinantIdx = header.indexOf((String) dependencyAnalysis.getDeterminant());
            dependencyIdx = header.indexOf((String) dependencyAnalysis.getDependency());
        }
        // request : column index
        else if (dependencyAnalysis.getDeterminant().getClass().getName()
                .equals("java.lang.Integer") && dependencyAnalysis.getDependency().getClass()
                .getName().equals("java.lang.Integer")) {
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

    /**
     * 현재 테이블의 컬럼과 타 테이블의 컬럼간 참조무결성의 유효성 여부를 검사
     */
    private FKAnalysisResult referentialIntegrity(FKAnalysis fkAnalysis) throws IOException {
        boolean is_valid = Boolean.FALSE;
        List<String> invalid_values = new ArrayList<>();

        String refFilePath = fkAnalysis.getReferenced_file(); // 참조되는 원격 파일 경로에 대한 URL
        String refFileType = refFilePath.substring(0, 4); // 참조되는 원격 파일 type(url | path)
        String foreignKey; // 외래키 후보 컬럼
        String refColumn; // 참조되는 컬럼명
        String refFileName; // 참조되는 원격 파일 이름
        List<String> refFileHeader; // 참조되는 원격 파일 헤더

        // 파일 경로 재설정
        if (refFileType.equals("file")) {
            refFileType = "path";
            refFilePath = fileService.seperate_file(refFilePath);
        } else {
            refFileType = "url";
        }
        // url의 파일을 로컬에 복사
        if (refFileType.equals("url")) {
            refFilePath = fileService.storeUrlFile(refFilePath);
        }

        refFileHeader = fileService.getHeader(refFilePath);
        refFileName = fileService.getFileName(refFileType, refFilePath) + ".csv";

        FKAnalysisResult fkAnalysisResult = new FKAnalysisResult();
        fkAnalysisResult.setForeign_key(fkAnalysis.getForeign_key());

        /**
         * foreignKey
         */
        // 컬럼번호로 올 때
        if (fkAnalysis.getForeign_key().getClass().getName().equals("java.lang.Integer")) {
            foreignKey = header.get((int) fkAnalysis.getForeign_key() - 1);
        }
        // 컬럼명으로 올 때
        else {
            foreignKey = fkAnalysis.getForeign_key().toString();
        }

        /**
         * refColumn
         */
        // 컬럼번호로 올 때
        if (fkAnalysis.getReferenced_column().getClass().getName().equals("java.lang.Integer")) {
            refColumn = refFileHeader.get((int) fkAnalysis.getReferenced_column() - 1);
        }
        // 컬럼명으로 올 때
        else {
            refColumn = fkAnalysis.getReferenced_column().toString();
        }

        // 기존 파일(프로파일링 대상) builder 재시작
        builder.reset();

        // builder에 프로파일링 할 외래키 후보 컬럼 add
        builder.addSourceColumns(foreignKey);
        InputColumn<?> fkInputColumn = builder.getSourceColumnByName(foreignKey);

        // 참조되는 원격 파일의 DataStore 생성
        FKDataStoreService refDataStoreService = new FKDataStoreService();
        refDataStoreService.createLocalDataStore(refFileName, refFilePath);

        // Schema name : 상위 폴더 이름
        String schemaName = "";
        String os = System.getProperty("os.name").toLowerCase();

        // windows 이면
        if (os.contains("win")) {
            String[] array = refFilePath.split("\\\\");
            if (refFileType.equals("path")) {
                schemaName = array[array.length - 2];
            }
            else {
                schemaName = "targetfiles";
            }
        }
        // linux 이면
        else {
            String[] array = refFilePath.split("/");
            if (refFileType.equals("path")) {
                schemaName = array[array.length - 2];
            }
            else {
                schemaName = "tmp";
            }
        }
        System.out.println("Schema name = " + schemaName);

        // builder에 Analyzer add
        AnalyzerComponentBuilder<ReferentialIntegrityAnalyzer> referentialIntegrityAnalyzer = builder.addAnalyzer(
                ReferentialIntegrityAnalyzer.class);
        referentialIntegrityAnalyzer.addInputColumn(fkInputColumn);
        referentialIntegrityAnalyzer.setConfiguredProperty("Datastore",
                refDataStoreService.getDataStore());
        referentialIntegrityAnalyzer.setConfiguredProperty("Schema name",
                schemaName);
        referentialIntegrityAnalyzer.setConfiguredProperty("Table name", refFileName);
        referentialIntegrityAnalyzer.setConfiguredProperty("Column name", refColumn);

        // Job Builder로 프로파일링을 수행
        AnalysisJob analysisJob = builder.toAnalysisJob();
        AnalysisRunner runner = new AnalysisRunnerImpl(dataStoreService.getConfiguration());
        AnalysisResultFuture resultFuture = runner.run(analysisJob);

        resultFuture.await();

        System.out.println("==========run===========");
        // 에러 발생 혹은 취소시,
        if (resultFuture.isCancelled() || resultFuture.isErrornous()) {
            System.out.println("referentialIntegrityAnalyzer error !");
            List<Throwable> errors = resultFuture.getErrors();
            System.out.println(errors.toString());

            resultFuture.cancel();
            resultFuture = null;

        } else {
            System.out.println("referentialIntegrityAnalyzer success !");

            // 성공시 결과 저장.
            AnalysisResult analysisResult = resultFuture;
            List<AnalyzerResult> results = analysisResult.getResults();

            for (AnalyzerResult result : results) {
                if (result instanceof ReferentialIntegrityAnalyzerResult) {
                    List<InputRow> resultList = ((ReferentialIntegrityAnalyzerResult) result).getSampleRows();
                    if (resultList.isEmpty()){
                        is_valid = Boolean.TRUE;
                    } else{
                        for (InputRow inputRow : resultList) {
                            String temp = inputRow.toString().split("MetaModelInputRow\\[Row\\[values=\\[")[1];
                            invalid_values.add(temp.split("]]]")[0]);
                        }
                    }
                }
            }
        }

        // 응답 데이터에 결과 입력
        fkAnalysisResult.setReferenced_table(refFileName);
        fkAnalysisResult.setReferenced_column(refColumn);
        fkAnalysisResult.setIs_valid(is_valid);
        if (!is_valid) {
            fkAnalysisResult.setInvalid_values(invalid_values);
        }

        return fkAnalysisResult;
    }

}