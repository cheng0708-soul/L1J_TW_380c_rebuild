# 数据库文件说明

本目录包含两个 SQL 文件：

| 文件 | 大小 | 说明 |
|---|---|---|
| `l1jtw.sql` | 8.6MB | 原始项目纯净数据库（69,789行）。包含游戏框架、物品、NPC、技能等默认数据，**不含任何自定义修改和玩家数据**。保留此文件仅作为原始参考。 |
| `l1j380.sql` | ~4MB | **线上实时备份**（通过 `db-backup.sh` 自动生成）。包含所有自定义改动（自定义武器、道具等）和当前玩家数据。**迁移/还原服务器时用这一个文件即可**，无需单独打补丁。 |

## 备份命令

```bash
cd /opt/l1j-tw
./db-backup.sh "你想写的备注信息"
```

## 还原步骤（新服务器）

```bash
git clone https://github.com/cheng0708-soul/L1J_TW_380c_rebuild.git /opt/l1j-tw
mysql -u root -p < /opt/l1j-tw/db/l1j380.sql
# 修改 config/server.properties 里的数据库连接信息
cd /opt/l1j-tw && ./start.sh
```
