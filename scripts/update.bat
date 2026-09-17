@echo off
setlocal
set PACKAGE=%~1
set INSTALL_DIR=%~2
set PID=%~3
set LOG_FILE=%INSTALL_DIR%.update.log
call :log Update started: package=%PACKAGE% install_dir=%INSTALL_DIR% pid=%PID%
set STAGING=%TEMP%\retail-pos-update-%RANDOM%
set BACKUP=%INSTALL_DIR%.previous

:wait
tasklist /FI "PID eq %PID%" 2>NUL | find "%PID%" >NUL
if not errorlevel 1 (
    timeout /T 1 /NOBREAK >>"%LOG_FILE%" 2>&1
    goto wait
)

powershell -NoProfile -NonInteractive -Command "Expand-Archive -LiteralPath '%PACKAGE%' -DestinationPath '%STAGING%' -Force" >>"%LOG_FILE%" 2>&1
if errorlevel 1 (call :log ERROR: Could not extract package & exit /B 1)
for /D %%D in ("%STAGING%\*") do set NEW_DIR=%%~fD
if not defined NEW_DIR (call :log ERROR: Package has no application directory & exit /B 1)
if exist "%BACKUP%" rmdir /S /Q "%BACKUP%"
move "%INSTALL_DIR%" "%BACKUP%" >>"%LOG_FILE%" 2>&1
if errorlevel 1 (call :log ERROR: Could not move current installation to backup & exit /B 1)
move "%NEW_DIR%" "%INSTALL_DIR%"
if errorlevel 1 (
    call :log ERROR: Could not install new application; restoring backup
    move "%BACKUP%" "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
    exit /B 1
)
rmdir /S /Q "%BACKUP%"
call :log Application files replaced successfully
start "Retail POS" /B "%INSTALL_DIR%\start.bat"
rmdir /S /Q "%STAGING%"
call :log Restart command sent
exit /B 0

:log
echo %DATE% %TIME% %*>>"%LOG_FILE%"
exit /B 0
