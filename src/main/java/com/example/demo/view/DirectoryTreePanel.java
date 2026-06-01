package com.example.demo.view;

import com.example.demo.model.ThemeManager;
import com.example.demo.util.DesignConstants;
import com.example.demo.util.FileUtils;
import com.example.demo.util.StyleUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * 目录树面板
 * <p>
 * 左侧边栏组件，提供磁盘目录浏览和路径导航功能。
 * 包含三个区域：
 * <ol>
 *   <li>标题栏：显示 "Explorer" 标题</li>
 *   <li>地址栏：[📁按钮] [路径输入框] [→按钮]，支持手动输入路径和文件夹选择器</li>
 *   <li>目录树：懒加载的 TreeView，展示磁盘根目录和子目录结构</li>
 * </ol>
 * <p>
 * 关键设计：
 * <ul>
 *   <li>懒加载：目录节点仅在展开时加载子目录，避免一次性扫描整个磁盘</li>
 *   <li>路径导航：支持输入绝对路径跳转，自动展开并选中目标目录</li>
 *   <li>主题感知：注册主题变更监听器，切换主题时刷新所有样式</li>
 *   <li>回调模式：通过 {@code Consumer<File>} 回调通知主应用目录选中事件</li>
 * </ul>
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class DirectoryTreePanel extends VBox {

    /** 目录树控件 */
    private final TreeView<File> directoryTree;

    /** 地址栏输入框 */
    private TextField addressField;

    /** 目录树滚动容器 */
    private final ScrollPane treeScrollPane;

    /** 目录选中回调，通知主应用加载该目录下的图片 */
    private final Consumer<File> onDirectorySelected;

    /** 侧边栏标题标签 */
    private Label sidebarTitleLabel;

    /** 地址栏容器 */
    private HBox addressBarContainer;

    /** 标题栏容器 */
    private HBox titleHeader;

    /** 跳转按钮（→），固定尺寸 32×32 */
    private Button goBtn;

    /** 打开文件夹按钮（📁），固定尺寸 32×32 */
    private Button folderBtn;

    /**
     * 构造目录树面板
     *
     * @param onDirectorySelected 目录选中回调，参数为选中的目录文件
     */
    public DirectoryTreePanel(Consumer<File> onDirectorySelected) {
        this.onDirectorySelected = onDirectorySelected;

        createSidebarTitle();
        createAddressBar();

        directoryTree = new TreeView<>();
        treeScrollPane = new ScrollPane(directoryTree);
        treeScrollPane.setFitToWidth(true);
        treeScrollPane.setFitToHeight(true);
        StyleUtils.applySidebarScrollStyle(treeScrollPane);

        initDirectoryTree();

        this.getChildren().addAll(titleHeader, addressBarContainer, treeScrollPane);
        VBox.setVgrow(treeScrollPane, Priority.ALWAYS);
        this.setStyle("-fx-background-color: " + DesignConstants.bgSidebar() + ";");

        ThemeManager.getInstance().addThemeChangeListener(this::applyTheme);
    }

    /** 创建侧边栏标题 "Explorer" */
    private void createSidebarTitle() {
        sidebarTitleLabel = new Label("Explorer");
        sidebarTitleLabel.setStyle(
                "-fx-font-size: 13px;"
                        + " -fx-font-weight: 700;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-font-family: 'Segoe UI', sans-serif;"
                        + " -fx-letter-spacing: 1;"
        );
        titleHeader = new HBox(sidebarTitleLabel);
        titleHeader.setPadding(new Insets(12, 16, 10, 16));
        titleHeader.setStyle(
                "-fx-background-color: " + DesignConstants.bgSidebar() + ";"
                        + " -fx-border-color: " + DesignConstants.border() + ";"
                        + " -fx-border-width: 0 0 1 0;"
        );
    }

    /** 创建地址栏：[📁] [路径输入框] [→]，按钮固定尺寸不随窗口变化 */
    private void createAddressBar() {
        addressField = new TextField();
        addressField.setPromptText("Enter path (e.g. C:\\Users\\Photos)");
        addressField.setStyle(buildAddressFieldStyle());
        addressField.setPrefHeight(32);

        goBtn = new Button("\u2192");
        goBtn.setPrefSize(32, 32);
        goBtn.setMinSize(32, 32);
        goBtn.setMaxSize(32, 32);
        goBtn.setStyle(buildGoBtnStyle());

        folderBtn = new Button("\uD83D\uDCC1");
        folderBtn.setPrefSize(32, 32);
        folderBtn.setMinSize(32, 32);
        folderBtn.setMaxSize(32, 32);
        folderBtn.setStyle(buildFolderBtnStyle());

        addressBarContainer = new HBox(folderBtn, addressField, goBtn);
        addressBarContainer.setAlignment(Pos.CENTER_LEFT);
        addressBarContainer.setPadding(new Insets(8, 10, 8, 10));
        addressBarContainer.setSpacing(0);
        HBox.setHgrow(addressField, Priority.ALWAYS);
        addressBarContainer.setStyle(
                "-fx-background-color: " + DesignConstants.bgSidebar() + ";"
                        + " -fx-border-color: " + DesignConstants.border() + ";"
                        + " -fx-border-width: 0 0 1 0;"
        );

        goBtn.setOnAction(e -> navigateToPath(addressField.getText().trim()));
        addressField.setOnAction(e -> navigateToPath(addressField.getText().trim()));
        folderBtn.setOnAction(e -> chooseFolder());

        goBtn.setOnMouseEntered(e -> goBtn.setStyle(buildGoBtnHoverStyle()));
        goBtn.setOnMouseExited(e -> goBtn.setStyle(buildGoBtnStyle()));
        folderBtn.setOnMouseEntered(e -> folderBtn.setStyle(buildFolderBtnHoverStyle()));
        folderBtn.setOnMouseExited(e -> folderBtn.setStyle(buildFolderBtnStyle()));
    }

    /** 构建地址输入框样式（等宽字体、提示文字颜色区分） */
    private String buildAddressFieldStyle() {
        return "-fx-background-color: " + DesignConstants.bgSidebarHover() + ";"
                + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                + " -fx-prompt-text-fill: " + DesignConstants.textPrompt() + ";"
                + " -fx-font-size: 12px;"
                + " -fx-font-family: 'Consolas', 'Monaco', monospace;"
                + " -fx-padding: 7 10;"
                + " -fx-background-radius: 6;"
                + " -fx-border-color: transparent;"
                + " -fx-border-radius: 6;";
    }

    private String buildGoBtnStyle() {
        return "-fx-background-color: " + DesignConstants.PRIMARY() + ";"
                + " -fx-text-fill: white;"
                + " -fx-font-size: 14px;"
                + " -fx-font-weight: bold;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 0 6 6 0;"
                + " -fx-padding: 0;";
    }

    private String buildGoBtnHoverStyle() {
        return "-fx-background-color: " + DesignConstants.PRIMARY_DARK() + ";"
                + " -fx-text-fill: white;"
                + " -fx-font-size: 14px;"
                + " -fx-font-weight: bold;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 0 6 6 0;"
                + " -fx-padding: 0;";
    }

    private String buildFolderBtnStyle() {
        return "-fx-background-color: " + DesignConstants.bgSidebarHover() + ";"
                + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                + " -fx-font-size: 14px;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 6 0 0 6;"
                + " -fx-padding: 0;";
    }

    private String buildFolderBtnHoverStyle() {
        return "-fx-background-color: " + DesignConstants.PRIMARY() + ";"
                + " -fx-text-fill: white;"
                + " -fx-font-size: 14px;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 6 0 0 6;"
                + " -fx-padding: 0;";
    }

    /** 主题切换时刷新所有组件样式，并调用 directoryTree.refresh() 强制更新 TreeCell */
    private void applyTheme() {
        this.setStyle("-fx-background-color: " + DesignConstants.bgSidebar() + ";");

        if (sidebarTitleLabel != null) {
            sidebarTitleLabel.setStyle(
                    "-fx-font-size: 13px;"
                            + " -fx-font-weight: 700;"
                            + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                            + " -fx-font-family: 'Segoe UI', sans-serif;"
                            + " -fx-letter-spacing: 1;"
            );
        }
        if (titleHeader != null) {
            titleHeader.setStyle(
                    "-fx-background-color: " + DesignConstants.bgSidebar() + ";"
                            + " -fx-border-color: " + DesignConstants.border() + ";"
                            + " -fx-border-width: 0 0 1 0;"
            );
        }
        if (addressField != null) {
            addressField.setStyle(buildAddressFieldStyle());
        }
        if (folderBtn != null) {
            folderBtn.setStyle(buildFolderBtnStyle());
        }
        if (goBtn != null) {
            goBtn.setStyle(buildGoBtnStyle());
        }
        if (addressBarContainer != null) {
            addressBarContainer.setStyle(
                    "-fx-background-color: " + DesignConstants.bgSidebar() + ";"
                            + " -fx-border-color: " + DesignConstants.border() + ";"
                            + " -fx-border-width: 0 0 1 0;"
            );
        }
        StyleUtils.applySidebarScrollStyle(treeScrollPane);
        StyleUtils.applyTreeViewStyle(directoryTree);

        directoryTree.refresh();
    }

    /** 打开文件夹选择器对话框 */
    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");
        File selected = chooser.showDialog(directoryTree.getScene().getWindow());
        if (selected != null) {
            addressField.setText(selected.getAbsolutePath());
            navigateToPath(selected.getAbsolutePath());
        }
    }

    /**
     * 导航到指定路径
     * <p>
     * 支持目录路径和文件路径（自动取父目录）。
     * 路径不存在时静默返回。
     *
     * @param pathText 目标路径字符串
     */
    public void navigateToPath(String pathText) {
        if (pathText == null || pathText.trim().isEmpty()) {
            return;
        }
        File targetFile = new File(pathText.trim());
        if (!targetFile.exists()) {
            return;
        }
        File targetDir = targetFile.isDirectory()
                ? targetFile
                : targetFile.getParentFile();
        if (targetDir == null) {
            return;
        }
        expandTreeTo(targetDir);
    }

    /**
     * 在目录树中展开路径到目标目录
     * <p>
     * 算法：先定位磁盘根节点，再逐级向下匹配路径段，最终选中目标节点。
     */
    private void expandTreeTo(File targetDir) {
        String targetPath = FileUtils.normalizePath(targetDir.getAbsolutePath());
        TreeItem<File> root = directoryTree.getRoot();
        if (root == null) return;

        TreeItem<File> driveItem = findDriveRoot(root, targetPath);
        if (driveItem == null) {
            onDirectorySelected.accept(targetDir);
            addressField.setText(targetDir.getAbsolutePath());
            return;
        }

        driveItem.setExpanded(true);
        TreeItem<File> matched = walkDownPath(driveItem, targetPath);
        if (matched == null) matched = driveItem;

        final TreeItem<File> selectTarget = matched;
        Platform.runLater(() -> {
            int row = directoryTree.getRow(selectTarget);
            if (row >= 0) {
                directoryTree.scrollTo(row);
                directoryTree.getSelectionModel().select(selectTarget);
            }
        });
        addressField.setText(targetDir.getAbsolutePath());
        onDirectorySelected.accept(targetDir);
    }

    /** 在根节点的子节点中查找目标路径所在的磁盘根节点 */
    private TreeItem<File> findDriveRoot(TreeItem<File> root, String targetPath) {
        root.setExpanded(true);
        for (TreeItem<File> child : root.getChildren()) {
            File val = child.getValue();
            if (val != null) {
                String childPath = FileUtils.normalizePath(val.getAbsolutePath());
                if (targetPath.startsWith(childPath)) {
                    return child;
                }
            }
        }
        return null;
    }

    /** 从起始节点逐级向下匹配路径段，返回最深的匹配节点 */
    private TreeItem<File> walkDownPath(TreeItem<File> start, String targetPath) {
        File startVal = start.getValue();
        String startPath = startVal != null
                ? FileUtils.normalizePath(startVal.getAbsolutePath()) : "";
        if (!targetPath.startsWith(startPath)) return null;

        String remaining = targetPath
                .substring(startPath.length())
                .trim();
        if (remaining.isEmpty() || remaining.equals("\\")) return start;

        String[] parts = remaining.split("\\\\");
        List<String> segments = new ArrayList<>();
        for (String s : parts) {
            if (!s.isEmpty()) segments.add(s);
        }

        TreeItem<File> current = start;
        current.setExpanded(true);

        for (String segment : segments) {
            TreeItem<File> found = null;
            current.setExpanded(true);
            for (TreeItem<File> child : current.getChildren()) {
                File childVal = child.getValue();
                if (childVal != null && childVal.getName().equalsIgnoreCase(segment)) {
                    found = child;
                    break;
                }
            }
            if (found == null) {
                return current;
            }
            found.setExpanded(true);
            current = found;
        }
        return current;
    }

    /**
     * 初始化目录树
     * <p>
     * 创建虚拟根节点 "This PC"，将所有磁盘根目录作为子节点。
     * 使用 {@link BeautifulTreeCell} 自定义单元格渲染。
     * 选中节点时触发目录选中回调。
     */
    private void initDirectoryTree() {
        TreeItem<File> rootNode = new TreeItem<>(new File("This PC"));
        rootNode.setExpanded(true);

        for (File drive : File.listRoots()) {
            rootNode.getChildren().add(createLazyTreeItem(drive));
        }

        directoryTree.setRoot(rootNode);
        directoryTree.setShowRoot(false);
        directoryTree.setCellFactory(tv -> new BeautifulTreeCell());
        StyleUtils.applyTreeViewStyle(directoryTree);

        directoryTree.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.getValue() != null
                            && newVal.getValue().isDirectory()) {
                        addressField.setText(newVal.getValue().getAbsolutePath());
                        onDirectorySelected.accept(newVal.getValue());
                    }
                });
    }

    /**
     * 创建懒加载的目录 TreeItem
     * <p>
     * 重写 {@code isLeaf()} 和 {@code getChildren()} 实现懒加载：
     * 子目录仅在首次展开时加载，之后缓存结果。
     */
    private TreeItem<File> createLazyTreeItem(File dir) {
        return new TreeItem<File>(dir) {
            private boolean isLeaf;
            private boolean ftc = true;
            private boolean ftl = true;

            @Override
            public boolean isLeaf() {
                if (ftl) {
                    ftl = false;
                    isLeaf = getValue().isFile();
                }
                return isLeaf;
            }

            @Override
            public javafx.collections.ObservableList<TreeItem<File>> getChildren() {
                if (ftc) {
                    ftc = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }
        };
    }

    /** 构建指定目录的子目录列表（过滤隐藏目录，按名称排序） */
    private List<TreeItem<File>> buildChildren(TreeItem<File> parent) {
        File dir = parent.getValue();
        if (dir != null && dir.isDirectory()) {
            File[] subDirs = dir.listFiles(f -> f.isDirectory() && !f.isHidden());
            if (subDirs != null) {
                Arrays.sort(subDirs, Comparator.comparing(
                        File::getName, String.CASE_INSENSITIVE_ORDER));
                return Arrays.stream(subDirs)
                        .map(this::createLazyTreeItem)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    /** 更新地址栏文本 */
    public void updateAddressField(String path) {
        addressField.setText(path);
    }

    /** 获取目录树控件 */
    public TreeView<File> getDirectoryTree() {
        return directoryTree;
    }

    /** 获取地址栏输入框（用于快捷键判断焦点） */
    public TextField getAddressField() {
        return addressField;
    }
}
