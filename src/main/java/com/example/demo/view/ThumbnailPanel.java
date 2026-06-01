package com.example.demo.view;

import com.example.demo.model.AppState;
import com.example.demo.model.StatusManager;
import com.example.demo.model.ThemeManager;
import com.example.demo.service.FileOperationService;
import com.example.demo.service.ImageCacheService;
import com.example.demo.util.DesignConstants;
import com.example.demo.util.FileUtils;
import com.example.demo.util.StyleUtils;
import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Duration;

/**
 * 缩略图面板
 * <p>
 * 右侧主内容区组件，以网格卡片形式展示当前目录下的图片缩略图。
 * 核心功能：
 * <ul>
 *   <li>图片加载：扫描目录中的图片文件，异步加载缩略图并显示</li>
 *   <li>排序：支持按名称、修改日期、文件大小排序（递增/递减）</li>
 *   <li>选择：单击选中、Ctrl+单击多选、拖拽框选、全选/清除</li>
 *   <li>交互：双击打开幻灯片、右键上下文菜单、悬停预览</li>
 *   <li>导航：左右键切换选中图片，自动滚动到可见区域</li>
 * </ul>
 * <p>
 * 布局结构：
 * <pre>
 * StackPane
 * └── ScrollPane
 *     └── VBox (thumbnailContainer)
 *         └── StackPane (overlayContainer)
 *             ├── FlowPane (thumbnailPane) — 卡片网格
 *             └── Rectangle (selectionRectangle) — 框选矩形
 * </pre>
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class ThumbnailPanel extends StackPane {

    /** 应用共享状态 */
    private final AppState appState;

    /** 状态栏管理器 */
    private final StatusManager statusManager;

    /** 文件操作服务 */
    private final FileOperationService fileOperationService;

    /** 图片缓存服务 */
    private final ImageCacheService imageCacheService;

    /** 打开幻灯片的回调，参数为起始图片索引 */
    private final java.util.function.IntConsumer onOpenSlideshow;

    /** 缩略图卡片网格容器 */
    private final FlowPane thumbnailPane = new FlowPane();

    /** 滚动容器 */
    private final ScrollPane scrollPane = new ScrollPane();

    /** 缩略图内容容器（用于绑定最小高度） */
    private final VBox thumbnailContainer = new VBox();

    /** 覆盖层容器（包含卡片网格和框选矩形） */
    private final StackPane overlayContainer = new StackPane();

    /** 框选矩形 */
    private Rectangle selectionRectangle;

    /** 是否正在框选 */
    private boolean isSelecting = false;

    /** 框选起始 X 坐标 */
    private double startX, startY;

    /** 当前活跃的右键菜单 */
    private ContextMenu activeContextMenu;

    /** 悬停预览定时器 */
    private Timeline hoverTimeline;

    /** 悬停预览弹出窗口 */
    private Popup hoverPopup;

    /** 最近键盘导航的图片索引 */
    private int lastNavigatedIndex = -1;

    /**
     * 构造缩略图面板
     *
     * @param appState             应用共享状态
     * @param statusManager        状态栏管理器
     * @param fileOperationService 文件操作服务
     * @param imageCacheService    图片缓存服务
     * @param onOpenSlideshow      打开幻灯片的回调
     */
    public ThumbnailPanel(AppState appState, StatusManager statusManager,
                           FileOperationService fileOperationService,
                           ImageCacheService imageCacheService,
                           java.util.function.IntConsumer onOpenSlideshow) {
        this.appState = appState;
        this.statusManager = statusManager;
        this.fileOperationService = fileOperationService;
        this.imageCacheService = imageCacheService;
        this.onOpenSlideshow = onOpenSlideshow;

        setupLayout();
        setupSelectionRectangle();
        setupContextMenu();
        setupGlobalMenuHide();
        setupThemeListener();
    }

    /** 初始化布局：FlowPane + ScrollPane + 覆盖层 */
    private void setupLayout() {
        thumbnailPane.setHgap(14);
        thumbnailPane.setVgap(14);
        thumbnailPane.setPadding(new Insets(16));
        thumbnailPane.setStyle("-fx-background-color: " + DesignConstants.bgMain() + ";");

        StackPane.setAlignment(thumbnailPane, Pos.TOP_LEFT);
        overlayContainer.getChildren().add(thumbnailPane);

        thumbnailContainer.getChildren().add(overlayContainer);
        VBox.setVgrow(overlayContainer, Priority.ALWAYS);
        thumbnailContainer.minHeightProperty().bind(
                Bindings.selectDouble(scrollPane.viewportBoundsProperty(), "height"));

        scrollPane.setContent(thumbnailContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle(
                "-fx-background: " + DesignConstants.bgMain() + ";"
                        + " -fx-background-color: " + DesignConstants.bgMain() + ";"
        );

        getChildren().add(scrollPane);
    }

    /** 注册主题变更监听器，切换主题时刷新卡片样式和标签颜色 */
    private void setupThemeListener() {
        ThemeManager.getInstance().addThemeChangeListener(() -> {
            thumbnailPane.setStyle("-fx-background-color: " + DesignConstants.bgMain() + ";");
            scrollPane.setStyle(
                    "-fx-background: " + DesignConstants.bgMain() + ";"
                            + " -fx-background-color: " + DesignConstants.bgMain() + ";"
            );
            for (Node node : thumbnailPane.getChildren()) {
                if (node instanceof VBox) {
                    VBox card = (VBox) node;
                    if (appState.getSelectedNodes().contains(card)) {
                        StyleUtils.applySelectedStyle(card);
                    } else {
                        StyleUtils.applyNormalStyle(card);
                    }
                    updateCardLabelStyles(card);
                }
            }
            updateSelectionRectangleTheme();
        });
    }

    /** 更新卡片内标签的文字颜色（主题切换时调用） */
    private void updateCardLabelStyles(VBox card) {
        for (Node child : card.getChildren()) {
            if (child instanceof VBox) {
                VBox infoBox = (VBox) child;
                for (Node labelNode : infoBox.getChildren()) {
                    if (labelNode instanceof Label) {
                        Label label = (Label) labelNode;
                        String current = label.getStyle();
                        if (current.contains("-fx-font-size: 10px;")) {
                            label.setStyle(
                                    "-fx-font-size: 10px;"
                                            + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                                            + " -fx-font-family: 'Segoe UI', sans-serif;"
                            );
                        } else if (current.contains("-fx-font-size: 9px;")) {
                            label.setStyle(
                                    "-fx-font-size: 9px;"
                                            + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                                            + " -fx-opacity: 0.7;"
                            );
                        }
                    }
                }
            }
        }
    }

    /** 更新框选矩形的主题颜色 */
    private void updateSelectionRectangleTheme() {
        if (selectionRectangle != null) {
            selectionRectangle.setFill(Color.web(DesignConstants.selectionFill()));
            selectionRectangle.setStroke(Color.web(DesignConstants.selectionStroke()));
        }
    }

    /**
     * 从指定目录加载图片并显示缩略图
     * <p>
     * 流程：清空选中 → 扫描图片文件 → 排序 → 创建卡片 → 播放入场动画
     *
     * @param directory 目标目录
     */
    public void loadImagesFromDirectory(File directory) {
        appState.setCurrentDirectory(directory);
        appState.getSelectedNodes().clear();
        thumbnailPane.getChildren().clear();
        stopHoverPreview();

        if (directory == null || !directory.isDirectory()) return;

        File[] files = directory.listFiles(f -> {
            if (!f.isFile()) return false;
            String name = f.getName().toLowerCase();
            for (String ext : DesignConstants.SUPPORTED_EXTENSIONS) {
                if (name.endsWith(ext)) return true;
            }
            return false;
        });

        if (files == null || files.length == 0) {
            statusManager.setStatus("No images found in: " + directory.getName());
            statusManager.setDirectoryInfo(directory.getName());
            statusManager.setImageCount("0 photos");
            appState.setCurrentImages(FXCollections.observableArrayList());
            return;
        }

        List<File> imageFiles = new ArrayList<>(Arrays.asList(files));
        sortImageFiles(imageFiles);

        ObservableList<File> currentImages = FXCollections.observableArrayList(imageFiles);
        appState.setCurrentImages(currentImages);

        statusManager.setDirectoryInfo(directory.getName());
        statusManager.setImageCount(imageFiles.size() + " photos");

        for (int i = 0; i < imageFiles.size(); i++) {
            File file = imageFiles.get(i);
            VBox card = createThumbnailCard(file, i);
            thumbnailPane.getChildren().add(card);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), card);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setDelay(Duration.millis(i * 15));
            fadeIn.play();
        }

        statusManager.setStatus("Loaded " + imageFiles.size() + " images from " + directory.getName());
    }

    /** 根据当前排序设置对图片文件列表排序 */
    private void sortImageFiles(List<File> files) {
        Comparator<File> comparator = buildComparator();
        files.sort(comparator);
    }

    /** 根据排序字段和排序顺序构建比较器 */
    private Comparator<File> buildComparator() {
        Comparator<File> comparator;

        switch (appState.getSortField()) {
            case DATE:
                comparator = Comparator.comparingLong(File::lastModified);
                break;
            case SIZE:
                comparator = Comparator.comparingLong(File::length);
                break;
            case NAME:
            default:
                comparator = Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER);
                break;
        }

        if (appState.getSortOrder() == AppState.SortOrder.DESC) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    /**
     * 创建单张图片的缩略图卡片
     * <p>
     * 卡片结构：VBox[ImageView, VBox[文件名Label, 文件大小Label]]
     *
     * @param file  图片文件
     * @param index 在列表中的索引
     * @return 配置好的 VBox 卡片
     */
    private VBox createThumbnailCard(File file, int index) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(DesignConstants.THUMBNAIL_SIZE);
        imageView.setFitHeight(DesignConstants.THUMBNAIL_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        imageCacheService.loadThumbnailAsync(file, img -> {
            imageView.setImage(img);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), imageView);
            scaleIn.setFromX(0.8);
            scaleIn.setFromY(0.8);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);
            scaleIn.play();
        });

        Label nameLabel = new Label(file.getName());
        nameLabel.setStyle(
                "-fx-font-size: 10px;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-font-family: 'Segoe UI', sans-serif;"
        );
        nameLabel.setMaxWidth(DesignConstants.THUMBNAIL_SIZE + 10);

        Label sizeLabel = new Label(FileUtils.formatFileSize(file.length()));
        sizeLabel.setStyle(
                "-fx-font-size: 9px;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-opacity: 0.7;"
        );

        VBox infoBox = new VBox(2, nameLabel, sizeLabel);
        infoBox.setAlignment(Pos.CENTER);

        VBox card = new VBox(6, imageView, infoBox);
        card.setAlignment(Pos.CENTER);
        card.setUserData(file);
        StyleUtils.applyNormalStyle(card);
        card.setCursor(Cursor.HAND);

        setupCardInteraction(card, file, index);

        return card;
    }

    /** 设置卡片的鼠标交互：单击选中、Ctrl多选、双击打开幻灯片、右键菜单、悬停效果 */
    private void setupCardInteraction(VBox card, File file, int index) {
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 2) {
                    onOpenSlideshow.accept(index);
                    return;
                }
                if (e.isControlDown()) {
                    toggleSelection(card);
                } else {
                    clearSelection();
                    appState.getSelectedNodes().add(card);
                    StyleUtils.applySelectedStyle(card);
                    lastNavigatedIndex = index;
                }
                updateStatusAfterSelection();
            }
        });

        card.setOnMouseEntered(e -> {
            if (!appState.getSelectedNodes().contains(card)) {
                StyleUtils.applyHoverStyle(card);
            }
            startHoverPreview(card, file, e);
        });

        card.setOnMouseExited(e -> {
            if (!appState.getSelectedNodes().contains(card)) {
                StyleUtils.applyNormalStyle(card);
            }
            stopHoverPreview();
        });

        card.setOnContextMenuRequested(e -> {
            hideActiveContextMenu();
            if (!appState.getSelectedNodes().contains(card)) {
                clearSelection();
                appState.getSelectedNodes().add(card);
                StyleUtils.applySelectedStyle(card);
                lastNavigatedIndex = index;
                updateStatusAfterSelection();
            }
            ContextMenu menu = createContextMenu();
            activeContextMenu = menu;
            menu.show(card, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    /** 清除所有选中状态 */
    public void clearSelection() {
        for (VBox node : appState.getSelectedNodes()) {
            StyleUtils.applyNormalStyle(node);
        }
        appState.getSelectedNodes().clear();
        updateStatusAfterSelection();
    }

    /** 全选当前目录下的所有图片 */
    public void selectAllImages() {
        appState.getSelectedNodes().clear();
        for (Node node : thumbnailPane.getChildren()) {
            if (node instanceof VBox) {
                VBox box = (VBox) node;
                appState.getSelectedNodes().add(box);
                StyleUtils.applySelectedStyle(box);
            }
        }
        updateStatusAfterSelection();
    }

    /**
     * 键盘导航图片
     * <p>
     * direction=-1 向前，direction=1 向后。到达边界时循环。
     *
     * @param direction 导航方向（-1 或 1）
     */
    public void navigateImage(int direction) {
        List<File> images = appState.getCurrentImages();
        if (images == null || images.isEmpty()) return;

        if (lastNavigatedIndex < 0) {
            lastNavigatedIndex = 0;
        } else {
            lastNavigatedIndex += direction;
        }

        if (lastNavigatedIndex < 0) lastNavigatedIndex = images.size() - 1;
        if (lastNavigatedIndex >= images.size()) lastNavigatedIndex = 0;

        clearSelection();

        int idx = lastNavigatedIndex;
        if (idx < thumbnailPane.getChildren().size()) {
            Node node = thumbnailPane.getChildren().get(idx);
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                appState.getSelectedNodes().add(card);
                StyleUtils.applySelectedStyle(card);
                ensureVisible(card);
            }
        }
        updateStatusAfterSelection();
    }

    /** 确保指定节点在滚动面板的可见区域内 */
    private void ensureVisible(Node node) {
        if (node == null || scrollPane == null) return;
        Bounds bounds = node.getBoundsInParent();
        double scrollV = scrollPane.getVvalue();
        double viewH = scrollPane.getHeight();
        double contentH = thumbnailContainer.getHeight();

        if (contentH <= viewH) return;

        double nodeTop = bounds.getMinY();
        double nodeBottom = bounds.getMaxY();

        double flowOffset = thumbnailPane.getBoundsInParent().getMinY();
        nodeTop += flowOffset;
        nodeBottom += flowOffset;

        double viewTop = scrollV * (contentH - viewH);
        double viewBottom = viewTop + viewH;

        if (nodeTop < viewTop) {
            scrollPane.setVvalue(nodeTop / (contentH - viewH));
        } else if (nodeBottom > viewBottom) {
            scrollPane.setVvalue((nodeBottom - viewH) / (contentH - viewH));
        }
    }

    /** 切换卡片的选中/未选中状态 */
    private void toggleSelection(VBox card) {
        if (appState.getSelectedNodes().contains(card)) {
            appState.getSelectedNodes().remove(card);
            StyleUtils.applyNormalStyle(card);
        } else {
            appState.getSelectedNodes().add(card);
            StyleUtils.applySelectedStyle(card);
        }
    }

    /** 选中状态变更后更新状态栏信息 */
    private void updateStatusAfterSelection() {
        int count = appState.getSelectedNodes().size();
        statusManager.setSelectedCount(count + " Selected", count > 0);
        if (count == 1) {
            File file = (File) appState.getSelectedNodes().iterator().next().getUserData();
            statusManager.setStatus("Selected: " + file.getName());
        } else if (count > 1) {
            statusManager.setStatus(count + " images selected");
        }
    }

    /** 初始化框选矩形和鼠标框选事件 */
    private void setupSelectionRectangle() {
        selectionRectangle = new Rectangle();
        selectionRectangle.setFill(Color.web(DesignConstants.selectionFill()));
        selectionRectangle.setStroke(Color.web(DesignConstants.selectionStroke()));
        selectionRectangle.getStrokeDashArray().addAll(8.0, 4.0);
        selectionRectangle.setVisible(false);
        selectionRectangle.setMouseTransparent(true);
        selectionRectangle.setManaged(false);

        overlayContainer.getChildren().add(selectionRectangle);

        overlayContainer.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressForSelection);
        overlayContainer.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragForSelection);
        overlayContainer.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleaseForSelection);
    }

    /** 处理鼠标按下事件：开始框选 */
    private void handleMousePressForSelection(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;
        hideActiveContextMenu();
        if (isTargetOnCard(e.getTarget())) return;
        if (!e.isControlDown()) clearSelection();
        isSelecting = true;
        startX = e.getX();
        startY = e.getY();
        selectionRectangle.setX(startX);
        selectionRectangle.setY(startY);
        selectionRectangle.setWidth(0);
        selectionRectangle.setHeight(0);
        selectionRectangle.setVisible(true);
        e.consume();
    }

    /** 判断鼠标点击目标是否在卡片上（避免在卡片上启动框选） */
    private boolean isTargetOnCard(Object target) {
        if (target instanceof VBox) {
            return ((VBox) target).getUserData() instanceof File;
        }
        if (target instanceof ImageView || target instanceof Label) return true;
        if (target instanceof Node) {
            Node parent = ((Node) target).getParent();
            while (parent != null) {
                if (parent instanceof VBox && ((VBox) parent).getUserData() instanceof File)
                    return true;
                parent = parent.getParent();
            }
        }
        return false;
    }

    /** 处理鼠标拖拽事件：更新框选矩形区域 */
    private void handleMouseDragForSelection(MouseEvent e) {
        if (!isSelecting) return;
        double curX = e.getX(), curY = e.getY();
        selectionRectangle.setX(Math.min(startX, curX));
        selectionRectangle.setY(Math.min(startY, curY));
        selectionRectangle.setWidth(Math.abs(curX - startX));
        selectionRectangle.setHeight(Math.abs(curY - startY));
        e.consume();
    }

    /** 处理鼠标释放事件：结束框选，选中矩形区域内的卡片 */
    private void handleMouseReleaseForSelection(MouseEvent e) {
        if (!isSelecting) return;
        isSelecting = false;
        selectionRectangle.setVisible(false);
        selectInRect(selectionRectangle.getBoundsInParent(), e.isControlDown());
        e.consume();
    }

    /** 选中与矩形区域相交的卡片（支持追加选择） */
    private void selectInRect(Bounds rect, boolean add) {
        if (!add) clearSelection();
        int idx = 0;
        for (Node node : thumbnailPane.getChildren()) {
            if (node instanceof VBox) {
                VBox box = (VBox) node;
                Bounds boxInScene = box.localToScene(box.getBoundsInLocal());
                Bounds rectInScene = overlayContainer.localToScene(
                        new BoundingBox(rect.getMinX(), rect.getMinY(),
                                rect.getWidth(), rect.getHeight()));
                if (rectInScene.intersects(boxInScene)
                        && !appState.getSelectedNodes().contains(box)) {
                    appState.getSelectedNodes().add(box);
                    StyleUtils.applySelectedStyle(box);
                    if (lastNavigatedIndex < 0) lastNavigatedIndex = idx;
                }
            }
            idx++;
        }
        updateStatusAfterSelection();
    }

    /** 初始化右键上下文菜单 */
    private void setupContextMenu() {
        thumbnailPane.setOnContextMenuRequested(e -> {
            hideActiveContextMenu();
            ContextMenu blankMenu = new ContextMenu();
            MenuItem pasteItem = new MenuItem("Paste");
            pasteItem.setOnAction(ev -> fileOperationService.pasteFiles());
            blankMenu.getItems().add(pasteItem);
            blankMenu.setStyle(
                    "-fx-background-color: " + DesignConstants.bgCard() + ";"
                            + " -fx-border-color: " + DesignConstants.border() + ";"
                            + " -fx-border-radius: 8;"
                            + " -fx-background-radius: 8;"
            );
            activeContextMenu = blankMenu;
            blankMenu.show(thumbnailPane, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private void setupGlobalMenuHide() {
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (activeContextMenu != null && activeContextMenu.isShowing()) {
                        hideActiveContextMenu();
                    }
                });
            }
        });
    }

    private void hideActiveContextMenu() {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
            activeContextMenu = null;
        }
    }

    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-border-color: " + DesignConstants.border() + ";"
                        + " -fx-border-radius: 8;"
                        + " -fx-background-radius: 8;"
        );

        MenuItem openItem = new MenuItem("Open Slideshow");
        openItem.setStyle("-fx-text-fill: " + DesignConstants.textPrimary() + ";");
        openItem.setOnAction(e -> {
            if (!appState.getSelectedNodes().isEmpty()) {
                VBox selected = appState.getSelectedNodes().iterator().next();
                int idx = thumbnailPane.getChildren().indexOf(selected);
                onOpenSlideshow.accept(Math.max(0, idx));
            }
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setStyle("-fx-text-fill: " + DesignConstants.textPrimary() + ";");
        copyItem.setOnAction(e -> fileOperationService.copySelectedFiles());

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setStyle("-fx-text-fill: " + DesignConstants.textPrimary() + ";");
        pasteItem.setOnAction(e -> fileOperationService.pasteFiles());

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setStyle("-fx-text-fill: " + DesignConstants.textPrimary() + ";");
        renameItem.setOnAction(e -> fileOperationService.renameFiles());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setStyle("-fx-text-fill: " + DesignConstants.textPrimary() + ";");
        deleteItem.setOnAction(e -> fileOperationService.deleteFiles());

        SeparatorMenuItem sep1 = new SeparatorMenuItem();
        SeparatorMenuItem sep2 = new SeparatorMenuItem();

        MenuItem propsItem = new MenuItem("Properties");
        propsItem.setStyle("-fx-text-fill: " + DesignConstants.textPrimary() + ";");
        propsItem.setOnAction(e -> showProperties());

        menu.getItems().addAll(openItem, sep1, copyItem, pasteItem, sep2, renameItem, deleteItem, new SeparatorMenuItem(), propsItem);
        return menu;
    }

    private void startHoverPreview(VBox box, File file, MouseEvent e) {
        stopHoverPreview();
        hoverTimeline = new Timeline(
                new KeyFrame(Duration.seconds(2), ev -> showHoverPopup(box, file, e))
        );
        hoverTimeline.play();
    }

    private void stopHoverPreview() {
        if (hoverTimeline != null) {
            hoverTimeline.stop();
            hoverTimeline = null;
        }
        if (hoverPopup != null) {
            hoverPopup.hide();
            hoverPopup = null;
        }
    }

    private void showHoverPopup(VBox box, File file, MouseEvent e) {
        if (hoverPopup != null) hoverPopup.hide();

        StringBuilder info = new StringBuilder();
        info.append("File: ").append(file.getName()).append("\n");
        info.append("Size: ").append(FileUtils.formatFileSize(file.length())).append("\n");

        try {
            Image img = new Image(file.toURI().toString());
            if (!img.isError()) {
                info.append("Dimensions: ").append((int) img.getWidth())
                        .append(" x ").append((int) img.getHeight()).append(" px\n");
            }
        } catch (Exception ex) {
            info.append("Dimensions: N/A\n");
        }

        String ext = FileUtils.getExtension(file.getName());
        info.append("Type: ").append(ext.isEmpty() ? "Unknown" : ext.toUpperCase());

        Label infoLabel = new Label(info.toString());
        infoLabel.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-border-color: " + DesignConstants.border() + ";"
                        + " -fx-border-radius: 8;"
                        + " -fx-background-radius: 8;"
                        + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 5);"
                        + " -fx-padding: 12 16;"
                        + " -fx-font-size: 11px;"
                        + " -fx-font-family: 'Segoe UI', sans-serif;"
        );

        hoverPopup = new Popup();
        hoverPopup.getContent().add(infoLabel);
        hoverPopup.show(box, e.getScreenX() + 15, e.getScreenY() + 15);
    }

    private void showProperties() {
        if (appState.getSelectedNodes().isEmpty()) return;
        File file = (File) appState.getSelectedNodes().iterator().next().getUserData();

        StringBuilder info = new StringBuilder();
        info.append("File: ").append(file.getName()).append("\n\n");
        info.append("Size: ").append(FileUtils.formatFileSize(file.length())).append("\n\n");

        try {
            Image img = new Image(file.toURI().toString());
            if (!img.isError()) {
                info.append("Dimensions: ").append((int) img.getWidth())
                        .append(" x ").append((int) img.getHeight()).append(" px\n\n");
            }
        } catch (Exception ex) {
            info.append("Dimensions: Unable to read\n\n");
        }

        String ext = FileUtils.getExtension(file.getName());
        info.append("Type: ").append(ext.isEmpty() ? "Unknown" : ext.toUpperCase())
                .append(" Image\n\n");

        try {
            BasicFileAttributes attrs = java.nio.file.Files.readAttributes(
                    file.toPath(), BasicFileAttributes.class);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            info.append("Created: ").append(sdf.format(attrs.creationTime().toMillis())).append("\n\n");
            info.append("Modified: ").append(sdf.format(attrs.lastModifiedTime().toMillis())).append("\n\n");
        } catch (Exception ex) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            info.append("Modified: ").append(sdf.format(file.lastModified())).append("\n\n");
        }

        info.append("Readable: ").append(file.canRead() ? "Yes" : "No").append("\n");
        info.append("Writable: ").append(file.canWrite() ? "Yes" : "No");

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Properties");
        alert.setHeaderText(file.getName());
        alert.setContentText(info.toString());
        alert.getDialogPane().setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
        );
        alert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK).setStyle(
                "-fx-background-color: " + DesignConstants.PRIMARY() + ";"
                        + " -fx-text-fill: white;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 7 20;"
                        + " -fx-background-radius: 6;"
                        + " -fx-cursor: hand;"
        );
        alert.showAndWait();
    }
}
