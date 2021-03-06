package kr.co.promptech.profiler.service;

import kr.co.promptech.profiler.model.profile.ProfileDetail;
import kr.co.promptech.profiler.model.profile.ProfileValue;
import kr.co.promptech.profiler.service.profile.ProfileTargetService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.stereotype.Component;

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
    private final ProfileTargetService profileTargetService;

    /**
     * 타겟 테이블의 모든 컬럼에 대해 프로파일을 수행하는 메소드
     *
     * @param tableName   타겟 테이블명
     * @param columnNames 타겟 테이블의 컬럼리스트
     */
    public void profileColumns(String tableName, List<String> columnNames) {

        for (String columnName : columnNames) {

            this.profileSingleColumn(tableName, columnName);
        }

    }

    /**
     * 타겟 테이블의 한 컬럼별 프로파일을 수행하는 메소드
     *
     * @param tableName  타겟 테이블명
     * @param columnName 타겟 테이블의 1개 컬럼
     */
    private void profileSingleColumn(String tableName, String columnName) {
        String inputColumnName = "profiler." + tableName + "." + columnName;
        System.out.println("inputColumnName : " + inputColumnName);

        DataStoreService.setDataStore(DataStoreService.DEFAULT_DB);
        AnalysisJobBuilder builder = DataStoreService.getBuilder();
        builder.addSourceColumns(inputColumnName);
        InputColumn<?> targetInputColumn = builder.getSourceColumnByName(columnName);

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
        ProfileDetail profileDetailModel = new ProfileDetail();
        int totalCnt = 0;

        List<ProfileValue> vfModelList = new ArrayList<>();

        profileDetailModel.setNullCnt(0);
        profileDetailModel.setBlankCnt(0);

        profileDetailModel.setColumnName(columnName);
        //TODO: COLUMN COMMENT 가져오기
//        resultModel.setColumnDesc(columnInfo.getColumnDesc());

        for (AnalyzerResult result : results) {
            if (result instanceof ValueDistributionAnalyzerResult) {
                if (((ValueDistributionAnalyzerResult) result).getNullCount() > 0) {
                    profileDetailModel.setNullCnt(((ValueDistributionAnalyzerResult) result).getNullCount());
                }

                profileDetailModel.setDistinctCnt(((ValueDistributionAnalyzerResult) result).getDistinctCount());
                profileDetailModel.setRowCnt(((ValueDistributionAnalyzerResult) result).getTotalCount());

                Collection<ValueFrequency> vfList = ((ValueDistributionAnalyzerResult) result).getValueCounts();
                for (ValueFrequency vf : vfList) {
                    if (vf.getChildren() != null) {
                        Collection<ValueFrequency> vfChildren = vf.getChildren();
                        for (ValueFrequency vfChild : vfChildren) {
                            ProfileValue profileValueModel = new ProfileValue();

                            profileValueModel.setColumnName(columnName);
                            //TODO: COLUMN COMMENT 가져오기
//                            resultVFModel.setColumnDesc(columnInfo.getColumnDesc());
                            profileValueModel.setColumnGroupVal(vfChild.getValue());
                            profileValueModel.setColumnGroupCount(vfChild.getCount());
                            vfModelList.add(profileValueModel);
                        }
                    } else {
                        ProfileValue profileValueModel = new ProfileValue();

                        profileValueModel.setColumnName(columnName);
                        //TODO: COLUMN COMMENT 가져오기
//                        resultVFModel.setColumnDesc(columnInfo.getColumnDesc());
                        profileValueModel.setColumnGroupVal(vf.getValue());
                        profileValueModel.setColumnGroupCount(vf.getCount());
                        vfModelList.add(profileValueModel);
                    }
                }

                totalCnt = ((ValueDistributionAnalyzerResult) result).getTotalCount();
            }
        }

        for (AnalyzerResult result : results) {
            if (result instanceof StringAnalyzerResult) {
                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && profileDetailModel.getNullCnt() == 0) {
                    profileDetailModel.setNullCnt(((StringAnalyzerResult) result).getNullCount(targetInputColumn));
                }

                if (((StringAnalyzerResult) result).getNullCount(targetInputColumn) < totalCnt) {
                    profileDetailModel.setStrAvgLenVal(((StringAnalyzerResult) result).getAvgChars(targetInputColumn));
                    profileDetailModel.setStrMaxLenVal(((StringAnalyzerResult) result).getMaxChars(targetInputColumn));
                    profileDetailModel.setStrMinLenVal(((StringAnalyzerResult) result).getMinChars(targetInputColumn));
                }
                profileDetailModel.setBlankCnt(((StringAnalyzerResult) result).getBlankCount(targetInputColumn));
            }

            if (result instanceof NumberAnalyzerResult) {

                if (((NumberAnalyzerResult) result).getNullCount(targetInputColumn).intValue() > 0 && profileDetailModel.getNullCnt() == 0)
                    profileDetailModel.setNullCnt(((NumberAnalyzerResult) result).getNullCount(targetInputColumn).intValue());


                if (((NumberAnalyzerResult) result).getHighestValue(targetInputColumn) != null) {
                    profileDetailModel.setNumMaxVal((Double) ((NumberAnalyzerResult) result).getHighestValue(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getLowestValue(targetInputColumn) != null) {
                    profileDetailModel.setNumMinVal((Double) ((NumberAnalyzerResult) result).getLowestValue(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getMean(targetInputColumn) != null) {
                    profileDetailModel.setNumMeanVal((Double) ((NumberAnalyzerResult) result).getMean(targetInputColumn));
                }
                if (((NumberAnalyzerResult) result).getMedian(targetInputColumn) != null) {
                    profileDetailModel.setNumMedianVal((Double) ((NumberAnalyzerResult) result).getMedian(targetInputColumn));
                }
            }

            if (result instanceof DateAndTimeAnalyzer) {
                if (((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn) > 0 && profileDetailModel.getNullCnt() == 0) {
                    profileDetailModel.setNullCnt(((DateAndTimeAnalyzerResult) result).getNullCount(targetInputColumn));
                }
            }
        }

        profileDetailModel.setDuplicateCnt(totalCnt - profileDetailModel.getDistinctCnt());
        profileDetailModel.setFrquentMaxVal(findFrequentMaxVal(vfModelList));
        profileDetailModel.setFrquentMinVal(findFrequentMinVal(vfModelList));

        // add Results to ProfileTarget instance
        this.profileTargetService.addProfileDetail(profileDetailModel, tableName);
        this.profileTargetService.addProfileValue(vfModelList, tableName);
    }

    /**
     * Value Frequency 분석값 중 최다 빈도값을 찾는 메소드
     *
     * @param profileValues Value Frequency 분석에 대한 Resultset
     * @return 최다 빈도수 데이터 반환
     */
    private String findFrequentMaxVal(List<ProfileValue> profileValues) {
        int max = 0;
        int index = -1;

        if (profileValues.size() == 0 || (profileValues.size() == 1 && StringUtils.isBlank(profileValues.get(0).getColumnGroupVal())))
            return null;

        for (int i = 0; i < profileValues.size(); i++) {
            String val = profileValues.get(i).getColumnGroupVal();
            if (StringUtils.isBlank(val)) {
                continue;
            }

            if (max < profileValues.get(i).getColumnGroupCount()) {
                max = profileValues.get(i).getColumnGroupCount();
                index = i;
            }
        }

        return profileValues.get(index).getColumnGroupVal();
    }

    /**
     * Value Frequency 분석값 중 최소 빈도값을 찾는 메소드
     *
     * @param profileValues Value Frequency 분석에 대한 Resultset
     * @return 최다 빈도수 데이터 반환
     */
    private String findFrequentMinVal(List<ProfileValue> profileValues) {
        int min = Integer.MAX_VALUE;
        int index = -1;

        if (profileValues.size() == 0 || (profileValues.size() == 1 && StringUtils.isBlank(profileValues.get(0).getColumnGroupVal())))
            return null;

        for (int i = 0; i < profileValues.size(); i++) {
            String val = profileValues.get(i).getColumnGroupVal();
            if (StringUtils.isBlank(val)) {
                continue;
            }

            if (min > profileValues.get(i).getColumnGroupCount()) {
                min = profileValues.get(i).getColumnGroupCount();
                index = i;
            }
        }
        return profileValues.get(index).getColumnGroupVal();
    }

//    private boolean isRemovalColumn(String tableName, String columnName, Map<String, Set<String>> removals) {
//        if (!removals.containsKey(tableName)) {
//            return false;
//        }
//
//        Set<String> removalColumns = removals.get(tableName);
//        return removalColumns.contains(columnName);
//    }
}

