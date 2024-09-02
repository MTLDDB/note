package com.huang.note;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MyNotepad extends JFrame implements DropTargetListener {
    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private JMenu fileMenu, viewMenu;
    private JMenuItem newMenuItem, openMenuItem, saveMenuItem, closeMenuItem, exitMenuItem, reSaveMenuItem;
    private JMenuItem changeMenuItem, fontMenuItem;

    Map<String, FileInfo> titleFilePathMap = new HashMap<>();

    private static int newFileNum = 1;
    private static java.util.List<Integer> notUseFileNums = new ArrayList<>();
    SearchDialog myDialog;
    private Highlighter.HighlightPainter selectHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);

    private static boolean isModified = false;

    public MyNotepad() {
        super();
        setTitle("My Notepad");

        // 设置图标
        URL iconURL = getClass().getResource("static/img/touxiang.png"); // 替换为实际的图标路径
        if (iconURL != null) {
            setIconImage(new ImageIcon(iconURL).getImage());
        } else {
            System.err.println("Could not find icon file.");
        }

        // 设置背景
        getContentPane().setBackground(Color.LIGHT_GRAY); // 可以设置背景颜色

        setSize(500, 400);
        //设置窗体显示的位置
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        //菜单栏
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);
        newMenuItem = new JMenuItem("新建", KeyEvent.VK_N);
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        newMenuItem.addActionListener(e -> createNewTab());
        fileMenu.add(newMenuItem);

        openMenuItem = new JMenuItem("打开");
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openMenuItem.addActionListener(e -> openFile());
        fileMenu.add(openMenuItem);

        saveMenuItem = new JMenuItem("保存");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveMenuItem.addActionListener(e -> saveFile());
        fileMenu.add(saveMenuItem);
        reSaveMenuItem = new JMenuItem("另存为");
        reSaveMenuItem.addActionListener(e -> reSaveFile());
        fileMenu.add(reSaveMenuItem);
        closeMenuItem = new JMenuItem("退出");
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
        closeMenuItem.addActionListener(e -> closeTab());
        fileMenu.add(closeMenuItem);


        viewMenu = new JMenu("视图");
        changeMenuItem = new JMenuItem("自动换行");
        changeMenuItem.addActionListener(e -> changeWordWrap());
        viewMenu.add(changeMenuItem);


        fontMenuItem = new JMenuItem("字体大小");
        // 添加动作监听器，显示字体设置对话框
        fontMenuItem.addActionListener(e -> {
            JPanel panel = new JPanel();
            JLabel fontLabel = new JLabel("字体大小:");
            JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(16, 8, 72, 1));
            panel.add(fontLabel);
            panel.add(fontSizeSpinner);

            int result = JOptionPane.showConfirmDialog(this, panel, "Set Font", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                int fontSize = (Integer) fontSizeSpinner.getValue();
                Component component = tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
                if (component instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) component;
                    JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
                    textArea.setFont(new Font("宋体", Font.PLAIN, fontSize));
                }
            }
        });
        viewMenu.add(fontMenuItem);
        menuBar.add(viewMenu);

//        // 创建菜单项 "+"
//        JMenuItem addTabMenuItem = new JMenuItem("+");
//        addTabMenuItem.addActionListener(e -> createNewTab());
//        menuBar.add(addTabMenuItem);
//
//        JMenuItem searchMenuItem = new JMenuItem("搜索");
//        searchMenuItem.setPreferredSize(new Dimension(50, 30)); // 设置宽度和高度
//        searchMenuItem.addActionListener(e -> openSearchDialog());
//        menuBar.add(searchMenuItem);
        // 创建一个面板来放置 "+" 和 "搜索" 按钮
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // 左对齐

        // 创建并添加 "+" 菜单项
        JButton addTabButton = new JButton("+");
        addTabButton.setToolTipText("新建");
        addTabButton.setMargin(new Insets(0, 0, 0, 0)); // 移除按钮边距
        addTabButton.setBorderPainted(false); // 去除边框
        addTabButton.setFocusPainted(false); // 去除焦点框
        addTabButton.setContentAreaFilled(false); // 去除背景填充
        addTabButton.setPreferredSize(new Dimension(30, 30)); // 设置宽度和高度
        addTabButton.addActionListener(e -> createNewTab());
        toolPanel.add(addTabButton);

        // 创建并添加 "搜索" 菜单项
        JButton searchButton = new JButton("搜索");
        searchButton.setMargin(new Insets(0, 0, 0, 0)); // 移除按钮边距
        searchButton.setBorderPainted(false); // 去除边框
        searchButton.setFocusPainted(false); // 去除焦点框
        searchButton.setContentAreaFilled(false); // 去除背景填充
        searchButton.setPreferredSize(new Dimension(70, 30)); // 设置宽度和高度
        searchButton.addActionListener(e -> openSearchDialog());
        toolPanel.add(searchButton);

        // 将工具面板添加到菜单栏
        menuBar.add(Box.createHorizontalBox()); // 添加弹性空间，将工具面板推到右边
        menuBar.add(toolPanel);

        //创建用于多个文档的选项卡窗格
        tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().add(tabbedPane);
        createNewTab();

        //监听选项卡的切换事件
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // 获取当前选中的标签索引
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex != -1) {
                    // 打印或处理标签切换
                    JTextArea textArea = (JTextArea) ((JScrollPane) tabbedPane.getComponentAt(selectedIndex)).getViewport().getView();
                    if (textArea != null && myDialog.isOpen()) {
                        myDialog.addTextArea(textArea);
                    }
                }
            }
        });
        // 创建一个 DropTarget 并关联到 JFrame
        new DropTarget(this, this);
    }

    private void openSearchDialog() {
        {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex != -1) {
                JTextArea textArea = (JTextArea) ((JScrollPane) tabbedPane.getComponentAt(selectedIndex)).getViewport().getView();
                if (textArea != null) {
                    textArea.setSelectionStart(textArea.getSelectionEnd());
                    if (myDialog.isOpen()) {
                        myDialog.addTextArea(textArea);
                    } else {
                        myDialog = SearchDialog.getInstance(MyNotepad.this, textArea);
                    }
                }
            }
        }
    }

    private void changeWordWrap() {
        {
            if (isModified) {
                isModified = false;
                changeMenuItem.setText("自动换行");
            } else {
                isModified = true;
                changeMenuItem.setText("取消自动换行");
            }
            if (tabbedPane.getSelectedIndex() != -1) {
                int num = tabbedPane.getTabCount();
                for (int i = 0; i < num; i++) {
                    Component component = tabbedPane.getComponentAt(i);
                    if (component instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) component;
                        JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
                        textArea.setLineWrap(!textArea.getLineWrap());
                    }
                }

            }
        }
    }

    private void reSaveFile() {
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) return;
        Component component = tabbedPane.getComponentAt(index);
        if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
            // 获取当前选中的标签页的标题
            String tabTitle = getCustomTabTitle(index).replaceAll("\\*", "").replace(".txt", "").trim(); // 去掉标题中的 "*" 和空白
            // 设置默认保存文件名为当前标签的标题，带上 ".txt" 扩展名


            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(tabTitle + "(1).txt"));
            // 设置文件过滤器，限定为 .txt 文件
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
            fileChooser.setFileFilter(filter);

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();

                // 检查文件名是否已经以 ".txt" 结尾，如果没有则添加 ".txt"
                if (!selectedFile.getName().endsWith(".txt")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
                }
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(selectedFile), StandardCharsets.UTF_8))) {
//                try (PrintWriter writer = new PrintWriter(new FileWriter(selectedFile))) {
                    writer.print(textArea.getText());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }


        }

    }

    private void closeTab() {
        boolean canExit = true;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String tabTitle = getCustomTabTitle(i);
            if (tabTitle.endsWith("*")) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "The document '" + tabTitle.substring(0, tabTitle.length() - 1) + "' has been modified. Do you want to save your changes?",
                        "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
                if (confirm == JOptionPane.CANCEL_OPTION) {
                    canExit = false;
                    break;
                } else if (confirm == JOptionPane.YES_OPTION) {
                    saveFile();
                }
            }
        }

        if (canExit) {
            System.exit(0);
        }
    }

    private void saveFile() {
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) return;

        String title = getCustomTabTitle(index);
        if (!title.endsWith("*")) return;
        Component component = tabbedPane.getComponentAt(index);
        if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();
            // 设置默认保存文件名为当前标签的标题，带上 ".txt" 扩展名
            if (titleFilePathMap.containsKey(tabbedPane.getTitleAt(index))) {
                FileInfo fileInfo = titleFilePathMap.get(tabbedPane.getTitleAt(index));
                File file = new File(fileInfo.getFilePath());
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
//                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.print(textArea.getText());
                    // 保存成功后，将当前标签页的标题改成保存的文件名
                    JLabel titleLabel = (JLabel) ((JPanel) tabbedPane.getTabComponentAt(index)).getComponent(0);

                    titleLabel.setText(fileInfo.getFileName()); // 更新标签页标题
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // 获取当前选中的标签页的标题
                String tabTitle = title.replaceAll("\\*", "").trim(); // 去掉标题中的 "*" 和空白
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(tabTitle + ".txt"));
                // 设置文件过滤器，限定为 .txt 文件
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
                fileChooser.setFileFilter(filter);

                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

                    // 检查文件名是否已经以 ".txt" 结尾，如果没有则添加 ".txt"
                    if (!selectedFile.getName().endsWith(".txt")) {
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
                    }

                    FileInfo fileInfo = new FileInfo(selectedFile.getName(), selectedFile.getAbsolutePath());
                    File targetDirectory = new File(selectedFile.getParent()); // 目标目录

                    if (checkIfFileExists(selectedFile, targetDirectory)) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "文件'" + fileInfo.getFileName() + "' 已存在. 覆盖原文件吗?",
                                "文件已存在", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (confirm == JOptionPane.CANCEL_OPTION) {
                            return;
                        } else if (confirm == JOptionPane.YES_OPTION) {
                            if (titleFilePathMap.containsKey(fileInfo.getFileNo())) {
                                FileInfo oldFileInfo = titleFilePathMap.get(fileInfo.getFileNo());
                                oldFileInfo = new FileInfo(oldFileInfo.getFileName().replace(".txt", "") + "（1）*", selectedFile.getAbsolutePath());
                                int oldIndex = tabbedPane.indexOfTab(fileInfo.getFileNo());
                                JLabel titleLabel = (JLabel) ((JPanel) tabbedPane.getTabComponentAt(oldIndex)).getComponent(0);
                                titleLabel.setText(oldFileInfo.getFileName()); // 更新标签页标题
                                tabbedPane.setTitleAt(oldIndex, oldFileInfo.getFileNo());
                                tabbedPane.setToolTipTextAt(oldIndex, "");
                                titleFilePathMap.remove(fileInfo.getFileNo());
                            }
                        } else if (confirm == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(selectedFile), StandardCharsets.UTF_8))) {
//                    try (PrintWriter writer = new PrintWriter(new FileWriter(selectedFile))) {
                        writer.print(textArea.getText());
                        // 保存成功后，将当前标签页的标题改成保存的文件名
                        JLabel titleLabel = (JLabel) ((JPanel) tabbedPane.getTabComponentAt(index)).getComponent(0);
                        titleLabel.setText(fileInfo.getFileName()); // 更新标签页标题
                        tabbedPane.setTitleAt(index, fileInfo.getFileNo());
                        tabbedPane.setToolTipTextAt(index, fileInfo.getFilePath());
                        titleFilePathMap.put(fileInfo.getFileNo(), fileInfo);
                        newFileNumDeal(tabTitle);
                        tabbedPane.setSelectedIndex(index);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }

                }
            }

        }
    }

    private boolean checkIfFileExists(File fileToCheck, File directory) {
        if (directory.exists() && directory.isDirectory()) {
            String fileName = fileToCheck.getName();
            File[] filesInDirectory = directory.listFiles((dir, name) -> name.equals(fileName));
            return filesInDirectory != null && filesInDirectory.length > 0;
        }
        return false;
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            FileInfo fileInfo = new FileInfo(selectedFile.getName(), selectedFile.getAbsolutePath());
            if (titleFilePathMap.containsKey(fileInfo.getFileNo())) {
                int fileTabIndex = tabbedPane.indexOfTab(fileInfo.getFileNo());
                tabbedPane.setSelectedIndex(fileTabIndex);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile), StandardCharsets.UTF_8))) {
                JTextArea textArea = new JTextArea();
                JScrollPane scrollPane = new JScrollPane(textArea);
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                textArea.setText(content.toString());
                if (titleFilePathMap.isEmpty() && tabbedPane.getTabCount() == 1 && !getCustomTabTitle(0).endsWith("*")) {
                    tabbedPane.removeTabAt(0);
                    notUseFileNums.clear();
                    newFileNum = 1;
                }
                addTabWithCloseButton(fileInfo, scrollPane);
                titleFilePathMap.put(fileInfo.getFileNo(), fileInfo);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createNewTab() {
        JTextArea textArea = new JTextArea();
        int num = 1;
        if (!notUseFileNums.isEmpty()) {
            num = notUseFileNums.get(0);
            notUseFileNums.remove(0);
        } else {
            num = newFileNum++;
        }
        JScrollPane scrollPane = new JScrollPane(textArea);
        FileInfo fileInfo = new FileInfo("Untitled" + num, "Untitled" + num);
        addTabWithCloseButton(fileInfo, scrollPane);

    }

    /**
     * tab with close button
     *
     * @param fileInfo
     * @param scrollPane
     */
    private void addTabWithCloseButton(FileInfo fileInfo, JScrollPane scrollPane) {
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10)); // 添加边框给每个面板
        // 创建关闭按钮
        JButton closeButton = new JButton("X");
        closeButton.setMargin(new Insets(0, 0, 0, 0)); // 移除按钮边距，使其外观更好
        closeButton.setPreferredSize(new Dimension(15, 15)); // 设置关闭按钮的首选大小
        // 为关闭按钮添加事件监听器
        closeButton.addActionListener(e -> {
            int index = tabbedPane.indexOfComponent(scrollPane);

            if (index != -1) { // 确保标签页存在
                if (saveSingleFile(index)) {
                    tabbedPane.remove(index);
                    if (!titleFilePathMap.containsKey(fileInfo.getFileNo())) {//新建文件退出处理文件编号
                        newFileNumDeal(fileInfo.getFileName());
                    } else {
                        titleFilePathMap.remove(fileInfo.getFileNo());
                    }
                }
            }

            if (tabbedPane.getTabCount() == 0) {
                titleFilePathMap.clear();
                notUseFileNums.clear();
                newFileNum = 1;
                createNewTab();
            }
        });

        // 创建一个面板用于放置标题和关闭按钮
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        // 创建标签用于显示标签页标题
        JLabel titleLabel = new JLabel(fileInfo.getFileName());
//        if (fileInfo.getFileName().startsWith("Untitled")) {
        titleLabel.setToolTipText(fileInfo.getFilePath()); // 设置工具提示文本
//        }
        // 添加鼠标监听器来处理点击事件
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = tabbedPane.indexOfTabComponent(titleLabel.getParent());
                if (index != -1) {
                    tabbedPane.setSelectedIndex(index);
                }
            }
        });

        // 将标题和关闭按钮添加到面板中
        tabPanel.add(titleLabel);
        tabPanel.add(closeButton);

        // 设置面板的边距（可选）
        tabPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        // 添加新标签页，并设置自定义的标签组件
        tabbedPane.addTab(fileInfo.getFileNo(), scrollPane);

        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);
        int index = tabbedPane.indexOfTabComponent(tabPanel);
        tabbedPane.setSelectedIndex(index);
        // 从 JScrollPane 的视图中获取 JTextArea
        JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

        if (isModified) {
            // 启用自动换行
            textArea.setLineWrap(true);
            // 在单词边界处换行
            textArea.setWrapStyleWord(true);
        }
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK);
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "findAction");

        // 绑定Action到快捷键
        textArea.getActionMap().put("findAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myDialog = SearchDialog.getInstance(MyNotepad.this, textArea);
            }
        });


        // 添加CaretListener以监听选中文本的变化
        textArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                if (!myDialog.isOpen()) {
                    highlightSelectedText(textArea);
                }
            }
        });
        // 为 JTextArea 的 Document 添加监听器，监听内容变化
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            //            boolean isModified = false;
            Timer timer = new Timer(500, e -> onTextChanged());

            {
                timer.setRepeats(false); // 只在停止输入后执行一次
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                restartTimer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                restartTimer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                restartTimer();
            }

            private void restartTimer() {
                if (timer.isRunning()) {
                    timer.restart(); // 如果计时器正在运行，重新启动
                } else {
                    timer.start(); // 否则启动计时器
                }
            }

            private void onTextChanged() {
                // 处理文本变化事件
                if (index != -1 && !getCustomTabTitle(index).endsWith("*")) {
                    // 将标题和关闭按钮添加到面板中
                    JLabel titleLabel = (JLabel) ((JPanel) tabbedPane.getTabComponentAt(index)).getComponent(0);
                    titleLabel.setText(titleLabel.getText() + " *"); // 设置新的标题
                }
            }
        });
    }

    private boolean saveSingleFile(int index) {
        String tabTitle = getCustomTabTitle(index);
        if (tabTitle.endsWith("*")) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "文件 '" + tabTitle.substring(0, tabTitle.length() - 1) + "' 被修改了. 保存你的修改吗?",
                    "不保存", JOptionPane.YES_NO_CANCEL_OPTION);
            if (confirm == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (confirm == JOptionPane.YES_OPTION) {
                saveFile();
                return true;
            }
        }
        return true;
    }

    void newFileNumDeal(String title) {
        if (!title.contains("Untitled")) return;
        notUseFileNums.add(Integer.valueOf(title.replace("Untitled", "")));
        notUseFileNums.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
    }

    // 执行高亮显示的方法
    private void highlightSelectedText(JTextArea textArea) {
        // 清除之前的高亮显示
        textArea.getHighlighter().removeAllHighlights();
        String searchTerm = textArea.getSelectedText();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            String content = textArea.getText();
            int index = content.indexOf(searchTerm);
            while (index >= 0) {
                try {
                    int end = index + searchTerm.length();
                    textArea.getHighlighter().addHighlight(index, end, selectHighlightPainter);
                    index = content.indexOf(searchTerm, end);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private String getCustomTabTitle(int index) {
        JPanel tabPanel = (JPanel) tabbedPane.getTabComponentAt(index); // 获取自定义组件
        JLabel titleLabel = (JLabel) tabPanel.getComponent(0); // 获取标签中的标题部分（假设它是第一个组件）
        return titleLabel.getText(); // 返回标题文本
    }


    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new MyNotepad());
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // 当拖入元素进入区域时
        dtde.acceptDrag(dtde.getDropAction());
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // 当拖动元素在区域内移动时
        dtde.acceptDrag(dtde.getDropAction());
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // 当拖动元素的动作发生变化时
        dtde.acceptDrag(dtde.getDropAction());
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        // 当拖动元素离开区域时
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(dtde.getDropAction());
            Transferable transferable = dtde.getTransferable();
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorJavaFileListType()) {
                    java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(flavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                            FileInfo fileInfo = new FileInfo(file.getName(), file.getAbsolutePath());
                            if (titleFilePathMap.containsKey(fileInfo.getFileNo())) {
                                int fileTabIndex = tabbedPane.indexOfTab(fileInfo.getFileNo());
                                tabbedPane.setSelectedIndex(fileTabIndex);
                                return;
                            }
                            String content = readFileContent(file);
                            JTextArea textArea = new JTextArea();
                            JScrollPane scrollPane = new JScrollPane(textArea);
                            textArea.setText(content);
                            if (titleFilePathMap.isEmpty() && tabbedPane.getTabCount() == 1 && !getCustomTabTitle(0).endsWith("*")) {
                                tabbedPane.removeTabAt(0);
                                notUseFileNums.clear();
                                newFileNum = 1;
                            }
                            addTabWithCloseButton(fileInfo, scrollPane);
                            titleFilePathMap.put(fileInfo.getFileNo(), fileInfo);
                        } else {
                            JOptionPane.showMessageDialog(this, "Please drop a .txt file.");
                        }
                    }
                }
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))){
//        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

}
