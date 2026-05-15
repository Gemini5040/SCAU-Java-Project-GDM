@echo off
chcp 65001 >nul
title Photo Manager App - JavaFX Launcher

cd /d "%~dp0"

echo ============================================
echo   Photo Manager App - JavaFX Launcher
echo ============================================
echo.

if not exist "target\classes" mkdir "target\classes"
if not exist "lib" (
    echo [ERROR] lib directory not found!
    echo Please run run.ps1 first to download JavaFX libraries.
    pause
    exit /b 1
)

echo [1/2] Compiling...
javac -encoding UTF-8 -d target\classes -sourcepath src\main\java src\main\java\com\example\demo\PhotoManagerApp.java -cp "lib\javafx-base-21.0.2.jar;lib\javafx-graphics-21.0.2.jar;lib\javafx-controls-21.0.2.jar;lib\javafx-fxml-21.0.2.jar"

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)
echo [OK] Compiled successfully!
echo.
echo [2/2] Starting application...
echo.
java --module-path lib --add-modules javafx.controls,javafx.fxml -cp target\classes com.example.demo.PhotoManagerApp

echo.
echo [OK] Application exited.
pause
