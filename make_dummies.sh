#!/bin/bash
DIR="src/l1j/server/server/"
BASE="/Users/wangluke/Localprojects/java380c/L1J_TW_3.80c_Original/"
cd $BASE

# AFK classes
cat << 'EOF' > ${DIR}afk/AfkScheduler.java
package l1j.server.server.afk; public class AfkScheduler { public static void scheduleAtFixedRate(Runnable r, int i1, int i2) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkCenterEngine.java
package l1j.server.server.afk; public class AfkCenterEngine { public static void process(Object pc) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkCombatUtil.java
package l1j.server.server.afk; public class AfkCombatUtil { public static void physicalAttack(Object pc, Object target) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkEventBridge.java
package l1j.server.server.afk; public class AfkEventBridge { public static void onTownWarp(Object pc) {} public static void onDeath(Object pc) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkController.java
package l1j.server.server.afk; public class AfkController { public static void forceOff(Object pc) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkService.java
package l1j.server.server.afk; public class AfkService { public static void onDisconnect(Object pc) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkMagicRegistry.java
package l1j.server.server.afk; public class AfkMagicRegistry { public static void setEnabled(Object pc, boolean b) {} }
EOF
cat << 'EOF' > ${DIR}afk/AfkUiRouter.java
package l1j.server.server.afk; public class AfkUiRouter { public static boolean tryHandleNpcAction(Object o1, Object o2, Object o3, Object o4) { return false; } }
EOF

# ITEM FUNCTIONS
cat << 'EOF' > ${DIR}model/item/function/AutoFarmToggle.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class AutoFarmToggle extends ItemExecutorAdapter { public static AutoFarmToggle get() { return new AutoFarmToggle(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/AfkRecharge10m.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class AfkRecharge10m extends ItemExecutorAdapter { public static AfkRecharge10m get() { return new AfkRecharge10m(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/AfkRecharge30m.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class AfkRecharge30m extends ItemExecutorAdapter { public static AfkRecharge30m get() { return new AfkRecharge30m(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/AfkRecharge60m.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class AfkRecharge60m extends ItemExecutorAdapter { public static AfkRecharge60m get() { return new AfkRecharge60m(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/AfkUpgradeHighGrade.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class AfkUpgradeHighGrade extends ItemExecutorAdapter { public static AfkUpgradeHighGrade get() { return new AfkUpgradeHighGrade(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/PriestIQPotion2.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class PriestIQPotion2 extends ItemExecutorAdapter { public static PriestIQPotion2 get() { return new PriestIQPotion2(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/PriestGrowthPotion.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class PriestGrowthPotion extends ItemExecutorAdapter { public static PriestGrowthPotion get() { return new PriestGrowthPotion(); } }
EOF
cat << 'EOF' > ${DIR}model/item/function/PrincessGrowthPotion.java
package l1j.server.server.model.item.function; import l1j.server.server.model.item.executor.ItemExecutorAdapter; public class PrincessGrowthPotion extends ItemExecutorAdapter { public static PrincessGrowthPotion get() { return new PrincessGrowthPotion(); } }
EOF

# PRIEST CLASSES
cat << 'EOF' > ${DIR}priest/NpcIntelResolver.java
package l1j.server.server.priest; public class NpcIntelResolver { public static int getBaseIntByItemAndName(Object id, Object name) { return 0; } }
EOF
cat << 'EOF' > ${DIR}priest/PriestHtmlHook.java
package l1j.server.server.priest; public class PriestHtmlHook { public static void tryOpen(Object o1, Object o2, Object o3) {} }
EOF
cat << 'EOF' > ${DIR}priest/PriestItemSelector.java
package l1j.server.server.priest; public class PriestItemSelector { public static void findSameTier(Object pc, Object npc) {} }
EOF
cat << 'EOF' > ${DIR}priest/PriestIqDAO2.java
package l1j.server.server.priest; public class PriestIqDAO2 { public static class Row { public int iq; public int exp; } public static Row byItemObjId(int id) { return null; } public static void ensure(Object pc, Object npc, int a, int b) {} public static void ensure(Object a, Object b) {} }
EOF
cat << 'EOF' > ${DIR}priest/PriestSettingsStore.java
package l1j.server.server.priest; public class PriestSettingsStore { public static class Settings { public boolean autoSupport; } public static Settings get(int id) { return new Settings(); } public static void setAutoSupport(int id, boolean b) {} public static void ensureLoaded(int id, int m, int r) {} public static void regenTick(int id) {} public static void deltaThreshold(int id, int t) {} public static void setThreshold(int id, int v) {} }
EOF
cat << 'EOF' > ${DIR}priest/PriestSupportRunner.java
package l1j.server.server.priest; public class PriestSupportRunner { public static void startFor(Object pc) {} public static void stopFor(int id) {} }
EOF

# C_PShopBypass
cat << 'EOF' > ${DIR}clientpackets/C_PShopBypass.java
package l1j.server.server.clientpackets; public class C_PShopBypass { public static class PShopSellOrder {} public static class PShopBuyOrder {} public static void handle(Object pc, String cmd) {} public static PShopSellOrder getPendingSellOrder(int id) { return null; } public static PShopBuyOrder getPendingBuyOrder(int id) { return null; } public static void processPShopBuy(Object pc, Object npc, Object order, Object idxArr, Object qtyArr) {} public static void processPShopSell(Object pc, Object npc, Object order, Object idxArr, Object qtyArr) {} public static void clearPendingSellOrder(int id) {} public static void clearPendingBuyOrder(int id) {} }
EOF
