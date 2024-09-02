package com.huang.note;

import java.util.UUID;

public class FileInfo {
    private String fileName;
    private String filePath;

    private String fileNo;
    public FileInfo(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileNo = UUID.nameUUIDFromBytes((fileName + filePath).getBytes()).toString().replace("-","");
    }

    public String getFileNo() {
        return fileNo;
    }

    public String getFileName() {
        return fileName;
    }


    public String getFilePath() {
        return filePath;
    }
}
