package com.QuFa.profiler.service;


import com.QuFa.profiler.config.ActiveProfileProperty;
import com.QuFa.profiler.model.Local;
import com.QuFa.profiler.model.profile.*;
import com.opencsv.CSVReader;
import freemarker.template.SimpleDate;
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

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
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

    public String typeDetection(String path, String columnName) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(path));
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
            for(DateFormat df : dfs) {
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
//            if(i<=99)
//                System.out.println(rowVal + " : " + rowType.get(rowVal));
//            i++;
        }
        i = 0;
        int n;
        Map<String, Integer> vdTypes = new HashMap<>();
        vdTypes.put("string", 0);
        vdTypes.put("number", 0);
        vdTypes.put("date", 0);
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

        return "string";
    }

    public ProfileTableResult profileLocalCSV(Local local){
        //ProfileTableResult profileTableResult = new ProfileTableResult();

        if(local.getSource().getType().equals("path")) {
            String path = local.getSource().getPath();
            try {
                CSVReader csvReader = new CSVReader(new FileReader(path));

                List<String> header = Arrays.asList(csvReader.readNext().clone());

                profileLocalColumns("path", path, header);

                csvReader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return profileTableResult;
        }
        else if(local.getSource().getType().equals("url")){
            String url = local.getSource().getUrl();
            try {
                URL file = new URL(url);
                dataStoreService.storeUrlFile(file);

                BufferedReader reader = new BufferedReader(new InputStreamReader(file.openStream()));

                List<String> header = Arrays.asList(reader.readLine().split(","));
                System.out.println("header : " + header);

                profileLocalColumns("url", url, header);

                reader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return profileTableResult;
        }
        else{
            //TODO:type에러 추가
        }
        return null;
    }

    public void profileLocalColumns(String type, String path, List<String> columnNames) {
        profileTableResult = new ProfileTableResult();
        System.out.println(path);

        //파일이름만 분리
        String[] split = null;
        if(type.equals("path"))
            split = path.split("\\\\");
        else if(type.equals("url"))
            split = path.split("/");
        String filename = split[split.length-1].split("\\.")[0];
        System.out.println("filename:"+filename);

        //CsvDatastore
        if(type.equals("path"))
            DataStoreService.createLocalDataStore(path);
        else if(type.equals("url")) {
            path = "./src/main/resources/targetfiles/" + filename + ".csv";
            DataStoreService.createLocalDataStore(path);
        }


        profileTableResult.setDataset_name(filename);
        profileTableResult.setDataset_type("csv");

        File file = new File(path);
        profileTableResult.setDataset_size((int)file.length());
        file = null;

        profileTableResult.setDataset_column_cnt(columnNames.size());


        for (String columnName : columnNames) {
            profileColumnResult = new ProfileColumnResult();
            profileColumnResult.setColumn_id(columnNames.indexOf(columnName) + 1);
            profileColumnResult.setColumn_name(columnName);
            try {
                profileColumnResult.setColumn_type(typeDetection(path, columnName));
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
        InputColumn<?> dateTargetInputColumn = null;

        //Convert column data type
        if (type.equals("number")){
            TransformerComponentBuilder<ConvertToNumberTransformer> ctn = builder.addTransformer(ConvertToNumberTransformer.class);
            ctn.addInputColumns(targetInputColumn);
            targetInputColumn = ctn.getOutput()[0];

            AnalyzerComponentBuilder<ValueMatchAnalyzer> vma = builder.addAnalyzer(ValueMatchAnalyzer.class);
            vma.addInputColumn(targetInputColumn);
            String[] expected_values = { "0" };
            vma.setConfiguredProperty("Expected values", expected_values);

        } else if (type.equals("date")){
            TransformerComponentBuilder<ConvertToDateTransformer> ctd = builder.addTransformer(ConvertToDateTransformer.class);
            ctd.setConfiguredProperty("Time zone","Asia/Seoul");
//            Date DateNullReplacement = new Date();
//            ctd.setConfiguredProperty("Null replacement", DateNullReplacement);
            ctd.addInputColumns(targetInputColumn);
            dateTargetInputColumn = targetInputColumn;
            targetInputColumn = ctd.getOutput()[0];
        }
        System.out.println(targetInputColumn); // null로 나옴


        // analyzer config
        // val
        AnalyzerComponentBuilder<ValueDistributionAnalyzer> valDistAnalyzer = builder.addAnalyzer(ValueDistributionAnalyzer.class);
        if(type.equals("date"))
            valDistAnalyzer.addInputColumns(dateTargetInputColumn);
        else
            valDistAnalyzer.addInputColumns(targetInputColumn);
        valDistAnalyzer.setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_UNIQUE_VALUES, true);
        valDistAnalyzer.setConfiguredProperty(ValueDistributionAnalyzer.PROPERTY_RECORD_DRILL_DOWN_INFORMATION, true);


        //Column data type과 매핑되는 analyzer config
        if (type.equals("string")) {
            AnalyzerComponentBuilder<StringAnalyzer> stringAnalyzer = builder.addAnalyzer(StringAnalyzer.class);
            stringAnalyzer.addInputColumn(targetInputColumn);
        } else if (type.equals("number")) {
            AnalyzerComponentBuilder<NumberAnalyzer> numberAnalyzer = builder.addAnalyzer(NumberAnalyzer.class);
            numberAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            numberAnalyzer.addInputColumn(targetInputColumn);
        } else if (type.equals("date")) {
            AnalyzerComponentBuilder<DateAndTimeAnalyzer> dateAnalyzer = builder.addAnalyzer(DateAndTimeAnalyzer.class);
            dateAnalyzer.setConfiguredProperty("Descriptive statistics", true);
            dateAnalyzer.addInputColumn(targetInputColumn);

            AnalyzerComponentBuilder<MonthDistributionAnalyzer> monthDistAnalyzer = builder.addAnalyzer(MonthDistributionAnalyzer.class);
            monthDistAnalyzer.addInputColumns(targetInputColumn);

            AnalyzerComponentBuilder<YearDistributionAnalyzer> yearDistAnalyzer = builder.addAnalyzer(YearDistributionAnalyzer.class);
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

    private String SerialNumberToDate(long num){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(num * 86400000L));
    }

    private Map<Object, Object> getTop100(Map<Object, Object> map){
        Map<Object, Object> top100 = new HashMap<>();

        return top100;
    }

    private Map<Object, Object> valueSortByDesc(Map<Object, Object> map){
        List<Map.Entry<Object, Object>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<Integer>) ((Map.Entry<Integer, Integer>) (o2)).getValue()).compareTo(((Map.Entry<Integer, Integer>) (o1)).getValue());
            }
        });
        Map<Object, Object> resultMap = new LinkedHashMap<>();
        for (Iterator<Map.Entry<Object, Object>> it = list.iterator(); it.hasNext();) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
            resultMap.put(entry.getKey(), entry.getValue());
        }

        return resultMap;
    }

    private List<Map<Object, Object>> monthSort(Map<Object, Object> map){
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August",
                "September", "October", "November", "December"};
        List<Map<Object, Object>> resultList = new ArrayList<>();

        for(String month : months)
            resultList.add(Map.of(month, map.get(month)));

        return resultList;
    }

    private Map<Object, Object> numberKeySortByAsc(Map<Object, Object> map, String type){
        Object[] keyArray = map.keySet().toArray();
        Map<Object, Object> resultMap = new LinkedHashMap<>();

        if(type.equals("int")){
            int[] intArray = new int[keyArray.length];
            for(int i=0; i<keyArray.length; i++){
                intArray[i] = Integer.parseInt(keyArray[i].toString());
            }

            Arrays.sort(intArray);

            for(int i : intArray)
                resultMap.put(i, map.get(Integer.toString(i)));
        }
        else if(type.equals("double")){
            double[] doubleArray = new double[keyArray.length];
            for(int i=0; i<keyArray.length; i++){
                doubleArray[i] = Double.parseDouble(keyArray[i].toString());
            }

            Arrays.sort(doubleArray);

            for(double d : doubleArray)
                resultMap.put(Double.toString(d), map.get(Double.toString(d)));
        }

        return resultMap;
    }

    private List<Map<Object, Object>> getRange(Map<Object, Object> vfModelList){
        List<Map<Object, Object>> range = new ArrayList<>();
        Map<Object, Object> LastMapInRange = new HashMap<>();
        Map<Object, Object> ascList;
        Object[] keyArray = vfModelList.keySet().toArray();

        Map<Object, Object> vfModelList_copy = new LinkedHashMap<>(vfModelList);

        for(Object o : vfModelList.keySet()){
            double d = Double.parseDouble(o.toString());
            if(!((Double.toString(d)).equals(o.toString()))){
                vfModelList_copy.put(Double.toString(d),vfModelList_copy.remove(o));
            }
        }

        boolean isDouble=false;
        for(int i=0; i<100; i++){
            if(keyArray[i].toString().contains(".")){
                isDouble=true;
                break;
            }
        }

        if(isDouble){
            ascList = numberKeySortByAsc(vfModelList_copy, "double");}
        else{
            ascList = numberKeySortByAsc(vfModelList, "int");}


        Object[] objectKeyArray = ascList.keySet().toArray();
        String[] stringKeyArray = new String[objectKeyArray.length];
        for(int i = 0; i < stringKeyArray.length; i++)
            stringKeyArray[i] = objectKeyArray[i].toString();
        //String[] stringKeyArray = Arrays.copyOf(objectKeyArray,objectKeyArray.length,String[].class);


        int valueSum=0;

        if(isDouble){ //실수
            BigDecimal BDmin = new BigDecimal(stringKeyArray[0]);
            BigDecimal BDmax = new BigDecimal(stringKeyArray[stringKeyArray.length-1]);
            BigDecimal BigDist = (BDmax.subtract(BDmin)).divide(BigDecimal.valueOf(10), 100, RoundingMode.HALF_EVEN);
            BigDist = BigDecimal.valueOf(BigDist.doubleValue());


            for(String s : stringKeyArray){
                BigDecimal b;

                try {
                    b=new BigDecimal(s);
                } catch (Exception e) {
                    System.out.println("[BigDecimal except]:"+s);
                    e.printStackTrace();
                    continue;
                }

                if((b.compareTo(BDmin.add(BigDist))) < 0) {
                    try{
                        valueSum+=Integer.parseInt(ascList.get(s).toString());
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                else if(stringKeyArray[stringKeyArray.length-1].equals(s)){
                    valueSum+= Integer.parseInt(ascList.get(s).toString());
                    range.add(Map.of(BDmin.toString(), valueSum));
                }
                else{
                    range.add(Map.of(BDmin.toString(), valueSum));
                    BDmin = BDmin.add(BigDist);
                    valueSum = Integer.parseInt(ascList.get(s).toString());
                }
            }

            LastMapInRange.put(BDmax.toString(), null);
            range.add(LastMapInRange);
        }
        else{ //정수
            int Imin = Integer.parseInt(stringKeyArray[0]);
            int Imax = Integer.parseInt(stringKeyArray[stringKeyArray.length - 1]);
            int dist = (Imax-Imin)/10;
            int remains = (Imax-Imin)%10;
            int isRemain;

            for(String s : stringKeyArray) {
                int i;

                try {
                    i = Integer.parseInt(s);
                } catch (Exception e) {
                    System.out.println("[integer parse except]:" + s);
                    e.printStackTrace();
                    continue;
                }

                if(remains>0)
                    isRemain=1;
                else
                    isRemain=0;

                if (i < (Imin + dist + isRemain)) {
                    valueSum += Integer.parseInt(vfModelList.get(s).toString());
                }
                else if (stringKeyArray[stringKeyArray.length - 1].equals(s)) {
                    valueSum += Integer.parseInt(vfModelList.get(s).toString());
                    range.add(Map.of(Integer.toString(Imin), valueSum));
                }
                else {
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
                    basicProfile.setNull_cnt(((ValueDistributionAnalyzerResult) result).getNullCount());
                }

                int distinct_cnt = ((ValueDistributionAnalyzerResult) result).getDistinctCount();
                int row_cnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();

                profileTableResult.setDataset_row_cnt(row_cnt);
                //TODO: if(header=yes) Table row cnt=row_cnt+1

                basicProfile.setDistinct_cnt(distinct_cnt);
                basicProfile.setRow_cnt(row_cnt);
                basicProfile.setUnique_cnt(((ValueDistributionAnalyzerResult) result).getUniqueCount());
                basicProfile.setDistinctness((double) distinct_cnt / row_cnt);

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

                vfModelList = valueSortByDesc(vfModelList);

                if(distinct_cnt > 100){
                    vdModel.setType("top100");

                    Object[] keys = vfModelList.keySet().toArray();
                    for(int i = 0; i < 100; i++) {
                        vdValueList.add(Map.of(
                                keys[i], vfModelList.get(keys[i])
                        ));
                    }
                    vdModel.setValue(vdValueList);

                    if(profileColumnResult.getColumn_type().equals("number")){
                        List<Map<Object, Object>> range = getRange(vfModelList);
                        vdModel.setRange(range);
                    }
                    else
                        vdModel.setRange("-");

                }
                else{
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
            if (profileColumnResult.getColumn_type().equals("date") && result instanceof CrosstabResult &&
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

                vdMonthList = monthSort(monthList);
                dateProfile.setMonth_distribution(vdMonthList);

                yearList = numberKeySortByAsc(yearList,"int");
                for(Object year : yearList.keySet().toArray()){
                    vdYearList.add(Map.of(year, yearList.get(year)));
                }
                dateProfile.setYear_distribution(vdYearList);
            }
        }


        DecimalFormat form = new DecimalFormat("#.###");

        for (AnalyzerResult result : results) {
            if (result instanceof StringAnalyzerResult) {
                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(((StringAnalyzerResult) result).getNullCount(targetInputColumn));
                }

                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) < totalCnt) {
                    stringProfile.setAvg_len(Double.parseDouble(form.format(((StringAnalyzerResult) result).getAvgChars(targetInputColumn))));
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
                    numberProfile.setSum(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getSum(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getMean(targetInputColumn) != null) {
                    numberProfile.setMean(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getMean(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getMedian(targetInputColumn) != null) {
                    numberProfile.setMedian(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getMedian(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getStandardDeviation(targetInputColumn) != null) {
                    numberProfile.setSd(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getStandardDeviation(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getVariance(targetInputColumn) != null) {
                    numberProfile.setVariance(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getVariance(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getPercentile25(targetInputColumn) != null) {
                    numberProfile.setPercentile_25th(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getPercentile25(targetInputColumn))));
                }
                if (((NumberAnalyzerResult) result).getPercentile75(targetInputColumn) != null) {
                    numberProfile.setPercentile_75th(Double.parseDouble(form.format(((NumberAnalyzerResult) result).getPercentile75(targetInputColumn))));
                }
            }

            if (result instanceof ValueMatchAnalyzerResult) {
                if (((ValueMatchAnalyzerResult) result).getCount("0") != null) {
                    numberProfile.setZero_cnt(((ValueMatchAnalyzerResult) result).getCount("0"));
                }
            }

            if (result instanceof DateAndTimeAnalyzerResult) {
                Object value;

                if (((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && basicProfile.getNull_cnt() == 0) {
                    basicProfile.setNull_cnt(((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn));
                }

                value = ((CrosstabResult) result).getCrosstab().where("Column", targetInputColumn.getName()).where("Measure", "Highest date")
                        .safeGet(null);
                if(value!=null)
                    dateProfile.setHighest_date((String)value);
                else
                    dateProfile.setHighest_date("-");

                value = ((CrosstabResult) result).getCrosstab().where("Column", targetInputColumn.getName()).where("Measure", "Lowest date")
                        .safeGet(null);
                if(value!=null)
                    dateProfile.setLowest_date((String)value);
                else
                    dateProfile.setLowest_date("-");

                value = ((CrosstabResult) result).getCrosstab().where("Column", targetInputColumn.getName()).where("Measure", "Mean")
                        .safeGet(null);
                if(value!=null)
                    dateProfile.setMean_date((String)value);
                else
                    dateProfile.setMean_date("-");

                value = ((CrosstabResult) result).getCrosstab().where("Column", targetInputColumn.getName()).where("Measure", "Median")
                        .safeGet(null);
                if(value!=null)
                    dateProfile.setMedian_date((String)value);
                else
                    dateProfile.setMedian_date("-");

                value = ((CrosstabResult) result).getCrosstab().where("Column", targetInputColumn.getName()).where("Measure", "25th percentile")
                        .safeGet(null);
                if(value!=null)
                    dateProfile.setPercentile_25th((String)value);
                else
                    dateProfile.setPercentile_25th("-");

                value = ((CrosstabResult) result).getCrosstab().where("Column", targetInputColumn.getName()).where("Measure", "75th percentile")
                        .safeGet(null);
                if(value!=null)
                    dateProfile.setPercentile_75th((String)value);
                else
                    dateProfile.setPercentile_75th("-");

                //basicProfile.setValue_distribution(vfModelList);
            }
        }

        profileColumnResult.getProfiles().put("basic_profile", basicProfile);
        if(profileColumnResult.getColumn_type().equals("number"))
            profileColumnResult.getProfiles().put("number_profile", numberProfile);
        if(profileColumnResult.getColumn_type().equals("string"))
            profileColumnResult.getProfiles().put("string_profile", stringProfile);
        if(profileColumnResult.getColumn_type().equals("date"))
            profileColumnResult.getProfiles().put("date_profile", dateProfile);
    }
}