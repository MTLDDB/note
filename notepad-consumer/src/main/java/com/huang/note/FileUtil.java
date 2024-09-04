package com.huang.note;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    public static boolean writeFile(String filePath, String text, Frame frame) {
        Path selectedFile = Paths.get(filePath);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(selectedFile), StandardCharsets.UTF_8))) {
            writer.print(text);
            return true;
        } catch (IOException e) {
            logger.error("Error saving file: ", e);
            JOptionPane.showMessageDialog( frame,"Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    public static StringBuilder readFile(String filePath, Frame frame) {
        Path selectedFile = Paths.get(filePath);
        StringBuilder content = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(selectedFile), StandardCharsets.UTF_8))) {
            String line;
            content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error("找不到文件:{}", e.getMessage());
            JOptionPane.showMessageDialog( frame,"找不到文件: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return content;
    }
    public static String getFileType(File file) {
        if (file == null) {
            return "";
        }
        String fileName = file.getName();
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) { // 没有找到点，说明没有扩展名
            return "";
        }
        return fileName.substring(lastIndexOfDot + 1).toLowerCase(); // 转换为小写以便统一处理
    }

    public static String getFileType(String  fileName) {
        if (fileName == null) {
            return "";
        }
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) { // 没有找到点，说明没有扩展名
            return "";
        }
        return fileName.substring(lastIndexOfDot + 1).toLowerCase(); // 转换为小写以便统一处理
    }
}
