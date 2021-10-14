package com.example.datafairnessmodule.controller;

import com.example.datafairnessmodule.payload.UploadFileResponse;
import com.example.datafairnessmodule.property.FileStorageProperties;
import com.example.datafairnessmodule.repository.MainRepository;
import com.example.datafairnessmodule.service.FileStorageService;
import com.opencsv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.datafairnessmodule.config.MainConfig.*;

@RestController
@RequestMapping(value = "/api")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @Autowired
    private MainRepository mainRepository;

    private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/")
                .path(fileName).toUriString();
        return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
    }

    @PostMapping("/uploadMultipleFiles")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        return Arrays.asList(files).stream().map(file -> uploadFile(file)).collect(Collectors.toList());
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /*
     * ********************************************************************************
     * Module 1
     * ********************************************************************************
     */

    @PostMapping("/module1/submitUpload")
    public Map<String, Object> module1SubmitUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        String sourceFileName = fileStorageService.storeFile(file);
        String sourceFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(sourceFileName).toUriString();
        String exceptFirstRow = request.getParameter("except");
        if (exceptFirstRow == null || exceptFirstRow.equals("")) {
            exceptFirstRow = "false";
        }
        Resource resource = fileStorageService.loadFileAsResource(sourceFileName);
        String filepath = resource.getURI().toString();
        Map<String, Object> csvMap = readDataCsv(filepath, exceptFirstRow);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("origin", sourceFileName);
        paramMap.put("filename", sourceFileName);
        paramMap.put("fileurl", sourceFileDownloadUri);
        paramMap.put("filetype", file.getContentType());
        paramMap.put("filesize", file.getSize());
        paramMap.put("data", csvMap);
//        return new SubmitFormResponse(sourceFileName, sourceFileDownloadUri, file.getContentType(), file.getSize());
        return paramMap;
    }

    private Map<String, Object> readDataCsv(String filepath, String exceptFirstRow) throws Exception {
        Reader reader = Files.newBufferedReader(Paths.get(URI.create(filepath)));
        int skipLines = exceptFirstRow.equals("true") ? 1 : 0;
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(skipLines).build();
        List<String[]> rows = csvReader.readAll();
        String[] columns;
        String[] exampleRow;
        try {
            columns = rows.get(skipLines);
            exampleRow = arrayTrim(rows.get(0));
        } catch (Exception e) {
            columns = new String[0];
            exampleRow = new String[0];
        }
        Map<String, Object> csvMap = new HashMap<>();
        csvMap.put("columns", columns.length);
        csvMap.put("rows", rows.size());
        csvMap.put("example", exampleRow);
        return csvMap;
    }

    private String[] arrayTrim(String[] arr) {
        String[] newArr = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = arr[i].trim();
        }
        return newArr;
    }

    @PostMapping("/module1/submitConvert")
    public Map<String, Object> module1SubmitConvert(@RequestParam(value = "select") List<String> select, HttpServletRequest request) throws Exception {
        // 선택한 체크박스(컬럼)가 없으면 종료
        if (select.size() < 1) {
            return new HashMap<>();
        }
        // 입력 파라미터 정의 & 초기화
        String origin = request.getParameter("origin");
        String sourceFileName = request.getParameter("filename");
        String[] exclude = request.getParameterValues("exclude");
        String[] include = request.getParameterValues("include");
        String except = request.getParameter("except");
        String limit_row = request.getParameter("limit-row");
        String[][][] exclude_arr = parseIncludeString(exclude);
        String[][][] include_arr = parseIncludeString(include);
        int[][] count_exclude = initialCount(exclude_arr[1]);
        int[][] count_include = initialCount(include_arr[1]);
        int limit = 0;
        if (limit_row != null && !limit_row.trim().equals("")) {
            try {
                limit = Integer.parseInt(limit_row);
            } catch (Exception e) {
                limit = 0;
            }
        }
        // 새로만들 CSV 파일명 정의
        Map<String, String> map_temp = genNewFilename(origin);
        String filepath = map_temp.get("filepath");
        String filename2 = map_temp.get("filename2");
        String filepath2 = map_temp.get("filepath2");
        // CSV 쓰기: 새파일, 읽기: 업로드파일 정의
        int count_write = 0;
        boolean isDebug = false;
        if (isDebug) logger.info("\n");
        Writer writer = Files.newBufferedWriter(Paths.get(filepath2));
        ICSVWriter icsvWriter = new CSVWriterBuilder(writer).withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).build();
        int skipLines = except.equals("true") ? 1 : 0;
        Reader reader = Files.newBufferedReader(Paths.get(filepath));
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(skipLines).build();
        List<String[]> rows = csvReader.readAll();
        if (skipLines > 0) {
            Reader reader_temp = Files.newBufferedReader(Paths.get(filepath));
            CSVReader csvReader_temp = new CSVReaderBuilder(reader_temp).build();
            List<String[]> rows_temp = csvReader_temp.readAll();
            icsvWriter.writeNext(rows_temp.get(0));
        }
        // 업로드파일을 라인단위로 처리
        for (int i = 0; i < rows.size(); i++) {
            String[] columns = rows.get(i);
            String[] columns_temp = new String[select.size()];
            int columns_temp_index = -1;
            // 데이터 쓰기 판단
            boolean match_row = true;
            if (isDebug) System.out.print("row" + i + "::");
            // 컬럼단위 처리
            for (int j = 0; j < columns.length; j++) {
                if (isDebug) System.out.print("col" + j + ":" + columns[j] + ":");
                boolean match_select = matchSelect(select, Integer.toString(j));
                boolean match_exclude = matchExclude(columns[j].trim(), exclude_arr[0][j]);
                boolean match_include = matchInclude(columns[j].trim(), include_arr[0][j]);
                // checkbox 판단
                if (match_select) {
                    // 체크박스가 선택된 컬럼은 새컬럼에 담는다
                    // 새컬럼을 새파일에 담을지는 아래에서 판단한다
                    columns_temp_index++;
                    columns_temp[columns_temp_index] = columns[j].trim();
                } else {
                }
                if (isDebug) System.out.print("(check:" + match_select + ",");
                // 제외문자(exclude) 판단
                if (match_exclude) {
                } else {
                    // 제외문자가 매칭되면 제한개수없이 포함되지 않는다
//                    match_row = false;
                    // 제외문자가 매칭되어 새파일에 포함안되는 컬럼이나,
                    // 제한개수를 이미 넘은 경우이면 새파일에 포함시킨다
                    for (int k = 0; k < exclude_arr[0][j].length; k++) {
                        String s1 = columns[j].trim().toUpperCase();
                        String s2 = exclude_arr[0][j][k].trim().toUpperCase();
                        if (s1.contains(s2) && !s2.equals("")) {
                            count_exclude[j][k]++;
                            int d = Integer.parseInt(exclude_arr[1][j][k]);
                            if (d == 0 || count_exclude[j][k] <= d) {
                                match_row = false;
                            }
                        }
                    }
                }
                if (isDebug) System.out.print("exclude:" + match_exclude + ",");
                // 포함문자(include) 판단
                if (match_include) {
                    // 포함문자가 매칭되어 새파일에 포함되는 컬럼이나
                    // 제한개수를 이미 넘은 경우이면 새파일에 쓰지 않는다
                    for (int k = 0; k < include_arr[0][j].length; k++) {
                        String s1 = columns[j].trim().toUpperCase();
                        String s2 = include_arr[0][j][k].trim().toUpperCase();
                        if (s1.contains(s2) && !s2.equals("")) {
                            count_include[j][k]++;
                            int d = Integer.parseInt(include_arr[1][j][k]);
                            if (d > 0 && count_include[j][k] > d) {
                                match_row = false;
                            }
                        }
                    }
                } else {
                    match_row = false;
                }
                if (isDebug) System.out.print("include:" + match_include + "):");
            }
            if (isDebug) System.out.print(":" + match_row + "\n");
            // 새파일에 쓰기
            if (match_row) {
                if (limit > 0) {
                    count_write++;
                    if (count_write <= limit) {
                        icsvWriter.writeNext(columns_temp);
                    }
                } else {
                    icsvWriter.writeNext(columns_temp);
                }
            }
        }
        reader.close();
        csvReader.close();
        icsvWriter.close();
        writer.close();
        String sourceFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(filename2).toUriString();
        Resource resource = fileStorageService.loadFileAsResource(filename2);
        String filepath_temp = resource.getURI().toString();
        Map<String, Object> csvMap = readDataCsv(filepath_temp, except);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", csvMap);
        resultMap.put("origin", origin);
        resultMap.put("filename", filename2);
        resultMap.put("fileurl", sourceFileDownloadUri);
        /*
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("filename", filename2);
        resultMap.put("fileurl", sourceFileDownloadUri);
        resultMap.put("select", select);
        resultMap.put("exclude", exclude.length);
        resultMap.put("include", include.length);
        resultMap.put("except", except);
        resultMap.put("rows", rows.size());
        resultMap.put("cols", rows.get(0).length);
         */
//        return new SubmitFormResponse(sourceFileName, sourceFileDownloadUri, file.getContentType(), file.getSize());
        return resultMap;
    }

    // Exclude, Include 입력값을 파싱하여 배열을 리턴한다
    // 1st array : 0: 매칭문자열, 1: 매칭개수
    // 2nd array : 컬럼인덱스
    // 3rd array : 콤마구분값
    private String[][][] parseIncludeString(String[] s) {
        String[][][] a1 = new String[2][s.length][];
        for (int i = 0; i < s.length; i++) {
            String[] a2 = s[i].trim().split(",");
            String[] a3 = new String[a2.length];
            String[] a4 = new String[a2.length];
            for (int j = 0; j < a2.length; j++) {
                String[] a5 = a2[j].trim().split(":");
                a3[j] = a5[0].trim();
                if (a5.length == 1) {
                    a4[j] = "0";
                } else {
                    a4[j] = a5[1].trim();
                }
            }
            a1[0][i] = a3;
            a1[1][i] = a4;
        }
        return a1;
    }

    private String[][][] parseIncludeString(List<String> s) {
        String[][][] a1 = new String[2][s.size()][];
        for (int i = 0; i < s.size(); i++) {
            String[] a2 = s.get(i).trim().split(",");
            String[] a3 = new String[a2.length];
            String[] a4 = new String[a2.length];
            for (int j = 0; j < a2.length; j++) {
                String[] a5 = a2[j].trim().split(":");
                a3[j] = a5[0].trim();
                if (a5.length == 1) {
                    a4[j] = "0";
                } else {
                    a4[j] = a5[1].trim();
                }
            }
            a1[0][i] = a3;
            a1[1][i] = a4;
        }
        return a1;
    }

    // 매칭카운트배열 초기화
    private int[][] initialCount(String[][] s) {
        int[][] a = new int[s.length][];
        for (int i = 0; i < s.length; i++) {
            int[] a1 = new int[s[i].length];
            for (int j = 0; j < s[i].length; j++) {
                a1[j] = 0;
            }
            a[i] = a1;
        }
        return a;
    }

    // 리스트에 문자열이 포함되는지를 리턴한다
    private boolean matchSelect(List<String> list, String s) {
        return list.contains(s);
    }

    // 문자열1에 문자열2가 포함되는지를 리턴한다
    private boolean matchExclude(String s1, String[] s2) {
        if (s2.length == 1 && s2[0].trim().equals("")) {
            return true;
        }
        for (String s : s2) {
            if (s1.toUpperCase().contains(s.trim().toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    // 문자열1에 문자열2가 포함되는지를 리턴한다
    private boolean matchInclude(String s1, String[] s2) {
        for (String s : s2) {
            if (s1.toUpperCase().contains(s.trim().toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    // 새파일명만들기
    private Map<String, String> genNewFilename(String s) {
        Map<String, String> map = new HashMap<>();
        try {
            Resource resource = fileStorageService.loadFileAsResource(s);
            File file = resource.getFile();
            String filepath = file.getPath();
            String filename = file.getName();
            int pos = filename.lastIndexOf(".");
            String str1 = filename.substring(0, pos);
            String str2 = filename.substring(pos + 1);
            long l = System.currentTimeMillis();
            String s1 = Long.toString(l);
            s1 = s1.substring(4);
            String filename2 = str1 + "_" + s1 + "." + str2;
            String filepath2 = filepath.replace(filename, filename2);
            File file2 = new File(filepath2);
            if (file2.exists()) {
                file2.delete();
            }
            map.put("filepath", filepath);
            map.put("filename2", filename2);
            map.put("filepath2", filepath2);
        } catch (Exception e) {
        }
        return map;
    }

    @PostMapping("/module1/submitDownload")
    public Map<String, Object> module1SubmitDownload(@RequestParam(value = "filenames") List<String> filenames, HttpServletRequest request) throws Exception {
        String origin = request.getParameter("origin");
        String except = request.getParameter("except");
        if (except == null || except.equals("")) {
            except = "false";
        }
        Map<String, String> map_temp = genNewFilename(origin);
        String originpath = map_temp.get("filepath");
        String filename2 = map_temp.get("filename2");
        String filepath2 = map_temp.get("filepath2");
        Writer writer = Files.newBufferedWriter(Paths.get(filepath2));
        ICSVWriter icsvWriter = new CSVWriterBuilder(writer).withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).build();
        int skipLines = except.equals("true") ? 1 : 0;
        if (skipLines > 0) {
            Reader reader_temp = Files.newBufferedReader(Paths.get(originpath));
            CSVReader csvReader_temp = new CSVReaderBuilder(reader_temp).build();
            List<String[]> rows_temp = csvReader_temp.readAll();
            icsvWriter.writeNext(rows_temp.get(0));
        }
        for (String s : filenames) {
            Resource resource = fileStorageService.loadFileAsResource(s);
            String filepath = resource.getURI().toString();
            Reader reader = Files.newBufferedReader(Paths.get(URI.create(filepath)));
            CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(skipLines).build();
            List<String[]> rows = csvReader.readAll();
            icsvWriter.writeAll(rows);
            reader.close();
            csvReader.close();
            logger.info("writeAll: " + s);
        }
        icsvWriter.close();
        writer.close();
        String sourceFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(filename2).toUriString();
        Map<String, Object> map = new HashMap<>();
        map.put("filename", filename2);
        map.put("fileurl", sourceFileDownloadUri);
        return map;
    }

    @PostMapping("/module1/convertRun")
    @ResponseBody
    public Map<String, Object> module1CsvConvertRun(@RequestBody Map<String, Object> map1) throws Exception {
        logger.info("ConvertRun " + map1);
        printConvertRunMap(map1);
        String filename = (String) map1.get("filename");
        String except = (String) map1.get("except");
        List<Map<String, Object>> list2 = new ArrayList<>();
        List<Map<String, Object>> list1 = (List<Map<String, Object>>) map1.get("convert");
        for (Map<String, Object> map3 : list1) {
            Map<String, Object> map4 = module1CsvConvert(filename, except, map3);
            list2.add(map4);
            logger.info(String.valueOf(map4));
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        if (except == null || !except.equals("true")) {
            except = "false";
        }
        Map<String, String> map_temp = genNewFilename(filename);
        String originpath = map_temp.get("filepath");
        String newFilename = map_temp.get("filename2");
        String newFilepath = map_temp.get("filepath2");
        Writer writer = Files.newBufferedWriter(Paths.get(newFilepath));
        ICSVWriter icsvWriter = new CSVWriterBuilder(writer).withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).build();
        int skipLines = except.equals("true") ? 1 : 0;
        if (skipLines > 0) {
            Reader reader_temp = Files.newBufferedReader(Paths.get(originpath));
            CSVReader csvReader_temp = new CSVReaderBuilder(reader_temp).build();
            List<String[]> rows_temp = csvReader_temp.readAll();
            icsvWriter.writeNext(rows_temp.get(0));
        }
        for (Map<String, Object> map4 : list2) {
            String s = (String) map4.get("filename");
            Resource resource = fileStorageService.loadFileAsResource(s);
            String filepath = resource.getURI().toString();
            Reader reader = Files.newBufferedReader(Paths.get(URI.create(filepath)));
            CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(skipLines).build();
            List<String[]> rows = csvReader.readAll();
            icsvWriter.writeAll(rows);
            reader.close();
            csvReader.close();
            logger.info("writeAll: " + s);
        }
        icsvWriter.close();
        writer.close();
        String sourceFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(newFilename).toUriString();
        Map<String, Object> map2 = new HashMap<>();
        map2.put("convert", list2);
        map2.put("filename", newFilename);
        map2.put("fileurl", sourceFileDownloadUri);
        return map2;
    }

    public Map<String, Object> module1CsvConvert(String filename, String except, Map<String, Object> map1) throws Exception {
        logger.info("Convert " + map1);
        printConvertMap(map1);
        List<String> select = (List<String>) map1.get("select");
        List<String> exclude = (List<String>) map1.get("exclude");
        List<String> include = (List<String>) map1.get("include");
        String limit_row = (String) map1.get("limit");
        String[][][] exclude_arr = parseIncludeString(exclude);
        String[][][] include_arr = parseIncludeString(include);
        int[][] count_exclude = initialCount(exclude_arr[1]);
        int[][] count_include = initialCount(include_arr[1]);
        int limit = 0;
        if (limit_row != null && !limit_row.trim().equals("")) {
            try {
                limit = Integer.parseInt(limit_row);
            } catch (Exception e) {
                limit = 0;
            }
        }
        Map<String, String> map_temp = genNewFilename(filename);
        String filepath = map_temp.get("filepath");
        String newFilename = map_temp.get("filename2");
        String newFilepath = map_temp.get("filepath2");
        int count_write = 0;
        Writer writer = Files.newBufferedWriter(Paths.get(newFilepath));
        ICSVWriter icsvWriter = new CSVWriterBuilder(writer).withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER).build();
        int skipLines = except.equals("true") ? 1 : 0;
        Reader reader = Files.newBufferedReader(Paths.get(filepath));
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(skipLines).build();
        List<String[]> rows = csvReader.readAll();
        if (skipLines > 0) {
            Reader reader_temp = Files.newBufferedReader(Paths.get(filepath));
            CSVReader csvReader_temp = new CSVReaderBuilder(reader_temp).build();
            List<String[]> rows_temp = csvReader_temp.readAll();
            icsvWriter.writeNext(rows_temp.get(0));
        }
        for (int i = 0; i < rows.size(); i++) {
            String[] columns = rows.get(i);
            String[] columns_temp = new String[select.size()];
            int columns_temp_index = -1;
            boolean match_row = true;
            for (int j = 0; j < columns.length; j++) {
                boolean match_select = Boolean.parseBoolean(select.get(j));
                boolean match_exclude = matchExclude(columns[j].trim(), exclude_arr[0][j]);
                boolean match_include = matchInclude(columns[j].trim(), include_arr[0][j]);
//                logger.info(i + "." + j + ":" + columns[j].trim() + "::select:" + match_select + ",exclude:" + match_exclude + ",include:" + match_include);
                if (match_select) {
                    columns_temp_index++;
                    columns_temp[columns_temp_index] = columns[j].trim();
                } else {
                }
                if (match_exclude) {
                } else {
                    for (int k = 0; k < exclude_arr[0][j].length; k++) {
                        String s1 = columns[j].trim().toUpperCase();
                        String s2 = exclude_arr[0][j][k].trim().toUpperCase();
                        if (s1.contains(s2) && !s2.equals("")) {
                            count_exclude[j][k]++;
                            int d = Integer.parseInt(exclude_arr[1][j][k]);
                            if (d == 0 || count_exclude[j][k] <= d) {
                                match_row = false;
                            }
                        }
                    }
                }
                if (match_include) {
                    for (int k = 0; k < include_arr[0][j].length; k++) {
                        String s1 = columns[j].trim().toUpperCase();
                        String s2 = include_arr[0][j][k].trim().toUpperCase();
                        if (s1.contains(s2) && !s2.equals("")) {
                            count_include[j][k]++;
                            int d = Integer.parseInt(include_arr[1][j][k]);
                            if (d > 0 && count_include[j][k] > d) {
                                match_row = false;
                            }
                        }
                    }
                } else {
                    match_row = false;
                }
            }
            if (match_row) {
                if (limit > 0) {
                    count_write++;
                    if (count_write <= limit) {
                        icsvWriter.writeNext(columns_temp);
                    } else {
                        break;
                    }
                } else {
                    icsvWriter.writeNext(columns_temp);
                }
            }
        }
        reader.close();
        csvReader.close();
        icsvWriter.close();
        writer.close();
        String sourceFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(newFilename).toUriString();
        Resource resource = fileStorageService.loadFileAsResource(newFilename);
        String filepath_temp = resource.getURI().toString();
        Map<String, Object> csvMap = readDataCsv(filepath_temp, except);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", csvMap);
        resultMap.put("origin", filename);
        resultMap.put("filename", newFilename);
        resultMap.put("fileurl", sourceFileDownloadUri);
        return resultMap;
    }

    public Map<String, Object> module1Merge(Map<String, Object> map1) throws Exception {
        Map<String, Object> map2 = new HashMap<>();
        return map2;
    }

    private void printConvertRunMap(Map<String, Object> map) throws Exception {
        logger.info("filename: " + map.get("filename"));
        logger.info("except: " + map.get("except"));
        List<Map<String, Object>> convert = (List<Map<String, Object>>) map.get("convert");
        for (Map<String, Object> map1 : convert) {
            printConvertMap(map1);
        }
    }

    private void printConvertMap(Map<String, Object> map) throws Exception {
        List<String> select = (List<String>) map.get("select");
        List<String> exclude = (List<String>) map.get("exclude");
        List<String> include = (List<String>) map.get("include");
        String log = "";
        for (String s : select) {
            log += s + ", ";
        }
        log = log.substring(0, log.length() - 2);
        logger.info("select: " + log);
        log = "";
        for (String s : exclude) {
            log += s + ", ";
        }
        log = log.substring(0, log.length() - 2);
        logger.info("exclude: " + log);
        log = "";
        for (String s : include) {
            log += s + ", ";
        }
        log = log.substring(0, log.length() - 2);
        logger.info("include: " + log);
        logger.info("limit: " + map.get("limit"));
    }

    /*
     * ********************************************************************************
     * Module 2
     * ********************************************************************************
     */

    @PostMapping("/module2/submitUpload")
    public Map<String, Object> module2SubmitUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        String sourceFileName = fileStorageService.storeFile(file);
        String sourceFileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/downloadFile/").path(sourceFileName).toUriString();
        Resource resource = fileStorageService.loadFileAsResource(sourceFileName);
        String filepath = resource.getURI().toString();
        Map<String, Object> csvMap = module2ReadDataCsv2(filepath);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("origin", sourceFileName);
        paramMap.put("filename", sourceFileName);
        paramMap.put("fileurl", sourceFileDownloadUri);
        paramMap.put("filetype", file.getContentType());
        paramMap.put("filesize", file.getSize());
        paramMap.put("data", csvMap);
        trimColumnData(csvMap);
//        return new SubmitFormResponse(sourceFileName, sourceFileDownloadUri, file.getContentType(), file.getSize());
        return paramMap;
    }

    private Map<String, Object> module2ReadDataCsv2(String filepath) throws Exception {
        Reader reader = Files.newBufferedReader(Paths.get(URI.create(filepath)));
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(0).build();
        List<String[]> rows = csvReader.readAll();
        Map<String, Object> inputResultMap = module2InputDatabase(rows, filepath);
        String[] columns;
        String[] header;
        try {
            columns = rows.get(0);
            header = arrayTrim(rows.get(0));
        } catch (Exception e) {
            columns = new String[0];
            header = new String[0];
        }
        Map<String, Object> dbParamMap = new HashMap<>();
        dbParamMap.put("tablename", inputResultMap.get("tablename"));
        List<String> binaryList = new ArrayList<>();
        for (int i = 0; i < header.length; i++) {
            dbParamMap.put("columnname", header[i]);
            Map<String, Object> dbResultMap = mainRepository.v2Step1ColumnCategoryCount(dbParamMap);
            if ((int) (long) dbResultMap.get("count") == 2) {
                binaryList.add(header[i]);
            }
        }
        /*
        List<String> nonNumeric = new ArrayList<>();
        int rowsCnt = rows.size();
        for (int i = 0; i < header.length; i++) {
            for (int j = 1; j <= LimitRowsTestNonNumeric; j++) {
                if (j > rowsCnt) {
                    break;
                }
                String[] testStr = rows.get(j);
                if (!isNumeric(testStr[i])) {
                    nonNumeric.add(header[i]);
                    break;
                }
            }
        }
         */
        Map<String, Object> csvMap = new HashMap<>();
        csvMap.put("tablename", inputResultMap.get("tablename"));
        csvMap.put("columns", columns.length);
        csvMap.put("rows", rows.size() - 1);
        csvMap.put("header", header);
        csvMap.put("binary", binaryList.toArray(new String[binaryList.size()]));
//        csvMap.put("nonnumeric", nonNumeric.toArray(new String[nonNumeric.size()]));
        return csvMap;
    }

    private boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        return pattern.matcher(str).matches();
    }

    private void trimColumnData(Map<String, Object> csvMap) throws Exception {
        String tablename = (String) csvMap.get("tablename");
        String[] header = (String[]) csvMap.get("header");
        for (String column_name : header) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("tablename", tablename);
            paramMap.put("column_name", column_name);
            mainRepository.v2TrimColumnData(paramMap);
        }
    }

    private Map<String, Object> module2InputDatabase(List<String[]> rows, String filepath) throws Exception {
        module2RemoveOldData();
        String[] header = rows.get(0);
        String tablename = PrefixSourceTableName + System.currentTimeMillis();
        String createQuery = "CREATE TABLE " + tablename + " (";
        for (int i = 0; i < header.length; i++) {
            createQuery += "`" + header[i] + "` varchar(255), ";
        }
        createQuery = createQuery.substring(0, createQuery.length() - 2);
        createQuery += ")";
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("tablename", tablename);
        resultMap.put("query", createQuery);
        mainRepository.v2CreateTable(resultMap);
        String csvPath = String.valueOf(Paths.get(URI.create(filepath)));
        csvPath = csvPath.replaceAll("\\\\", "/");
        String insertQuery = "LOAD DATA LOCAL INFILE '" + csvPath + "' REPLACE INTO TABLE " + tablename +
                " CHARACTER SET utf8 COLUMNS TERMINATED BY ',' ENCLOSED BY '\"' LINES TERMINATED BY '\\r\\n' IGNORE 1 LINES";
        resultMap.put("query", insertQuery);
        mainRepository.v2InsertCsv(resultMap);
        return resultMap;
    }

    private void module2RemoveOldData() throws Exception {
        List<String> dropTables = new ArrayList<>();
        long tsNow = System.currentTimeMillis();
        Map<String, Object> paramMap = new HashMap<>();
        String querySelectTempTable = "SHOW TABLES LIKE '" + PrefixSourceTableName + "%'";
        paramMap.put("query", querySelectTempTable);
        List<String> showTables = mainRepository.v2SelectTempTable(paramMap);
        for (String table : showTables) {
            long ts = Long.parseLong(table.replaceAll("[^0-9]", ""));
            if (tsNow > ts + RemoveOlderThen) {
                dropTables.add(table);
            }
        }
        querySelectTempTable = "SHOW TABLES LIKE '" + PrefixResultTableName + "%'";
        paramMap.put("query", querySelectTempTable);
        showTables = mainRepository.v2SelectTempTable(paramMap);
        for (String table : showTables) {
//            String[] splitArr = table.split(TailDelimiter);
//            String splitTable = splitArr[0];
//            long ts = Long.parseLong(splitTable.replaceAll("[^0-9]", ""));
            long ts = Long.parseLong(table.split(TailDelimiter)[0].replaceAll("[^0-9]", ""));
            if (tsNow > ts + RemoveOlderThen) {
                dropTables.add(table);
            }
        }
        if (dropTables.size() < 1) {
            return;
        }
        String dropQuery = "DROP TABLE IF EXISTS ";
        String deleteQuery = "DELETE FROM %s WHERE tablename IN (";
        for (int i = 0; i < dropTables.size(); i++) {
            String dropTable = dropTables.get(i);
            dropQuery += dropTable + ", ";
            deleteQuery += "'" + dropTable + "', ";
            if (i % 10 == 9) {
                dropQuery = dropQuery.substring(0, dropQuery.length() - 2);
                deleteQuery = deleteQuery.substring(0, deleteQuery.length() - 2) + ")";
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("query", dropQuery);
                mainRepository.v2DropTempTable(resultMap);
                resultMap.put("query", String.format(deleteQuery, MetaTableName));
                mainRepository.v2DeleteDataMeta(resultMap);
                resultMap.put("query", String.format(deleteQuery, StructureTableName));
                mainRepository.v2DeleteDataMeta(resultMap);
                dropQuery = "DROP TABLE IF EXISTS ";
                deleteQuery = "DELETE FROM %s WHERE tablename IN (";
            }
        }
        if (dropTables.size() % 10 != 0) {
            dropQuery = dropQuery.substring(0, dropQuery.length() - 2);
            deleteQuery = deleteQuery.substring(0, deleteQuery.length() - 2) + ")";
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("query", dropQuery);
            mainRepository.v2DropTempTable(resultMap);
            resultMap.put("query", String.format(deleteQuery, MetaTableName));
            mainRepository.v2DeleteDataMeta(resultMap);
            resultMap.put("query", String.format(deleteQuery, StructureTableName));
            mainRepository.v2DeleteDataMeta(resultMap);
        }
        try {
            Path path = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
            File dir = new File(path.toString());
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                for (String table : dropTables) {
                    if (file.getName().contains(table)) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
        }

        /*
        String dropQuery = "DROP TABLE IF EXISTS ";
        String deleteQuery = "DELETE FROM %s WHERE tablename IN (";
        for (String dropTable : dropTables) {
            dropQuery += dropTable + ", ";
            deleteQuery += "'" + dropTable + "', ";
        }
        dropQuery = dropQuery.substring(0, dropQuery.length() - 2);
        deleteQuery = deleteQuery.substring(0, deleteQuery.length() - 2) + ")";
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("query", dropQuery);
        mainRepository.v2DropTempTable(resultMap);
        resultMap.put("query", String.format(deleteQuery, MetaTableName));
        mainRepository.v2DeleteDataMeta(resultMap);
        resultMap.put("query", String.format(deleteQuery, StructureTableName));
        mainRepository.v2DeleteDataMeta(resultMap);
        try {
            Path path = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
            File dir = new File(path.toString());
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                for (String table : dropTables) {
                    if (file.getName().contains(table)) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
        }
         */
    }

}
