@echo off
echo ========================================================
echo   Building Minecraft Paper Plugin
echo ========================================================
echo.

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven (mvn) not found in PATH!
    echo Please install Apache Maven or run within an environment with Maven and JDK configured.
    pause
    exit /b 1
)

mvn clean package
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================================
    echo   [SUCCESS] Build complete!
    echo   JAR output is located in the target/ folder.
    echo ========================================================
) else (
    echo.
    echo [ERROR] Build failed! Check compiler output above.
)

pause
