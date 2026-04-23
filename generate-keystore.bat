@echo off
REM This script generates a self-signed SSL certificate and keystore for Proto-Nova
REM The keystore will be used for secure socket connections between client and server

setlocal enabledelayedexpansion

echo ===================================================
echo Proto-Nova SSL Certificate Generation Script
echo ===================================================
echo.

REM Set variables
set KEYSTORE_FILE=keystore.jks
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

echo Creating self-signed certificate...
echo Keystore File: %KEYSTORE_FILE%
echo Password: %KEYSTORE_PASSWORD%
echo Certificate Alias: %CERT_ALIAS%
echo Validity: %VALIDITY_DAYS% days
echo.

REM Delete existing keystore if it exists
if exist %KEYSTORE_FILE% (
    echo Deleting existing keystore...
    del %KEYSTORE_FILE%
)

REM Generate the keystore with self-signed certificate
keytool -genkeypair ^
    -alias %CERT_ALIAS% ^
    -keyalg %KEY_ALGORITHM% ^
    -keysize %KEY_SIZE% ^
    -keystore %KEYSTORE_FILE% ^
    -storepass %KEYSTORE_PASSWORD% ^
    -keypass %KEYSTORE_PASSWORD% ^
    -validity %VALIDITY_DAYS% ^
    -dname "CN=%COMMON_NAME%, O=%ORGANIZATION%, L=%LOCATION%"

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
    echo IMPORTANT: Copy the keystore file to:
    echo - Server root directory: proto-nova-Server\%KEYSTORE_FILE%
    echo - Client root directory: proto-nova-Client\%KEYSTORE_FILE%
    echo.
    echo Your SSL/TLS connection is now configured!
) else (
    echo ERROR: Failed to create keystore!
    pause
    exit /b 1
)

pause
