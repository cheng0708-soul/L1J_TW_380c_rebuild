#!/bin/bash
cd /opt/l1j-tw
pkill -f l1j.server.Server 2>/dev/null
sleep 2
nohup java -Xmx384m -Xms128m -cp 'lib/*:l1jserver.jar' l1j.server.Server > server.log 2>&1 &
echo 'PID='
sleep 3
ss -tlnp | grep 2000 && echo 'UP' || echo 'STARTING...'
