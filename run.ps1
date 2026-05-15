# Photo Manager App Launcher (Final Version)
# Auto-downloads JavaFX Windows platform libraries and runs the app

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ProjectDir = $PSScriptRoot
if (-not $ProjectDir) { $ProjectDir = Get-Location }
$LibDir = Join-Path $ProjectDir "lib"
$SrcDir = Join-Path $ProjectDir "src\main\java"
$OutDir = Join-Path $ProjectDir "target\classes"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Photo Manager App - JavaFX Launcher" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $LibDir)) { New-Item -ItemType Directory -Path $LibDir | Out-Null }
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }

# JavaFX JAR files - using Windows platform versions
$javafxJars = @{
    "javafx-base-21.0.2.jar" = "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-win.jar"
    "javafx-graphics-21.0.2.jar" = "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-win.jar"
    "javafx-controls-21.0.2.jar" = "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-win.jar"
    "javafx-fxml-21.0.2.jar" = "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-win.jar"
}

# Minimum expected file sizes (bytes) to detect incomplete downloads
$minSizes = @{
    "javafx-base-21.0.2.jar" = 700000
    "javafx-graphics-21.0.2.jar" = 5000000
    "javafx-controls-21.0.2.jar" = 2000000
    "javafx-fxml-21.0.2.jar" = 100000
}

Write-Host "[1/4] Checking JavaFX libraries..." -NoNewline

$needDownload = $false
foreach ($jar in $javafxJars.Keys) {
    $filePath = Join-Path $LibDir $jar
    if (-not (Test-Path $filePath)) {
        $needDownload = $true
        break
    }
    # Check if file is too small (incomplete download)
    $size = (Get-Item $filePath).Length
    if ($size -lt $minSizes[$jar]) {
        Write-Host ""
        Write-Host "    [!] $jar is too small ($size bytes), re-downloading..." -ForegroundColor Yellow
        Remove-Item $filePath -Force
        $needDownload = $true
        break
    }
}

if ($needDownload) {
    Write-Host ""
    Write-Host ""
    Write-Host "[!] Downloading JavaFX Windows platform libraries..." -ForegroundColor Yellow
    Write-Host "    Total size: ~9 MB" -ForegroundColor Yellow
    Write-Host ""
    
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    foreach ($jar in $javafxJars.Keys) {
        $destPath = Join-Path $LibDir $jar
        if ((Test-Path $destPath) -and (Get-Item $destPath).Length -ge $minSizes[$jar]) {
            Write-Host "    Skip: $jar (exists)" -ForegroundColor Gray
            continue
        }
        
        Write-Host "    Downloading: $jar ... " -NoNewline
        try {
            $wc = New-Object System.Net.WebClient
            $wc.DownloadFile($javafxJars[$jar], $destPath)
            $actualSize = (Get-Item $destPath).Length
            if ($actualSize -lt $minSizes[$jar]) {
                throw "File too small: $actualSize bytes (expected > $($minSizes[$jar]) bytes)"
            }
            Write-Host "[OK] ($([math]::Round($actualSize/1KB, 1)) KB)" -ForegroundColor Green
        } catch {
            Write-Host "[FAILED]" -ForegroundColor Red
            Write-Host "    Error: $_" -ForegroundColor Red
            Read-Host "Press Enter to exit"
            exit 1
        }
    }
    Write-Host ""
    Write-Host "[OK] All JavaFX libraries downloaded!" -ForegroundColor Green
} else {
    Write-Host " [OK]" -ForegroundColor Green
}

Write-Host ""

# Compile
Write-Host "[2/4] Compiling project..." -NoNewline

$classpath = ($javafxJars.Keys | ForEach-Object { Join-Path $LibDir $_ }) -join ";"

$mainClass = Join-Path $SrcDir "com\example\demo\PhotoManagerApp.java"

$compileArgs = @(
    "-encoding", "UTF-8",
    "-d", $OutDir,
    "-sourcepath", $SrcDir,
    $mainClass,
    "-cp", $classpath
)

$compileOutput = & javac @compileArgs 2>&1 | Out-String

if ($LASTEXITCODE -ne 0) {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Host $compileOutput
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host " [OK]" -ForegroundColor Green
Write-Host ""

# Run with module path for JavaFX
Write-Host "[3/4] Starting application..." -ForegroundColor Green
Write-Host ""
Write-Host "----------------------------------------------" -ForegroundColor DarkGray

$runArgs = @(
    "--module-path", $LibDir,
    "--add-modules", "javafx.controls,javafx.fxml",
    "-cp", $OutDir,
    "com.example.demo.PhotoManagerApp"
)

& java @runArgs 2>&1

Write-Host ""
Write-Host "----------------------------------------------" -ForegroundColor DarkGray
Write-Host ""
Write-Host "[OK] Application exited normally" -ForegroundColor Green
Read-Host "Press Enter to close"
