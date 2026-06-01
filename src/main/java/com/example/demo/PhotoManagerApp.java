package com.example.demo;

import com.example.demo.model.AppState;
import com.example.demo.model.StatusManager;
import com.example.demo.model.ThemeManager;
import com.example.demo.service.FileOperationService;
import com.example.demo.service.ImageCacheService;
import com.example.demo.service.ImageTransformService;
import com.example.demo.util.DesignConstants;
import com.example.demo.util.StyleUtils;
import com.example.demo.view.DirectoryTreePanel;
import com.example.demo.view.SlideshowWindow;
import com.example.demo.view.ThumbnailPanel;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 图片管理器主应用程序
 * <p>
 * 基于 JavaFX 构建的桌面图片浏览与管理工具，支持以下核心功能：
 * <ul>
 *   <li>目录树浏览：左侧面板展示磁盘目录结构，支持懒加载</li>
 *   <li>缩略图网格：右侧面板以卡片形式展示图片缩略图</li>
 *   <li>幻灯片播放：全屏/窗口模式浏览图片，支持自动播放和缩放</li>
 *   <li>文件操作：复制、粘贴、删除、重命名（含批量重命名）</li>
 *   <li>图片变换：旋转（90°/180°）、水平/垂直翻转</li>
 *   <li>主题切换：浅色/深色主题一键切换</li>
 *   <li>排序功能：按名称、修改日期、文件大小排序</li>
 *   <li>框选操作：鼠标拖拽框选多张图片</li>
 * </ul>
 * <p>
 * 布局结构：
 * <pre>
 * ┌─────────────────────────────────────────────┐
 * │              工具栏（ToolbarContent）          │
 * ├──────────┬──────────────────────────────────┤
 * │          │                                    │
 * │  目录树   │         缩略图面板                  │
 * │  面板     │     （ThumbnailPanel）             │
 * │          │                                    │
 * ├──────────┴──────────────────────────────────┤
 * │              状态栏（StatusBar）               │
 * └─────────────────────────────────────────────┘
 * </pre>
 * <p>
 * 架构模式：简化的 MVC（Model-View-Controller）
 * <ul>
 *   <li>Model：{@link AppState} 管理共享状态，{@link ThemeManager} 管理主题</li>
 *   <li>View：DirectoryTreePanel、ThumbnailPanel、SlideshowWindow 等</li>
 *   <li>Controller：本类作为主控制器，协调各组件交互；Service 层封装业务逻辑</li>
 * </ul>
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class PhotoManagerApp extends Application {

    // ==================== 核心模型和服务 ====================

    /** 应用共享状态，存储当前目录、图片列表、选中状态等 */
    private AppState appState;

    /** 状态栏管理器，封装状态信息、目录信息、计数徽章的更新逻辑 */
    private StatusManager statusManager;

    /** 目录树面板，左侧边栏 */
    private DirectoryTreePanel directoryTreePanel;

    /** 缩略图面板，右侧主内容区 */
    private ThumbnailPanel thumbnailPanel;

    /** 文件操作服务，处理复制/粘贴/删除/重命名 */
    private FileOperationService fileOperationService;

    /** 图片变换服务，处理旋转/翻转 */
    private ImageTransformService imageTransformService;

    /** 图片缓存服务，管理缩略图和全尺寸图片的内存缓存 */
    private ImageCacheService imageCacheService;

    // ==================== UI 组件 ====================

    /** 工具栏内容容器（包含目录信息、计数徽章、按钮栏） */
    private HBox toolbarContent;

    /** 底部状态栏容器 */
    private HBox statusBar;

    /** 工具栏按钮栏 */
    private HBox toolbar;

    /** 快捷键提示标签 */
    private Label shortcutHint;

    /** 状态信息标签 */
    private Label statusLabel;

    /** 目录信息标签 */
    private Label directoryInfoLabel;

    /** 图片计数徽章 */
    private Label imageCountBadge;

    /** 选中计数徽章 */
    private Label selectedCountLabel;

    /** 主分割面板（左：目录树，右：缩略图） */
    private SplitPane mainSplitPane;

    /** 主题切换按钮 */
    private Button themeBtn;

    /** 幻灯片按钮（固定绿色，不随主题变化） */
    private Button slideshowBtn;

    /** 排序字段下拉框 */
    private ComboBox<String> sortFieldCombo;

    /** 排序顺序下拉框 */
    private ComboBox<String> sortOrderCombo;

    /** 紧凑模式标志：窗口宽度不足时按钮仅显示图标 */
    private boolean compactMode = false;

    /** 应用程序入口 */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 应用启动入口
     * <p>
     * 初始化流程：
     * <ol>
     *   <li>创建共享状态和缓存服务</li>
     *   <li>构建 UI 布局</li>
     *   <li>注册键盘快捷键</li>
     *   <li>绑定主题管理器</li>
     *   <li>播放启动动画（缩放 + 淡入）</li>
     *   <li>显示欢迎对话框</li>
     * </ol>
     */
    @Override
    public void start(Stage primaryStage) {
        appState = new AppState();
        imageCacheService = new ImageCacheService();

        primaryStage.setTitle(DesignConstants.APP_TITLE);

        createLayoutStructure(primaryStage);
        setupKeyboardShortcuts(primaryStage.getScene());

        ThemeManager.getInstance().setMainScene(primaryStage.getScene());
        ThemeManager.getInstance().addThemeChangeListener(this::applyThemeToDynamicComponents);

        primaryStage.show();

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), primaryStage.getScene().getRoot());
        scaleIn.setFromX(0.95);
        scaleIn.setFromY(0.95);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.play();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), primaryStage.getScene().getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        showWelcomeMessage();
    }

    /** 应用关闭时释放缓存服务资源 */
    @Override
    public void stop() {
        if (imageCacheService != null) {
            imageCacheService.shutdown();
        }
    }

    /**
     * 创建整体布局结构
     * <p>
     * 构建顺序：状态标签 → 服务对象 → 面板组件 → 工具栏 → 分割面板 → 场景
     *
     * @param primaryStage 主舞台
     */
    private void createLayoutStructure(Stage primaryStage) {
        statusLabel = new Label("Ready - Browse your photos");
        StyleUtils.applyStatusLabelStyle(statusLabel);
        directoryInfoLabel = new Label("Select a folder to browse images");
        imageCountBadge = new Label("");
        selectedCountLabel = new Label("");

        StyleUtils.applyToolbarTitleStyle(directoryInfoLabel);
        StyleUtils.applyCountBadgeStyle(imageCountBadge);
        StyleUtils.applyStatusBadgeStyle(selectedCountLabel);

        statusManager = new StatusManager(
                statusLabel, directoryInfoLabel, imageCountBadge, selectedCountLabel);

        fileOperationService = new FileOperationService(
                appState, statusManager, () -> {
                    if (appState.getCurrentDirectory() != null) {
                        thumbnailPanel.loadImagesFromDirectory(appState.getCurrentDirectory());
                    }
                });

        imageTransformService = new ImageTransformService(
                appState, statusManager, () -> {
                    if (appState.getCurrentDirectory() != null) {
                        thumbnailPanel.loadImagesFromDirectory(appState.getCurrentDirectory());
                    }
                });

        directoryTreePanel = new DirectoryTreePanel(dir -> {
            thumbnailPanel.loadImagesFromDirectory(dir);
        });

        thumbnailPanel = new ThumbnailPanel(
                appState, statusManager, fileOperationService, imageCacheService,
                startIndex -> openSlideshow(startIndex, primaryStage));

        toolbar = createToolbar();
        toolbarContent = createToolbarContent(directoryInfoLabel, imageCountBadge, toolbar);

        BorderPane rightPane = new BorderPane();
        rightPane.setTop(toolbarContent);
        rightPane.setCenter(thumbnailPanel);

        mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(directoryTreePanel, rightPane);
        mainSplitPane.setDividerPositions(0.20);
        mainSplitPane.setStyle("-fx-background-color: " + DesignConstants.bgMain() + ";");

        statusBar = createStatusBar(statusLabel, selectedCountLabel);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setCenter(mainSplitPane);
        rootLayout.setBottom(statusBar);

        Scene scene = new Scene(rootLayout, 1400, 850);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(680);

        scene.widthProperty().addListener((obs, oldVal, newVal) -> updateToolbarButtonDisplay(newVal.doubleValue()));
    }

    /**
     * 主题切换时刷新动态组件的样式
     * <p>
     * 静态 CSS 无法覆盖的 inline style 需要在此方法中手动刷新。
     * 包括工具栏背景、状态栏、标签颜色、按钮样式、下拉框样式等。
     * 注意：slideshow 按钮使用固定绿色，不在此刷新。
     */
    private void applyThemeToDynamicComponents() {
        if (toolbarContent != null) {
            toolbarContent.setStyle(
                    "-fx-background-color: " + DesignConstants.bgCard() + ";"
                            + " -fx-border-color: " + DesignConstants.border() + ";"
                            + " -fx-border-width: 0 0 1 0;"
            );
        }
        if (statusBar != null) {
            statusBar.setStyle(
                    "-fx-background-color: " + DesignConstants.bgCard() + ";"
                            + " -fx-border-color: " + DesignConstants.border() + ";"
                            + " -fx-border-width: 1 0 0 0;"
            );
        }
        if (shortcutHint != null) {
            shortcutHint.setStyle(
                    "-fx-font-size: 10px;"
                            + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
            );
        }
        if (statusLabel != null) {
            StyleUtils.applyStatusLabelStyle(statusLabel);
        }
        if (directoryInfoLabel != null) {
            StyleUtils.applyToolbarTitleStyle(directoryInfoLabel);
        }
        if (imageCountBadge != null) {
            StyleUtils.applyCountBadgeStyle(imageCountBadge);
        }
        if (selectedCountLabel != null) {
            StyleUtils.applyStatusBadgeStyle(selectedCountLabel);
        }
        if (mainSplitPane != null) {
            mainSplitPane.setStyle("-fx-background-color: " + DesignConstants.bgMain() + ";");
        }
        if (themeBtn != null) {
            updateThemeButtonIcon();
        }
        if (toolbar != null) {
            refreshToolbarButtons();
        }
        if (sortFieldCombo != null) {
            StyleUtils.applyComboBoxStyle(sortFieldCombo);
        }
        if (sortOrderCombo != null) {
            StyleUtils.applyComboBoxStyle(sortOrderCombo);
        }
    }

    /** 刷新工具栏按钮样式（跳过 theme 和 slideshow 按钮） */
    private void refreshToolbarButtons() {
        for (Node node : toolbar.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn == themeBtn) continue;
                if (btn == slideshowBtn) continue;
                btn.setStyle(StyleUtils.buildToolbarBtnNormalStyle());
            }
        }
    }

    /**
     * 根据场景宽度切换按钮显示模式
     * <p>
     * 仅在模式变化时触发更新，避免频繁重绘。
     *
     * @param sceneWidth 当前场景宽度
     */
    private void updateToolbarButtonDisplay(double sceneWidth) {
        boolean newCompact = sceneWidth < 1250;
        if (newCompact == compactMode) return;
        compactMode = newCompact;
        applyToolbarButtonTexts();
    }

    /** 根据当前紧凑模式统一更新所有工具栏按钮的文字显示 */
    private void applyToolbarButtonTexts() {
        for (Node node : toolbar.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                Object iconObj = btn.getProperties().get("icon");
                Object textObj = btn.getProperties().get("text");
                if (iconObj == null || textObj == null) continue;
                String icon = iconObj.toString();
                String text = textObj.toString();
                if (compactMode) {
                    btn.setText(icon);
                } else {
                    btn.setText(icon + " " + text);
                }
            }
        }
    }

    /** 更新主题按钮的图标（浅色主题显示月亮🌙，深色主题显示太阳☀） */
    private void updateThemeButtonIcon() {
        boolean isDark = ThemeManager.getInstance().isDarkTheme();
        String icon = isDark ? "\u2600" : "\uD83C\uDF19";
        themeBtn.getProperties().put("icon", icon);
        if (compactMode) {
            themeBtn.setText(icon);
        } else {
            themeBtn.setText(icon + " Theme");
        }
    }

    /**
     * 创建工具栏内容区域
     * <p>
     * 布局：[目录信息] ──弹性间距── [图片计数徽章] [按钮栏]
     */
    private HBox createToolbarContent(Label directoryInfoLabel,
                                       Label imageCountBadge, HBox toolbar) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(15, directoryInfoLabel, spacer, imageCountBadge, toolbar);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10, 20, 10, 20));
        content.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-border-color: " + DesignConstants.border() + ";"
                        + " -fx-border-width: 0 0 1 0;"
        );
        return content;
    }

    /**
     * 创建工具栏按钮栏
     * <p>
     * 按钮分组：
     * [Slideshow] | [Refresh][Select All][Clear] | [90°][-90°][Flip H][Flip V] | [排序字段][排序顺序] | [Theme]
     */
    private HBox createToolbar() {
        slideshowBtn = StyleUtils.createModernButton("\u25B6 Slideshow", "#4CAF50", true);
        slideshowBtn.setTooltip(new Tooltip("Open slideshow (Enter)"));
        slideshowBtn.setOnAction(e -> openSlideshow(0, null));

        Button refreshBtn = StyleUtils.createToolbarButton("Refresh", "\uD83D\uDD04");
        refreshBtn.setTooltip(new Tooltip("Reload current folder (F5)"));
        refreshBtn.setOnAction(e -> {
            if (appState.getCurrentDirectory() != null) {
                thumbnailPanel.loadImagesFromDirectory(appState.getCurrentDirectory());
            }
        });

        Button selectAllBtn = StyleUtils.createToolbarButton("Select All", "\u2714");
        selectAllBtn.setTooltip(new Tooltip("Select all images (Ctrl+A)"));
        selectAllBtn.setOnAction(e -> thumbnailPanel.selectAllImages());

        Button clearBtn = StyleUtils.createToolbarButton("Clear", "\u2716");
        clearBtn.setTooltip(new Tooltip("Clear selection (Esc)"));
        clearBtn.setOnAction(e -> thumbnailPanel.clearSelection());

        Separator sep1 = new Separator(Orientation.VERTICAL);
        sep1.setPrefHeight(24);

        Button rotateRightBtn = StyleUtils.createToolbarButton("90\u00B0", "\u21BB");
        rotateRightBtn.setTooltip(new Tooltip("Rotate 90\u00B0 clockwise (Ctrl+R)"));
        rotateRightBtn.setOnAction(e -> imageTransformService.rotateRight());

        Button rotateLeftBtn = StyleUtils.createToolbarButton("-90\u00B0", "\u21BA");
        rotateLeftBtn.setTooltip(new Tooltip("Rotate 90\u00B0 counter-clockwise (Ctrl+L)"));
        rotateLeftBtn.setOnAction(e -> imageTransformService.rotateLeft());

        Button flipHBtn = StyleUtils.createToolbarButton("Flip H", "\u2194");
        flipHBtn.setTooltip(new Tooltip("Flip horizontal (Ctrl+H)"));
        flipHBtn.setOnAction(e -> imageTransformService.flipHorizontal());

        Button flipVBtn = StyleUtils.createToolbarButton("Flip V", "\u2195");
        flipVBtn.setTooltip(new Tooltip("Flip vertical (Ctrl+Shift+H)"));
        flipVBtn.setOnAction(e -> imageTransformService.flipVertical());

        Separator sep2 = new Separator(Orientation.VERTICAL);
        sep2.setPrefHeight(24);

        sortFieldCombo = new ComboBox<>();
        sortFieldCombo.getItems().addAll("Name", "Date", "Size");
        sortFieldCombo.setValue("Name");
        sortFieldCombo.setTooltip(new Tooltip("Sort by field"));
        sortFieldCombo.setMinWidth(Region.USE_PREF_SIZE);
        StyleUtils.applyComboBoxStyle(sortFieldCombo);
        sortFieldCombo.setOnAction(e -> applySortAndRefresh());

        sortOrderCombo = new ComboBox<>();
        sortOrderCombo.getItems().addAll("\u2191 Asc", "\u2193 Desc");
        sortOrderCombo.setValue("\u2191 Asc");
        sortOrderCombo.setTooltip(new Tooltip("Sort order"));
        sortOrderCombo.setMinWidth(Region.USE_PREF_SIZE);
        StyleUtils.applyComboBoxStyle(sortOrderCombo);
        sortOrderCombo.setOnAction(e -> applySortAndRefresh());

        Separator sep3 = new Separator(Orientation.VERTICAL);
        sep3.setPrefHeight(24);

        themeBtn = StyleUtils.createToolbarButton("Theme", "\uD83C\uDF19");
        themeBtn.setTooltip(new Tooltip("Toggle dark/light theme (Ctrl+T)"));
        themeBtn.setOnAction(e -> ThemeManager.getInstance().toggleTheme());

        return new HBox(4,
                slideshowBtn,
                sep1,
                refreshBtn, selectAllBtn, clearBtn,
                sep2,
                rotateRightBtn, rotateLeftBtn, flipHBtn, flipVBtn,
                sep3,
                sortFieldCombo, sortOrderCombo,
                new Separator(Orientation.VERTICAL) {{ setPrefHeight(24); }},
                themeBtn);
    }

    /** 应用排序设置并刷新图片列表 */
    private void applySortAndRefresh() {
        String field = sortFieldCombo.getValue();
        String order = sortOrderCombo.getValue();

        if ("Name".equals(field)) {
            appState.setSortField(AppState.SortField.NAME);
        } else if ("Date".equals(field)) {
            appState.setSortField(AppState.SortField.DATE);
        } else if ("Size".equals(field)) {
            appState.setSortField(AppState.SortField.SIZE);
        }

        if (order != null && order.contains("Desc")) {
            appState.setSortOrder(AppState.SortOrder.DESC);
        } else {
            appState.setSortOrder(AppState.SortOrder.ASC);
        }

        if (appState.getCurrentDirectory() != null) {
            thumbnailPanel.loadImagesFromDirectory(appState.getCurrentDirectory());
        }
    }

    /**
     * 创建底部状态栏
     * <p>
     * 布局：[ℹ 状态信息] ──弹性间距── [快捷键提示] [选中计数]
     */
    private HBox createStatusBar(Label statusLabel, Label selectedCountLabel) {
        shortcutHint = new Label("F5:Refresh | Del:Delete | Ctrl+C/V:Copy/Paste | \u2190\u2192:Navigate");
        shortcutHint.setStyle(
                "-fx-font-size: 10px;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
        );

        HBox statusLeft = new HBox(StyleUtils.createInfoIcon(), statusLabel);
        statusLeft.setSpacing(8);
        statusLeft.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(15, statusLeft, spacer, shortcutHint, selectedCountLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-border-color: " + DesignConstants.border() + ";"
                        + " -fx-border-width: 1 0 0 0;"
        );
        return bar;
    }

    /**
     * 打开幻灯片窗口
     *
     * @param startIndex  起始图片索引
     * @param ownerStage  父窗口（可为 null，自动获取）
     */
    private void openSlideshow(int startIndex, Stage ownerStage) {
        if (appState.getCurrentImages().isEmpty()) {
            showAlert("Info", "No images!");
            return;
        }
        Stage owner = ownerStage != null ? ownerStage
                : (Stage) directoryTreePanel.getDirectoryTree().getScene().getWindow();
        String dirName = appState.getCurrentDirectory() != null
                ? appState.getCurrentDirectory().getName() : "";
        SlideshowWindow slideshow = new SlideshowWindow(
                appState.getCurrentImages(), dirName, owner, imageCacheService);
        slideshow.open(startIndex);
    }

    /**
     * 注册全局键盘快捷键
     * <p>
     * 使用 {@code addEventFilter} 在事件捕获阶段处理，优先级高于各控件的按键处理。
     * 当地址栏获得焦点时，除 Escape 外的所有快捷键被跳过，避免干扰文字输入。
     *
     * @param scene 主场景
     */
    private void setupKeyboardShortcuts(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (directoryTreePanel.getAddressField().isFocused()
                    && e.getCode() != KeyCode.ESCAPE) {
                return;
            }
            if (isComboBoxFocused(e)) return;

            switch (e.getCode()) {
                case DELETE:
                    fileOperationService.deleteFiles();
                    e.consume();
                    break;
                case C:
                    if (e.isControlDown() && !e.isShiftDown()) {
                        fileOperationService.copySelectedFiles();
                        e.consume();
                    }
                    break;
                case V:
                    if (e.isControlDown()) {
                        fileOperationService.pasteFiles();
                        e.consume();
                    }
                    break;
                case A:
                    if (e.isControlDown()) {
                        thumbnailPanel.selectAllImages();
                        e.consume();
                    }
                    break;
                case R:
                    if (e.isControlDown() && !e.isShiftDown()) {
                        imageTransformService.rotateRight();
                        e.consume();
                    }
                    break;
                case L:
                    if (e.isControlDown()) {
                        imageTransformService.rotateLeft();
                        e.consume();
                    }
                    break;
                case H:
                    if (e.isControlDown() && !e.isShiftDown()) {
                        imageTransformService.flipHorizontal();
                        e.consume();
                    } else if (e.isControlDown() && e.isShiftDown()) {
                        imageTransformService.flipVertical();
                        e.consume();
                    }
                    break;
                case T:
                    if (e.isControlDown()) {
                        ThemeManager.getInstance().toggleTheme();
                        e.consume();
                    }
                    break;
                case F5:
                    if (appState.getCurrentDirectory() != null) {
                        thumbnailPanel.loadImagesFromDirectory(appState.getCurrentDirectory());
                    }
                    e.consume();
                    break;
                case LEFT:
                    thumbnailPanel.navigateImage(-1);
                    e.consume();
                    break;
                case RIGHT:
                    thumbnailPanel.navigateImage(1);
                    e.consume();
                    break;
                default:
                    break;
            }
        });
    }

    /** 判断当前按键事件的焦点是否在下拉框上，避免快捷键冲突 */
    private boolean isComboBoxFocused(KeyEvent e) {
        javafx.scene.Node focusOwner = e.getTarget() instanceof javafx.scene.Node
                ? (javafx.scene.Node) e.getTarget() : null;
        if (focusOwner == null) return false;
        if (focusOwner instanceof ComboBox) return true;
        javafx.scene.Node parent = focusOwner.getParent();
        while (parent != null) {
            if (parent instanceof ComboBox) return true;
            parent = parent.getParent();
        }
        return false;
    }

    /** 显示欢迎对话框，介绍应用功能 */
    private void showWelcomeMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Welcome to Photo Manager Pro");
        alert.setHeaderText("Modern Photo Management Application");
        alert.setContentText(
                "\nFeatures:\n"
                        + " Browse disk directories from the beautiful sidebar\n"
                        + " Address bar: type any path and press Enter to jump\n"
                        + " View image thumbnails in card layout\n"
                        + " Click to select, Ctrl+Click for multi-select\n"
                        + " Drag to box-select multiple images\n"
                        + " Double-click to enter slideshow mode\n"
                        + " Right-click for Copy/Paste/Rename/Delete\n"
                        + " Rotate & Flip images (90\u00B0, 180\u00B0, H/V flip)\n"
                        + " Sort images by Name, Date, or Size\n"
                        + " Dark/Light theme toggle\n"
                        + "\nKeyboard Shortcuts:\n"
                        + " \u2190/\u2192       = Navigate previous/next image\n"
                        + " Delete     = Delete selected images\n"
                        + " Ctrl+C/V   = Copy / Paste images\n"
                        + " Ctrl+A     = Select all images\n"
                        + " Ctrl+R/L   = Rotate right / left\n"
                        + " Ctrl+H     = Flip horizontal\n"
                        + " Ctrl+Shift+H = Flip vertical\n"
                        + " Ctrl+T     = Toggle dark/light theme\n"
                        + " F5         = Refresh current folder\n"
                        + "\nSlideshow Controls:\n"
                        + " \u2190/\u2192       = Navigate previous/next image\n"
                        + " Ctrl+Wheel  = Zoom in/out\n"
                        + " Adjustable auto-play interval\n"
                        + "\nSupported formats: JPG, JPEG, GIF, PNG, BMP"
        );
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
