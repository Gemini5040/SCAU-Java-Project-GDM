package com.example.demo.service;

import com.example.demo.util.DesignConstants;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.image.Image;

/**
 * 图片缓存服务
 * <p>
 * 管理缩略图和全尺寸图片的内存缓存，采用 LRU + SoftReference 策略：
 * <ul>
 *   <li>LRU 淘汰：基于 {@link LinkedHashMap} 的访问顺序模式，最多缓存 {@value #MAX_CACHE_SIZE} 条</li>
 *   <li>软引用：使用 {@link SoftReference} 包装 Image 对象，内存不足时由 GC 自动回收</li>
 *   <li>异步加载：缩略图通过线程池（{@value #THUMBNAIL_THREADS} 个守护线程）异步加载，
 *       加载完成后在 JavaFX 应用线程回调</li>
 * </ul>
 * <p>
 * 缓存键格式：{@code "thumb_" 或 "full_" + 绝对路径 + "_" + 最后修改时间}，
 * 文件修改后自动失效。
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class ImageCacheService {

    /** 最大缓存条目数，超出后 LRU 淘汰最久未访问的条目 */
    private static final int MAX_CACHE_SIZE = 200;

    /** 缩略图异步加载线程数 */
    private static final int THUMBNAIL_THREADS = 4;

    /** 缩略图加载线程池（守护线程，不阻止 JVM 退出） */
    private final ExecutorService thumbnailExecutor =
            Executors.newFixedThreadPool(THUMBNAIL_THREADS, r -> {
                Thread t = new Thread(r, "Thumbnail-Loader");
                t.setDaemon(true);
                return t;
            });

    /** LRU 缓存映射，访问顺序模式，值使用软引用 */
    private final Map<String, SoftReference<Image>> cache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SoftReference<Image>> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    /**
     * 同步获取缩略图
     * <p>
     * 优先从缓存读取，缓存未命中时同步加载并缓存。
     * 缩略图尺寸为 {@link DesignConstants#THUMBNAIL_SIZE}，保持宽高比。
     *
     * @param file 图片文件
     * @return 缩略图 Image 对象
     */
    public Image getThumbnail(File file) {
        String key = "thumb_" + file.getAbsolutePath() + "_" + file.lastModified();
        SoftReference<Image> ref = cache.get(key);
        if (ref != null) {
            Image img = ref.get();
            if (img != null && !img.isError()) {
                return img;
            }
        }
        Image img = new Image(file.toURI().toString(),
                DesignConstants.THUMBNAIL_SIZE, DesignConstants.THUMBNAIL_SIZE,
                true, true, true);
        cache.put(key, new SoftReference<>(img));
        return img;
    }

    /**
     * 异步加载缩略图
     * <p>
     * 在后台线程池中加载缩略图，加载完成后在 JavaFX 应用线程回调。
     * 加载失败时降级为原始尺寸图片。
     *
     * @param file     图片文件
     * @param callback 加载完成回调，参数为 Image 对象
     */
    public void loadThumbnailAsync(File file, java.util.function.Consumer<Image> callback) {
        String key = "thumb_" + file.getAbsolutePath() + "_" + file.lastModified();
        SoftReference<Image> ref = cache.get(key);
        if (ref != null) {
            Image img = ref.get();
            if (img != null && !img.isError()) {
                Platform.runLater(() -> callback.accept(img));
                return;
            }
        }
        thumbnailExecutor.submit(() -> {
            try {
                Image img = new Image(file.toURI().toString(),
                        DesignConstants.THUMBNAIL_SIZE, DesignConstants.THUMBNAIL_SIZE,
                        true, true, false);
                if (img.isError()) {
                    Platform.runLater(() -> {
                        Image placeholder = new Image(file.toURI().toString());
                        cache.put(key, new SoftReference<>(placeholder));
                        callback.accept(placeholder);
                    });
                    return;
                }
                cache.put(key, new SoftReference<>(img));
                Platform.runLater(() -> callback.accept(img));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 同步获取全尺寸图片（最大 1920×1440，保持宽高比）
     *
     * @param file 图片文件
     * @return 全尺寸 Image 对象
     */
    public Image getFullImage(File file) {
        String key = "full_" + file.getAbsolutePath() + "_" + file.lastModified();
        SoftReference<Image> ref = cache.get(key);
        if (ref != null) {
            Image img = ref.get();
            if (img != null && !img.isError()) {
                return img;
            }
        }
        Image img = new Image(file.toURI().toString(), 1920, 1440, true, true, true);
        cache.put(key, new SoftReference<>(img));
        return img;
    }

    /** 清空缓存 */
    public void clear() {
        cache.clear();
    }

    /** 关闭线程池并清空缓存（应用退出时调用） */
    public void shutdown() {
        thumbnailExecutor.shutdownNow();
        cache.clear();
    }
}
