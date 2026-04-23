#!/bin/bash

# This script generates a self-signed SSL certificate and keystore for Proto-Nova
# The keystore will be used for secure socket connections between client and server

echo "=================================================="
echo "Proto-Nova SSL Certificate Generation Script"
echo "=================================================="
echo ""

# Set variables
KEYSTORE_FILE="keystore.jks"
KEYSTORE_PASSWORD="proto-nova-secure"
CERT_ALIAS="proto-nova-server"
KEY_ALGORITHM="RSA"
KEY_SIZE="2048"
VALIDITY_DAYS="365"
COMMON_NAME="localhost"
ORGANIZATION="Proto-Nova"
LOCATION="WorldRoot"

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo "ERROR: keytool not found. Please ensure Java JDK is installed."
    exit 1
fi

echo "Creating self-signed certificate..."
echo "Keystore File: $KEYSTORE_FILE"
echo "Password: $KEYSTORE_PASSWORD"
echo "Certificate Alias: $CERT_ALIAS"
echo "Validity: $VALIDITY_DAYS days"
echo ""

# Delete existing keystore if it exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo "Deleting existing keystore..."
    rm "$KEYSTORE_FILE"
fi

# Generate the keystore with self-signed certificate
keytool -genkeypair \
    -alias "$CERT_ALIAS" \
    -keyalg "$KEY_ALGORITHM" \
    -keysize "$KEY_SIZE" \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEYSTORE_PASSWORD" \
    -validity "$VALIDITY_DAYS" \
    -dname "CN=$COMMON_NAME, O=$ORGANIZATION, L=$LOCATION"

if [ $? -eq 0 ]; then
    echo ""
    echo "=================================================="
    echo "SUCCESS: Keystore created successfully!"
    echo "=================================================="
    echo ""
    echo "Keystore Details:"
    echo "- File: $KEYSTORE_FILE"
    echo "- Password: $KEYSTORE_PASSWORD"
    echo "- Location: $(pwd)/$KEYSTORE_FILE"
    echo ""
    echo "IMPORTANT: Copy the keystore file to:"
    echo "- Server root directory: proto-nova-Server/$KEYSTORE_FILE"
    echo "- Client root directory: proto-nova-Client/$KEYSTORE_FILE"
    echo ""
    echo "Your SSL/TLS connection is now configured!"
else
    echo "ERROR: Failed to create keystore!"
    exit 1
fi
