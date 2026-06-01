package com.example.demo.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.layout.VBox;

/**
 * 应用共享状态模型
 * <p>
 * 集中管理应用程序中需要在多个组件间共享的可变状态。
 * 这种设计模式称为"共享状态对象"（Shared State Object），是 MVC 架构中
 * Model 层的简化实现，适用于中小型 JavaFX 应用。
 * <p>
 * 为什么需要共享状态？
 * <ul>
 *   <li>多个视图组件需要访问同一份数据（如当前目录、图片列表）</li>
 *   <li>避免在各组件之间传递大量参数，降低耦合度</li>
 *   <li>便于后续扩展为观察者模式（Observable），实现数据变更自动通知</li>
 * </ul>
 * <p>
 * 注意：本类不是线程安全的。由于 JavaFX UI 操作必须在 JavaFX Application Thread
 * 上执行，而本项目的所有状态修改均在 UI 线程中完成，因此无需额外同步。
 * 若后续引入后台线程修改状态，需要使用 {@code Platform.runLater()} 或加锁。
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class AppState {

    /**
     * 排序依据枚举
     * <p>
     * 定义图片列表可用的排序字段：
     * <ul>
     *   <li>{@link #NAME}：按文件名排序（默认）</li>
     *   <li>{@link #DATE}：按最后修改日期排序</li>
     *   <li>{@link #SIZE}：按文件大小排序</li>
     * </ul>
     */
    public enum SortField {
        /** 按文件名排序 */
        NAME,
        /** 按最后修改日期排序 */
        DATE,
        /** 按文件大小排序 */
        SIZE
    }

    /**
     * 排序顺序枚举
     * <p>
     * 定义排序的方向：
     * <ul>
     *   <li>{@link #ASC}：递增（升序，默认）</li>
     *   <li>{@link #DESC}：递减（降序）</li>
     * </ul>
     */
    public enum SortOrder {
        /** 递增（升序） */
        ASC,
        /** 递减（降序） */
        DESC
    }

    /** 当前浏览的目录 */
    private File currentDirectory;

    /** 当前目录中的图片文件列表（按当前排序规则排序） */
    private final List<File> currentImages = new ArrayList<>();

    /** 当前被选中的缩略图卡片集合，每个 VBox 的 userData 存储对应的 File 对象 */
    private final Set<VBox> selectedNodes = new HashSet<>();

    /** 剪贴板中的文件列表，用于复制/粘贴操作 */
    private final List<File> clipboardFiles = new ArrayList<>();

    /** 当前焦点图片在列表中的索引，用于键盘导航 */
    private int currentImageIndex = -1;

    /** 当前排序依据（默认：按名称） */
    private SortField sortField = SortField.NAME;

    /** 当前排序顺序（默认：递增） */
    private SortOrder sortOrder = SortOrder.ASC;

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public List<File> getCurrentImages() {
        return currentImages;
    }

    public void setCurrentImages(List<File> images) {
        this.currentImages.clear();
        this.currentImages.addAll(images);
    }

    public Set<VBox> getSelectedNodes() {
        return selectedNodes;
    }

    public List<File> getClipboardFiles() {
        return clipboardFiles;
    }

    public int getCurrentImageIndex() {
        return currentImageIndex;
    }

    public void setCurrentImageIndex(int currentImageIndex) {
        this.currentImageIndex = currentImageIndex;
    }

    public SortField getSortField() {
        return sortField;
    }

    public void setSortField(SortField sortField) {
        this.sortField = sortField;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
