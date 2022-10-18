package com.QuFa.profiler.service;

import com.QuFa.profiler.controller.exception.CustomException;
import com.QuFa.profiler.controller.exception.ErrorCode;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

@Component
@Getter
public class FileService {

    private String targetFolderPath;
    private String osType;

    public FileService() {
        String os = System.getProperty("os.name").toLowerCase();

        // 운영체제별로 targetfiles 다르게 설정
        if (os.contains("win")) {
            targetFolderPath = "C://Temp/targetfiles/";
            osType = "win";
        } else {
            targetFolderPath = "~/tmp/";
            osType = "linux";
        }

        System.out.println("targetFolderPath = " + targetFolderPath);
    }

    public String seperate_file(String url){
        if (osType.equals("win"))
            return url.substring(8).replace('/','\\');
        else
            return url.substring(7);
    }

    public String getFileName(String type, String path) {
        String[] split = null;
        if (Objects.equals(osType, "win")) {
            if (type.equals("path")) {
                split = path.split("\\\\");
            } else if (type.equals("url")) {
                split = path.split("/");
            }
        }
        else {
            split = path.split("/");
        }

        return split[split.length - 1].split("\\.")[0];
    }

    public int getFileLength(String filePath) {
        File file = new File(filePath);
        return (int)file.length();
    }

    public String writeHeader(String fileName, String path) throws IOException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        List<String> header = new ArrayList<>();

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

        return newFilePath;
    }

    public List<String> getHeader(String path) throws IOException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        List<String> header = Arrays.asList(csvReader.readNext().clone());
        csvReader.close();
        return header;
    }

    public String storeUrlFile(String url){
        String type = "url";
        String filePath = targetFolderPath + getFileName(type, url) + ".csv";
        File f = new File(filePath);
        try {
            FileUtils.copyURLToFile(new URL(url), f);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.REQUEST_DATA_MALFORMED);
        }

        return filePath;
    }

}
