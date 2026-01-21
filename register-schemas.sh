#!/bin/bash

# Apicurio Registry URL
REGISTRY_URL="http://157.66.219.174:9081/apis/registry/v2"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Registering AsyncAPI schema to Apicurio Registry..."

# Check if artifact already exists
check_response=$(curl -s -w "\n%{http_code}" \
  "${REGISTRY_URL}/groups/com.freedraw/artifacts/FreeNoteAPI")

check_http_code=$(echo "$check_response" | tail -n1)

if [ "$check_http_code" -eq 200 ]; then
  echo -e "${YELLOW}⚠ Artifact already exists. Creating new version...${NC}"
  
  # Create a new version
  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${REGISTRY_URL}/groups/com.freedraw/artifacts/FreeNoteAPI/versions" \
    -H "Content-Type: application/x-yaml" \
    --data-binary @asyncapi.yaml)
  
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    echo -e "${GREEN}✓ New version created successfully!${NC}"
    echo "$body" | jq '.'
  else
    echo -e "${RED}✗ Failed to create new version. HTTP Status: $http_code${NC}"
    echo "$body"
    exit 1
  fi
else
  # Artifact doesn't exist, create it
  echo "Creating new artifact..."
  response=$(curl -s -w "\n%{http_code}" -X POST \
    "${REGISTRY_URL}/groups/com.freedraw/artifacts" \
    -H "Content-Type: application/x-yaml" \
    -H "X-Registry-ArtifactId: FreeNoteAPI" \
    -H "X-Registry-ArtifactType: ASYNCAPI" \
    --data-binary @asyncapi.yaml)
  
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    echo -e "${GREEN}✓ AsyncAPI schema registered successfully!${NC}"
    echo "$body" | jq '.'
  else
    echo -e "${RED}✗ Failed to register schema. HTTP Status: $http_code${NC}"
    echo "$body"
    exit 1
  fi
fi

echo ""
echo "✓ Schema registered successfully!"
echo ""
echo "API Endpoint: ${REGISTRY_URL}/groups/com.freedraw/artifacts/FreeNoteAPI"
echo ""
echo "Test with curl:"
echo "  curl ${REGISTRY_URL}/groups/com.freedraw/artifacts/FreeNoteAPI | jq '.info.title'"
echo ""
echo "Note: The current registry image (latest-snapshot) does not include a UI."
echo "      To use the UI, switch to: apicurio/apicurio-registry:2.5.8.Final"
