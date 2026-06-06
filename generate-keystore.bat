@echo off
REM This script generates a self-signed SSL certificate and keystore for Proto-Nova
REM The keystore will be embedded in the application for automatic SSL/TLS

setlocal enabledelayedexpansion

echo ===================================================
echo Proto-Nova SSL Certificate Generation Script
echo ===================================================
echo.

REM Set variables
set KEYSTORE_PASSWORD=proto-nova-secure
set CERT_ALIAS=proto-nova-server
set KEY_ALGORITHM=RSA
set KEY_SIZE=2048
set VALIDITY_DAYS=365
set COMMON_NAME=localhost
set ORGANIZATION=Proto-Nova
set LOCATION=WorldRoot

REM Check if JDK keytool is available
where keytool >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: keytool not found in PATH. Please ensure Java JDK is installed.
    pause
    exit /b 1
)

REM Create resources directory
if not exist "src\main\resources" (
    mkdir "src\main\resources"
    echo Created src\main\resources directory
)

set KEYSTORE_FILE=src\main\resources\keystore.jks

echo Creating self-signed certificate...
echo Keystore File: %KEYSTORE_FILE%
echo Password: %KEYSTORE_PASSWORD%
echo Certificate Alias: %CERT_ALIAS%
echo Validity: %VALIDITY_DAYS% days
echo.

REM Delete existing keystore if it exists
if exist "%KEYSTORE_FILE%" (
    echo Deleting existing keystore...
    del "%KEYSTORE_FILE%"
)

REM Generate the keystore with self-signed certificate
keytool -genkeypair ^
    -alias %CERT_ALIAS% ^
    -keyalg %KEY_ALGORITHM% ^
    -keysize %KEY_SIZE% ^
    -keystore "%KEYSTORE_FILE%" ^
    -storepass %KEYSTORE_PASSWORD% ^
    -keypass %KEYSTORE_PASSWORD% ^
    -validity %VALIDITY_DAYS% ^
    -dname "CN=%COMMON_NAME%, O=%ORGANIZATION%, L=%LOCATION%" ^
    -noprompt

if %ERRORLEVEL% equ 0 (
    echo.
    echo ===================================================
    echo SUCCESS: Keystore created successfully!
    echo ===================================================
    echo.
    echo Keystore Details:
    echo - File: %KEYSTORE_FILE%
    echo - Password: %KEYSTORE_PASSWORD%
    echo - Location: %CD%\%KEYSTORE_FILE%
    echo.
    echo NEXT STEPS:
    echo 1. Copy the keystore to the client resources:
    echo    copy "%KEYSTORE_FILE%" "..\proto-nova-Client\src\main\resources\keystore.jks"
    echo.
    echo 2. Rebuild the projects:
    echo    gradle build
    echo.
    echo 3. Your application is now ready! Run it anywhere without setup!
    echo.
) else (
    echo ERROR: Failed to create keystore!
    pause
    exit /b 1
)

pause
