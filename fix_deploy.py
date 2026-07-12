import re
import os

# 1. Modify docker-compose-prod.yml
with open('docker-compose-prod.yml', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'build:\s*\./([a-zA-Z0-9_-]+)', r'image: ${DOCKER_USERNAME}/\1:latest', content)
content = content.replace('222.255.181.66', '${VPS_HOST}')
content = re.sub(r'GEMINI_API_KEY=\$\{GEMINI_API_KEY:-[^}]+\}', r'GEMINI_API_KEY=${GEMINI_API_KEY}', content)
content = re.sub(r'COHERE_API_KEY=\$\{COHERE_API_KEY:-[^}]+\}', r'COHERE_API_KEY=${COHERE_API_KEY}', content)
content = re.sub(r'TAVILY_API_KEY=\$\{TAVILY_API_KEY:-[^}]+\}', r'TAVILY_API_KEY=${TAVILY_API_KEY}', content)

with open('docker-compose-prod.yml', 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Create .github/workflows/deploy.yml
os.makedirs('.github/workflows', exist_ok=True)

workflow = '''name: CI/CD Pipeline

on:
  push:
    branches:
      - main

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build Java Services with Maven
        run: mvn clean package -DskipTests

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and Push All Docker Images
        run: |
          SERVICES=("discovery-service" "api-gateway" "identity-service" "course-service" "assessment-service" "learning-service" "interaction-service" "order-service" "notification-service" "media-service" "search-service" "worker-service" "ai-service" "stt-service" "pdf-parser-service")
          for SERVICE in "${SERVICES[@]}"; do
            echo "Building image for $SERVICE..."
            docker build -t ${{ secrets.DOCKER_USERNAME }}/$SERVICE:latest ./$SERVICE
            docker push ${{ secrets.DOCKER_USERNAME }}/$SERVICE:latest
          done

  deploy-to-vps:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Copy Config Files to VPS
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          password: ${{ secrets.VPS_PASSWORD }}
          source: "docker-compose-prod.yml,docker_dev/"
          target: "/root/e-learning-platform/"
          overwrite: true

      - name: Execute Docker Compose on VPS
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          password: ${{ secrets.VPS_PASSWORD }}
          script: |
            cd /root/e-learning-platform
            export DOCKER_USERNAME=${{ secrets.DOCKER_USERNAME }}
            export VPS_HOST=${{ secrets.VPS_HOST }}
            export GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}
            export COHERE_API_KEY=${{ secrets.COHERE_API_KEY }}
            export TAVILY_API_KEY=${{ secrets.TAVILY_API_KEY }}
            
            docker-compose -f docker-compose-prod.yml up -d postgres-db mongo-db redis zookeeper kafka keycloak elasticsearch minio
            sleep 10
            
            docker-compose -f docker-compose-prod.yml pull
            docker-compose -f docker-compose-prod.yml up -d
'''

with open('.github/workflows/deploy.yml', 'w', encoding='utf-8') as f:
    f.write(workflow)

print("Xong! Đã chuẩn bị CI/CD thành công!")
