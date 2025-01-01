#!/bin/bash

# Log file path
LOG_FILE="/var/log/ssl-renewal.log"

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "Please run as root"
    exit 1
fi

# Create log file if it doesn't exist
touch "$LOG_FILE"

# Start renewal process
log_message "Starting SSL certificate renewal process"

# Stop Nginx
log_message "Stopping Nginx service"
systemctl stop nginx
if [ $? -ne 0 ]; then
    log_message "ERROR: Failed to stop Nginx"
    exit 1
fi

# Renew certificates
log_message "Renewing SSL certificates"
certbot certonly -a standalone -d citegraph.io -d www.citegraph.io --non-interactive
if [ $? -ne 0 ]; then
    log_message "ERROR: Certificate renewal failed"
    # Try to restart Nginx even if renewal failed
    systemctl start nginx
    exit 1
fi

# Start Nginx
log_message "Starting Nginx service"
systemctl start nginx
if [ $? -ne 0 ]; then
    log_message "ERROR: Failed to start Nginx"
    exit 1
fi

log_message "SSL certificate renewal completed successfully"
