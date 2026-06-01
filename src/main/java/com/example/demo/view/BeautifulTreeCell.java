package com.example.demo.view;

import com.example.demo.util.DesignConstants;
import java.io.File;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;

/**
 * 美化目录树单元格
 * <p>
 * 自定义 {@link TreeCell} 实现，为目录树中的每个节点提供：
 * <ul>
 *   <li>目录图标：展开时显示 📂，折叠时显示 📁，颜色为主色调</li>
 *   <li>文件图标：显示 📎，颜色为主题文字色</li>
 *   <li>悬停效果：鼠标悬停时显示侧边栏悬停背景色</li>
 *   <li>统一文字颜色：使用 {@link DesignConstants#textPrimary()} 确保主题切换一致</li>
 * </ul>
 * <p>
 * 注意：由于 TreeCell 是虚拟化控件，单元格会被复用。
 * {@link #updateItem} 在每次单元格内容更新时调用，必须在此方法中重置所有样式。
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class BeautifulTreeCell extends TreeCell<File> {

    /** 当前项是否为目录 */
    private boolean isDirectory = false;

    /** 构造器，设置内容显示模式为左对齐图标+文字 */
    public BeautifulTreeCell() {
        setContentDisplay(ContentDisplay.LEFT);
    }

    /**
     * 更新单元格内容
     * <p>
     * JavaFX 虚拟化控件的核心回调方法。每次单元格需要显示新数据时调用。
     * 必须在此方法中完整设置文本、图标和样式，因为单元格会被复用。
     */
    @Override
    protected void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-background-color:transparent;"
                    + " -fx-text-fill:" + DesignConstants.textPrimary() + ";");
            isDirectory = false;
        } else {
            String displayName = item.getName().isEmpty()
                    ? item.getAbsolutePath()
                    : item.getName();
            setText(displayName);
            isDirectory = item.isDirectory();

            if (isDirectory) {
                applyDirectoryStyle();
            } else {
                applyFileStyle();
            }
            setupHoverEffects();
        }
    }

    private void applyDirectoryStyle() {
        boolean expanded = getTreeItem() != null && getTreeItem().isExpanded();
        Label icon = new Label(expanded ? "\uD83D\uDCC2" : "\uD83D\uDCC1");
        icon.setStyle("-fx-font-size:14px;"
                + " -fx-text-fill:" + DesignConstants.PRIMARY() + ";"
                + " -fx-padding:0 2 0 0;");
        setGraphic(icon);
        setStyle("-fx-background-color:transparent;"
                + " -fx-text-fill:" + DesignConstants.textPrimary() + ";"
                + " -fx-font-size:13px;"
                + " -fx-font-family:'Segoe UI',sans-serif;"
                + " -fx-font-weight:500;");
    }

    private void applyFileStyle() {
        Label icon = new Label("\uD83D\uDCCE");
        icon.setStyle("-fx-font-size:12px;"
                + " -fx-text-fill:" + DesignConstants.textPrimary() + ";"
                + " -fx-padding:0 2 0 0;");
        setGraphic(icon);
        setStyle("-fx-background-color:transparent;"
                + " -fx-text-fill:" + DesignConstants.textPrimary() + ";"
                + " -fx-font-size:13px;"
                + " -fx-font-family:'Segoe UI',sans-serif;");
    }

    private void setupHoverEffects() {
        setOnMouseEntered(e -> {
            if (!isSelected()) {
                setStyle("-fx-background-color:" + DesignConstants.bgSidebarHover() + ";"
                        + " -fx-background-radius:6;"
                        + " -fx-text-fill:" + DesignConstants.textPrimary() + ";"
                        + (isDirectory ? " -fx-font-weight:500;" : ""));
            }
        });
        setOnMouseExited(e -> {
            if (!isSelected()) {
                setStyle("-fx-background-color:transparent;"
                        + " -fx-text-fill:" + DesignConstants.textPrimary() + ";"
                        + " -fx-font-size:13px;"
                        + " -fx-font-family:'Segoe UI',sans-serif;"
                        + (isDirectory ? " -fx-font-weight:500;" : ""));
            }
        });
    }
}
