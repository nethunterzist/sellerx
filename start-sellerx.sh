#!/bin/bash

# SellerX Development Environment Launcher
# Opens 4 separate Terminal windows for monitoring all services

SELLERX_DIR="$HOME/Desktop/Sellerx"

echo "🚀 Starting SellerX Development Environment..."

# Start database first (if not already running)
echo "Starting database..."
cd "$SELLERX_DIR" && ./db.sh start 2>/dev/null
sleep 2

# Open 4 separate Terminal windows using AppleScript
osascript <<EOF
tell application "Terminal"
    activate

    -- Window 1: Backend (Spring Boot)
    do script "cd $SELLERX_DIR/sellerx-backend && echo '🚀 BACKEND - Spring Boot (port 8080)' && echo '================================' && export JWT_SECRET='sellerx-development-jwt-secret-key-2026-minimum-256-bits-required' && ./mvnw spring-boot:run"

    -- Window 2: Frontend (Next.js)
    do script "cd $SELLERX_DIR/sellerx-frontend && echo '🌐 FRONTEND - Next.js (port 3000)' && echo '================================' && npm run dev"

    -- Window 3: Database Logs
    do script "cd $SELLERX_DIR && echo '🗄️ DATABASE LOGS' && echo '================================' && ./db.sh logs -f"

    -- Window 4: Health Monitor
    do script "echo '💓 HEALTH MONITOR' && echo '================================' && echo 'Waiting 15 seconds for backend...' && sleep 15 && while true; do clear; echo '💓 HEALTH MONITOR - ' && date && echo '================================' && curl -s http://localhost:8080/actuator/health 2>/dev/null | python3 -m json.tool || echo 'Backend not ready...'; echo '' && echo 'Next check in 5 seconds...'; sleep 5; done"

end tell
EOF

echo ""
echo "✅ SellerX Development Environment Started!"
echo ""
echo "📊 4 Terminal windows opened:"
echo "   Window 1: Backend (Spring Boot) - port 8080"
echo "   Window 2: Frontend (Next.js) - port 3000"
echo "   Window 3: Database Logs (PostgreSQL)"
echo "   Window 4: Health Monitor"
echo ""
echo "🔗 URLs:"
echo "   Frontend:  http://localhost:3000"
echo "   Backend:   http://localhost:8080"
echo "   Health:    http://localhost:8080/actuator/health"
echo ""
