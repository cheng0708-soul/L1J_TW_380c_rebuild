# 更新歷史 (Changelog)

## [2026-05-01] 武器魔法系統 v1.0

### 新增功能

#### weapon_magic 表驅動架構
- 新增 WeaponMagicTable.java — 從資料庫 weapon_magic 表動態載入武器觸發配置
- 新增 L1WeaponMagic.java — 武器魔法資料模型
- 新增武器無需修改 Java 程式碼，僅需操作資料庫即可

#### trigger_type 路由規則
weapon_magic 表新增 trigger_type 欄位控制技能執行路徑：
- trigger_type=1 — 減益/狀態技能，addChaserAttack 內直接對目標生效
- trigger_type=0/2 — 走 L1SkillUse.handleCommands 標準流程

#### 自定義武器
- 200172 火焰武士刀 — 火球術(25) 30% L1SkillUse
- 200173 緩速武士刀 — 緩速術(29) 25% 直接應用
- 200174 迷魅武士刀 — 迷魅術(36) 15% 直接 L1BuffUtil

### Bug 修復
- 緩速術動畫位置: S_SkillSound 改發到 target 而非 caster
- 迷魅術無法抓寵物: 繞過 L1SkillUse 第二層概率判定，直接 L1BuffUtil.skillEffect
- 移除 WeaponMagicTable.java 重複的 setTriggerType 調用
- GameServer.java 效能限流、高併發安全修復
