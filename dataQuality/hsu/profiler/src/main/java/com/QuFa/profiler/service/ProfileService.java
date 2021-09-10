package com.QuFa.profiler.service;


import com.QuFa.profiler.config.ActiveProfileProperty;
import com.QuFa.profiler.model.Local;
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

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
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
            dfs.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            dfs.add(new SimpleDateFormat("yyyy-MM-dd"));
            dfs.add(new SimpleDateFormat("MM/dd/yyyy"));
            dfs.add(new SimpleDateFormat("HH:mm:ss"));
            dfs.add(new SimpleDateFormat("dd/MM/yyyy HH:mm"));
            dfs.add(new SimpleDateFormat("dd/MM/yy HH:mm"));
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

    private Map<Object, Object> valueSort(Map<Object, Object> map){
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

    private Map<Object, Object> monthSort(Map<Object, Object> map){
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August",
                "September", "October", "November", "December"};
        Map<Object, Object> resultMap = new LinkedHashMap<>();

        for(String month : months)
            resultMap.put(month, map.get(month));

        return resultMap;
    }

    private Map<Object, Object> yearSort(Map<Object, Object> map){
        Object[] years = map.keySet().toArray();
        Map<Object, Object> resultMap = new LinkedHashMap<>();

        Arrays.sort(years);
        for(Object year : years)
            resultMap.put(year, map.get(year));

        return resultMap;

    }

    private Map<Object, Integer> getRange(Map<Object, Object> vfModelList){
        Map<Object, Integer> range = new LinkedHashMap<>();
        Map<Object, Object> ascList = yearSort(vfModelList);
        Object[] objectKeyArray = ascList.keySet().toArray();

        //정수실수 판별

        boolean isDouble=false;
        Object[] keyArray = vfModelList.keySet().toArray();
        for(int i=0; i<100; i++){
            if(keyArray[i].toString().contains(".")){
                isDouble=true;
                break;
            }
        }

        int count=0;

        String[] stringKeyArray = Arrays.copyOf(objectKeyArray,objectKeyArray.length,String[].class);
        //String[] array = ascList.keySet().toArray(new String[ascList.size()]);

        //if(isDouble){ //실수
            double Dmin = Double.parseDouble(stringKeyArray[0]);
            double Dmax = Double.parseDouble(stringKeyArray[stringKeyArray.length - 1]);
        BigDecimal BDmin = new BigDecimal(stringKeyArray[0]);
        BigDecimal BDmax = new BigDecimal(stringKeyArray[stringKeyArray.length-1]);
            BigDecimal BigDist = (BDmax.subtract(BDmin)).divide(new BigDecimal("10"), 100, RoundingMode.HALF_EVEN);
            double dist = Double.parseDouble(BigDist.toString());
            BigDist = new BigDecimal(""+dist);

        System.out.println("Dmin:"+Dmin);
        System.out.println("Dmax:"+Dmax);
        System.out.println("dist:"+dist);
        System.out.println("len:"+stringKeyArray.length);


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
                    count+=Integer.parseInt(vfModelList.get(s).toString());
                }
                else if(stringKeyArray[stringKeyArray.length-1].equals(s)){
                    count+= Integer.parseInt(vfModelList.get(s).toString());
                    range.put(BDmin.toString(), count);
                }
                else{
                    range.put(BDmin.toString(), count);
                    BDmin = BDmin.add(BigDist);
                    count = Integer.parseInt(vfModelList.get(s).toString());
                }
            }
//        }
//        else{ //정수
//            Integer[] IList = (Integer[])ascList.keySet().toArray();
//            int Imin = IList[0];
//            int Imax = IList[IList.length - 1];
//            int dist = (Imax-Imin)/10;
//
//            for(int i : IList){
//                if(i < Imin+dist)
//                    count++;
//                else{
//                    range.put(Imin, count);
//                    Imin += dist;
//                    count = 1;
//                }
//            }
//        }

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

                vfModelList = valueSort(vfModelList);

                if(distinct_cnt > 100){
                    vdModel.setType("top100");

                    Map<Object, Object> top = new LinkedHashMap<>();
                    Object[] keys = vfModelList.keySet().toArray();
                    for(int i = 0; i < 100; i++)
                        top.put(keys[i], vfModelList.get(keys[i]));
                    vdModel.setValue(top);

                    if(profileColumnResult.getColumn_type().equals("number")){
                        Map<Object, Integer> range = getRange(vfModelList);

                        vdModel.setRange(range);
                    }
                    else
                        vdModel.setRange("-");

                }
                else{
                    vdModel.setType("all");
                    vdModel.setValue(vfModelList);
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

                monthList = monthSort(monthList);
                dateProfile.setMonth_distribution(monthList);

                yearList = yearSort(yearList);
                dateProfile.setYear_distribution(yearList);
            }
        }

        /*
         *
         *
//         */


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
                    System.out.println("SUM::"+((NumberAnalyzerResult) result).getSum(targetInputColumn));
                    //TODO:Sum,Median double 오차 해결
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