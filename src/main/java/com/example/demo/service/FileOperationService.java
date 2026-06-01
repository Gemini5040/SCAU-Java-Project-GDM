package com.example.demo.service;

import com.example.demo.model.AppState;
import com.example.demo.model.StatusManager;
import com.example.demo.util.DesignConstants;
import com.example.demo.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * 文件操作服务
 * <p>
 * 提供图片文件的复制、粘贴、删除、重命名（单个/批量）操作。
 * 所有操作均通过 {@link AppState} 管理选中状态和剪贴板，
 * 操作完成后通过 {@code onRefreshCallback} 通知主界面刷新缩略图列表。
 * <p>
 * 对话框样式：所有弹窗均使用主题感知的内联样式，确保浅色/深色主题下视觉一致。
 *
 * @author Photo Manager Team
 * @version 1.1
 */
public class FileOperationService {

    /** 应用共享状态（选中节点、剪贴板、当前目录） */
    private final AppState appState;

    /** 状态栏管理器，用于显示操作结果信息 */
    private final StatusManager statusManager;

    /** 操作完成后的刷新回调（重新加载缩略图） */
    private final Runnable onRefreshCallback;

    /**
     * 构造文件操作服务
     *
     * @param appState          应用共享状态
     * @param statusManager     状态栏管理器
     * @param onRefreshCallback 操作完成后的刷新回调
     */
    public FileOperationService(AppState appState, StatusManager statusManager,
                                Runnable onRefreshCallback) {
        this.appState = appState;
        this.statusManager = statusManager;
        this.onRefreshCallback = onRefreshCallback;
    }

    /** 对话框面板基础样式（背景色 + 文字色） */
    private String dialogStyle() {
        return "-fx-background-color: " + DesignConstants.bgCard() + ";"
                + " -fx-text-fill: " + DesignConstants.textPrimary() + ";";
    }

    /** 对话框内容区样式（背景色 + 内边距） */
    private String dialogContentStyle() {
        return "-fx-background-color: " + DesignConstants.bgCard() + ";"
                + " -fx-padding: 20;";
    }

    /** 对话框标签样式（文字色 + 字号 + 加粗） */
    private String dialogLabelStyle() {
        return "-fx-text-fill: " + DesignConstants.textPrimary() + ";"
                + " -fx-font-size: 13px;"
                + " -fx-font-weight: 600;";
    }

    /** 对话框输入框样式（背景色 + 文字色 + 边框 + 圆角） */
    private String dialogFieldStyle() {
        return "-fx-background-color: " + DesignConstants.bgInput() + ";"
                + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                + " -fx-border-color: " + DesignConstants.PRIMARY() + ";"
                + " -fx-border-radius: 6;"
                + " -fx-background-radius: 6;"
                + " -fx-padding: 8 12;"
                + " -fx-font-size: 13px;";
    }

    /** 对话框预览文字样式（次要文字色 + 斜体） */
    private String dialogPreviewStyle() {
        return "-fx-text-fill: " + DesignConstants.textSecondary() + ";"
                + " -fx-font-style: italic;"
                + " -fx-font-size: 12px;";
    }

    /** 复制选中的图片文件到剪贴板 */
    public void copySelectedFiles() {
        if (appState.getSelectedNodes().isEmpty()) {
            showAlert("Info", "Select images first!");
            return;
        }
        List<File> copied = appState.getSelectedNodes().stream()
                .map(n -> (File) n.getUserData())
                .collect(Collectors.toList());
        appState.getClipboardFiles().clear();
        appState.getClipboardFiles().addAll(copied);
        statusManager.setStatus("Copied " + copied.size() + " images");
    }

    /** 将剪贴板中的图片文件粘贴到当前目录（自动处理重名） */
    public void pasteFiles() {
        if (appState.getClipboardFiles().isEmpty()) {
            showAlert("Info", "Clipboard empty!");
            return;
        }
        if (appState.getCurrentDirectory() == null) {
            showAlert("Info", "Select target!");
            return;
        }
        int successCount = 0;
        for (File src : appState.getClipboardFiles()) {
            File dest = resolveDest(src);
            try {
                Files.copy(src.toPath(), dest.toPath(),
                        StandardCopyOption.COPY_ATTRIBUTES);
                successCount++;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        statusManager.setStatus("Pasted: " + successCount + " images");
        onRefreshCallback.run();
    }

    /** 解析目标路径，若文件已存在则添加 _copy(N) 后缀 */
    private File resolveDest(File src) {
        File dest = new File(appState.getCurrentDirectory(), src.getName());
        int counter = 1;
        String baseName = FileUtils.getBaseName(src.getName());
        String extension = FileUtils.getExtension(src.getName());
        while (dest.exists()) {
            dest = new File(appState.getCurrentDirectory(),
                    baseName + "_copy(" + (counter++) + ")" + extension);
        }
        return dest;
    }

    /** 删除选中的图片文件（需用户确认） */
    public void deleteFiles() {
        if (appState.getSelectedNodes().isEmpty()) {
            showAlert("Info", "Select images first!");
            return;
        }
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete " + appState.getSelectedNodes().size() + " images?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        applyDialogStyle(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            int successCount = 0;
            for (VBox node : appState.getSelectedNodes()) {
                if (((File) node.getUserData()).delete()) {
                    successCount++;
                }
            }
            statusManager.setStatus(
                    "Deleted: " + successCount + "/" + appState.getSelectedNodes().size());
            onRefreshCallback.run();
        }
    }

    /** 重命名选中的图片文件（单个走简单对话框，多个走批量重命名） */
    public void renameFiles() {
        if (appState.getSelectedNodes().isEmpty()) {
            showAlert("Info", "Select first!");
            return;
        }
        List<File> files = appState.getSelectedNodes().stream()
                .map(n -> (File) n.getUserData())
                .collect(Collectors.toList());
        if (files.size() == 1) {
            showSingleRename(files.get(0));
        } else {
            showBatchRename(files);
        }
    }

    /** 单文件重命名对话框（保留扩展名） */
    private void showSingleRename(File file) {
        TextInputDialog dialog = new TextInputDialog(FileUtils.getBaseName(file.getName()));
        dialog.setTitle("Rename");
        dialog.setHeaderText("Enter new name (extension will be preserved):");
        applyDialogStyle(dialog);
        dialog.showAndWait().ifPresent(newName -> {
            if (newName.trim().isEmpty()) {
                showAlert("Error", "Name cannot be empty!");
                return;
            }
            String trimmed = newName.trim();
            File newFile = new File(file.getParent(),
                    trimmed + FileUtils.getExtension(file.getName()));
            if (newFile.exists() && !newFile.equals(file)) {
                showAlert("Error", "File already exists: " + newFile.getName());
                return;
            }
            if (file.renameTo(newFile)) {
                statusManager.setStatus("Renamed: " + trimmed
                        + FileUtils.getExtension(file.getName()));
                onRefreshCallback.run();
            } else {
                showAlert("Error", "Rename failed!");
            }
        });
    }

    /** 批量重命名对话框（前缀 + 起始编号 + 位数，实时预览） */
    private void showBatchRename(List<File> files) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Batch Rename");
        dialog.setHeaderText("Rename " + files.size() + " selected images");

        GridPane grid = new GridPane(15, 14);
        grid.setPadding(new Insets(24, 20, 20, 20));
        grid.setStyle(dialogContentStyle());

        Label prefixLabel = new Label("Prefix:");
        prefixLabel.setStyle(dialogLabelStyle());
        TextField prefixField = new TextField("NewName");
        prefixField.setStyle(dialogFieldStyle());
        prefixField.setPrefWidth(220);

        Label startLabel = new Label("Start #:");
        startLabel.setStyle(dialogLabelStyle());
        TextField startField = new TextField("1");
        startField.setStyle(dialogFieldStyle());
        startField.setPrefWidth(220);

        Label digitsLabel = new Label("Digits:");
        digitsLabel.setStyle(dialogLabelStyle());
        TextField digitsField = new TextField("4");
        digitsField.setStyle(dialogFieldStyle());
        digitsField.setPrefWidth(220);

        Label previewLabel = new Label();
        previewLabel.setStyle(dialogPreviewStyle());
        previewLabel.setPrefWidth(440);

        grid.addRow(0, prefixLabel, prefixField);
        grid.addRow(1, startLabel, startField);
        grid.addRow(2, digitsLabel, digitsField);
        grid.addRow(3, previewLabel);

        updatePreview(previewLabel, prefixField.getText(),
                startField.getText(), digitsField.getText(), files.size());

        prefixField.textProperty().addListener((o, a, b) ->
                updatePreview(previewLabel, b, startField.getText(),
                        digitsField.getText(), files.size()));
        startField.textProperty().addListener((o, a, b) ->
                updatePreview(previewLabel, prefixField.getText(), b,
                        digitsField.getText(), files.size()));
        digitsField.textProperty().addListener((o, a, b) ->
                updatePreview(previewLabel, prefixField.getText(),
                        startField.getText(), b, files.size()));

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(grid);
        dialogPane.setStyle(dialogStyle());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        applyDialogButtonStyle(dialogPane);

        dialog.setResultConverter(button ->
                button == ButtonType.OK
                        ? new String[]{prefixField.getText(),
                        startField.getText(), digitsField.getText()}
                        : null
        );
        dialog.showAndWait().ifPresent(params -> execBatchRename(files, params));
    }

    /** 执行批量重命名（跳过重名文件） */
    private void execBatchRename(List<File> files, String[] params) {
        try {
            String prefix = params[0].isEmpty() ? "Image" : params[0];
            int startNum = Integer.parseInt(params[1].isEmpty() ? "1" : params[1]);
            int digits = Integer.parseInt(params[2].isEmpty() ? "4" : params[2]);
            String format = "%0" + Math.max(digits, 1) + "d";
            int successCount = 0;
            int skippedCount = 0;
            for (File file : files) {
                String ext = FileUtils.getExtension(file.getName());
                String newName = prefix + String.format(format, startNum) + ext;
                File newFile = new File(file.getParent(), newName);
                startNum++;
                if (newFile.exists() && !newFile.equals(file)) {
                    skippedCount++;
                    continue;
                }
                if (file.renameTo(newFile)) {
                    successCount++;
                }
            }
            String msg = "Renamed: " + successCount + "/" + files.size();
            if (skippedCount > 0) {
                msg += " (skipped " + skippedCount + " - name conflict)";
            }
            statusManager.setStatus(msg);
            onRefreshCallback.run();
        } catch (NumberFormatException ex) {
            showAlert("Error", "Invalid numbers! Start# and Digits must be integers.");
        }
    }

    /** 更新批量重命名预览文字 */
    private void updatePreview(Label label, String prefix, String start,
                                String digits, int fileCount) {
        try {
            int startNum = start.isEmpty() ? 1 : Integer.parseInt(start);
            int digitsNum = digits.isEmpty() ? 4 : Integer.parseInt(digits);
            String fmt = "%0" + Math.max(digitsNum, 1) + "d";
            String p = prefix.isEmpty() ? "NewName" : prefix;
            if (fileCount <= 2) {
                StringBuilder sb = new StringBuilder("Preview: ");
                for (int i = 0; i < fileCount; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(p).append(String.format(fmt, startNum + i)).append(".JPG");
                }
                label.setText(sb.toString());
            } else {
                label.setText(String.format("Preview: %s%s.JPG, %s%s.JPG, ..., %s%s.JPG",
                        p, String.format(fmt, startNum),
                        p, String.format(fmt, startNum + 1),
                        p, String.format(fmt, startNum + fileCount - 1)));
            }
        } catch (Exception ex) {
            label.setText("Preview: [enter valid numbers]");
        }
    }

    /** 为 Alert 对话框应用主题样式 */
    private void applyDialogStyle(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.setStyle(dialogStyle());
        applyDialogButtonStyle(pane);
    }

    /** 为 TextInputDialog 对话框应用主题样式 */
    private void applyDialogStyle(TextInputDialog dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(dialogStyle());
        applyDialogButtonStyle(pane);
    }

    /** 为对话框按钮（OK/Cancel/Yes/No）应用主题感知样式 */
    private void applyDialogButtonStyle(DialogPane pane) {
        javafx.scene.Node okBtn = pane.lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setStyle(
                    "-fx-background-color: " + DesignConstants.PRIMARY() + ";"
                            + " -fx-text-fill: white;"
                            + " -fx-font-weight: bold;"
                            + " -fx-padding: 7 20;"
                            + " -fx-background-radius: 6;"
                            + " -fx-cursor: hand;"
            );
        }
        javafx.scene.Node cancelBtn = pane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setStyle(
                    "-fx-background-color: " + DesignConstants.bgInput() + ";"
                            + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                            + " -fx-font-weight: 600;"
                            + " -fx-padding: 7 20;"
                            + " -fx-background-radius: 6;"
                            + " -fx-cursor: hand;"
            );
        }
        javafx.scene.Node yesBtn = pane.lookupButton(ButtonType.YES);
        if (yesBtn != null) {
            yesBtn.setStyle(
                    "-fx-background-color: " + DesignConstants.ACCENT() + ";"
                            + " -fx-text-fill: white;"
                            + " -fx-font-weight: bold;"
                            + " -fx-padding: 7 20;"
                            + " -fx-background-radius: 6;"
                            + " -fx-cursor: hand;"
            );
        }
        javafx.scene.Node noBtn = pane.lookupButton(ButtonType.NO);
        if (noBtn != null) {
            noBtn.setStyle(
                    "-fx-background-color: " + DesignConstants.bgInput() + ";"
                            + " -fx-text-fill: " + DesignConstants.textPrimary() + ";"
                            + " -fx-font-weight: 600;"
                            + " -fx-padding: 7 20;"
                            + " -fx-background-radius: 6;"
                            + " -fx-cursor: hand;"
            );
        }
    }

    /** 显示信息提示对话框 */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyDialogStyle(alert);
        alert.showAndWait();
    }
}
