#!/bin/bash

# ApexPay Distributed System - Stop Script
# Stops all running microservices

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Stopping ApexPay services...${NC}"

stopped=0

for pid_file in "$LOG_DIR"/*.pid; do
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        service_name=$(basename "$pid_file" .pid)
        
        if kill -0 $pid 2>/dev/null; then
            echo -e "Stopping $service_name (PID: $pid)"
            kill $pid 2>/dev/null
            stopped=$((stopped + 1))
        else
            echo -e "${YELLOW}$service_name already stopped${NC}"
        fi
        rm -f "$pid_file"
    fi
done

if [ $stopped -eq 0 ]; then
    echo -e "${YELLOW}No running services found via PID files.${NC}"
else
    echo -e "${GREEN}Stopped $stopped service(s) via PID files.${NC}"
fi

# Kill any processes still running on service ports
echo -e "\n${YELLOW}Checking for processes on service ports...${NC}"

PORTS=(8761 8081 8082 8083 9000)
PORT_NAMES=("Discovery Server" "User Service" "Wallet Service" "Payment Service" "API Gateway")

port_killed=0
for i in "${!PORTS[@]}"; do
    port=${PORTS[$i]}
    name=${PORT_NAMES[$i]}
    
    pid=$(lsof -ti :$port 2>/dev/null)
    if [ -n "$pid" ]; then
        echo -e "Killing $name on port $port (PID: $pid)"
        kill $pid 2>/dev/null || kill -9 $pid 2>/dev/null
        port_killed=$((port_killed + 1))
    fi
done

if [ $port_killed -eq 0 ]; then
    echo -e "${GREEN}All service ports are clear.${NC}"
else
    echo -e "${GREEN}Killed $port_killed process(es) on service ports.${NC}"
fi

echo -e "\n${GREEN}Cleanup complete.${NC}"
