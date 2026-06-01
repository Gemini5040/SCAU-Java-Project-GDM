package com.example.demo.util;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 样式工具类
 * <p>
 * 提供统一的 UI 组件样式应用方法，确保整个应用程序的视觉风格一致。
 * 所有样式均通过 JavaFX inline style（内联样式）实现，颜色值通过
 * {@link DesignConstants} 动态获取，支持主题切换时自动更新。
 * <p>
 * 职责划分：
 * <ul>
 *   <li>卡片样式：{@link #applyNormalStyle}、{@link #applySelectedStyle}、{@link #applyHoverStyle}</li>
 *   <li>标签样式：{@link #applyToolbarTitleStyle}、{@link #applyCountBadgeStyle} 等</li>
 *   <li>按钮工厂：{@link #createToolbarButton}、{@link #createModernButton}、{@link #createSlideBtn}</li>
 *   <li>容器样式：{@link #applySidebarScrollStyle}、{@link #applyTreeViewStyle}、{@link #applyComboBoxStyle}</li>
 *   <li>颜色工具：{@link #darkenColor}、{@link #hexToRgba}</li>
 * </ul>
 * <p>
 * 设计原则：
 * <ul>
 *   <li>所有方法为 static，无需实例化</li>
 *   <li>样式字符串集中管理，避免散落在各组件中</li>
 *   <li>按钮设置 {@code -fx-ellipsis-string: ''} 和 {@code minWidth = USE_PREF_SIZE}，
 *       防止文字被截断为省略号</li>
 * </ul>
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public final class StyleUtils {

    /**
     * 私有构造器，防止外部实例化
     */
    private StyleUtils() {
        throw new AssertionError("Utility class");
    }

    /**
     * 应用卡片正常样式
     * <p>
     * 卡片处于默认状态时的样式：白色/深灰背景、透明边框、轻微阴影。
     *
     * @param node 要应用样式的 VBox 卡片节点
     */
    public static void applyNormalStyle(VBox node) {
        node.getStyleClass().removeAll("card-selected", "card-hover");
        node.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-background-radius: 10;"
                        + " -fx-border-color: transparent;"
                        + " -fx-border-width: 2.5;"
                        + " -fx-border-radius: 10;"
                        + " -fx-padding: 8;"
                        + " -fx-effect: dropshadow(three-pass-box, "
                        + DesignConstants.shadowColor() + ", 10, 0, 0, 3);"
        );
    }

    /**
     * 应用卡片选中样式
     * <p>
     * 卡片被选中时的样式：主色调边框、加强阴影，视觉上突出选中状态。
     *
     * @param node 要应用样式的 VBox 卡片节点
     */
    public static void applySelectedStyle(VBox node) {
        node.getStyleClass().removeAll("card-hover");
        node.getStyleClass().add("card-selected");
        node.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-background-radius: 10;"
                        + " -fx-border-color: " + DesignConstants.PRIMARY() + ";"
                        + " -fx-border-width: 2.5;"
                        + " -fx-border-radius: 10;"
                        + " -fx-padding: 8;"
                        + " -fx-effect: dropshadow(three-pass-box, "
                        + DesignConstants.shadowSelectedColor() + ", 14, 0, 0, 5);"
        );
    }

    /**
     * 应用卡片悬停样式
     * <p>
     * 鼠标悬停在卡片上时的样式：主色调浅色边框、中等阴影。
     *
     * @param node 要应用样式的 VBox 卡片节点
     */
    public static void applyHoverStyle(VBox node) {
        node.getStyleClass().removeAll("card-selected");
        node.getStyleClass().add("card-hover");
        node.setStyle(
                "-fx-background-color: " + DesignConstants.bgCard() + ";"
                        + " -fx-background-radius: 10;"
                        + " -fx-border-color: " + DesignConstants.PRIMARY_LIGHT() + ";"
                        + " -fx-border-width: 1.5;"
                        + " -fx-border-radius: 10;"
                        + " -fx-padding: 8;"
                        + " -fx-effect: dropshadow(three-pass-box, "
                        + DesignConstants.shadowHoverColor() + ", 14, 0, 0, 5);"
        );
    }

    /** 应用工具栏标题标签样式（14px 加粗） */
    public static void applyToolbarTitleStyle(Label label) {
        label.setStyle(
                "-fx-font-size: 14px;"
                        + " -fx-font-weight: 600;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-font-family: 'Segoe UI', sans-serif;"
        );
    }

    /** 应用图片计数徽章样式（主色调背景、白色文字、圆角胶囊形） */
    public static void applyCountBadgeStyle(Label label) {
        label.setStyle(
                "-fx-background-color: " + DesignConstants.PRIMARY() + ";"
                        + " -fx-text-fill: white;"
                        + " -fx-font-size: 11px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 4 10;"
                        + " -fx-background-radius: 10;"
                        + " -fx-cursor: default;"
        );
        label.setVisible(false);
    }

    /** 应用选中计数徽章样式（与图片计数徽章风格一致） */
    public static void applyStatusBadgeStyle(Label label) {
        label.setStyle(
                "-fx-background-color: " + DesignConstants.PRIMARY() + ";"
                        + " -fx-text-fill: white;"
                        + " -fx-font-size: 11px;"
                        + " -fx-font-weight: bold;"
                        + " -fx-padding: 4 12;"
                        + " -fx-background-radius: 10;"
                        + " -fx-cursor: default;"
        );
        label.setVisible(false);
    }

    /** 应用状态栏信息标签样式（12px 常规字重） */
    public static void applyStatusLabelStyle(Label label) {
        label.setStyle(
                "-fx-font-size: 12px;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-font-family: 'Segoe UI', sans-serif;"
        );
    }

    /** 应用侧边栏滚动面板样式（透明背景、无内边距） */
    public static void applySidebarScrollStyle(ScrollPane scrollPane) {
        scrollPane.setStyle(
                "-fx-background: " + DesignConstants.bgSidebar() + ";"
                        + " -fx-background-color: " + DesignConstants.bgSidebar() + ";"
                        + " -fx-padding: 0;"
        );
    }

    /** 应用目录树样式（透明背景、主色调选中条） */
    public static void applyTreeViewStyle(TreeView<?> tree) {
        tree.setStyle(
                "-fx-background-color: transparent;"
                        + " -fx-selection-bar: " + DesignConstants.PRIMARY() + ";"
                        + " -fx-selection-bar-non-focused: " + DesignConstants.PRIMARY_LIGHT() + ";"
        );
    }

    /**
     * 应用下拉框样式
     * <p>
     * 透明背景、无边框，与工具栏按钮风格统一。
     * 下拉弹出列表的样式由 CSS 文件控制。
     *
     * @param combo 要应用样式的 ComboBox
     */
    public static void applyComboBoxStyle(ComboBox<?> combo) {
        combo.setStyle(
                "-fx-background-color: transparent;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-font-size: 11px;"
                        + " -fx-font-family: 'Segoe UI', sans-serif;"
                        + " -fx-padding: 4 8;"
                        + " -fx-background-radius: 6;"
                        + " -fx-border-color: transparent;"
                        + " -fx-border-radius: 6;"
                        + " -fx-border-width: 0;"
                        + " -fx-cursor: hand;"
        );
    }

    /**
     * 创建工具栏按钮
     * <p>
     * 工具栏按钮有两种显示状态：
     * <ul>
     *   <li>完整模式：显示 "图标 文字"（如 "🔄 Refresh"）</li>
     *   <li>紧凑模式：仅显示图标（如 "🔄"）</li>
     * </ul>
     * 图标和文字通过 {@code properties} Map 存储，供外部切换显示模式。
     * <p>
     * 关键设置：
     * <ul>
     *   <li>{@code minWidth = USE_PREF_SIZE}：防止按钮被 HBox 压缩</li>
     *   <li>{@code -fx-ellipsis-string: ''}：禁止文字截断时显示省略号</li>
     * </ul>
     *
     * @param text 按钮文字标签
     * @param icon 按钮 Unicode 图标字符
     * @return 配置好的 Button 对象
     */
    public static Button createToolbarButton(String text, String icon) {
        Button btn = new Button(icon + " " + text);
        btn.getProperties().put("icon", icon);
        btn.getProperties().put("text", text);
        btn.setMinWidth(Region.USE_PREF_SIZE);
        btn.setStyle(buildToolbarBtnNormalStyle());

        btn.setOnMouseEntered(e -> btn.setStyle(buildToolbarBtnHoverStyle()));
        btn.setOnMouseExited(e -> btn.setStyle(buildToolbarBtnNormalStyle()));
        return btn;
    }

    /**
     * 创建现代风格按钮（带背景色和阴影）
     * <p>
     * 用于需要视觉突出的操作按钮（如 Slideshow）。
     * 支持主色按钮（白色文字）和次色按钮（主题文字色）。
     *
     * @param text      按钮文本（格式为 "图标 文字"，如 "▶ Slideshow"）
     * @param color     按钮背景色（十六进制，如 "#4CAF50"）
     * @param isPrimary 是否为主色按钮（true=白色文字，false=主题文字色）
     * @return 配置好的 Button 对象
     */
    public static Button createModernButton(String text, String color, boolean isPrimary) {
        String icon = text.contains(" ") ? text.substring(0, text.indexOf(' ')) : "";
        String label = text.contains(" ") ? text.substring(text.indexOf(' ') + 1) : text;
        Button btn = new Button(text);
        btn.getProperties().put("icon", icon);
        btn.getProperties().put("text", label);
        btn.setMinWidth(Region.USE_PREF_SIZE);
        String textColor = isPrimary ? "white" : DesignConstants.textPrimary();
        btn.setStyle(buildModernButtonStyle(color, textColor));

        btn.setOnMouseEntered(e ->
                btn.setStyle(buildModernButtonStyle(darkenColor(color), textColor))
        );
        btn.setOnMouseExited(e ->
                btn.setStyle(buildModernButtonStyle(color, textColor))
        );
        return btn;
    }

    /**
     * 构建现代风格按钮的内联样式字符串
     *
     * @param color     背景颜色
     * @param textColor 文字颜色
     * @return JavaFX 内联样式字符串
     */
    public static String buildModernButtonStyle(String color, String textColor) {
        return "-fx-background-color: " + color + ";"
                + " -fx-text-fill: " + textColor + ";"
                + " -fx-font-weight: 600;"
                + " -fx-font-size: 11px;"
                + " -fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-padding: 7 16;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 8;"
                + " -fx-ellipsis-string: '';"
                + " -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 4, 0, 0, 1);";
    }

    /**
     * 创建幻灯片控制按钮
     *
     * @param text 按钮图标/文字
     * @param tip  按钮提示文字
     * @param col  按钮背景色
     * @return 配置好的 Button 对象
     */
    public static Button createSlideBtn(String text, String tip, String col) {
        Button btn = new Button(text + " " + tip);
        btn.setStyle(buildSlideBtnStyle(col));

        btn.setOnMouseEntered(e ->
                btn.setStyle(buildSlideBtnStyle(darkenColor(col)))
        );
        btn.setOnMouseExited(e ->
                btn.setStyle(buildSlideBtnStyle(col))
        );
        return btn;
    }

    /** 构建幻灯片按钮样式字符串 */
    private static String buildSlideBtnStyle(String col) {
        return "-fx-background-color: " + col + ";"
                + " -fx-text-fill: #0F172A;"
                + " -fx-font-weight: bold;"
                + " -fx-font-size: 11px;"
                + " -fx-padding: 7 16;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 6;";
    }

    /**
     * 构建工具栏按钮正常状态样式字符串
     * <p>
     * 透明背景、主题文字色、圆角。
     *
     * @return JavaFX 内联样式字符串
     */
    public static String buildToolbarBtnNormalStyle() {
        return "-fx-background-color: transparent;"
                + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                + " -fx-font-size: 11px;"
                + " -fx-font-weight: 500;"
                + " -fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-padding: 6 12;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 6;"
                + " -fx-ellipsis-string: '';";
    }

    /** 构建工具栏按钮悬停状态样式字符串（输入框背景色） */
    private static String buildToolbarBtnHoverStyle() {
        return "-fx-background-color: " + DesignConstants.bgInput() + ";"
                + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                + " -fx-font-size: 11px;"
                + " -fx-font-weight: 500;"
                + " -fx-font-family: 'Segoe UI', sans-serif;"
                + " -fx-padding: 6 12;"
                + " -fx-cursor: hand;"
                + " -fx-background-radius: 6;"
                + " -fx-ellipsis-string: '';";
    }

    /**
     * 将十六进制颜色值变暗
     * <p>
     * 用于按钮悬停时产生视觉反馈。对已知语义颜色（PRIMARY、ACCENT 等）
     * 返回预定义的深色变体，对其他颜色返回默认深灰色。
     *
     * @param hex 原始十六进制颜色值
     * @return 变暗后的颜色值
     */
    public static String darkenColor(String hex) {
        if (hex.equals(DesignConstants.PRIMARY())) return DesignConstants.PRIMARY_DARK();
        if (hex.equals(DesignConstants.ACCENT())) return "#C62828";
        if (hex.equals(DesignConstants.SUCCESS())) return "#388E3C";
        if (hex.equals("#4CAF50")) return "#388E3C";
        if (hex.equals(DesignConstants.WARNING())) return "#E65100";
        if (hex.equals(DesignConstants.bgMain())) return DesignConstants.border();
        return "#333333";
    }

    /**
     * 将十六进制颜色值转换为 RGBA 格式
     * <p>
     * 用于需要透明度的场景，如阴影、半透明背景等。
     *
     * @param hex   十六进制颜色值（如 "#1976D2"）
     * @param alpha 透明度（0.0 完全透明 ~ 1.0 完全不透明）
     * @return RGBA 格式字符串（如 "rgba(25,118,210,0.5)"）
     */
    public static String hexToRgba(String hex, double alpha) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
    }

    /**
     * 创建信息图标标签（ℹ）
     * <p>
     * 用于状态栏左侧的装饰性图标。
     *
     * @return 配置好的 Label 对象
     */
    public static Label createInfoIcon() {
        Label icon = new Label("\u2139");
        icon.setStyle(
                "-fx-font-size: 13px;"
                        + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                        + " -fx-font-weight: bold;"
        );
        return icon;
    }
}
