@echo off
setlocal
set "PACKAGE=%~1"
set "INSTALL_DIR=%~2"
set "PID=%~3"
set "LOG_FILE=%INSTALL_DIR%.update.log"
call :log Update started: package=%PACKAGE% install_dir=%INSTALL_DIR% pid=%PID%
set "STAGING=%TEMP%\retail-pos-update-%RANDOM%"
set "BACKUP=%INSTALL_DIR%.previous"
set "RETAIL_POS_PACKAGE=%PACKAGE%"
set "RETAIL_POS_STAGING=%STAGING%"
set "NEW_DIR="

if not defined PACKAGE (call :log ERROR: Package is missing & exit /B 1)
if not defined INSTALL_DIR (call :log ERROR: Installation directory is missing & exit /B 1)
if not defined PID (call :log ERROR: Process ID is missing & exit /B 1)

set /A WAIT_SECONDS=0

:wait
tasklist /FI "PID eq %PID%" 2>NUL | find "%PID%" >NUL
if errorlevel 1 goto stopped
if %WAIT_SECONDS% GEQ 120 (
    call :log ERROR: Timed out waiting for application process %PID%
    exit /B 1
)
timeout /T 1 /NOBREAK >NUL
set /A WAIT_SECONDS+=1
goto wait

:stopped
call :log Application process stopped after %WAIT_SECONDS% seconds

powershell -NoProfile -NonInteractive -Command "Expand-Archive -LiteralPath $env:RETAIL_POS_PACKAGE -DestinationPath $env:RETAIL_POS_STAGING -Force" >>"%LOG_FILE%" 2>&1
if errorlevel 1 (call :log ERROR: Could not extract package & exit /B 1)
for /D %%D in ("%STAGING%\*") do set NEW_DIR=%%~fD
if not defined NEW_DIR (call :log ERROR: Package has no application directory & exit /B 1)
rem Leave the installation directory before replacing it so the restarted process
rem does not inherit a deleted working directory.
cd /d "%TEMP%"
if errorlevel 1 (call :log ERROR: Could not change to a stable working directory & exit /B 1)
if exist "%BACKUP%" rmdir /S /Q "%BACKUP%"
move "%INSTALL_DIR%" "%BACKUP%" >>"%LOG_FILE%" 2>&1
if errorlevel 1 (call :log ERROR: Could not move current installation to backup & exit /B 1)
move "%NEW_DIR%" "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
if errorlevel 1 (
    call :log ERROR: Could not install new application; restoring backup
    move "%BACKUP%" "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
    if errorlevel 1 call :log ERROR: Could not restore previous installation
    exit /B 1
)
call :log Application files replaced successfully
if not exist "%INSTALL_DIR%\start.bat" (
    call :log ERROR: New application has no start.bat; restoring backup
    rmdir /S /Q "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
    move "%BACKUP%" "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
    if errorlevel 1 call :log ERROR: Could not restore previous installation
    exit /B 1
)
start "Retail POS" /B /D "%INSTALL_DIR%" "%ComSpec%" /D /C call "%INSTALL_DIR%\start.bat" >>"%LOG_FILE%" 2>&1
if errorlevel 1 (
    call :log ERROR: Could not start the updated application; restoring backup
    rmdir /S /Q "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
    move "%BACKUP%" "%INSTALL_DIR%" >>"%LOG_FILE%" 2>&1
    if errorlevel 1 call :log ERROR: Could not restore previous installation
    exit /B 1
)
rmdir /S /Q "%BACKUP%" >>"%LOG_FILE%" 2>&1
rmdir /S /Q "%STAGING%"
call :log Updated application launcher started
exit /B 0

:log
echo %DATE% %TIME% %*>>"%LOG_FILE%"
exit /B 0
