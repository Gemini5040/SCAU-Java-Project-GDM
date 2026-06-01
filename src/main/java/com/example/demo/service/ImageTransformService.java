package com.example.demo.service;

import com.example.demo.model.AppState;
import com.example.demo.model.StatusManager;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;

/**
 * 图片变换服务
 * <p>
 * 提供图片旋转和翻转操作，直接修改源文件（原地覆盖写入）。
 * 使用 AWT {@link BufferedImage} 进行像素级变换，支持：
 * <ul>
 *   <li>顺时针旋转 90° / 逆时针旋转 90° / 旋转 180°</li>
 *   <li>水平翻转 / 垂直翻转</li>
 * </ul>
 * <p>
 * 变换流程：读取原图 → 执行变换 → 覆盖写回 → 刷新缩略图列表。
 * 注意：此操作不可撤销，建议用户提前备份。
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class ImageTransformService {

    /** 应用共享状态 */
    private final AppState appState;

    /** 状态栏管理器 */
    private final StatusManager statusManager;

    /** 操作完成后的刷新回调 */
    private final Runnable onRefreshCallback;

    /**
     * 构造图片变换服务
     *
     * @param appState          应用共享状态
     * @param statusManager     状态栏管理器
     * @param onRefreshCallback 操作完成后的刷新回调
     */
    public ImageTransformService(AppState appState, StatusManager statusManager,
                                  Runnable onRefreshCallback) {
        this.appState = appState;
        this.statusManager = statusManager;
        this.onRefreshCallback = onRefreshCallback;
    }

    /** 顺时针旋转 90° */
    public void rotateRight() {
        transformSelected(img -> rotate(img, 90), "Rotated 90\u00B0 CW");
    }

    /** 逆时针旋转 90° */
    public void rotateLeft() {
        transformSelected(img -> rotate(img, -90), "Rotated 90\u00B0 CCW");
    }

    /** 旋转 180° */
    public void rotate180() {
        transformSelected(img -> rotate(img, 180), "Rotated 180\u00B0");
    }

    /** 水平翻转（左右镜像） */
    public void flipHorizontal() {
        transformSelected(img -> flip(img, true), "Flipped horizontal");
    }

    /** 垂直翻转（上下镜像） */
    public void flipVertical() {
        transformSelected(img -> flip(img, false), "Flipped vertical");
    }

    /**
     * 对所有选中图片执行变换操作
     * <p>
     * 流程：遍历选中节点 → 读取原图 → 执行变换 → 覆盖写回 → 更新状态栏
     *
     * @param transform  变换函数（BufferedImage → BufferedImage）
     * @param successMsg 成功提示信息
     */
    private void transformSelected(java.util.function.Function<BufferedImage, BufferedImage> transform,
                                    String successMsg) {
        Set<VBox> selected = appState.getSelectedNodes();
        if (selected.isEmpty()) {
            statusManager.setStatus("Select images first!");
            return;
        }
        int successCount = 0;
        for (VBox node : selected) {
            File file = (File) node.getUserData();
            try {
                BufferedImage original = ImageIO.read(file);
                if (original == null) continue;
                BufferedImage transformed = transform.apply(original);
                String format = getFormat(file.getName());
                ImageIO.write(transformed, format, file);
                successCount++;
            } catch (IOException e) {
                System.err.println("Transform failed: " + file.getName());
            }
        }
        statusManager.setStatus(successMsg + ": " + successCount + "/" + selected.size());
        onRefreshCallback.run();
    }

    /**
     * 旋转图片
     * <p>
     * 使用 Graphics2D 仿射变换实现任意角度旋转。
     * 自动计算旋转后的画布尺寸，使用双线性插值保证画质。
     *
     * @param img     原始图片
     * @param degrees 旋转角度（正数顺时针，负数逆时针）
     * @return 旋转后的图片
     */
    private BufferedImage rotate(BufferedImage img, int degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newW = (int) Math.round(img.getWidth() * cos + img.getHeight() * sin);
        int newH = (int) Math.round(img.getWidth() * sin + img.getHeight() * cos);

        int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
        BufferedImage rotated = new BufferedImage(newW, newH, type);

        java.awt.Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.translate((newW - img.getWidth()) / 2.0, (newH - img.getHeight()) / 2.0);
        g2d.rotate(radians, img.getWidth() / 2.0, img.getHeight() / 2.0);
        g2d.drawRenderedImage(img, null);
        g2d.dispose();
        return rotated;
    }

    /**
     * 翻转图片
     * <p>
     * 使用 {@link java.awt.geom.AffineTransform} 缩放变换实现水平/垂直翻转。
     *
     * @param img         原始图片
     * @param horizontal  true 为水平翻转，false 为垂直翻转
     * @return 翻转后的图片
     */
    private BufferedImage flip(BufferedImage img, boolean horizontal) {
        int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), type);
        java.awt.geom.AffineTransform tx = horizontal
                ? java.awt.geom.AffineTransform.getScaleInstance(-1, 1)
                : java.awt.geom.AffineTransform.getScaleInstance(1, -1);
        if (horizontal) {
            tx.translate(-img.getWidth(), 0);
        } else {
            tx.translate(0, -img.getHeight());
        }
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        op.filter(img, flipped);
        return flipped;
    }

    /** 根据文件扩展名推断图片格式（PNG/GIF/BMP/JPEG） */
    private String getFormat(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "PNG";
        if (lower.endsWith(".gif")) return "GIF";
        if (lower.endsWith(".bmp")) return "BMP";
        return "JPEG";
    }
}
