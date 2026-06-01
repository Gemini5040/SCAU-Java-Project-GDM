package com.example.demo.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * 主题管理器（单例模式）
 * <p>
 * 负责管理应用程序的浅色/深色主题切换，提供主题感知的颜色值查询接口。
 * 采用单例设计模式，确保全局只有一个主题状态源，所有组件通过统一接口获取颜色值，
 * 保证视觉一致性。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>维护当前主题状态（浅色/深色），通过 {@link ObjectProperty} 实现可观察</li>
 *   <li>在主题切换时更新 CSS 样式表和根节点样式类</li>
 *   <li>通知所有注册的主题变更监听器，驱动 UI 组件刷新</li>
 *   <li>提供统一的颜色查询方法，根据当前主题返回对应的颜色值</li>
 * </ul>
 * <p>
 * 颜色体系设计原则：
 * <ul>
 *   <li>文字颜色统一：浅色主题下所有文字为 #212121（深灰），深色主题下为 #E0E0E0（浅灰）</li>
 *   <li>提示文字区分：使用 {@link #textPrompt()} 返回更淡的颜色，与输入文字形成层次</li>
 *   <li>语义化命名：primary（主色）、accent（强调色）、success（成功色）等，便于理解用途</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *   ThemeManager tm = ThemeManager.getInstance();
 *   tm.toggleTheme();                          // 切换主题
 *   String color = tm.textPrimary();           // 获取当前主题的主文字颜色
 *   tm.addThemeChangeListener(this::refresh);  // 注册主题变更监听
 * </pre>
 *
 * @author Photo Manager Team
 * @version 1.1
 * @see DesignConstants DesignConstants 提供静态便捷访问方法
 */
public class ThemeManager {

    /**
     * 主题枚举
     * <p>
     * 定义系统支持的两种主题模式：
     * <ul>
     *   <li>{@link #LIGHT}：浅色主题，白色背景 + 深色文字</li>
     *   <li>{@link #DARK}：深色主题，深灰背景 + 浅色文字</li>
     * </ul>
     */
    public enum Theme {
        /** 浅色主题 */
        LIGHT,
        /** 深色主题 */
        DARK
    }

    /** 单例实例，在类加载时创建 */
    private static final ThemeManager INSTANCE = new ThemeManager();

    /** 当前主题属性，使用 JavaFX ObjectProperty 支持属性绑定和监听 */
    private final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>(Theme.LIGHT);

    /** 主场景引用，用于切换 CSS 样式表和根节点样式类 */
    private Scene mainScene;

    /** 主题变更监听器列表，主题切换时逐一调用 */
    private final java.util.List<Runnable> themeChangeListeners = new java.util.ArrayList<>();

    /**
     * 私有构造器，防止外部实例化（单例模式核心）
     */
    private ThemeManager() {}

    /**
     * 获取单例实例
     *
     * @return 主题管理器的唯一实例
     */
    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置主场景并应用当前主题
     * <p>
     * 必须在 UI 初始化后调用，否则主题切换无法生效。
     *
     * @param scene JavaFX 主场景
     */
    public void setMainScene(Scene scene) {
        this.mainScene = scene;
        applyTheme(currentTheme.get());
    }

    /**
     * 获取当前主题
     *
     * @return 当前主题枚举值
     */
    public Theme getCurrentTheme() {
        return currentTheme.get();
    }

    /**
     * 获取当前主题属性（可用于 JavaFX 属性绑定）
     *
     * @return 主题 ObjectProperty
     */
    public ObjectProperty<Theme> currentThemeProperty() {
        return currentTheme;
    }

    /**
     * 切换主题（浅色 ↔ 深色）
     * <p>
     * 切换流程：
     * <ol>
     *   <li>计算新主题值并更新属性</li>
     *   <li>调用 {@link #applyTheme} 更新 CSS 和样式类</li>
     *   <li>通知所有注册的监听器刷新 UI</li>
     * </ol>
     */
    public void toggleTheme() {
        Theme next = currentTheme.get() == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        currentTheme.set(next);
        applyTheme(next);
        notifyListeners();
    }

    /**
     * 判断当前是否为深色主题
     *
     * @return 若当前为深色主题则返回 true
     */
    public boolean isDarkTheme() {
        return currentTheme.get() == Theme.DARK;
    }

    /**
     * 注册主题变更监听器
     * <p>
     * 监听器在主题切换时被调用，用于刷新组件的颜色、样式等。
     * 建议在组件构造时注册，避免遗漏主题切换刷新。
     *
     * @param listener 主题变更时执行的回调
     */
    public void addThemeChangeListener(Runnable listener) {
        themeChangeListeners.add(listener);
    }

    /**
     * 移除主题变更监听器
     *
     * @param listener 要移除的监听器
     */
    public void removeThemeChangeListener(Runnable listener) {
        themeChangeListeners.remove(listener);
    }

    /**
     * 通知所有主题变更监听器
     */
    private void notifyListeners() {
        for (Runnable listener : themeChangeListeners) {
            listener.run();
        }
    }

    /**
     * 应用指定主题到主场景
     * <p>
     * 实现方式：
     * <ol>
     *   <li>重新加载 CSS 样式表（CSS 中使用变量区分主题色）</li>
     *   <li>在根节点上添加/移除 "dark-theme" 样式类，供 CSS 选择器使用</li>
     * </ol>
     *
     * @param theme 要应用的主题
     */
    private void applyTheme(Theme theme) {
        if (mainScene == null) return;

        String css = getClass().getResource("/css/theme.css").toExternalForm();
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(css);

        Parent root = mainScene.getRoot();
        root.getStyleClass().removeAll("dark-theme");
        if (theme == Theme.DARK) {
            root.getStyleClass().add("dark-theme");
        }
    }

    // ==================== 颜色查询方法 ====================
    // 以下方法根据当前主题返回对应的颜色值。
    // 浅色主题使用 Material Design Light 色板，深色主题使用 Dark 色板。

    /** 主背景色：浅色 #F5F5F5，深色 #121212 */
    public String bgMain() {
        return isDarkTheme() ? "#121212" : "#F5F5F5";
    }

    /** 卡片背景色：浅色 #FFFFFF，深色 #1E1E1E */
    public String bgCard() {
        return isDarkTheme() ? "#1E1E1E" : "#FFFFFF";
    }

    /** 工具栏背景色 */
    public String bgToolbar() {
        return isDarkTheme() ? "#1E1E1E" : "#FFFFFF";
    }

    /** 状态栏背景色 */
    public String bgStatusbar() {
        return isDarkTheme() ? "#1E1E1E" : "#FFFFFF";
    }

    /** 侧边栏背景色：浅色 #F0F0F0，深色 #1A1A1A */
    public String bgSidebar() {
        return isDarkTheme() ? "#1A1A1A" : "#F0F0F0";
    }

    /** 侧边栏悬停背景色 */
    public String bgSidebarHover() {
        return isDarkTheme() ? "#2C2C2C" : "#E0E0E0";
    }

    /** 输入框背景色 */
    public String bgInput() {
        return isDarkTheme() ? "#2C2C2C" : "#F0F0F0";
    }

    /** 主文字颜色：浅色 #212121，深色 #E0E0E0（统一） */
    public String textPrimary() {
        return isDarkTheme() ? "#E0E0E0" : "#212121";
    }

    /** 次要文字颜色（与主文字统一） */
    public String textSecondary() {
        return isDarkTheme() ? "#E0E0E0" : "#212121";
    }

    /** 侧边栏文字颜色（与主文字统一） */
    public String textSidebar() {
        return isDarkTheme() ? "#E0E0E0" : "#212121";
    }

    /** 侧边栏暗淡文字颜色（与主文字统一） */
    public String textSidebarDim() {
        return isDarkTheme() ? "#E0E0E0" : "#212121";
    }

    /** 提示文字颜色：浅色 #999999，深色 #888888（比输入文字更淡） */
    public String textPrompt() {
        return isDarkTheme() ? "#888888" : "#999999";
    }

    /** 边框颜色 */
    public String border() {
        return isDarkTheme() ? "#424242" : "#E0E0E0";
    }

    /** 浅边框颜色 */
    public String borderLight() {
        return isDarkTheme() ? "#333333" : "#EEEEEE";
    }

    /** 主色调：浅色 #1976D2，深色 #42A5F5 */
    public String primary() {
        return isDarkTheme() ? "#42A5F5" : "#1976D2";
    }

    /** 主色调浅色变体 */
    public String primaryLight() {
        return isDarkTheme() ? "#64B5F6" : "#42A5F5";
    }

    /** 主色调深色变体 */
    public String primaryDark() {
        return isDarkTheme() ? "#1976D2" : "#1565C0";
    }

    /** 强调色（红色系，用于删除等危险操作） */
    public String accent() {
        return isDarkTheme() ? "#EF5350" : "#F44336";
    }

    /** 成功色（绿色系） */
    public String success() {
        return isDarkTheme() ? "#66BB6A" : "#4CAF50";
    }

    /** 警告色（橙色系） */
    public String warning() {
        return isDarkTheme() ? "#FFA726" : "#FF9800";
    }

    /** 阴影颜色 */
    public String shadowColor() {
        return isDarkTheme() ? "rgba(0,0,0,0.3)" : "rgba(0,0,0,0.06)";
    }

    /** 悬停阴影颜色 */
    public String shadowHoverColor() {
        return isDarkTheme() ? "rgba(0,0,0,0.5)" : "rgba(0,0,0,0.12)";
    }

    /** 选中阴影颜色 */
    public String shadowSelectedColor() {
        return isDarkTheme() ? "rgba(66,165,245,0.4)" : "rgba(25,118,210,0.3)";
    }

    /** 框选填充颜色 */
    public String selectionFill() {
        return isDarkTheme() ? "rgba(66,165,245,0.15)" : "rgba(25,118,210,0.12)";
    }

    /** 框选边框颜色 */
    public String selectionStroke() {
        return isDarkTheme() ? "rgba(66,165,245,0.8)" : "rgba(25,118,210,0.75)";
    }
}
