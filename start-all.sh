#!/bin/bash

# ApexPay Distributed System - Startup Script
# Starts all microservices in the correct order

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Log file directory
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  ApexPay Distributed System Startup${NC}"
echo -e "${GREEN}========================================${NC}"

# Build all modules first (ensures common module is up to date)
echo -e "\n${YELLOW}Building all modules...${NC}"
mvn clean install -DskipTests -q
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed! Check for compilation errors.${NC}"
    exit 1
fi
echo -e "${GREEN}Build successful!${NC}"

# Function to check if a port is available
wait_for_port() {
    local port=$1
    local service=$2
    local max_attempts=60
    local attempt=0
    
    echo -e "${YELLOW}Waiting for $service to be ready on port $port...${NC}"
    
    while ! nc -z localhost $port 2>/dev/null; do
        attempt=$((attempt + 1))
        if [ $attempt -ge $max_attempts ]; then
            echo -e "${RED}Timeout waiting for $service on port $port${NC}"
            return 1
        fi
        sleep 2
    done
    
    echo -e "${GREEN}$service is ready on port $port${NC}"
    return 0
}

# Function to start a service
start_service() {
    local service_dir=$1
    local service_name=$2
    local port=$3
    
    echo -e "\n${YELLOW}Starting $service_name...${NC}"
    
    cd "$SCRIPT_DIR/$service_dir"
    ./mvnw spring-boot:run -q > "$LOG_DIR/$service_name.log" 2>&1 &
    local pid=$!
    echo $pid > "$LOG_DIR/$service_name.pid"
    
    echo -e "  PID: $pid"
    echo -e "  Log: $LOG_DIR/$service_name.log"
    
    cd "$SCRIPT_DIR"
}

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Shutting down services...${NC}"
    
    for pid_file in "$LOG_DIR"/*.pid; do
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            service_name=$(basename "$pid_file" .pid)
            if kill -0 $pid 2>/dev/null; then
                echo -e "Stopping $service_name (PID: $pid)"
                kill $pid 2>/dev/null || true
            fi
            rm -f "$pid_file"
        fi
    done
    
    echo -e "${GREEN}All services stopped.${NC}"
    exit 0
}

# Trap SIGINT (Ctrl+C) and SIGTERM
trap cleanup SIGINT SIGTERM

# 1. Start Discovery Server (Eureka) - Must be first
start_service "discovery-server" "discovery-server" 8761
wait_for_port 8761 "Discovery Server (Eureka)"

# 2. Start business services (can start in parallel after Eureka is up)
echo -e "\n${YELLOW}Starting business services...${NC}"
start_service "user-service" "user-service" 8081
start_service "wallet-service" "wallet-service" 8082
start_service "payment-service" "payment-service" 8083

# Wait for business services
wait_for_port 8081 "User Service"
wait_for_port 8082 "Wallet Service"
wait_for_port 8083 "Payment Service"

# 3. Start API Gateway (after other services are registered)
start_service "api-gateway" "api-gateway" 9000
wait_for_port 9000 "API Gateway"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  All services started successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "\nService URLs:"
echo -e "  Eureka Dashboard: http://localhost:8761"
echo -e "  API Gateway:      http://localhost:9000"
echo -e "  User Service:     http://localhost:8081"
echo -e "  Wallet Service:   http://localhost:8082"
echo -e "  Payment Service:  http://localhost:8083"
echo -e "\nLogs directory: $LOG_DIR"
echo -e "\n${YELLOW}Press Ctrl+C to stop all services${NC}"

# Keep script running to handle Ctrl+C
wait
