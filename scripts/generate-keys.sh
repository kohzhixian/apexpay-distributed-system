#!/bin/bash

# RSA Key Generation Script for ApexPay JWT Authentication
# Generates RSA 2048-bit keys in the correct format for Java

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Key directories
USER_SERVICE_KEYS="$PROJECT_ROOT/user-service/src/main/resources/keys"
API_GATEWAY_KEYS="$PROJECT_ROOT/api-gateway/src/main/resources/keys"

echo "=========================================="
echo "ApexPay RSA Key Generation Script"
echo "=========================================="
echo ""

# Create directories
echo "Creating key directories..."
mkdir -p "$USER_SERVICE_KEYS"
mkdir -p "$API_GATEWAY_KEYS"

# Check if keys already exist
if [ -f "$USER_SERVICE_KEYS/private.pem" ]; then
    echo ""
    echo "WARNING: Keys already exist!"
    read -p "Do you want to overwrite them? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted. Existing keys were not modified."
        exit 0
    fi
fi

# Generate private key in PKCS#8 format (required by Java)
echo ""
echo "Generating RSA 2048-bit private key (PKCS#8 format)..."
openssl genpkey -algorithm RSA \
    -out "$USER_SERVICE_KEYS/private.pem" \
    -pkeyopt rsa_keygen_bits:2048

# Extract public key
echo "Extracting public key..."
openssl rsa -pubout \
    -in "$USER_SERVICE_KEYS/private.pem" \
    -out "$USER_SERVICE_KEYS/public.pem" \
    2>/dev/null

# Copy public key to api-gateway
echo "Copying public key to api-gateway..."
cp "$USER_SERVICE_KEYS/public.pem" "$API_GATEWAY_KEYS/public.pem"

# Verify the keys
echo ""
echo "Verifying key formats..."

PRIVATE_HEADER=$(head -1 "$USER_SERVICE_KEYS/private.pem")
PUBLIC_HEADER=$(head -1 "$USER_SERVICE_KEYS/public.pem")

if [[ "$PRIVATE_HEADER" == "-----BEGIN PRIVATE KEY-----" ]]; then
    echo "✓ Private key is in correct PKCS#8 format"
else
    echo "✗ WARNING: Private key may not be in PKCS#8 format"
fi

if [[ "$PUBLIC_HEADER" == "-----BEGIN PUBLIC KEY-----" ]]; then
    echo "✓ Public key is in correct X.509 format"
else
    echo "✗ WARNING: Public key may not be in correct format"
fi

echo ""
echo "=========================================="
echo "Keys generated successfully!"
echo "=========================================="
echo ""
echo "Files created:"
echo "  - $USER_SERVICE_KEYS/private.pem"
echo "  - $USER_SERVICE_KEYS/public.pem"
echo "  - $API_GATEWAY_KEYS/public.pem"
echo ""
echo "IMPORTANT: These keys are NOT committed to git."
echo "Make sure to securely store a backup of your private key!"
echo ""
echo "For deployment:"
echo "  1. Keys will be bundled in the Docker images during build"
echo "  2. Ensure keys are generated before running docker build"
echo ""
