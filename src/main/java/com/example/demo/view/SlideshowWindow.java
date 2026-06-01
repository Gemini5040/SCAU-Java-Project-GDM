package com.example.demo.view;

import com.example.demo.service.ImageCacheService;
import com.example.demo.util.DesignConstants;
import com.example.demo.util.FileUtils;
import com.example.demo.util.StyleUtils;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 幻灯片浏览窗口
 * <p>
 * 独立 Stage 窗口，提供沉浸式图片浏览体验。功能包括：
 * <ul>
 *   <li>图片导航：左右箭头键/按钮切换，到达首尾时显示提示</li>
 *   <li>自动播放：可调节间隔的自动轮播，播完最后一张自动停止</li>
 *   <li>缩放：鼠标滚轮（Ctrl+滚轮）或按钮缩放，支持 0.1x ~ 8x</li>
 *   <li>全屏：F11 或按钮切换全屏模式</li>
 *   <li>信息显示：底部显示文件名、分辨率、大小、序号</li>
 * </ul>
 * <p>
 * 布局结构：
 * <pre>
 * BorderPane
 * ├── Center: StackPane（图片 + 信息标签 + 导航按钮 + 边界提示）
 * └── Bottom: HBox（控制栏：上一张/下一张 | 放大/缩小/适应 | 间隔 | 播放 | 全屏）
 * </pre>
 * <p>
 * 关键设计：
 * <ul>
 *   <li>使用 {@code addEventFilter} 处理键盘事件，在捕获阶段优先于控件处理</li>
 *   <li>使用 {@link AtomicInteger} 和 {@link AtomicReference} 管理可变状态</li>
 *   <li>图片通过 {@link ImageCacheService} 缓存，避免重复加载</li>
 * </ul>
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class SlideshowWindow {

    /** 当前图片文件列表 */
    private final List<File> currentImages;

    /** 目录名称，用于窗口标题 */
    private final String directoryName;

    /** 父窗口，用于设置模态关系 */
    private final Stage ownerStage;

    /** 图片缓存服务（可为 null，降级为直接加载） */
    private final ImageCacheService imageCacheService;

    /** 幻灯片窗口 Stage */
    private Stage slideshowStage;

    /** 图片显示控件 */
    private ImageView imageView;

    /** 图片信息标签（文件名、分辨率、大小） */
    private Label imageInfoLabel;

    /** 图片序号标签（如 "3 / 15"） */
    private Label indexLabel;

    /** 边界提示标签（到达首/尾时显示） */
    private Label boundaryLabel;

    /** 当前图片索引（线程安全） */
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /** 当前缩放比例（线程安全） */
    private final AtomicReference<Double> currentScale = new AtomicReference<>(1.0);

    /** 自动播放定时器 */
    private Timeline autoPlayTimeline;

    /** 是否正在自动播放 */
    private volatile boolean isAutoPlaying = false;

    /** 上一张按钮 */
    private Button prevBtn;

    /** 下一张按钮 */
    private Button nextBtn;

    /** 自动播放间隔 Spinner（秒） */
    private Spinner<Integer> intervalSpinner;

    /** 播放/暂停按钮 */
    private Button playPauseBtn;

    /**
     * 构造幻灯片窗口
     *
     * @param currentImages     图片文件列表
     * @param directoryName     目录名称（用于窗口标题）
     * @param ownerStage        父窗口
     * @param imageCacheService 图片缓存服务（可为 null）
     */
    public SlideshowWindow(List<File> currentImages, String directoryName,
                            Stage ownerStage, ImageCacheService imageCacheService) {
        this.currentImages = currentImages;
        this.directoryName = directoryName;
        this.ownerStage = ownerStage;
        this.imageCacheService = imageCacheService;
    }

    public SlideshowWindow(List<File> currentImages, String directoryName,
                            Stage ownerStage) {
        this(currentImages, directoryName, ownerStage, null);
    }

    /**
     * 打开幻灯片窗口
     *
     * @param startIndex 起始图片索引
     */
    public void open(int startIndex) {
        if (currentImages.isEmpty()) return;

        Stage stage = new Stage();
        stage.setTitle("Slideshow - " + directoryName);
        stage.initOwner(ownerStage);

        AtomicInteger currentIndex = new AtomicInteger(
                Math.max(0, Math.min(startIndex, currentImages.size() - 1)));
        AtomicReference<Double> zoom = new AtomicReference<>(1.0);
        AtomicReference<Double> interval = new AtomicReference<>(
                DesignConstants.DEFAULT_SLIDESHOW_INTERVAL);

        ImageView imageView = createImageView();
        Label infoLabel = createSlideInfoLabel();
        Label firstImageNotice = createFirstImageNotice();
        Label lastImageNotice = createLastImageNotice();

        Runnable updateImage = createImageUpdater(
                currentIndex, zoom, imageView, infoLabel);
        updateImage.run();

        AtomicReference<Timeline> timelineRef = new AtomicReference<>(
                createAutoPlayTimeline(currentIndex, updateImage, interval,
                        null, lastImageNotice));

        HBox controlBar = createSlideControls(
                imageView, currentIndex, zoom, interval,
                timelineRef, updateImage, infoLabel,
                stage, firstImageNotice, lastImageNotice);

        Button leftNav = createNavButton("\u25C0", true);
        Button rightNav = createNavButton("\u25B6", false);
        leftNav.setOnAction(e -> navPrev(currentIndex, updateImage, firstImageNotice));
        rightNav.setOnAction(e -> navNext(currentIndex, updateImage, lastImageNotice));

        StackPane leftZone = new StackPane(leftNav);
        leftZone.setAlignment(Pos.CENTER_LEFT);
        leftZone.setPickOnBounds(false);
        StackPane.setMargin(leftNav, new Insets(0, 0, 0, 20));

        StackPane rightZone = new StackPane(rightNav);
        rightZone.setAlignment(Pos.CENTER_RIGHT);
        rightZone.setPickOnBounds(false);
        StackPane.setMargin(rightNav, new Insets(0, 20, 0, 0));

        StackPane center = new StackPane(
                imageView, infoLabel, firstImageNotice, lastImageNotice,
                leftZone, rightZone);
        center.setStyle(
                "-fx-background-color:linear-gradient("
                        + "135deg,#0F172A 0%,#1E293B 50%,#334155 100%);");
        StackPane.setAlignment(infoLabel, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(firstImageNotice, Pos.TOP_CENTER);
        StackPane.setAlignment(lastImageNotice, Pos.TOP_CENTER);

        BorderPane slidePane = new BorderPane();
        slidePane.setCenter(center);
        slidePane.setBottom(controlBar);

        Scene scene = new Scene(slidePane, 1050, 780);
        stage.setScene(scene);

        setupSlideKeys(scene, currentIndex, updateImage,
                timelineRef, stage, firstImageNotice, lastImageNotice);
        setupSlideScroll(imageView, zoom, infoLabel);

        stage.setOnCloseRequest(e -> timelineRef.get().stop());
        stage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), slidePane);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /** 创建图片显示控件，设置默认尺寸和缓存策略 */
    private ImageView createImageView() {
        ImageView iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        iv.setFitWidth(DesignConstants.SLIDEVIEW_WIDTH);
        iv.setFitHeight(DesignConstants.SLIDEVIEW_HEIGHT);
        iv.setStyle("-fx-background-color:#0F172A;");
        return iv;
    }

    /** 创建底部信息标签（文件名、分辨率、大小） */
    private Label createSlideInfoLabel() {
        Label label = new Label();
        label.setStyle(
                "-fx-text-fill:white;"
                        + " -fx-font-size:13px;"
                        + " -fx-font-family:'Consolas','Monaco',monospace;"
                        + " -fx-padding:8 14;"
                        + " -fx-background-color:rgba(0,0,0,0.75);"
                        + " -fx-background-radius:6;"
        );
        return label;
    }

    /** 创建"已是第一张"提示标签 */
    private Label createFirstImageNotice() {
        Label label = new Label("This is the first image");
        label.setStyle(
                "-fx-text-fill:white;"
                        + " -fx-font-size:18px;"
                        + " -fx-font-weight:bold;"
                        + " -fx-padding:12 28;"
                        + " -fx-background-color:rgba(59,130,246,0.85);"
                        + " -fx-background-radius:8;"
        );
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    /** 创建"已是最后一张"提示标签 */
    private Label createLastImageNotice() {
        Label label = new Label("This is the last image");
        label.setStyle(
                "-fx-text-fill:white;"
                        + " -fx-font-size:18px;"
                        + " -fx-font-weight:bold;"
                        + " -fx-padding:12 28;"
                        + " -fx-background-color:rgba(239,68,68,0.85);"
                        + " -fx-background-radius:8;"
        );
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    /** 创建导航按钮（左/右箭头） */
    private Button createNavButton(String symbol, boolean isLeft) {
        Button btn = new Button(symbol);
        String baseStyle =
                "-fx-background-color: rgba(255,255,255,0.15);"
                        + " -fx-text-fill: rgba(255,255,255,0.8);"
                        + " -fx-font-size: 28px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 12 16;"
                        + " -fx-background-radius: 40;"
                        + " -fx-cursor: hand;"
                        + " -fx-border-color: rgba(255,255,255,0.2);"
                        + " -fx-border-radius: 40;"
                        + " -fx-border-width: 1;";
        String hoverStyle =
                "-fx-background-color: rgba(255,255,255,0.35);"
                        + " -fx-text-fill: white;"
                        + " -fx-font-size: 28px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 12 16;"
                        + " -fx-background-radius: 40;"
                        + " -fx-cursor: hand;"
                        + " -fx-border-color: rgba(255,255,255,0.5);"
                        + " -fx-border-radius: 40;"
                        + " -fx-border-width: 1;"
                        + " -fx-scale-x: 1.1;"
                        + " -fx-scale-y: 1.1;";
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    /** 显示边界提示标签（带淡入淡出动画） */
    private void showBoundaryNotice(Label notice) {
        if (notice == null) return;
        notice.setVisible(true);
        notice.setManaged(true);
        Timeline hideTimer = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    notice.setVisible(false);
                    notice.setManaged(false);
                })
        );
        hideTimer.play();
    }

    /**
     * 创建图片更新逻辑
     * <p>
     * 返回一个 Runnable，执行时会加载当前索引的图片并更新 ImageView 和信息标签。
     *
     * @param currentIndex 当前索引
     * @param zoom         缩放比例
     * @param imageView    图片显示控件
     * @param infoLabel    信息标签
     * @return 图片更新 Runnable
     */
    private Runnable createImageUpdater(AtomicInteger currentIndex,
                                         AtomicReference<Double> zoom,
                                         ImageView imageView, Label infoLabel) {
        return () -> {
            if (currentImages.isEmpty()) return;
            File file = currentImages.get(currentIndex.get());

            Image img;
            if (imageCacheService != null) {
                img = imageCacheService.getFullImage(file);
            } else {
                img = FileUtils.createHighQualityImage(file, 1920, 1440);
            }
            imageView.setImage(img);
            zoom.set(1.0);
            imageView.setScaleX(1);
            imageView.setScaleY(1);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), imageView);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            infoLabel.setText(
                    String.format("%s (%dx%d)|%s|%d/%d",
                            file.getName(),
                            (int) img.getWidth(), (int) img.getHeight(),
                            FileUtils.formatFileSize(file.length()),
                            currentIndex.get() + 1,
                            currentImages.size()));
        };
    }

    /** 创建自动播放定时器，到达最后一张时自动停止 */
    private Timeline createAutoPlayTimeline(AtomicInteger currentIndex,
                                             Runnable updateImage,
                                             AtomicReference<Double> interval,
                                             Button playButton,
                                             Label lastNotice) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(interval.get()), e -> {
                    if (currentIndex.get() >= currentImages.size() - 1) {
                        ((Timeline) e.getSource()).stop();
                        showBoundaryNotice(lastNotice);
                        if (playButton != null) {
                            resetPlayButton(playButton);
                        }
                        return;
                    }
                    currentIndex.incrementAndGet();
                    updateImage.run();
                })
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        return tl;
    }

    /** 重置播放按钮为"播放"状态 */
    private void resetPlayButton(Button button) {
        button.setText("\u25B6 Play");
        button.setStyle(
                "-fx-background-color:#FBBF24;"
                        + " -fx-text-fill:#0F172A;"
                        + " -fx-font-weight:bold;"
                        + " -fx-font-size:11px;"
                        + " -fx-padding:7 16;"
                        + " -fx-cursor:hand;"
                        + " -fx-background-radius:6;"
        );
    }

    /**
     * 创建底部控制栏
     * <p>
     * 布局：[◀上一张][下一张▶] | [放大][缩小][适应] | [间隔Spinner] | [▶播放] | [全屏]
     */
    private HBox createSlideControls(ImageView iv, AtomicInteger ci,
                                      AtomicReference<Double> zl,
                                      AtomicReference<Double> ir,
                                      AtomicReference<Timeline> tlRef,
                                      Runnable u, Label info,
                                      Stage stage,
                                      Label firstNotice, Label lastNotice) {
        Button prev = StyleUtils.createSlideBtn("\u25C0", "Prev", "#60A5FA");
        Button next = StyleUtils.createSlideBtn("\u25B6", "Next", "#60A5FA");
        Button zin = StyleUtils.createSlideBtn("+", "Zoom", "#34D399");
        Button zout = StyleUtils.createSlideBtn("-", "Zoom", "#34D399");
        Button fit = StyleUtils.createSlideBtn("\u2630", "Fit", "#A78BFA");
        Button play = StyleUtils.createSlideBtn("\u25B6", "Play", "#FBBF24");
        Button fullscreen = StyleUtils.createSlideBtn("\u26F6", "Full", "#F472B6");

        Spinner<Double> spinner = new Spinner<>(0.5, 10.0, ir.get(), 0.5);
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        spinner.valueProperty().addListener((o, a, nv) -> {
            ir.set(nv);
            if (tlRef.get().getStatus() == Animation.Status.RUNNING) {
                tlRef.get().stop();
                Timeline newTl = createAutoPlayTimeline(ci, u, ir, play, lastNotice);
                tlRef.set(newTl);
                newTl.play();
            }
        });

        Label intervalLabel = new Label("Interval:");
        intervalLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;");
        Label secLabel = new Label("s");
        secLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px;");
        HBox intervalBox = new HBox(5, intervalLabel, spinner, secLabel);
        intervalBox.setAlignment(Pos.CENTER);
        intervalBox.setStyle(
                "-fx-padding:6 12;"
                        + " -fx-background-color:rgba(255,255,255,0.08);"
                        + " -fx-background-radius:6;"
        );

        prev.setOnAction(e -> navPrev(ci, u, firstNotice));
        next.setOnAction(e -> navNext(ci, u, lastNotice));
        zin.setOnAction(e -> zoomIn(zl, iv));
        zout.setOnAction(e -> zoomOut(zl, iv));
        fit.setOnAction(e -> {
            zl.set(1.0);
            iv.setScaleX(1);
            iv.setScaleY(1);
            iv.setFitWidth(DesignConstants.SLIDEVIEW_WIDTH);
            iv.setFitHeight(DesignConstants.SLIDEVIEW_HEIGHT);
        });
        play.setOnAction(e -> togglePlay(tlRef, ci, u, ir, play, lastNotice));
        fullscreen.setOnAction(e -> toggleFullscreen(stage, fullscreen));

        HBox bar = new HBox(10,
                prev, next,
                new Separator(Orientation.VERTICAL),
                zin, zout, fit,
                new Separator(Orientation.VERTICAL),
                intervalBox,
                new Separator(Orientation.VERTICAL),
                play,
                new Separator(Orientation.VERTICAL),
                fullscreen
        );
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setStyle("-fx-background-color:#1E293B;");
        return bar;
    }

    /** 导航到上一张图片，到达第一张时显示提示 */
    private void navPrev(AtomicInteger index, Runnable updateImage, Label firstNotice) {
        if (index.get() <= 0) {
            showBoundaryNotice(firstNotice);
            return;
        }
        index.decrementAndGet();
        updateImage.run();
    }

    /** 导航到下一张图片，到达最后一张时显示提示并停止自动播放 */
    private void navNext(AtomicInteger index, Runnable updateImage, Label lastNotice) {
        if (index.get() >= currentImages.size() - 1) {
            showBoundaryNotice(lastNotice);
            return;
        }
        index.incrementAndGet();
        updateImage.run();
    }

    /** 放大图片 */
    private void zoomIn(AtomicReference<Double> zoom, ImageView iv) {
        double newZoom = Math.min(zoom.get() + DesignConstants.ZOOM_STEP, DesignConstants.MAX_ZOOM);
        zoom.set(newZoom);
        iv.setScaleX(newZoom);
        iv.setScaleY(newZoom);
    }

    /** 缩小图片 */
    private void zoomOut(AtomicReference<Double> zoom, ImageView iv) {
        double newZoom = Math.max(zoom.get() - DesignConstants.ZOOM_STEP, DesignConstants.MIN_ZOOM);
        zoom.set(newZoom);
        iv.setScaleX(newZoom);
        iv.setScaleY(newZoom);
    }

    /** 重置缩放到 1.0 */
    private void zoomFit(AtomicReference<Double> zoom, ImageView iv) {
        zoom.set(1.0);
        iv.setScaleX(1.0);
        iv.setScaleY(1.0);
    }

    /** 切换自动播放状态（播放/暂停） */
    private void togglePlay(AtomicReference<Timeline> tlRef,
                             AtomicInteger currentIndex,
                             Runnable updateImage,
                             AtomicReference<Double> interval,
                             Button button, Label lastNotice) {
        Timeline timeline = tlRef.get();
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
            resetPlayButton(button);
        } else {
            Timeline newTl = createAutoPlayTimeline(
                    currentIndex, updateImage, interval, button, lastNotice);
            tlRef.set(newTl);
            newTl.play();
            button.setText("\u23F8 Stop");
            button.setStyle(
                    "-fx-background-color:" + DesignConstants.ACCENT() + ";"
                            + " -fx-text-fill:white;"
                            + " -fx-font-weight:bold;"
                            + " -fx-font-size:11px;"
                            + " -fx-padding:7 16;"
                            + " -fx-cursor:hand;"
                            + " -fx-background-radius:6;"
            );
        }
    }

    private void toggleFullscreen(Stage stage, Button button) {
        boolean isFull = stage.isFullScreen();
        stage.setFullScreen(!isFull);
        if (!isFull) {
            button.setText("\u26F6 Exit");
        } else {
            button.setText("\u26F6 Full");
        }
    }

    /**
     * 注册幻灯片窗口的键盘事件
     * <p>
     * 使用 {@code addEventFilter} 在捕获阶段处理，确保事件不被其他控件消费。
     * 快捷键：←→ 导航、Space 播放/暂停、F11 全屏、Escape 关闭
     */
    private void setupSlideKeys(Scene scene, AtomicInteger currentIndex,
                                 Runnable updateImage,
                                 AtomicReference<Timeline> tlRef,
                                 Stage stage,
                                 Label firstNotice, Label lastNotice) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case LEFT, UP:
                    if (currentIndex.get() > 0) {
                        currentIndex.decrementAndGet();
                        updateImage.run();
                    } else {
                        showBoundaryNotice(firstNotice);
                    }
                    e.consume();
                    break;
                case RIGHT, DOWN, SPACE:
                    if (currentIndex.get() < currentImages.size() - 1) {
                        currentIndex.incrementAndGet();
                        updateImage.run();
                    } else {
                        showBoundaryNotice(lastNotice);
                    }
                    e.consume();
                    break;
                case F11:
                    stage.setFullScreen(!stage.isFullScreen());
                    e.consume();
                    break;
                case ESCAPE:
                    if (stage.isFullScreen()) {
                        stage.setFullScreen(false);
                    } else {
                        tlRef.get().stop();
                        stage.close();
                    }
                    e.consume();
                    break;
                default:
                    break;
            }
        });
    }

    /** 注册鼠标滚轮缩放事件（Ctrl+滚轮缩放，普通滚轮翻页） */
    private void setupSlideScroll(ImageView iv, AtomicReference<Double> zoom, Label info) {
        iv.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnScroll(e -> {
                    if (e.isControlDown()) {
                        double delta = e.getDeltaY();
                        double newZoom;
                        if (delta > 0) {
                            newZoom = Math.min(zoom.get() + DesignConstants.ZOOM_STEP, DesignConstants.MAX_ZOOM);
                        } else {
                            newZoom = Math.max(zoom.get() - DesignConstants.ZOOM_STEP, DesignConstants.MIN_ZOOM);
                        }
                        zoom.set(newZoom);
                        iv.setScaleX(newZoom);
                        iv.setScaleY(newZoom);
                        info.setText(info.getText().replaceAll("Zoom:[\\d.]+x",
                                "Zoom:" + String.format("%.0f%%", newZoom * 100)));
                        e.consume();
                    }
                });
            }
        });
    }
}
