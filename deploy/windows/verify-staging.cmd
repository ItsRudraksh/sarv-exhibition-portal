@echo off
REM Run from any prompt. Do not paste this line twice.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0verify-staging.ps1"
exit /b %ERRORLEVEL%
