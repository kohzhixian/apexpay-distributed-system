#!/bin/bash

# =============================================================================
# RSA Key Pair Generator for JWT RS256 Signing
# =============================================================================
# Usage: ./generate-jwt-keys.sh [output_directory] [key_size]
#
# Arguments:
#   output_directory  - Where to save keys (default: ./keys)
#   key_size          - RSA key size in bits (default: 2048)
#
# Examples:
#   ./generate-jwt-keys.sh
#   ./generate-jwt-keys.sh ./my-keys
#   ./generate-jwt-keys.sh ./my-keys 4096
# =============================================================================

set -e

OUTPUT_DIR="${1:-./keys}"
KEY_SIZE="${2:-2048}"

echo "🔐 Generating RSA key pair for JWT RS256..."
echo "   Output directory: $OUTPUT_DIR"
echo "   Key size: $KEY_SIZE bits"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Generate private key (PKCS#8 format for Java compatibility)
echo "📝 Generating private key..."
openssl genrsa "$KEY_SIZE" 2>/dev/null | \
    openssl pkcs8 -topk8 -nocrypt -out "$OUTPUT_DIR/private.pem"

# Extract public key
echo "📝 Extracting public key..."
openssl rsa -in "$OUTPUT_DIR/private.pem" -pubout -out "$OUTPUT_DIR/public.pem" 2>/dev/null

# Set restrictive permissions on private key
chmod 600 "$OUTPUT_DIR/private.pem"
chmod 644 "$OUTPUT_DIR/public.pem"

echo ""
echo "✅ Keys generated successfully!"
echo ""
echo "Files created:"
echo "   Private key: $OUTPUT_DIR/private.pem (keep secret!)"
echo "   Public key:  $OUTPUT_DIR/public.pem (shareable)"
echo ""
echo "⚠️  IMPORTANT: Never commit private.pem to version control!"

