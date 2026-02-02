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
    echo -e "${YELLOW}No running services found.${NC}"
else
    echo -e "${GREEN}Stopped $stopped service(s).${NC}"
fi
