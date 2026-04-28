package l1j.server.server.model.Instance;

import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.shop.PShopManager;

public class L1PShopNpcInstance extends L1NpcInstance {

    private static final long serialVersionUID = 1L;

    private int _ownerId;
    private int _modeItemId;
    private String _customName;

    public L1PShopNpcInstance(l1j.server.server.templates.L1Npc template, int ownerId, int modeItemId) {
        super(template);
        _ownerId = ownerId;
        _modeItemId = modeItemId;
    }

    public int getOwnerId() { return _ownerId; }
    public int getModeItemId() { return _modeItemId; }

    public void setCustomName(String name) { _customName = name; }

    @Override
    public String getName() {
        return _customName != null ? _customName : super.getName();
    }

    @Override
    public void onTalkAction(L1PcInstance pc) {
        // 個人攤位 NPC 不自行處理對話邏輯，
        // 直接呼叫父類別的 onTalkAction，讓遊戲內建的買賣列表生效。
        // 攤主專用對話檔由 C_NPCTalk 判斷後發送，這裡不處理。
        super.onTalkAction(pc);
    }

    @Override
    public void deleteMe() {
        try {
            PShopManager.getInstance().onNpcDeleted(this);
        } catch (Throwable ignore) {}
        super.deleteMe();
    }
}
