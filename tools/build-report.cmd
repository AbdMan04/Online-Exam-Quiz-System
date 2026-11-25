@echo off
rem Builds docs\report\SE430-Online-Quiz-System-Test-Report.pdf from fresh test results.
rem Usage (from any folder):  tools\build-report.cmd
setlocal
cd /d "%~dp0.."

echo [1/3] Running all tests (coverage report + test case tables)...
call .\mvnw.cmd -q -B clean test
if errorlevel 1 (
    echo Tests failed - report not built.
    exit /b 1
)

echo [2/3] Generating the HTML report...
java tools\ReportGenerator.java
if errorlevel 1 exit /b 1

echo [3/3] Printing the PDF with Microsoft Edge...
set "EDGE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE%" set "EDGE=%ProgramFiles%\Microsoft\Edge\Application\msedge.exe"
if not exist docs\report mkdir docs\report
set "HTML=%CD%\target\report\report.html"
"%EDGE%" --headless=new --disable-gpu --no-pdf-header-footer --user-data-dir="%TEMP%\quiz-report-edge" ^
    --print-to-pdf="%CD%\docs\report\SE430-Online-Quiz-System-Test-Report.pdf" "file:///%HTML:\=/%" >nul 2>&1

echo Done: docs\report\SE430-Online-Quiz-System-Test-Report.pdf
