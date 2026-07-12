# Script khoi dong nhanh ha tang E-learning

# 1. Kiem tra va tao Network neu chua co
$networkName = "elearning-network"
$networkExists = docker network ls --filter name=$networkName -q
if (-not $networkExists) {
    docker network create $networkName
}

# 2. Khoi dong cac container
Write-Host "--- Dang khoi dong he thong (FAST MODE)... ---" -ForegroundColor Cyan
docker compose up -d

Write-Host "--- ✅ HE THONG DA SAN SANG! ---" -ForegroundColor Green
docker ps
