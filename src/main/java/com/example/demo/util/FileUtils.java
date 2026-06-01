package com.example.demo.util;

import java.io.File;
import javafx.scene.image.Image;

/**
 * 文件工具类
 * <p>
 * 提供与文件操作相关的静态工具方法，包括：
 * <ul>
 *   <li>文件大小格式化：将字节数转换为人类可读的字符串</li>
 *   <li>文件名解析：提取文件基本名和扩展名</li>
 *   <li>图片文件检测：根据扩展名判断是否为支持的图片格式</li>
 *   <li>路径规范化：统一路径分隔符，便于路径比较</li>
 *   <li>高质量图片加载：以指定尺寸加载图片并启用平滑渲染</li>
 * </ul>
 * <p>
 * 工具类设计原则：
 * <ul>
 *   <li>所有方法均为 static，无需创建对象即可调用</li>
 *   <li>构造器私有化，防止误实例化</li>
 *   <li>方法无副作用，不修改传入参数的状态</li>
 * </ul>
 *
 * @author Photo Manager Team
 * @version 1.0
 */
public final class FileUtils {

    /**
     * 私有构造器，防止外部实例化
     */
    private FileUtils() {
        throw new AssertionError("不允许实例化工具类");
    }

    /**
     * 将文件大小（字节数）格式化为人类可读的字符串
     * <p>
     * 转换规则：
     * <ul>
     *   <li>小于 1 KB → 显示为 "xxx B"</li>
     *   <li>小于 1 MB → 显示为 "xxx.x KB"</li>
     *   <li>小于 1 GB → 显示为 "xxx.x MB"</li>
     *   <li>大于等于 1 GB → 显示为 "xxx.x GB"</li>
     * </ul>
     *
     * @param bytes 文件大小，单位为字节
     * @return 格式化后的字符串，例如 "1.5 MB"
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 获取文件的基本名（不含扩展名）
     * <p>
     * 示例："photo.jpg" → "photo"，"README" → "README"
     *
     * @param filename 文件名字符串
     * @return 不含扩展名的文件基本名；若无扩展名则返回原字符串
     */
    public static String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    /**
     * 获取文件的扩展名（含点号）
     * <p>
     * 示例："photo.jpg" → ".jpg"，"README" → ""
     *
     * @param filename 文件名字符串
     * @return 含点号的扩展名字符串；若无扩展名则返回空字符串
     */
    public static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }

    /**
     * 判断文件是否为支持的图片格式
     * <p>
     * 通过文件扩展名进行判断（不区分大小写），支持的格式由
     * {@link DesignConstants#SUPPORTED_EXTENSIONS} 定义。
     *
     * @param file 待检测的文件对象
     * @return 若文件扩展名匹配支持的图片格式则返回 true
     */
    public static boolean isImageFile(File file) {
        String lowerName = file.getName().toLowerCase();
        for (String ext : DesignConstants.SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规范化文件路径，统一为 Windows 风格
     * <p>
     * 处理步骤：
     * <ol>
     *   <li>将正斜杠 '/' 替换为反斜杠 '\\'</li>
     *   <li>转换为小写（Windows 路径不区分大小写）</li>
     *   <li>确保路径以反斜杠结尾（便于前缀匹配）</li>
     *   <li>处理盘符根路径的特殊情况（如 "C:" → "C:\\"）</li>
     * </ol>
     *
     * @param path 原始路径字符串
     * @return 规范化后的路径字符串
     */
    public static String normalizePath(String path) {
        String normalized = path.replace('/', '\\').toLowerCase();
        if (!normalized.endsWith("\\")) {
            normalized = normalized + "\\";
        }
        if (normalized.length() >= 2
                && normalized.charAt(1) == ':'
                && normalized.length() == 2) {
            normalized = normalized + "\\";
        }
        return normalized;
    }

    /**
     * 以指定尺寸加载高质量图片
     * <p>
     * 该方法创建的 Image 对象启用了以下特性：
     * <ul>
     *   <li>保持宽高比（preserveRatio = true）</li>
     *   <li>启用平滑渲染（smooth = true），缩放时减少锯齿</li>
     *   <li>启用后台加载（backgroundLoading = true），避免阻塞 UI 线程</li>
     * </ul>
     *
     * @param file      图片文件对象
     * @param width     期望宽度（0 表示不限制）
     * @param height    期望高度（0 表示不限制）
     * @return 加载的 Image 对象
     */
    public static Image createHighQualityImage(File file, double width, double height) {
        return new Image(file.toURI().toString(), width, height, true, true, true);
    }
}
