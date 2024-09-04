package com.huang.note;

import java.io.File;
import java.util.Arrays;

/**
 * 文件类型枚举
 */
public enum FileType {
    TXT("txt","Text"),
    JSON("json","Text"),
    PROPERTIES("properties","Text"),
    YML("yml","Text"),
    HTML("html","Text"),
    PDF("pdf","Text"),
    JPEG("jpeg","Img"),
    JPG("jpg","Img"),
    PNG("png","Img"),
    GIF("gif","Img");

    private final String extension;
    private final String type;

    FileType(String extension,String type) {
        this.extension = extension.toLowerCase();
        this.type = type;
    }

    public String getExtension() {
        return "." + extension;
    }

    public String getNotPotExtension() {
        return extension;
    }

    public String getType() {
        return type;
    }

    public static boolean isTypeOfFile(File file, FileType... types) {
        String fileName = file.getName().toLowerCase();
        for (FileType type : types) {
            System.out.println(type.getExtension());
            if (fileName.endsWith(type.getExtension())) {
                return true;
            }
        }
        return false;
    }

//    public static String[] getExtensions() {
//        return Arrays.stream(FileType.values())
//                .map(FileType::getNotPotExtension)
//                .toArray(String[]::new);
//    }
    public static String[] getExtensionsByType(String type) {
        return Arrays.stream(values())
                .filter(ft -> ft.getType().equals(type))
                .map(FileType::getNotPotExtension)
                .toArray(String[]::new);
    }
}
