@echo off
rem Builds the Android app, starts the phone emulator if no device is connected, installs and opens the app.
setlocal
if "%JAVA_HOME%"=="" set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-21.0.12.1+1"
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "EMULATOR=%ANDROID_HOME%\emulator\emulator.exe"
cd /d "%~dp0"

call gradlew.bat :androidApp:assembleDebug --console=plain || goto :error

"%ADB%" get-state 1>nul 2>nul
if not errorlevel 1 goto :install

echo No phone connected - starting the Android emulator...
set "AVD="
for /f "delims=" %%A in ('""%EMULATOR%" -list-avds"') do if not defined AVD set "AVD=%%A"
if not defined AVD (
    echo No emulator found. Create one with: android emulator create medium_phone
    goto :error
)
start "" "%EMULATOR%" -avd "%AVD%"
"%ADB%" wait-for-device

:boot
set "BOOTED="
for /f "delims=" %%B in ('""%ADB%" shell getprop sys.boot_completed" 2^>nul') do set "BOOTED=%%B"
if "%BOOTED%"=="1" goto :install
timeout /t 2 /nobreak >nul
goto :boot

:install
"%ADB%" install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk || goto :error
"%ADB%" shell am start -n app.lang/app.lang.android.MainActivity
exit /b 0

:error
pause
exit /b 1
