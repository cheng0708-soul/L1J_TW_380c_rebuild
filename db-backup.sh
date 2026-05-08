#!/bin/bash
# 数据库备份并推送到 GitHub
# 用法: ./db-backup.sh "备注信息"

set -e
cd /opt/l1j-tw

# 1. 读取数据库密码
PASS=$(grep "^Password=" config/server.properties | cut -d= -f2)

# 2. 导出完整数据库
echo ">>> 导出数据库..."
mysqldump -u root -p"$PASS" --databases l1j380 --routines --triggers --add-drop-database > db/l1j380.sql
echo "    done ($(wc -c < db/l1j380.sql) bytes)"

# 3. 提交到 git
echo ">>> 提交到 Git..."
git add db/l1j380.sql
git commit -m "db: $1"
git push origin master

echo ""
echo "✅ 备份完成: $(date)"
echo "   备注: $1"
