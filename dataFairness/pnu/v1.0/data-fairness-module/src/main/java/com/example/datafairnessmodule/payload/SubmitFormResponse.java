package com.example.datafairnessmodule.payload;

public class SubmitFormResponse {
    private String sourceFileName;
    private String sourceFileDownloadUri;
    private String sourceFileType;
    private long sourceFileSize;

    public SubmitFormResponse(String sourceFileName, String sourceFileDownloadUri, String sourceFileType, long sourceFileSize) {
        this.sourceFileName = sourceFileName;
        this.sourceFileDownloadUri = sourceFileDownloadUri;
        this.sourceFileType = sourceFileType;
        this.sourceFileSize = sourceFileSize;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceFileDownloadUri() {
        return sourceFileDownloadUri;
    }

    public void setSourceFileDownloadUri(String sourceFileDownloadUri) {
        this.sourceFileDownloadUri = sourceFileDownloadUri;
    }

    public String getSourceFileType() {
        return sourceFileType;
    }

    public void setSourceFileType(String sourceFileType) {
        this.sourceFileType = sourceFileType;
    }

    public long getSourceFileSize() {
        return sourceFileSize;
    }

    public void setSourceFileSize(long sourceFileSize) {
        this.sourceFileSize = sourceFileSize;
    }
}
