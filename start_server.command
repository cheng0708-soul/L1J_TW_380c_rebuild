#!/bin/bash
# ============================================================
#  L1J_TW_3.80c_Original 一键启动脚本 (macOS)
#  双击 .command 文件即可在 Terminal 中运行
#  关闭终端窗口 = 自动停止服务器并释放所有资源
# ============================================================

# ---------- 1. 切换到脚本所在目录 ----------
cd "$(dirname "$0")"
echo "=========================================="
echo "  L1J_TW 3.80c Original Server Launcher"
echo "=========================================="
echo ""
echo "[INFO] 工作目录: $(pwd)"
echo ""

# ---------- 2. 强制使用 Java 8 (Zulu JDK 8) ----------
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
echo "[INFO] JAVA_HOME = $JAVA_HOME"
java -version 2>&1 | head -1
echo ""

# ---------- 3. 确保 log 目录存在 ----------
mkdir -p log
echo "[INFO] 日志目录: $(pwd)/log/"
echo "       ├── log/java*.log.*    (一般INFO级日志)"
echo "       ├── log/error*.log.*   (WARNING/ERROR日志)"
echo "       └── 控制台同步输出 (可直接看到启动过程)"
echo ""

# ---------- 4. 检查 l1jserver.jar 是否存在 ----------
if [ ! -f "l1jserver.jar" ]; then
    echo "[WARN] l1jserver.jar 不存在，正在编译打包..."
    ant clean compile_server jar_server 2>&1
    if [ $? -ne 0 ]; then
        echo ""
        echo "[ERROR] 编译失败！请先修复编译错误再启动。"
        echo "按回车键关闭..."
        read
        exit 1
    fi
    echo "[INFO] ✅ 编译打包完成"
    echo ""
fi

# ---------- 5. 注册退出钩子：关窗口/Ctrl-C 自动清理 ----------
cleanup() {
    echo ""
    echo "[INFO] 正在停止服务器..."
    if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
        kill "$SERVER_PID" 2>/dev/null
        wait "$SERVER_PID" 2>/dev/null
    fi
    echo "[INFO] 服务器已停止，所有资源已释放。"
    exit 0
}
trap cleanup SIGINT SIGTERM SIGHUP EXIT

# ---------- 6. 启动服务器 ----------
echo "=========================================="
echo "  正在启动 L1J Server ..."
echo "  按 Ctrl+C 或关闭窗口可停止服务器"
echo "=========================================="
echo ""

java -Xmx512m \
     -Djava.util.logging.config.file=config/log.properties \
     -jar l1jserver.jar &

SERVER_PID=$!
echo "[INFO] 服务器进程 PID: $SERVER_PID"
echo ""

# ---------- 7. 前台等待，关窗口自动触发 cleanup ----------
wait $SERVER_PID
