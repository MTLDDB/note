package com.huang.note;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class SearchDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(SearchDialog.class);
    private volatile static SearchDialog dialog;
    private final JTextField searchField;
    private final JLabel countLabel;

    private final JButton supperButton;
    private final JButton nextButton;

    private Highlighter.HighlightPainter highlightPainter;

    private Highlighter.HighlightPainter currentHighlightPainter;
    //搜索到的位置集合
    private java.util.List<Integer> searchPositions;
    //当前位置
    private int currentPositionIndex;
    //当前高亮的标签
    private Object currentHighlightTag;

    //用于存储高亮标签
    private java.util.List<Object> highlightTags;
    private  boolean open;
    private JTextArea textArea;
    private static WindowAdapter dialogIsClosing;

    public  boolean isOpen() {
        return open;
    }

    private void initHighlight() {
        if (highlightPainter == null) {
            highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
            currentHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);
        }
        currentPositionIndex = -1;
        currentHighlightTag = null;
        if (highlightTags == null) {
            highlightTags = new ArrayList<>();
        } else {
            highlightTags.clear();
        }
        if (searchPositions == null) {
            searchPositions = new ArrayList<>();
        } else {
            searchPositions.clear();
        }
    }

    public static SearchDialog getInstance(JFrame owner, JTextArea textArea) {
        if (dialog == null) {
            System.out.println("getInstance");
            synchronized (SearchDialog.class) {
                if (dialog == null) {
                    dialog = new SearchDialog(owner, textArea);
                }
            }
        } else {
            String searchText = textArea.getSelectedText();
            if (searchText != null && !searchText.isEmpty()) {
                dialog.searchField.setText(searchText);
            }
            dialog.addTextArea(textArea);
        }
        return dialog;
    }

    public SearchDialog(JFrame owner, JTextArea textArea) {
        setTitle("搜索");
        setSize(400, 200);
        setLocationRelativeTo(owner); // 居中显示
        setAlwaysOnTop(true); // 始终位于顶层
        setModal(false);

        setLayout(new BorderLayout());
        setResizable(true);

        searchField = new JTextField(20);
        countLabel = new JLabel("出现次数: 0");
        supperButton = new JButton("上一个");
        nextButton = new JButton("下一个");
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(searchField, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(supperButton);
        buttonPanel.add(nextButton);
        panel.add(buttonPanel, BorderLayout.EAST);

        add(panel, BorderLayout.NORTH);
        add(countLabel, BorderLayout.SOUTH);
//        pack();
//        setLocationRelativeTo(this);
        setVisible(true);
        // 设置关闭操作
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.open = true;
        initHighlight();

        this.textArea = textArea;
        addActionListener(this.textArea);
        String searchText = textArea.getSelectedText();
        if (searchText != null && !searchText.isEmpty()) {
            searchField.setText(searchText);
            searchField.postActionEvent();
        }
    }

    public void addTextArea(JTextArea textArea) {
        if (dialog != null) {
            searchField.removeActionListener(searchField.getActionListeners()[0]);
            supperButton.removeActionListener(supperButton.getActionListeners()[0]);
            nextButton.removeActionListener(nextButton.getActionListeners()[0]);
            removeWindowListener(dialogIsClosing);
        }
        initHighlight();
        this.textArea.getHighlighter().removeAllHighlights();

        this.textArea = textArea;
        this.textArea.getHighlighter().removeAllHighlights();

        dialog.setVisible(true);
        this.open = true;
        addActionListener(this.textArea);
        if (searchField.getText() != null && !searchField.getText().isEmpty()) {
            searchField.postActionEvent();
        }
    }

    private void addActionListener(JTextArea textArea) {
        textArea.getHighlighter().removeAllHighlights();
        searchField.addActionListener(e -> searchText(searchField, countLabel, textArea));
        nextButton.addActionListener(e -> {
            if (!searchPositions.isEmpty()) {
                int oldIndex = currentPositionIndex;
                currentPositionIndex = (currentPositionIndex + 1) % searchPositions.size();
                dealWithHighlight(oldIndex, currentPositionIndex);
            }
        });

        supperButton.addActionListener(e -> {
            if (!searchPositions.isEmpty()) {
                int oldIndex = currentPositionIndex;
                if (currentPositionIndex == -1 || currentPositionIndex == 0) {
                    currentPositionIndex = searchPositions.size();
                }
                currentPositionIndex = (currentPositionIndex - 1) % searchPositions.size();
                dealWithHighlight(oldIndex, currentPositionIndex);
            }
        });
        dialogIsClosing = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Dialog is closing");
                textArea.getHighlighter().removeAllHighlights();
                searchPositions.clear();
                currentHighlightTag = null;
                highlightTags.clear();
                open = false;
                // 在这里处理关闭时需要执行的逻辑
            }
        };
        addWindowListener(dialogIsClosing);
    }

    private void dealWithHighlight(int oldIndex, int newIndex) {
        if (!searchPositions.isEmpty()) {
            if (currentHighlightTag != null) {// 移除旧的高亮
                textArea.getHighlighter().removeHighlight(currentHighlightTag);
                try {// 添加统一的高亮
                    Object highlightTag = textArea.getHighlighter().addHighlight(searchPositions.get(oldIndex), searchPositions.get(oldIndex) + searchField.getText().length(), highlightPainter);
                    highlightTags.set(oldIndex, highlightTag);
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }
            int start = searchPositions.get(newIndex);
            int end = start + searchField.getText().length();
            try {
                textArea.getHighlighter().removeHighlight(highlightTags.get(newIndex));
                //添加当前高亮
                currentHighlightTag = textArea.getHighlighter().addHighlight(start, end, currentHighlightPainter);
                textArea.setCaretPosition(start);
                textArea.select(start, end);
            } catch (BadLocationException ex) {
                logger.error("Error occurred while highlighting text", ex);
            }
        }
    }

    private void searchText(JTextField searchField, JLabel countLabel, JTextArea textArea) {
        {
            String searchTerm = searchField.getText();
            textArea.getHighlighter().removeAllHighlights();
            if (!searchTerm.isEmpty()) {
                highlightAndCountOccurrences(searchTerm, textArea);
                countLabel.setText("出现次数: " + searchPositions.size());
                currentPositionIndex = -1;
            }
        }
    }

    // 高亮显示并计数匹配的文本
    private void highlightAndCountOccurrences(String searchTerm, JTextArea textArea) {
        // 清除之前的高亮显示
        searchPositions.clear();
        currentHighlightTag = null;
        highlightTags.clear();
        String content = textArea.getText();
        int index = content.indexOf(searchTerm);

        while (index >= 0) {
            try {
                int end = index + searchTerm.length();
                Object highlighter = textArea.getHighlighter().addHighlight(index, end, highlightPainter);
                searchPositions.add(index);
                highlightTags.add(highlighter);
                index = content.indexOf(searchTerm, end);
            } catch (BadLocationException ex) {
               logger.error("An error occurred", ex);
            }
        }
    }

}
