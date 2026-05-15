package com.example.demo;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PhotoManagerApp extends Application {

    // ==================== Design System ====================
    private static final String PRIMARY = "#6366F1";
    private static final String PRIMARY_LIGHT = "#818CF8";
    private static final String ACCENT = "#F43F5E";
    private static final String SUCCESS = "#10B981";
    private static final String WARNING = "#F59E0B";
    private static final String BG_MAIN = "#F1F5F9";
    private static final String BG_CARD = "#FFFFFF";
    private static final String BG_SIDEBAR = "#0F172A";
    private static final String BG_SIDEBAR_HOVER = "#1E293B";
    private static final String TEXT_PRIMARY = "#0F172A";
    private static final String TEXT_SECONDARY = "#64748B";
    private static final String TEXT_SIDEBAR = "#CBD5E1";
    private static final String TEXT_SIDEBAR_DIM = "#94A3B8";
    private static final String BORDER = "#E2E8F0";

    // ==================== Constants ====================
    private static final String APP_TITLE = "Photo Manager Pro";
    private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".gif", ".png", ".bmp"};
    private static final double THUMBNAIL_SIZE = 135.0;
    private static final double SLIDEVIEW_WIDTH = 900.0;
    private static final double SLIDEVIEW_HEIGHT = 600.0;
    private static final double DEFAULT_SLIDESHOW_INTERVAL = 1.5;
    private static final double ZOOM_STEP = 0.15;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 8.0;

    // ==================== UI Components ====================
    private TreeView<File> directoryTree;
    private TilePane thumbnailPane;
    private Label statusLabel;
    private Label directoryInfoLabel;
    private Label selectedCountLabel;
    private Label imageCountBadge;
    private ScrollPane treeScrollPane;
    private BorderPane rightPane;
    private SplitPane mainSplitPane;
    private TextField addressField;

    // ==================== Data State ====================
    private File currentDirectory;
    private List<File> currentImages = new ArrayList<>();
    private Set<VBox> selectedNodes = new HashSet<>();
    private List<File> clipboardFiles = new ArrayList<>();

    // ==================== Selection State ====================
    private Rectangle selectionRectangle;
    private boolean isSelecting = false;
    private double startX, startY;
    private int currentImageIndex = -1;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(APP_TITLE);
        createLayoutStructure(primaryStage);
        initDirectoryTree();
        initThumbnailArea();
        setupKeyboardShortcuts(primaryStage.getScene());
        primaryStage.show();
        showWelcomeMessage();
    }

    // ==================== Layout Structure (Optimized) ====================

    private void createLayoutStructure(Stage primaryStage) {
        // --- Sidebar with header + address bar ---
        VBox sidebarContainer = createSidebarWithHeader();

        treeScrollPane = (ScrollPane) sidebarContainer.getChildren().get(2);

        // --- Main content area ---
        HBox toolbar = createToolbar();

        directoryInfoLabel = new Label("Select a folder to browse images");
        applyToolbarTitleStyle(directoryInfoLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        imageCountBadge = new Label("");
        applyCountBadgeStyle(imageCountBadge);

        HBox toolbarContent = new HBox(15, directoryInfoLabel, spacer, imageCountBadge, toolbar);
        toolbarContent.setAlignment(Pos.CENTER_LEFT);
        toolbarContent.setPadding(new Insets(10, 20, 10, 20));
        toolbarContent.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        rightPane = new BorderPane();
        rightPane.setTop(toolbarContent);

        // --- Split Pane (optimized ratio) ---
        mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(sidebarContainer, rightPane);
        mainSplitPane.setDividerPositions(0.20);
        mainSplitPane.setStyle("-fx-background-color: " + BG_MAIN + ";");

        // --- Status Bar ---
        selectedCountLabel = new Label("");
        applyStatusBadgeStyle(selectedCountLabel);

        statusLabel = new Label("Ready - Browse your photos");

        HBox statusLeft = new HBox(createInfoIcon(), statusLabel);
        statusLeft.setSpacing(8);
        statusLeft.setAlignment(Pos.CENTER_LEFT);

        HBox statusBar = new HBox(statusLeft, selectedCountLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10, 20, 10, 20));
        statusBar.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 1 0 0 0;");
        BorderPane.setMargin(statusBar, new Insets(0));

        // --- Root Layout ---
        BorderPane rootLayout = new BorderPane();
        rootLayout.setCenter(mainSplitPane);
        rootLayout.setBottom(statusBar);

        Scene scene = new Scene(rootLayout, 1300, 850);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(680);
    }

    // ==================== Sidebar with Header + Address Bar ====================

    private VBox createSidebarWithHeader() {
        // --- Title Header ---
        Label sidebarTitle = new Label("Explorer");
        sidebarTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + TEXT_SIDEBAR + "; -fx-font-family: 'Segoe UI', sans-serif; -fx-letter-spacing: 1;");

        HBox sidebarHeader = new HBox(sidebarTitle);
        sidebarHeader.setPadding(new Insets(12, 16, 10, 16));
        sidebarHeader.setStyle("-fx-background-color: " + BG_SIDEBAR + "; -fx-border-color: #1E293B; -fx-border-width: 0 0 1 0;");

        // --- Address Bar ---
        HBox addressBar = createAddressBar();

        // --- Directory Tree ---
        directoryTree = new TreeView<>();
        treeScrollPane = new ScrollPane(directoryTree);
        treeScrollPane.setFitToWidth(true);
        treeScrollPane.setFitToHeight(true);
        applySidebarScrollStyle(treeScrollPane);

        VBox sidebar = new VBox(sidebarHeader, addressBar, treeScrollPane);
        VBox.setVgrow(treeScrollPane, Priority.ALWAYS);
        sidebar.setStyle("-fx-background-color: " + BG_SIDEBAR + ";");
        return sidebar;
    }

    private HBox createAddressBar() {
        addressField = new TextField();
        addressField.setPromptText("Enter path (e.g. C:\\Users\\Photos)");
        addressField.setStyle(
            "-fx-background-color: #1E293B;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #64748B;" +
            "-fx-font-size: 12px;" +
            "-fx-font-family: 'Consolas', 'Monaco', monospace;" +
            "-fx-padding: 7 10;" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: transparent;" +
            "-fx-border-radius: 6;"
        );
        addressField.setPrefHeight(32);

        Button goBtn = new Button("\u2192");
        goBtn.setPrefSize(32, 32);
        goBtn.setStyle(
            "-fx-background-color: " + PRIMARY + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 0 6 6 0;" +
            "-fx-padding: 0;"
        );

        Button folderBtn = new Button("\uD83D\uDCC1");
        folderBtn.setPrefSize(32, 32);
        folderBtn.setStyle(
            "-fx-background-color: #334155;" +
            "-fx-text-fill: " + TEXT_SIDEBAR_DIM + ";" +
            "-fx-font-size: 14px;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 6 0 0 6;" +
            "-fx-padding: 0;"
        );

        HBox container = new HBox(folderBtn, addressField, goBtn);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(8, 10, 8, 10));
        container.setSpacing(0);
        HBox.setHgrow(addressField, Priority.ALWAYS);
        container.setStyle("-fx-background-color: " + BG_SIDEBAR + "; -fx-border-color: #1E293B; -fx-border-width: 0 0 1 0;");

        goBtn.setOnAction(e -> navigateToPath(addressField.getText().trim()));
        addressField.setOnAction(e -> navigateToPath(addressField.getText().trim()));
        folderBtn.setOnAction(e -> chooseFolder());

        goBtn.setOnMouseEntered(e -> goBtn.setStyle(goBtn.getStyle().replace(PRIMARY, "#4F46E5")));
        goBtn.setOnMouseExited(e -> goBtn.setStyle(
            "-fx-background-color: " + PRIMARY + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 0 6 6 0;" +
            "-fx-padding: 0;"
        ));

        return container;
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");
        File selected = chooser.showDialog(directoryTree.getScene().getWindow());
        if (selected != null) {
            addressField.setText(selected.getAbsolutePath());
            navigateToPath(selected.getAbsolutePath());
        }
    }

    private void navigateToPath(String pathText) {
        if (pathText == null || pathText.trim().isEmpty()) {
            showAlert("Info", "Please enter a path.");
            return;
        }
        File targetFile = new File(pathText.trim());
        if (!targetFile.exists()) {
            showAlert("Error", "Path does not exist:\n" + pathText.trim());
            return;
        }
        File targetDir = targetFile.isDirectory() ? targetFile : targetFile.getParentFile();
        if (targetDir == null) {
            showAlert("Error", "Cannot determine parent directory.");
            return;
        }
        expandTreeTo(targetDir);
    }

    private void expandTreeTo(File targetDir) {
        String targetPath = normalizePath(targetDir.getAbsolutePath());
        TreeItem<File> root = directoryTree.getRoot();
        if (root == null) return;

        TreeItem<File> driveItem = findDriveRoot(root, targetPath);
        if (driveItem == null) {
            statusLabel.setText("Navigating to: " + targetDir.getAbsolutePath());
            loadImagesFromDirectory(targetDir);
            addressField.setText(targetDir.getAbsolutePath());
            return;
        }

        driveItem.setExpanded(true);
        TreeItem<File> matched = walkDownPath(driveItem, targetPath);
        if (matched == null) matched = driveItem;

        final TreeItem<File> selectTarget = matched;
        Platform.runLater(() -> {
            int row = directoryTree.getRow(selectTarget);
            if (row >= 0) {
                directoryTree.scrollTo(row);
                directoryTree.getSelectionModel().select(selectTarget);
            }
        });
        addressField.setText(targetDir.getAbsolutePath());
        loadImagesFromDirectory(targetDir);
    }

    private TreeItem<File> findDriveRoot(TreeItem<File> root, String targetPath) {
        root.setExpanded(true);
        var children = root.getChildren();
        for (TreeItem<File> child : children) {
            File val = child.getValue();
            if (val != null) {
                String childPath = normalizePath(val.getAbsolutePath());
                if (targetPath.startsWith(childPath)) return child;
            }
        }
        return null;
    }

    private String normalizePath(String path) {
        String p = path.replace('/', '\\').toLowerCase();
        if (!p.endsWith("\\")) p = p + "\\";
        if (p.length() >= 2 && p.charAt(1) == ':' && p.length() == 2) p = p + "\\";
        return p;
    }

    private TreeItem<File> walkDownPath(TreeItem<File> start, String targetPath) {
        File startVal = start.getValue();
        String startPath = startVal != null ? normalizePath(startVal.getAbsolutePath()) : "";
        if (!targetPath.startsWith(startPath)) return null;

        String remaining = targetPath.substring(startPath.length()).replace("\\", "\\").trim();
        if (remaining.isEmpty() || remaining.equals("\\")) return start;

        String[] parts = remaining.split("\\\\");
        List<String> segments = new ArrayList<>();
        for (String s : parts) { if (!s.isEmpty()) segments.add(s); }

        TreeItem<File> current = start;
        current.setExpanded(true);

        for (String segment : segments) {
            TreeItem<File> found = null;
            current.setExpanded(true);
            var children = current.getChildren();
            for (TreeItem<File> child : children) {
                File childVal = child.getValue();
                if (childVal != null && childVal.getName().equalsIgnoreCase(segment)) {
                    found = child;
                    break;
                }
            }
            if (found == null) {
                return current;
            }
            found.setExpanded(true);
            current = found;
        }
        return current;
    }

    private HBox createToolbar() {
        Button slideshowBtn = createModernButton("Slideshow", ACCENT, true);
        slideshowBtn.setOnAction(e -> openSlideshow(0));

        Button refreshBtn = createModernButton("Refresh", BG_MAIN, false);
        refreshBtn.setOnAction(e -> { if (currentDirectory != null) loadImagesFromDirectory(currentDirectory); });

        Button selectAllBtn = createModernButton("Select All", BG_MAIN, false);
        selectAllBtn.setOnAction(e -> selectAllImages());

        Button clearBtn = createModernButton("Clear", BG_MAIN, false);
        clearBtn.setOnAction(e -> clearSelection());

        return new HBox(8, slideshowBtn, new Separator(Orientation.VERTICAL), refreshBtn, selectAllBtn, clearBtn);
    }

    private void selectAllImages() {
        for (var node : thumbnailPane.getChildren()) {
            if (node instanceof VBox && !selectedNodes.contains(node)) {
                selectedNodes.add((VBox) node);
                applySelectedStyle((VBox) node);
            }
        }
        updateStatusAfterSelection();
    }

    // ==================== Directory Tree (Enhanced Visuals) ====================

    private void initDirectoryTree() {
        TreeItem<File> rootNode = new TreeItem<>(new File("This PC"));
        rootNode.setExpanded(true);

        for (File drive : File.listRoots()) {
            rootNode.getChildren().add(createLazyTreeItem(drive));
        }

        directoryTree.setRoot(rootNode);
        directoryTree.setShowRoot(false);
        directoryTree.setCellFactory(tv -> new BeautifulTreeCell());

        applyTreeViewStyle(directoryTree);

        directoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.getValue().isDirectory()) {
                addressField.setText(newVal.getValue().getAbsolutePath());
                loadImagesFromDirectory(newVal.getValue());
            }
        });
    }

    private void applySidebarScrollStyle(ScrollPane sp) {
        sp.setStyle(
            "-fx-background: " + BG_SIDEBAR + ";" +
            "-fx-background-color: " + BG_SIDEBAR + ";" +
            "-fx-padding: 0;"
        );
    }

    private void applyTreeViewStyle(TreeView<?> tree) {
        tree.setStyle("-fx-background-color: transparent; -fx-selection-bar: " + PRIMARY + "; -fx-selection-bar-non-focused: " + PRIMARY_LIGHT + ";");
    }

    // ==================== Thumbnail Area ====================

    private void initThumbnailArea() {
        thumbnailPane = new TilePane();
        thumbnailPane.setPadding(new Insets(18));
        thumbnailPane.setHgap(16);
        thumbnailPane.setVgap(16);
        thumbnailPane.setPrefColumns(5);
        thumbnailPane.setStyle("-fx-background-color: " + BG_MAIN + ";");

        StackPane thumbnailContainer = new StackPane(thumbnailPane);
        setupSelectionRectangle(thumbnailContainer);

        ScrollPane scrollPane = new ScrollPane(thumbnailContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: " + BG_MAIN + "; -fx-background-color: " + BG_MAIN + ";");

        thumbnailPane.setOnContextMenuRequested(e -> {
            if (!selectedNodes.isEmpty()) {
                ContextMenu ctxMenu = createContextMenu();
                ctxMenu.show(thumbnailPane, e.getScreenX(), e.getScreenY());
            }
        });

        rightPane.setCenter(scrollPane);
    }

    // ==================== Modern Component Styles ====================

    private void applyToolbarTitleStyle(Label label) {
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-family: 'Segoe UI', sans-serif;");
    }

    private void applyCountBadgeStyle(Label label) {
        label.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 10; -fx-cursor: default;");
        label.setVisible(false);
    }

    private void applyStatusBadgeStyle(Label label) {
        label.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 10; -fx-cursor: default;");
        label.setVisible(false);
    }

    private Button createModernButton(String text, String color, boolean isPrimary) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + "; -fx-text-fill: " + (color.equals(BG_MAIN) ? TEXT_PRIMARY : "white") + ";" +
            "-fx-font-weight: 600; -fx-font-size: 11px; -fx-font-family: 'Segoe UI', sans-serif;" +
            "-fx-padding: 7 16; -fx-cursor: hand; -fx-background-radius: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 4, 0, 0, 1);"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(color, darkenColor(color)).replace("0.08", "0.15")));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + color + "; -fx-text-fill: " + (color.equals(BG_MAIN) ? TEXT_PRIMARY : "white") + ";" +
            "-fx-font-weight: 600; -fx-font-size: 11px; -fx-font-family: 'Segoe UI', sans-serif;" +
            "-fx-padding: 7 16; -fx-cursor: hand; -fx-background-radius: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 4, 0, 0, 1);"
        ));
        return btn;
    }

    // ==================== Thumbnail Card Styles ====================

    private void applyNormalStyle(VBox node) {
        node.setStyle(
            "-fx-background-color: " + BG_CARD + "; -fx-background-radius: 10;" +
            "-fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 10;" +
            "-fx-padding: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 3);"
        );
    }

    private void applySelectedStyle(VBox node) {
        node.setStyle(
            "-fx-background-color: " + BG_CARD + "; -fx-background-radius: 10;" +
            "-fx-border-color: " + PRIMARY + "; -fx-border-width: 2.5; -fx-border-radius: 10;" +
            "-fx-padding: 8;" +
            "-fx-effect: dropshadow(three-pass-box, " + hexToRgba(PRIMARY, 0.3) + ", 14, 0, 0, 5);"
        );
    }

    private void applyHoverStyle(VBox node) {
        node.setStyle(
            "-fx-background-color: " + BG_CARD + "; -fx-background-radius: 10;" +
            "-fx-border-color: " + PRIMARY_LIGHT + "; -fx-border-width: 1.5; -fx-border-radius: 10;" +
            "-fx-padding: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 14, 0, 0, 5);"
        );
    }

    // ==================== Selection Logic ====================

    private void handleMousePressForSelection(MouseEvent e) {
        Object target = e.getTarget();
        if (target == e.getSource() || target instanceof TilePane || target instanceof Pane || target instanceof StackPane) {
            isSelecting = true; startX = e.getX(); startY = e.getY();
            selectionRectangle.setX(startX); selectionRectangle.setY(startY);
            selectionRectangle.setWidth(0); selectionRectangle.setHeight(0);
            selectionRectangle.setVisible(true);
            if (!e.isControlDown()) clearSelection();
        }
    }
    private void handleMouseDragForSelection(MouseEvent e) {
        if (isSelecting) {
            double x = Math.min(startX, e.getX()), y = Math.min(startY, e.getY());
            selectionRectangle.setX(x); selectionRectangle.setY(y);
            selectionRectangle.setWidth(Math.abs(e.getX()-startX)); selectionRectangle.setHeight(Math.abs(e.getY()-startY));
        }
    }
    private void handleMouseReleaseForSelection(MouseEvent e) {
        if (isSelecting) { isSelecting = false; selectionRectangle.setVisible(false); selectInRect(selectionRectangle.getBoundsInParent(), e.isControlDown()); }
    }
    private void selectInRect(Bounds rect, boolean add) {
        if (!add) clearSelection();
        for (var node : thumbnailPane.getChildren()) {
            if (node instanceof VBox) {
                VBox box = (VBox)node;
                Bounds bInScene = box.localToScene(box.getBoundsInLocal());
                Bounds rInScene = thumbnailPane.localToScene(new BoundingBox(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight()));
                if (rInScene.intersects(bInScene) && !selectedNodes.contains(box)) { selectedNodes.add(box); applySelectedStyle(box); }
            }
        }
        updateStatusAfterSelection();
    }
    private void handleSelection(VBox node, boolean ctrl) {
        if (!ctrl) clearSelection();
        if (selectedNodes.contains(node) && ctrl) { selectedNodes.remove(node); applyNormalStyle(node); }
        else { selectedNodes.add(node); applySelectedStyle(node); }
        updateStatusAfterSelection();
    }
    private void clearSelection() {
        for (VBox n : selectedNodes) applyNormalStyle(n);
        selectedNodes.clear(); updateStatusAfterSelection();
    }

    // ==================== Lazy Loading Tree ====================

    private TreeItem<File> createLazyTreeItem(File dir) {
        return new TreeItem<File>(dir) {
            private boolean isLeaf, ftc=true, ftl=true;
            @Override public boolean isLeaf() { if (ftl){ftl=false;isLeaf=getValue().isFile();}return isLeaf;}
            @Override public javafx.collections.ObservableList<TreeItem<File>> getChildren(){
                if(ftc){ftc=false;super.getChildren().setAll(buildChildren(this));}return super.getChildren();}
        };
    }
    private List<TreeItem<File>> buildChildren(TreeItem<File> p){
        File d=p.getValue();if(d!=null&&d.isDirectory()){
            File[]fs=d.listFiles(f->f.isDirectory()&&!f.isHidden());if(fs!=null){
                Arrays.sort(fs,Comparator.comparing(File::getName,String.CASE_INSENSITIVE_ORDER));
                return Arrays.stream(fs).map(this::createLazyTreeItem).collect(Collectors.toList());
            }
        }return Collections.emptyList();
    }

    // ==================== Image Loading ====================

    private boolean isImageFile(File f){String n=f.getName().toLowerCase();for(String e:SUPPORTED_EXTENSIONS)if(n.endsWith(e))return true;return false;}

    private void loadImagesFromDirectory(File dir){
        currentDirectory=dir;thumbnailPane.getChildren().clear();currentImages.clear();clearSelection();currentImageIndex=-1;
        File[]files=dir.listFiles(this::isImageFile);if(files==null){statusLabel.setText("Cannot access this directory");directoryInfoLabel.setText(dir.getName());return;}
        Arrays.sort(files,Comparator.comparing(File::getName,String.CASE_INSENSITIVE_ORDER));long total=0;int cnt=0;
        for(File file:files){
            try{currentImages.add(file);total+=file.length();thumbnailPane.getChildren().add(createThumbnailNode(file));cnt++;}
            catch(Exception ex){System.err.println("Cannot load: "+file.getName());}
        }
        directoryInfoLabel.setText(dir.getName());
        imageCountBadge.setText(cnt+" photos");imageCountBadge.setVisible(true);
        statusLabel.setText(String.format("%d images loaded | Total: %s",cnt,formatFileSize(total)));
    }

    private Image createHighQualityImage(File f,double w,double h){return new Image(f.toURI().toString(),w,h,true,true,true);}

    private VBox createThumbnailNode(File file){
        VBox box=new VBox(5);box.setAlignment(Pos.CENTER);box.setPadding(new Insets(8));box.setUserData(file);applyNormalStyle(box);
        Image img=createHighQualityImage(file,THUMBNAIL_SIZE,THUMBNAIL_SIZE);
        ImageView iv=new ImageView(img);iv.setPreserveRatio(true);iv.setSmooth(true);iv.setCache(true);iv.setFitWidth(THUMBNAIL_SIZE);iv.setFitHeight(THUMBNAIL_SIZE);
        iv.setStyle("-fx-clip-shape: rectangle(0,0,"+(int)(THUMBNAIL_SIZE)+","+(int)(THUMBNAIL_SIZE-8)+",8);");
        Label nameLbl=new Label(file.getName());nameLbl.setPrefWidth(140);nameLbl.setMaxWidth(140);
        nameLbl.setStyle("-fx-alignment:center;-fx-text-overrun:ellipsis;-fx-font-size:11px;-fx-font-weight:500;-fx-text-fill:"+TEXT_PRIMARY+";");
        Label sizeLbl=new Label(formatFileSize(file.length()));sizeLbl.setStyle("-fx-alignment:center;-fx-font-size:10px;-fx-text-fill:"+TEXT_SECONDARY+";");
        box.getChildren().addAll(iv,nameLbl,sizeLbl);setupThumbnailEvents(box,file);return box;
    }
    private void setupThumbnailEvents(VBox box,File file){
        box.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY){if(e.getClickCount()==2)openSlideshow(currentImages.indexOf(file));else{handleSelection(box,e.isControlDown());currentImageIndex=currentImages.indexOf(file);}}});
        box.setOnMouseEntered(e->{if(!selectedNodes.contains(box))applyHoverStyle(box);});
        box.setOnMouseExited(e->{if(!selectedNodes.contains(box))applyNormalStyle(box);});
    }

    private void navigateImage(int direction){
        if(currentImages.isEmpty())return;
        if(currentImageIndex<0)currentImageIndex=0;
        int newIndex=Math.max(0,Math.min(currentImages.size()-1,currentImageIndex+direction));
        if(newIndex==currentImageIndex)return;
        clearSelection();
        currentImageIndex=newIndex;
        var children=thumbnailPane.getChildren();
        if(newIndex<children.size()){
            VBox target=(VBox)children.get(newIndex);
            selectedNodes.add(target);
            applySelectedStyle(target);
            target.requestFocus();
            updateStatusAfterSelection();
        }
    }

    // ==================== Status Updates ====================

    private void updateStatusAfterSelection(){
        if(selectedNodes.isEmpty()&&currentDirectory!=null){statusLabel.setText(currentImages.size()+" images in folder | No selection");selectedCountLabel.setVisible(false);}
        else if(!selectedNodes.isEmpty()){
            long sz=selectedNodes.stream().mapToLong(n->((File)n.getUserData()).length()).sum();
            statusLabel.setText(String.format("%d selected | %s",selectedNodes.size(),formatFileSize(sz)));
            selectedCountLabel.setText(selectedNodes.size()+" Selected");selectedCountLabel.setVisible(true);}
    }

    // ==================== Context Menu ====================

    private ContextMenu createContextMenu(){ContextMenu m=new ContextMenu();MenuItem c=new MenuItem("Copy"),p=new MenuItem("Paste"),r=new MenuItem("Rename"),d=new MenuItem("Delete");
        c.setOnAction(e->copySelectedFiles());p.setOnAction(e->pasteFiles());r.setOnAction(e->renameFiles());d.setOnAction(e->deleteFiles());
        m.getItems().addAll(c,p,new SeparatorMenuItem(),r,new SeparatorMenuItem(),d);return m;}

    // ==================== File Operations ====================

    private void copySelectedFiles(){if(selectedNodes.isEmpty()){showAlert("Info","Select images first!");return;}clipboardFiles=selectedNodes.stream().map(n->(File)n.getUserData()).collect(Collectors.toList());statusLabel.setText("Copied "+clipboardFiles.size()+" images");}
    private void deleteFiles(){if(selectedNodes.isEmpty()){showAlert("Info","Select images first!");return;}Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Delete "+selectedNodes.size()+" images?",ButtonType.YES,ButtonType.NO);a.setTitle("Confirm Delete");a.setHeaderText(null);Optional<ButtonType>r=a.showAndWait();if(r.isPresent()&&r.get()==ButtonType.YES){int ok=0;for(VBox n:selectedNodes)if(((File)n.getUserData()).delete())ok++;statusLabel.setText("Deleted: "+ok+"/"+selectedNodes.size());loadImagesFromDirectory(currentDirectory);}}
    private void pasteFiles(){if(clipboardFiles.isEmpty()){showAlert("Info","Clipboard empty!");return;}if(currentDirectory==null){showAlert("Info","Select target!");return;}int ok=0;for(File src:clipboardFiles){File dest=resolveDest(src);try{Files.copy(src.toPath(),dest.toPath(),StandardCopyOption.COPY_ATTRIBUTES);ok++;}catch(IOException ex){ex.printStackTrace();}}statusLabel.setText("Pasted: "+ok+" images");loadImagesFromDirectory(currentDirectory);}
    private File resolveDest(File src){File d=new File(currentDirectory,src.getName());int c=1;String base=getBaseName(src.getName()),ext=getExtension(src.getName());while(d.exists())d=new File(currentDirectory,base+"_copy("+(c++)+")"+ext);return d;}
    private void renameFiles(){if(selectedNodes.isEmpty()){showAlert("Info","Select first!");return;}List<File>list=selectedNodes.stream().map(n->(File)n.getUserData()).collect(Collectors.toList());if(list.size()==1)showSingleRename(list.get(0));else showBatchRename(list);}
    private void showSingleRename(File f){String cur=getBaseName(f.getName());TextInputDialog d=new TextInputDialog(cur);d.setTitle("Rename");d.setHeaderText("New name:");d.showAndWait().ifPresent(nm->{if(nm.trim().isEmpty()||nm.equals(cur)){showAlert("Info","Invalid!");return;}File nf=new File(f.getParent(),nm.trim()+getExtension(f.getName()));if(nf.exists()){showAlert("Error","Exists!");return;}if(f.renameTo(nf)){statusLabel.setText("Renamed: "+nm);loadImagesFromDirectory(currentDirectory);}else showAlert("Error","Failed!");});}
    private void showBatchRename(List<File> files){Dialog<String[]>dlg=new Dialog<>();dlg.setTitle("Batch Rename");dlg.setHeaderText("Rename "+files.size()+" files");GridPane g=new GridPane(15,12);g.setPadding(new Insets(20));TextField pre=new TextField("NewName"),st=new TextField("1"),pd=new TextField("4");g.addRow(0,new Label("Prefix:"),pre);g.addRow(1,new Label("Start#:"),st);g.addRow(2,new Label("Digits:"),pd);Label pv=new Label("Preview: NewName0001.jpg...");pv.setStyle("-fx-text-fill:"+TEXT_SECONDARY+";-fx-font-style:italic;");g.addRow(3,pv,pv);pre.textProperty().addListener((o,a,b)->updatePreview(pv,b,st.getText(),pd.getText()));st.textProperty().addListener((o,a,b)->updatePreview(pv,pre.getText(),b,pd.getText()));pd.textProperty().addListener((o,a,b)->updatePreview(pv,pre.getText(),st.getText(),b));dlg.getDialogPane().setContent(g);dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);dlg.setResultConverter(b->b==ButtonType.OK?new String[]{pre.getText(),st.getText(),pd.getText()}:null);dlg.showAndWait().ifPresent(r->execBatchRename(files,r));}
    private void execBatchRename(List<File>fs,String[] p){try{String pr=p[0].isEmpty()?"Image":p[0];int s=Integer.parseInt(p[1].isEmpty()?"1":p[1]),n=Integer.parseInt(p[2].isEmpty()?"4":p[2]);String fmt="%0"+Math.max(n,1)+"d";int ok=0;for(File f:fs){String ext=getExtension(f.getName()),nm=pr+String.format(fmt,s++)+ext;File nf=new File(f.getParent(),nm);if(!nf.exists()&&f.renameTo(nf))ok++;}statusLabel.setText("Renamed: "+ok+"/"+fs.size());loadImagesFromDirectory(currentDirectory);}catch(NumberFormatException ex){showAlert("Error","Invalid numbers!");}}
    private void updatePreview(Label l,String pre,String s,String p){try{int si=s.isEmpty()?1:Integer.parseInt(s),pi=p.isEmpty()?4:Integer.parseInt(p);String f="%0"+Math.max(pi,1)+"d",pr=pre.isEmpty()?"NewName":pre;l.setText("Preview: "+pr+String.format(f,si)+".jpg, "+pr+String.format(f,si+1)+".jpg...");}catch(Exception ex){l.setText("Preview: [enter valid numbers]");}}

    // ==================== Slideshow ====================

    private void openSlideshow(int idx){if(currentImages.isEmpty()){showAlert("Info","No images!");return;}Stage stage=new Stage();stage.setTitle("Slideshow - "+currentDirectory.getName());stage.initOwner(directoryTree.getScene().getWindow());AtomicInteger ci=new AtomicInteger(Math.max(0,Math.min(idx,currentImages.size()-1)));AtomicReference<Double>zoom=new AtomicReference<>(1.0);AtomicReference<Double>interval=new AtomicReference<>(DEFAULT_SLIDESHOW_INTERVAL);ImageView iv=new ImageView();iv.setPreserveRatio(true);iv.setSmooth(true);iv.setCache(true);iv.setFitWidth(SLIDEVIEW_WIDTH);iv.setFitHeight(SLIDEVIEW_HEIGHT);iv.setStyle("-fx-background-color:#0F172A;");Label info=createSlideInfoLabel();Runnable upd=()->{if(currentImages.isEmpty())return;File f=currentImages.get(ci.get());Image img=createHighQualityImage(f,1920,1440);iv.setImage(img);zoom.set(1.0);iv.setScaleX(1);iv.setScaleY(1);info.setText(String.format("%s (%dx%d)|%s|%d/%d",f.getName(),(int)img.getWidth(),(int)img.getHeight(),formatFileSize(f.length()),ci.get()+1,currentImages.size()));};upd.run();Timeline tl=createTimeline(ci,upd,interval);HBox bar=createSlideControls(iv,ci,zoom,interval,tl,upd,info);BorderPane sp=new BorderPane();StackPane center=new StackPane(iv,info);center.setStyle("-fx-background-color:linear-gradient(135deg,#0F172A 0%,#1E293B 50%,#334155 100%);");StackPane.setAlignment(info,Pos.BOTTOM_LEFT);sp.setCenter(center);sp.setBottom(bar);Scene sc=new Scene(sp,1050,780);stage.setScene(sc);setupSlideKeys(sc,ci,upd,tl);setupSlideScroll(iv,zoom,info);stage.setOnCloseRequest(e->tl.stop());stage.show();}
    private Label createSlideInfoLabel(){Label l=new Label();l.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-family:'Consolas','Monaco',monospace;-fx-padding:8 14;-fx-background-color:rgba(0,0,0,0.75);-fx-background-radius:6;");return l;}
    private Timeline createTimeline(AtomicInteger ci,Runnable u,AtomicReference<Double>ir){return new Timeline(new KeyFrame(Duration.seconds(ir.get()),e->{ci.set((ci.get()+1)%currentImages.size());u.run();}));}
    private HBox createSlideControls(ImageView iv,AtomicInteger ci,AtomicReference<Double>zl,AtomicReference<Double>ir,Timeline tl,Runnable u,Label info){Button prev=createSlideBtn("\u25C0","Prev","#60A5FA"),next=createSlideBtn("\u25B6","Next","#60A5FA"),zin=createSlideBtn("+","Zoom","#34D399"),zout=createSlideBtn("-","Zoom","#34D399"),fit=createSlideBtn("\u2630","Fit","#A78BFA"),play=createSlideBtn("\u25B6","Play","#FBBF24");Spinner<Double>sp=new Spinner<>(0.5,10.0,ir.get(),0.5);sp.setEditable(true);sp.setPrefWidth(70);sp.valueProperty().addListener((o,a,nv)->{ir.set(nv);if(tl.getStatus()==Animation.Status.RUNNING){tl.stop();createTimeline(ci,u,ir).play();}});HBox ibox=new HBox(5,new Label("Interval:"),sp,new Label("s"));ibox.setAlignment(Pos.CENTER);ibox.setStyle("-fx-padding:6 12;-fx-background-color:rgba(255,255,255,0.08);-fx-background-radius:6;");prev.setOnAction(e->navPrev(ci,u));next.setOnAction(e->navNext(ci,u));zin.setOnAction(e->zoomIn(zl,iv,info));zout.setOnAction(e->zoomOut(zl,iv,info));fit.setOnAction(e->{zl.set(1.0);iv.setScaleX(1);iv.setScaleY(1);iv.setFitWidth(SLIDEVIEW_WIDTH);iv.setFitHeight(SLIDEVIEW_HEIGHT);});play.setOnAction(e->togglePlay(tl,play));HBox bar=new HBox(10,prev,next,new Separator(Orientation.VERTICAL),zin,zout,fit,new Separator(Orientation.VERTICAL),ibox,new Separator(Orientation.VERTICAL),play);bar.setAlignment(Pos.CENTER);bar.setPadding(new Insets(12,20,12,20));bar.setStyle("-fx-background-color:#1E293B;");return bar;}
    private Button createSlideBtn(String text,String tip,String col){Button b=new Button(text+" "+tip);b.setStyle("-fx-background-color:"+col+";-fx-text-fill:#0F172A;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:7 16;-fx-cursor:hand;-fx-background-radius:6;");b.setOnMouseEntered(e->b.setStyle(b.getStyle().replace(col,darkenColor(col))));b.setOnMouseExited(e->b.setStyle("-fx-background-color:"+col+";-fx-text-fill:#0F172A;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:7 16;-fx-cursor:hand;-fx-background-radius:6;"));return b;}
    private void navPrev(AtomicInteger i,Runnable u){if(i.get()==0)showAlert("Info","First!");else{i.decrementAndGet();u.run();}}
    private void navNext(AtomicInteger i,Runnable u){if(i.get()>=currentImages.size()-1)showAlert("Info","Last!");else{i.incrementAndGet();u.run();}}
    private void zoomIn(AtomicReference<Double>z,ImageView iv,Label l){double nz=Math.min(z.get()+ZOOM_STEP,MAX_ZOOM);z.set(nz);iv.setScaleX(nz);iv.setScaleY(nz);}
    private void zoomOut(AtomicReference<Double>z,ImageView iv,Label l){double nz=Math.max(z.get()-ZOOM_STEP,MIN_ZOOM);z.set(nz);iv.setScaleX(nz);iv.setScaleY(nz);}
    private void togglePlay(Timeline tl,Button b){if(tl.getStatus()==Animation.Status.RUNNING){tl.stop();b.setText("\u25B6 Play");b.setStyle("-fx-background-color:#FBBF24;-fx-text-fill:#0F172A;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:7 16;-fx-cursor:hand;-fx-background-radius:6;");}else{tl.play();b.setText("\u23F8 Stop");b.setStyle("-fx-background-color:"+ACCENT+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:7 16;-fx-cursor:hand;-fx-background-radius:6;");}}
    private void setupSlideScroll(ImageView iv,AtomicReference<Double>zl,Label il){iv.setOnScroll(e->{if(e.isControlDown()){if(e.getDeltaY()>0)zoomIn(zl,iv,il);else if(e.getDeltaY()<0)zoomOut(zl,iv,il);e.consume();}});}
    private void setupSlideKeys(Scene s,AtomicInteger ci,Runnable u,Timeline tl){s.addEventHandler(KeyEvent.KEY_PRESSED,e->{switch(e.getCode()){case LEFT:case UP:if(ci.get()>0){ci.decrementAndGet();u.run();}break;case RIGHT:case DOWN:case SPACE:if(ci.get()<currentImages.size()-1){ci.incrementAndGet();u.run();}break;case ESCAPE:((Stage)s.getWindow()).close();break;default:break;}});}

    // ==================== Utility Methods ====================

    private String formatFileSize(long s){if(s<1024)return s+" B";if(s<1024*1024)return String.format("%.1f KB",s/1024.0);if(s<1024*1024*1024)return String.format("%.1f MB",s/(1024.0*1024.0));return String.format("%.1f GB",s/(1024.0*1024.0*1024.0));}
    private String getBaseName(String f){int i=f.lastIndexOf('.');return i>0?f.substring(0,i):f;}
    private String getExtension(String f){int i=f.lastIndexOf('.');return i>0?f.substring(i):"";}
    private String darkenColor(String h){return h.equals(PRIMARY)?"#4F46E5":h.equals(ACCENT)?"#E11D48":h.equals(SUCCESS)?"#059669":h.equals(WARNING)?"#D97706":h.equals(BG_MAIN)?"#E2E8F0":"#333";}
    private String hexToRgba(String hex,double alpha){return"rgba("+Integer.parseInt(hex.substring(1,3),16)+","+Integer.parseInt(hex.substring(3,5),16)+","+Integer.parseInt(hex.substring(5,7),16)+","+alpha+")";}
    private ImageView createInfoIcon(){ImageView iv=new ImageView();iv.setFitHeight(14);iv.setFitWidth(14);iv.setStyle("-fx-opacity:0.5;");return iv;}
    private void showAlert(String t,String c){Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle(t);a.setHeaderText(null);a.setContentText(c);a.showAndWait();}
    private void setupSelectionRectangle(StackPane c){selectionRectangle=new Rectangle();selectionRectangle.setFill(Color.color(0.39,0.37,0.95,0.12));selectionRectangle.setStroke(Color.color(0.39,0.37,0.95,0.75));selectionRectangle.getStrokeDashArray().addAll(8.0,4.0);selectionRectangle.setVisible(false);c.getChildren().add(selectionRectangle);c.setOnMousePressed(this::handleMousePressForSelection);c.setOnMouseDragged(this::handleMouseDragForSelection);c.setOnMouseReleased(this::handleMouseReleaseForSelection);}
    private void setupKeyboardShortcuts(Scene sc){if(sc==null)return;sc.addEventFilter(KeyEvent.KEY_PRESSED,e->{if(addressField.isFocused()&&e.getCode()!=KeyCode.ESCAPE)return;switch(e.getCode()){case DELETE:deleteFiles();e.consume();break;case C:if(e.isControlDown()){copySelectedFiles();e.consume();}break;case V:if(e.isControlDown()){pasteFiles();e.consume();}break;case LEFT:navigateImage(-1);e.consume();break;case RIGHT:navigateImage(1);e.consume();break;default:break;}});}
    private void showWelcomeMessage(){Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle("Welcome to Photo Manager Pro");a.setHeaderText("Modern Photo Management Application");a.setContentText("\nFeatures:\n Browse disk directories from the beautiful sidebar\n Address bar: type any path and press Enter to jump\n Folder picker button to visually select a folder\n View beautiful image thumbnails in card layout\n Click to select, Ctrl+Click for multi-select\n Drag to box-select multiple images\n Double-click to enter slideshow mode\n Right-click for Copy/Paste/Rename/Delete\n\nKeyboard Shortcuts:\n Left/Right   = Navigate previous/next image\n Delete key   = Delete selected images\n Ctrl+C       = Copy images\n Ctrl+V       = Paste images\n\nAddress Bar:\n Enter Windows path (e.g. C:\\Users\\Pictures)\n Supports both folder and file paths\n Press Enter or click arrow button to navigate\n Auto-fills when clicking tree nodes\n\nSlideshow Controls:\n Ctrl+Wheel  = Zoom in/out\n Adjustable auto-play interval\n High-quality rendering\n\nSupported formats: JPG, JPEG, GIF, PNG, BMP");a.showAndWait();}

    // ==================== Inner Classes ====================

    /**
     * Enhanced TreeCell with icons and beautiful styling.
     * Shows folder icon for directories, highlights on hover/selection.
     */
    private static class BeautifulTreeCell extends TreeCell<File>{
        public BeautifulTreeCell(){
            setContentDisplay(ContentDisplay.LEFT);
            setStyle("-fx-background-color:transparent;");
        }

        @Override protected void updateItem(File item,boolean empty){
            super.updateItem(item,empty);
            if(empty||item==null){
                setText(null);setGraphic(null);setStyle("-fx-background-color:transparent;-fx-text-fill:"+TEXT_SIDEBAR_DIM+";");
            }else{
                String displayName=item.getName().isEmpty()?item.getAbsolutePath():item.getName();
                setText(displayName);
                if(item.isDirectory()){
                    boolean expanded=getTreeItem()!=null&&getTreeItem().isExpanded();
                    Label icon=new Label(expanded?"\uD83D\uDCC2":"\uD83D\uDCC1");
                    icon.setStyle("-fx-font-size:14px;-fx-text-fill:white;-fx-padding:0 2 0 0;");
                    setGraphic(icon);
                    setStyle("-fx-background-color:transparent;-fx-text-fill:white;-fx-font-size:13px;-fx-font-family:'Segoe UI',sans-serif;-fx-font-weight:500;");
                }else{
                    Label icon=new Label("\uD83D\uDCCE");
                    icon.setStyle("-fx-font-size:12px;-fx-text-fill:"+TEXT_SIDEBAR_DIM+";-fx-padding:0 2 0 0;");
                    setGraphic(icon);
                    setStyle("-fx-background-color:transparent;-fx-text-fill:"+TEXT_SIDEBAR_DIM+";-fx-font-size:13px;-fx-font-family:'Segoe UI',sans-serif;");
                }
                setupHoverEffects(item.isDirectory());
            }
        }

        private void setupHoverEffects(boolean isDir){
            setOnMouseEntered(e->{
                if(!isSelected()) setStyle("-fx-background-color:"+BG_SIDEBAR_HOVER+";-fx-background-radius:6;-fx-text-fill:white;"+(isDir?"-fx-font-weight:500;":""));
            });
            setOnMouseExited(e->{
                if(isDir) setStyle("-fx-background-color:transparent;-fx-text-fill:white;-fx-font-size:13px;-fx-font-family:'Segoe UI',sans-serif;-fx-font-weight:500;");
                else setStyle("-fx-background-color:transparent;-fx-text-fill:"+TEXT_SIDEBAR_DIM+";-fx-font-size:13px;-fx-font-family:'Segoe UI',sans-serif;");
            });
        }
    }
}
