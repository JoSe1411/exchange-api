#!/bin/bash

# Spring Boot Application Runner Script
# This script runs the Spring Boot backend application

echo "🚀 Starting Spring Boot Exchange API..."

# Check if Maven wrapper exists and is executable
if [ ! -f "mvnw" ]; then
    echo "❌ Error: Maven wrapper (mvnw) not found"
    exit 1
fi

if [ ! -x "mvnw" ]; then
    echo "📝 Making Maven wrapper executable..."
    chmod +x mvnw
fi

# Run the Spring Boot application
echo "📦 Building and running the application..."
./mvnw spring-boot:run

echo "✅ Application stopped"
