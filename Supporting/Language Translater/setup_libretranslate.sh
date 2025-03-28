#!/bin/bash
# LibreTranslate Setup Script

echo "Setting up LibreTranslate..."

# Check for Docker
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    echo "Visit https://docs.docker.com/get-docker/ for installation instructions."
    exit 1
fi

# Check if LibreTranslate is already running
if docker ps | grep -q libretranslate; then
    echo "LibreTranslate is already running."
else
    # Pull and run LibreTranslate in Docker
    echo "Starting LibreTranslate with Docker..."
    docker run -d --name libretranslate -p 5003:5000 libretranslate/libretranslate
    
    # Wait for LibreTranslate to initialize
    echo "Waiting for LibreTranslate to initialize..."
    sleep 5
    
    # Check if it's running
    if docker ps | grep -q libretranslate; then
        echo "LibreTranslate started successfully!"
    else
        echo "Failed to start LibreTranslate. Please check docker logs."
        exit 1
    fi
fi

echo "LibreTranslate is now running at http://localhost:5000"
echo "You can use the translation script to translate your app strings."
