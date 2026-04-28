package l1j.server.server.model.shop;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PShopNpcInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.S_SystemMessage;

/**
 * PShopManager － 你的核心用版（含 escrow、雙幣、修正 removeItem 回傳型別問題）。
 * 1) 一帳號限兩攤；同幣別不可雙開
 * 2) 開攤：把販售品託管到 DB；收購先從攤主扣對應幣別當保證金(escrow)
 * 3) 收攤：NPC deleteMe + S_RemoveObject + 關閉 DB 狀態
 * 4) getOwnerPc(L1PShopNpcInstance) 與 onNpcDeleted(...) 皆補齊
 */
public class PShopManager {
    // Mute PSHOP TRACE messages (UI spam)
    private static final boolean MUTE_PSHOP_TRACE = true;


    private static final PShopManager _instance = new PShopManager();
    public static PShopManager getInstance() { return _instance; }

    // ---- 狀態 ----
    private final Map<Integer, Long> _spawnCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1500L;

    // ownerId -> (modeItemId -> npcId)
    private final Map<Integer, Map<Integer, Integer>> _ownerStalls = new ConcurrentHashMap<>();
    // announcers
    private final Map<Integer, PShopAnnouncer> _announcers = new ConcurrentHashMap<>();
    // ownerId -> pending
    private final Map<Integer, Pending> _pending = new ConcurrentHashMap<>();

    private static final int MODE_COIN = PShopCurrencyUtil.COIN;   // 40308
    private static final int MODE_YUAN = PShopCurrencyUtil.YUAN;   // 240107

    private static final int TEMPLATE_INTERACTIVE = 81003; // 你指定的可互動模板

    private static final class Pending {
        String desc; List<?> sell; List<?> buy;
        Pending(String d, List<?> s, List<?> b){ desc=d; sell=s; buy=b; }
    }

    // ---- utils ----
    public boolean isSpawnCooling(L1PcInstance pc) {
        Long last = _spawnCooldown.get(pc.getId());
        return last != null && (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }
    public void markSpawnCooling(L1PcInstance pc) {
        _spawnCooldown.put(pc.getId(), System.currentTimeMillis());
    }
    public void setPending(L1PcInstance pc, String desc, List<?> sell, List<?> buy) {
        _pending.put(pc.getId(), new Pending(desc, sell, buy));
    }
    private Pending consumePending(L1PcInstance pc) { return _pending.remove(pc.getId()); }

    // ---- 開攤流程 ----
    public void openStallFromPending(L1PcInstance owner, int modeItemId) {
        Pending p = consumePending(owner);
        String desc = (p != null ? p.desc : "");
        List<?> sell = (p != null ? p.sell : java.util.Collections.emptyList());
        List<?> buy  = (p != null ? p.buy  : java.util.Collections.emptyList());
        openStall(owner, modeItemId, desc, sell, buy);
    }

    public void openStall(L1PcInstance owner, int modeItemId, String desc, List<?> sellList, List<?> buyList) {
        if (!canOpen(owner, modeItemId)) return;

        L1PShopNpcInstance npc = PShopSpawnUtil.spawn(owner, TEMPLATE_INTERACTIVE, modeItemId, 0);
        if (npc == null) {
            try { owner.sendPackets(new S_SystemMessage("PSHOP: 生成攤位失敗。")); } catch (Throwable ignore) {}
            return;
        }

        int stallId = PShopDB.getInstance().createStall(owner, modeItemId, npc.getId(), TEMPLATE_INTERACTIVE, desc == null ? "" : desc);
        rememberOpen(owner, modeItemId, npc.getId());

        // ---- escrow 計算（Σ buy_price*buy_count）並扣幣別 ----
        int escrowNeed = 0;
        try {
            for (Object o : buyList) {
                int price = (int) tryGet(o, "getPrice", 0);
                int count = (int) tryGet(o, "getCount", 0);
                escrowNeed += Math.max(0, price) * Math.max(0, count);
            }
        } catch (Throwable ignore) {}

        if (escrowNeed > 0) {
            if (!owner.getInventory().consumeItem(modeItemId, escrowNeed)) {
                try { owner.sendPackets(new S_SystemMessage("PSHOP: " + PShopCurrencyUtil.currencyName(modeItemId) + "不足以支付收購保證金。")); } catch (Throwable ignore) {}
                try { npc.deleteMe(); owner.sendPackets(new S_RemoveObject(npc)); } catch (Throwable ignore) {}
                rememberClose(owner, modeItemId, npc.getId());
                return;
            }
            PShopDB.getInstance().addEscrow(stallId, escrowNeed);
        }

        // ---- 託管販售品（從背包移除 → DB 建檔）----
        try {
            for (Object o : sellList) {
                int itemId = (int) tryGet(o, "getItemId", 0);
                int bless  = (int) tryGet(o, "getBless", 0);
                int ench   = (int) tryGet(o, "getEnchant", 0);
                int attrLevel = (int) tryGet(o, "getAttrEnchant", 0);
                int attrKind  = (int) tryGet(o, "getAttrEnchantKind", 0);
                int attr = 0;
                if (attrKind > 0 || attrLevel > 0) {
                    attr = ((attrKind & 0xFF) << 8) | (attrLevel & 0xFF);
                }
                int price  = (int) tryGet(o, "getPrice", 0);
                int count  = (int) tryGet(o, "getCount", 0);
                int objId  = (int) tryGet(o, "getItemObjId", 0);

                if (count <= 0 || price <= 0) continue;

                boolean ok = false;
                try {
                    // 版本差異：有些 removeItem 需要 L1ItemInstance，有些可用 (objId, count)
                    L1ItemInstance inst = owner.getInventory().getItem(objId);
                    if (inst != null) {
                        owner.getInventory().removeItem(inst, count);
                        ok = true;
                    } else {
                        // 若沒有 getItem(objId) 或為空，退回以 itemId 消耗
                        owner.getInventory().consumeItem(itemId, count);
                        ok = true;
                    }
                } catch (Throwable ignore) {}

                if (!ok) continue;

                PShopDB.getInstance().insertSellItem(stallId, itemId, bless, ench, attr, price, count, objId);
            }
        } catch (Throwable ignore) {}

        // ---- 託管收購條件到 DB ----
        try {
            for (Object o : buyList) {
                int itemId = (int) tryGet(o, "getItemId", 0);
                int bless  = (int) tryGet(o, "getBless", 0);
                int ench   = (int) tryGet(o, "getEnchant", 0);
                int attr   = (int) tryGet(o, "getAttrEnchant", 0);
                int price  = (int) tryGet(o, "getPrice", 0);
                int count  = (int) tryGet(o, "getCount", 0);
                if (count <= 0 || price <= 0) continue;
                PShopDB.getInstance().insertBuyItem(stallId, itemId, bless, ench, attr, price, count);
            }
        } catch (Throwable ignore) {}

        // ---- 外觀與發話 ----
        String prefix = (modeItemId == MODE_COIN) ? "金幣" : "元寶";
        String name = "[" + prefix + "]" + owner.getName() + "的商店";
        try { npc.setCustomName(name); npc.setTitle(""); } catch (Throwable ignore) {}
        try {
            String say = "[" + prefix + "]" + (desc == null ? "" : desc.trim());
            if (say.endsWith("?")) say = say.substring(0, say.length()-1);
            PShopAnnouncer ann = new PShopAnnouncer(npc, say);
            ann.start();
            _announcers.put(npc.getId(), ann);
        } catch (Throwable ignore) {}
    }

    private Object tryGet(Object obj, String getter, Object def) {
        try { return obj.getClass().getMethod(getter).invoke(obj); } catch (Throwable t) { return def; }
    }

    private boolean canOpen(L1PcInstance owner, int modeItemId) {
        Map<Integer, Integer> byMode = _ownerStalls.get(owner.getId());
        if (byMode != null) {
            if (byMode.containsKey(modeItemId)) {
                try { owner.sendPackets(new S_SystemMessage("PSHOP: 已有同模式攤位。")); } catch (Throwable ignore) {}
                return false;
            }
            if (byMode.size() >= 2) {
                try { owner.sendPackets(new S_SystemMessage("PSHOP: 每帳號最多 2 個攤位。")); } catch (Throwable ignore) {}
                return false;
            }
        }
        return true;
    }

    private void rememberOpen(L1PcInstance owner, int modeItemId, int npcId) {
        _ownerStalls.computeIfAbsent(owner.getId(), k -> new ConcurrentHashMap<>()).put(modeItemId, npcId);
    }
    protected void rememberClose(L1PcInstance owner, int modeItemId, int npcId) {
        try {
            Map<Integer, Integer> byMode = _ownerStalls.get(owner.getId());
            if (byMode != null) {
                byMode.remove(modeItemId);
                if (byMode.isEmpty()) _ownerStalls.remove(owner.getId());
            }
        } catch (Throwable ignore) {}
        try {
            PShopAnnouncer ann = _announcers.remove(npcId);
            if (ann != null) ann.stop();
        } catch (Throwable ignore) {}
    }

    // ---- Owner 操作（清單/提領/收攤）— 封包顯示與 escrow 提領可再接 ----
    public void showOwnerList(L1PcInstance pc) {
        try { pc.sendPackets(new S_SystemMessage("PSHOP: Owner 清單（DB 版）待接封包顯示。")); } catch (Throwable ignore) {}
    }
    public void claimEscrow(L1PcInstance pc) {
        try { pc.sendPackets(new S_SystemMessage("PSHOP: 提領收益（DB 版）待接。")); } catch (Throwable ignore) {}
    }
    public void closeStall(L1PcInstance pc, int currencyUnused) {
        L1PShopNpcInstance target = null;
        try {
            for (Object obj : L1World.getInstance().getVisibleObjects(pc)) {
                if (obj instanceof L1PShopNpcInstance) {
                    L1PShopNpcInstance n = (L1PShopNpcInstance) obj;
                    if (n.getOwnerId() == pc.getId()) { target = n; break; }
                }
            }
        } catch (Throwable ignore) {}
        if (target == null) { try { pc.sendPackets(new S_SystemMessage("PSHOP: 找不到可關閉的攤位。")); } catch (Throwable ignore) {} return; }

        int mode = target.getModeItemId();
        int npcId = target.getId();
        int stallId = PShopDB.getInstance().findStallIdByNpcObjId(npcId);
        try { target.deleteMe(); } catch (Throwable ignore) {}
        try { pc.sendPackets(new S_RemoveObject(target)); } catch (Throwable ignore) {}
        rememberClose(pc, mode, npcId);
        if (stallId > 0) PShopDB.getInstance().closeStall(stallId);
        try { pc.sendPackets(new S_SystemMessage("PSHOP: 已收回攤位。")); } catch (Throwable ignore) {}
    }

    // ---- 你要求補齊的兩個方法 ----
    /** 以 ownerId 從世界找回 L1PcInstance（避免 getPlayer(String) 型別不合）。 */
    public L1PcInstance getOwnerPc(L1PShopNpcInstance npc) {
        if (npc == null) return null;
        try {
            L1Object obj = L1World.getInstance().findObject(npc.getOwnerId());
            if (obj instanceof L1PcInstance) return (L1PcInstance) obj;
        } catch (Throwable ignore) {}
        return null;
    }

    /** 攤位 NPC 被移除時，停止發話並清除 owner→mode 占用。 */
    public void onNpcDeleted(L1PShopNpcInstance npc) {
        if (npc == null) return;
        try {
            PShopAnnouncer ann = _announcers.remove(npc.getId());
            if (ann != null) ann.stop();
        } catch (Throwable ignore) {}

        try {
            Map<Integer, Integer> byMode = _ownerStalls.get(npc.getOwnerId());
            if (byMode != null) {
                byMode.remove(npc.getModeItemId());
                if (byMode.isEmpty()) _ownerStalls.remove(npc.getOwnerId());
            }
        } catch (Throwable ignore) {}
    }
}
