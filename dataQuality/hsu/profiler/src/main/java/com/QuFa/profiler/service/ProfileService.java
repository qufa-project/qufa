package com.QuFa.profiler.service;


import com.QuFa.profiler.config.ActiveProfileProperty;
import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.profile.BasicProfile;
import com.QuFa.profiler.model.profile.DateProfile;
import com.QuFa.profiler.model.profile.NumberProfile;
import com.QuFa.profiler.model.profile.ProfileColumnResult;
import com.QuFa.profiler.model.profile.ProfileTableResult;
import com.QuFa.profiler.model.profile.StringProfile;
import com.QuFa.profiler.model.profile.VdModel;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.File;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.stereotype.Component;

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

    /* 컬럼 분리*/
    Map<String, List<Object>> profiles;
    private Map<Object,String> request = new HashMap<>();
    private List<Object> requestValue = new ArrayList<>();


    @Autowired
    private final DataStoreService dataStoreService;

    private List<String> header;

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
        CSVReader csvReader = new CSVReader(new FileReader(path));
        String[] nextLine;
        List<String> rowValues = new ArrayList<>();
        Map<String, String> rowType = new HashMap<>();
        while ((nextLine = csvReader.readNext()) != null) {
            if (nextLine.length == header.size()) {
                rowValues.add(nextLine[header.indexOf(columnName)]);
            }
        }

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
         * ->> 고쳐야 할 부분
         */
        i = 0;
        int n;
        Map<String, Integer> vdTypes = new HashMap<>();
        vdTypes.put("string", 0);
        vdTypes.put("number", 0);
        vdTypes.put("date", 0);
        for (String t : rowType.values()) {
            if (i >= 99) {
                break;
            }
            n = vdTypes.get(t) + 1;
            vdTypes.put(t, n);
            i++;
        }

        int maxVal = Collections.max(vdTypes.values());

        for (String key : vdTypes.keySet()) {
            if (vdTypes.get(key).equals(maxVal)) {
                return key;
            }
        }

        return "string";
    }

    public ProfileTableResult profileLocalCSV(Local local) {
        /* 컬럼 분리 */
        profiles = local.getProfiles();

        if (local.getSource().getType().equals("path")) {
            String fileName = getFileName(local.getSource().getType(), local.getSource().getPath());
            String path = local.getSource().getPath();
            try {
                /**
                 * FileNotFound 예외처리 해야함
                 */

                // header는 처음에 한번만 구하고, ProfileService 객체에 필드로 정의.
                header = getHeader(path, local.isHeader(), fileName);

                profileLocalColumns("path", path, header, local.isHeader());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return profileTableResult;
        } else if (local.getSource().getType().equals("url")) {
            String fileName = getFileName(local.getSource().getType(), local.getSource().getUrl());
            String url = local.getSource().getUrl();
            try {
                URL file = new URL(url);
                dataStoreService.storeUrlFile(file);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.openStream()));

                // header는 처음에 한번만 구하고, ProfileService 객체에 필드로 정의.
                header = Arrays.asList(reader.readLine().split(","));

                profileLocalColumns("url", url, header, local.isHeader());

                reader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return profileTableResult;
        } else {
            //TODO:type에러 추가
        }
        return null;
    }

    public void profileLocalColumns(String type, String path, List<String> columnNames, Boolean isHeader) {
        /* 컬럼 분리 */
        profiles.forEach((key,valueList)-> {
            valueList.forEach(value->{
                String valueType = value.getClass().getName();
                if (valueType.equals("java.lang.String")) {
                    for (String columnName : columnNames) {
                        if (Objects.equals(String.valueOf(value), columnName)){
                            int valueIndex = columnNames.indexOf(String.valueOf(value)) + 1;
                            if(!key.equals("basic")){
                                // 헤더 있는 경우 -> request = {컬럼이름=number}
                                request.put(valueIndex,key);
                            }
                            // 헤더 있는 경우 -> requestValue = [컬럼이름1,컬럼이름2,컬럼이름2]
                            requestValue.add(valueIndex);
                        }
                    }
                } else if (valueType.equals("java.lang.Integer")){
                    if(!key.equals("basic")){
                        // 헤더 없는 경우 -> request = {2=number}
                        request.put(value,key);
                    }
                    // 헤더 없는 경우 -> requestValue = [1, 2, 2]
                    requestValue.add(value);
                } else {
                    //TODO:type에러 추가
                }
            });
        });

        System.out.println("requestValue = " + requestValue);
        System.out.println("request = " + request);

        profileTableResult = new ProfileTableResult();
        System.out.println(path);

        String filename = getFileName(type, path);
        System.out.println("filename:" + filename);

        //CsvDatastore
        // 헤더가 있으면 original path
        if (type.equals("path") && isHeader) {
            DataStoreService.createLocalDataStore(path);
        } else if (type.equals("url") || !isHeader) { // url이거나, 헤더가 없으면 targetfiles~
            path = "./src/main/resources/targetfiles/" + filename + ".csv";
            DataStoreService.createLocalDataStore(path);
        }

        profileTableResult.setDataset_name(filename);
        profileTableResult.setDataset_type("csv");

        File file = new File(path);
        profileTableResult.setDataset_size((int) file.length());
        file = null;

        profileTableResult.setDataset_column_cnt(columnNames.size());

        for (String columnName : columnNames) {
            int index = columnNames.indexOf(columnName) + 1;

            if (requestValue.contains(index)) {
                profileColumnResult = new ProfileColumnResult();
                profileColumnResult.setColumn_id(columnNames.indexOf(columnName) + 1);
                profileColumnResult.setColumn_name(columnName);
                try {
                    String valueType = typeDetection(path, columnName);
                    if (valueType != request.get(index)){
                        System.out.println("valueType = " + valueType+", "+"request = " + request.get(index));
                        System.out.println("타입 에러!!!!!!!!!!!!!!!");
                        //TODO:type에러 추가
                    }
                    profileColumnResult.setColumn_type(typeDetection(path, columnName));
                } catch(IOException e){
                    e.printStackTrace();
                }
                this.profileSingleColumn(filename, columnName);
                profileTableResult.getResults().add(profileColumnResult);
            }
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

        DataStoreService.setDataStore("CSVDS");
        AnalysisJobBuilder builder = DataStoreService.getBuilder();
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
        }

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
        String folderName = "./src/main/resources/targetfiles/";

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
            boolean directoryCreated = new File(folderName).mkdir(); // 폴더 생성
            String newFilePath = folderName + fileName + ".csv";
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
}