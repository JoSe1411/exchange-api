#!/bin/bash

# Spring Boot Application Builder and Runner Script
# This script cleans, builds, and runs the Spring Boot backend application

echo "🧹 Cleaning and building Spring Boot Exchange API..."

# Check if Maven wrapper exists and is executable
if [ ! -f "mvnw" ]; then
    echo "❌ Error: Maven wrapper (mvnw) not found"
    exit 1
fi

if [ ! -x "mvnw" ]; then
    echo "📝 Making Maven wrapper executable..."
    chmod +x mvnw
fi

# Clean and install dependencies
echo "📦 Cleaning and installing dependencies..."
./mvnw clean install

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "🚀 Starting the application..."
    ./mvnw spring-boot:run
else
    echo "❌ Build failed! Please check the errors above."
    exit 1
fi

echo "✅ Application stopped"
