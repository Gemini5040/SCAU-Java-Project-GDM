package com.example.demo.util;

import com.example.demo.model.ThemeManager;

/**
 * 设计常量工具类
 * <p>
 * 集中管理应用程序中所有与视觉设计相关的常量和主题感知颜色值。
 * 作为 {@link ThemeManager} 的静态便捷访问层，使各组件无需持有 ThemeManager 引用
 * 即可获取当前主题的颜色值。
 * <p>
 * 设计理念：
 * <ul>
 *   <li>主题感知颜色：通过委托给 {@link ThemeManager} 的实例方法，动态返回当前主题对应的颜色</li>
 *   <li>不可变常量：缩略图尺寸、应用标题等与主题无关的常量直接声明为 static final</li>
 *   <li>工具类模式：私有构造器 + 静态方法，无需实例化</li>
 * </ul>
 * <p>
 * 注意：主题感知颜色方法（如 {@link #PRIMARY()}）在每次调用时查询 ThemeManager，
 * 因此返回值会随主题切换而变化。在事件处理器中使用时总能获取最新值，
 * 但在闭包中捕获返回值时需注意可能过期。
 *
 * @author Photo Manager Team
 * @version 1.1
 * @see ThemeManager
 */
public final class DesignConstants {

    // ==================== 主题感知颜色 ====================
    // 以下方法委托给 ThemeManager，根据当前主题返回对应颜色值。
    // 优先使用这些方法而非硬编码颜色值，确保主题切换时颜色自动更新。

    /** 主色调（蓝色系），用于选中状态、主按钮、链接等 */
    public static String PRIMARY() {
        return ThemeManager.getInstance().primary();
    }

    /** 主色调浅色变体，用于悬停状态、次要高亮 */
    public static String PRIMARY_LIGHT() {
        return ThemeManager.getInstance().primaryLight();
    }

    /** 主色调深色变体，用于按下状态、强调 */
    public static String PRIMARY_DARK() {
        return ThemeManager.getInstance().primaryDark();
    }

    /** 强调色（红色系），用于删除按钮、危险操作 */
    public static String ACCENT() {
        return ThemeManager.getInstance().accent();
    }

    /** 成功色（绿色系），用于确认按钮、成功提示 */
    public static String SUCCESS() {
        return ThemeManager.getInstance().success();
    }

    /** 警告色（橙色系），用于警告提示 */
    public static String WARNING() {
        return ThemeManager.getInstance().warning();
    }

    /** 主背景色，用于内容区域背景 */
    public static String bgMain() {
        return ThemeManager.getInstance().bgMain();
    }

    /** 卡片背景色，用于缩略图卡片、对话框面板 */
    public static String bgCard() {
        return ThemeManager.getInstance().bgCard();
    }

    /** 工具栏背景色 */
    public static String bgToolbar() {
        return ThemeManager.getInstance().bgToolbar();
    }

    /** 状态栏背景色 */
    public static String bgStatusbar() {
        return ThemeManager.getInstance().bgStatusbar();
    }

    /** 侧边栏背景色 */
    public static String bgSidebar() {
        return ThemeManager.getInstance().bgSidebar();
    }

    /** 侧边栏悬停背景色 */
    public static String bgSidebarHover() {
        return ThemeManager.getInstance().bgSidebarHover();
    }

    /** 输入框背景色 */
    public static String bgInput() {
        return ThemeManager.getInstance().bgInput();
    }

    /** 主文字颜色（浅色主题 #212121，深色主题 #E0E0E0） */
    public static String textPrimary() {
        return ThemeManager.getInstance().textPrimary();
    }

    /** 次要文字颜色 */
    public static String textSecondary() {
        return ThemeManager.getInstance().textSecondary();
    }

    /** 侧边栏文字颜色 */
    public static String textSidebar() {
        return ThemeManager.getInstance().textSidebar();
    }

    /** 侧边栏暗淡文字颜色 */
    public static String textSidebarDim() {
        return ThemeManager.getInstance().textSidebarDim();
    }

    /** 提示文字颜色（比输入文字更淡，用于 TextField 的 promptText） */
    public static String textPrompt() {
        return ThemeManager.getInstance().textPrompt();
    }

    /** 边框颜色 */
    public static String border() {
        return ThemeManager.getInstance().border();
    }

    /** 浅边框颜色 */
    public static String borderLight() {
        return ThemeManager.getInstance().borderLight();
    }

    /** 阴影颜色 */
    public static String shadowColor() {
        return ThemeManager.getInstance().shadowColor();
    }

    /** 悬停阴影颜色 */
    public static String shadowHoverColor() {
        return ThemeManager.getInstance().shadowHoverColor();
    }

    /** 选中阴影颜色 */
    public static String shadowSelectedColor() {
        return ThemeManager.getInstance().shadowSelectedColor();
    }

    /** 框选矩形填充颜色 */
    public static String selectionFill() {
        return ThemeManager.getInstance().selectionFill();
    }

    /** 框选矩形边框颜色 */
    public static String selectionStroke() {
        return ThemeManager.getInstance().selectionStroke();
    }

    // ==================== 不可变常量 ====================

    /** 应用程序标题 */
    public static final String APP_TITLE = "Photo Manager Pro";

    /** 支持的图片文件扩展名列表 */
    public static final String[] SUPPORTED_EXTENSIONS = {
            ".jpg", ".jpeg", ".gif", ".png", ".bmp"
    };

    /** 缩略图卡片中图片的显示尺寸（宽高相同） */
    public static final double THUMBNAIL_SIZE = 135.0;

    /** 幻灯片视图默认宽度 */
    public static final double SLIDEVIEW_WIDTH = 900.0;

    /** 幻灯片视图默认高度 */
    public static final double SLIDEVIEW_HEIGHT = 600.0;

    /** 默认自动播放间隔（秒） */
    public static final double DEFAULT_SLIDESHOW_INTERVAL = 1.5;

    /** 缩放步进值 */
    public static final double ZOOM_STEP = 0.15;

    /** 最小缩放比例 */
    public static final double MIN_ZOOM = 0.1;

    /** 最大缩放比例 */
    public static final double MAX_ZOOM = 8.0;

    /**
     * 私有构造器，防止外部实例化
     */
    private DesignConstants() {
        throw new AssertionError("Utility class");
    }
}
