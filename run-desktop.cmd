@echo off
rem Starts the Windows app. Double-click this file.
setlocal
if "%JAVA_HOME%"=="" set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-21.0.12.1+1"
cd /d "%~dp0"
call gradlew.bat :desktopApp:run --console=plain
if errorlevel 1 pause
