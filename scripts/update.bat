@echo off
setlocal
set PACKAGE=%~1
set INSTALL_DIR=%~2
set PID=%~3
set STAGING=%TEMP%\retail-pos-update-%RANDOM%
set BACKUP=%INSTALL_DIR%.previous

:wait
tasklist /FI "PID eq %PID%" 2>NUL | find "%PID%" >NUL
if not errorlevel 1 (
    timeout /T 1 /NOBREAK >NUL
    goto wait
)

powershell -NoProfile -NonInteractive -Command "Expand-Archive -LiteralPath '%PACKAGE%' -DestinationPath '%STAGING%' -Force"
if errorlevel 1 exit /B 1
for /D %%D in ("%STAGING%\*") do set NEW_DIR=%%~fD
if not defined NEW_DIR exit /B 1
if exist "%BACKUP%" rmdir /S /Q "%BACKUP%"
move "%INSTALL_DIR%" "%BACKUP%"
if errorlevel 1 exit /B 1
move "%NEW_DIR%" "%INSTALL_DIR%"
if errorlevel 1 (
    move "%BACKUP%" "%INSTALL_DIR%"
    exit /B 1
)
rmdir /S /Q "%BACKUP%"
start "Retail POS" /B "%INSTALL_DIR%\start.bat"
rmdir /S /Q "%STAGING%"
