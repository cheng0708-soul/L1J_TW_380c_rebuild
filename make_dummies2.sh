#!/bin/bash
DIR="src/l1j/server/server/"
BASE="/Users/wangluke/Localprojects/java380c/L1J_TW_3.80c_Original/"
cd $BASE

cat << 'EOF' > ${DIR}priest/PriestSettingsStore.java
package l1j.server.server.priest; public class PriestSettingsStore { public static class Settings { public boolean autoSupport; public int healThreshold; public int mp; } public static Settings get(int id) { return new Settings(); } public static void setAutoSupport(int id, boolean b) {} public static void ensureLoaded(int id, int m, int r) {} public static void regenTick(int id) {} public static void deltaThreshold(int id, int t) {} public static void setThreshold(int id, int v) {} }
EOF

cat << 'EOF' > ${DIR}priest/PriestIqDAO2.java
package l1j.server.server.priest; public class PriestIqDAO2 { public static class Row { public int iqTotal; public int iqBonus; } public static Row byItemObjId(int id) { return null; } public static void ensure(Object pc, Object npc, int a, int b) {} public static void ensure(Object a, Object b) {} }
EOF

cat << 'EOF' >> ${DIR}model/Instance/L1PcInstance.java

    // DUMMY METHODS FOR MISSING PSHOP CURRENCY
    public int getPrivateShopCurrencyItemId() { return 0; }
    public void setPrivateShopCurrencyItemId(int i) {}
EOF
