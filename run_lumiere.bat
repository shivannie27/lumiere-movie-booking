@echo off
title Lumiere Cinemas - Java Web App (Group 5)
cd /d "%~dp0"

echo ===================================================
echo   Lumiere Cinemas - Java-Powered Web Application
echo   Lead Architect and Backend: Kabir Ubale
echo ===================================================
echo.
echo Compiling code from all 4 teammates...
javac -encoding UTF-8 Kabir_Ubale/*.java Shaikh_Rahim/*.java Shruti_Wadhonkar/*.java Shivani_Dudhane/*.java
if errorlevel 1 (
    echo.
    echo [ERROR] Java compilation failed. Please make sure JDK 11+ is installed.
    pause
    exit /b
)
echo Compilation successful!
echo.
echo Starting Java Web Server...
echo (Your browser will open automatically)
echo Press Ctrl+C to stop the server anytime.
echo.
java -cp "Kabir_Ubale;Shaikh_Rahim;Shruti_Wadhonkar;Shivani_Dudhane;." App
pause
