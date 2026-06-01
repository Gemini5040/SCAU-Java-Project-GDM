package com.example.demo.model;

import com.example.demo.util.StyleUtils;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * 状态栏管理器
 * <p>
 * 封装底部状态栏中多个标签的更新逻辑，提供语义化的方法接口，
 * 避免各组件直接操作 UI 标签，降低视图组件之间的耦合。
 * <p>
 * 状态栏包含以下信息区域：
 * <ul>
 *   <li>状态信息标签：显示操作结果、图片数量等</li>
 *   <li>目录信息标签：显示当前目录名称</li>
 *   <li>图片计数徽章：显示图片总数</li>
 *   <li>选中计数徽章：显示当前选中的图片数量</li>
 * </ul>
 * <p>
 * 这种"管理器"模式是 MVC 中 Controller 的轻量级实现，
 * 将状态更新逻辑从业务代码中抽离，使代码更清晰。
 *
 * @author Photo Manager Team
 * @version 1.0
 */
public class StatusManager {

    /** 状态信息标签，显示在状态栏左侧 */
    private final Label statusLabel;

    /** 目录名称标签，显示在工具栏中 */
    private final Label directoryInfoLabel;

    /** 图片计数徽章，显示在工具栏右侧 */
    private final Label imageCountBadge;

    /** 选中计数徽章，显示在状态栏右侧 */
    private final Label selectedCountLabel;

    /**
     * 构造状态管理器
     * <p>
     * 传入的标签对象应已由调用方完成样式配置（通过 {@link StyleUtils}）。
     *
     * @param statusLabel        状态信息标签
     * @param directoryInfoLabel 目录名称标签
     * @param imageCountBadge    图片计数徽章
     * @param selectedCountLabel 选中计数徽章
     */
    public StatusManager(Label statusLabel, Label directoryInfoLabel,
                         Label imageCountBadge, Label selectedCountLabel) {
        this.statusLabel = statusLabel;
        this.directoryInfoLabel = directoryInfoLabel;
        this.imageCountBadge = imageCountBadge;
        this.selectedCountLabel = selectedCountLabel;
    }

    /**
     * 更新状态信息文本
     *
     * @param text 状态信息内容
     */
    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /**
     * 更新目录名称显示
     *
     * @param text 目录名称
     */
    public void setDirectoryInfo(String text) {
        directoryInfoLabel.setText(text);
    }

    /**
     * 更新图片计数徽章
     *
     * @param text 计数文本（如 "12 photos"）
     */
    public void setImageCount(String text) {
        imageCountBadge.setText(text);
        imageCountBadge.setVisible(true);
    }

    /**
     * 更新选中计数徽章
     *
     * @param text   计数文本（如 "3 Selected"）
     * @param visible 是否显示徽章
     */
    public void setSelectedCount(String text, boolean visible) {
        selectedCountLabel.setText(text);
        selectedCountLabel.setVisible(visible);
    }

    /**
     * 获取状态信息标签（用于构建状态栏布局）
     *
     * @return 状态信息标签
     */
    public Label getStatusLabel() {
        return statusLabel;
    }

    /**
     * 获取目录名称标签（用于构建工具栏布局）
     *
     * @return 目录名称标签
     */
    public Label getDirectoryInfoLabel() {
        return directoryInfoLabel;
    }

    /**
     * 获取图片计数徽章（用于构建工具栏布局）
     *
     * @return 图片计数徽章
     */
    public Label getImageCountBadge() {
        return imageCountBadge;
    }

    /**
     * 获取选中计数徽章（用于构建状态栏布局）
     *
     * @return 选中计数徽章
     */
    public Label getSelectedCountLabel() {
        return selectedCountLabel;
    }
}
