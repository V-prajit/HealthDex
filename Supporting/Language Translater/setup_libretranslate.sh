#!/bin/bash
# LibreTranslate Setup Script

echo "Setting up LibreTranslate..."

if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    echo "Visit https://docs.docker.com/get-docker/ for installation instructions."
    exit 1
fi

REQUIRED_LANGS="en,es,fr,hi,ar,bn,zh,de,id,it,ja,ko,pt,ru,ur"
echo "Will attempt to load the following languages: $REQUIRED_LANGS"

if docker ps -a --format '{{.Names}}' | grep -q "^libretranslate$"; then
    echo "Found existing LibreTranslate container. Removing it to apply new settings..."
    docker stop libretranslate || true
    docker rm libretranslate || true
    echo "Existing container removed."
fi

echo "Starting LibreTranslate with Docker..."
docker run -d --name libretranslate -p 5003:5000 libretranslate/libretranslate --load-only "$REQUIRED_LANGS"

echo "Waiting for LibreTranslate to initialize (this might take a while depending on model downloads)..."
sleep 15 # Increased sleep time

if docker ps | grep -q libretranslate; then
    echo "LibreTranslate started successfully!"
    echo "Checking loaded languages (may take a moment)..."
    sleep 5
    LOADED_LANGS=$(curl -s http://localhost:5003/languages | jq -r '.[].code' | paste -sd, || echo "jq not found or curl failed")
    echo "Currently loaded languages reported by API: $LOADED_LANGS"
else
    echo "Failed to start LibreTranslate. Please check docker logs:"
    echo "docker logs libretranslate"
    exit 1
fi


echo "LibreTranslate should now be running at http://localhost:5003"
echo "You can use the translation script to translate your app strings."