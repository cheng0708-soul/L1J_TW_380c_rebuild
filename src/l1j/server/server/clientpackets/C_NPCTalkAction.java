/**
 *                            License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS  
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). 
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.  
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR  
 * COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND  
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE  
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED 
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */
package l1j.server.server.clientpackets;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import l1j.server.server.ClientThread;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.Config;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 處理收到由客戶端傳來NPC講話動作的封包
 */
public class C_NPCTalkAction extends ClientBasePacket {
    // Mute PSHOP TRACE messages (UI spam)
    private static final boolean MUTE_PSHOP_TRACE = true;



	private static final String C_NPC_TALK_ACTION = "[C] C_NPCTalkAction";
	private static Logger _log = Logger.getLogger(C_NPCTalkAction.class
			.getName());

	public C_NPCTalkAction(byte decrypt[], ClientThread client)
			throws FileNotFoundException, Exception {
		super(decrypt);
		
		L1PcInstance activeChar = client.getActiveChar();
		if (activeChar == null) {
			return;
		}
		
		int objectId = readD();
		String action = readS();

        
		// /who 專用對話選單的 bypass 處理
		if (action != null) {
			String cmd = action.trim();
			String lower = cmd.toLowerCase(java.util.Locale.ROOT);
			if (lower.startsWith("who ")) {
				String arg = lower.substring(4).trim();

				if (arg.equals("online")) {
					int amount = L1World.getInstance().getAllPlayers().size();
					activeChar.sendPackets(new S_SystemMessage("目前線上人數: " + amount));
				} else if (arg.equals("droprate")) {
					activeChar.sendPackets(new S_SystemMessage("目前掉寶倍率: " + Config.RATE_DROP_ITEMS + " 倍，金幣倍率: " + Config.RATE_DROP_ADENA + " 倍。"));
				} else if (arg.equals("exprate")) {
					activeChar.sendPackets(new S_SystemMessage("目前經驗倍率: " + Config.RATE_XP + " 倍。"));
				} else if (arg.equals("friendly")) {
					activeChar.sendPackets(new S_SystemMessage("友好倍率: " + Config.RATE_KARMA + " 倍，正義倍率: " + Config.RATE_LA + " 倍。"));
				} else if (arg.equals("restart")) {
					activeChar.sendPackets(new S_SystemMessage("伺服器重開機時間請參考公告或官網說明。"));
				} else if (arg.equals("nowtime")) {
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String now = sdf.format(new java.util.Date());
					activeChar.sendPackets(new S_SystemMessage("目前時間: " + now));
				} else if (arg.equals("personaldrop")) {
					// 這裡僅示範：個人掉寶倍率 = 全域掉寶倍率，若你有個人加成效果可自行替換
					activeChar.sendPackets(new S_SystemMessage("你目前的個人掉寶倍率為 " + Config.RATE_DROP_ITEMS + " 倍。"));
				} else if (arg.equals("enchant")) {
					activeChar.sendPackets(new S_SystemMessage("武器強化機率: " + Config.ENCHANT_CHANCE_WEAPON + "%，防具強化機率: " + Config.ENCHANT_CHANCE_ARMOR + "%。"));
				} else if (arg.equals("attr")) {
					activeChar.sendPackets(new S_SystemMessage("武器屬性卷軸成功率: " + Config.ATTR_ENCHANT_CHANCE + "%。"));
				}
				return;
			}
		}
// === PRIEST DIALOG (NPCTalkAction) BEGIN ===
        try {
            l1j.server.server.model.L1Object __obj = l1j.server.server.model.L1World.getInstance().findObject(objectId);
            if (__obj instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                l1j.server.server.model.Instance.L1NpcInstance npc = (l1j.server.server.model.Instance.L1NpcInstance) __obj;
                int __nid = (npc.getNpcTemplate() != null) ? npc.getNpcTemplate().get_npcId() : 0;
                if (__nid==961123 || __nid==961124 || __nid==961125 || __nid==961126 || __nid==961127) {
                    String cmd = (action != null) ? action.trim() : "";
                    String lower = cmd.toLowerCase(java.util.Locale.ROOT);
                    String arg = lower;
                    if (arg.startsWith(".priest")) arg = arg.substring(7).trim();
                    if (arg.startsWith("priest")) arg = arg.substring(6).trim();
                    if (arg.startsWith("_")) arg = arg.substring(1).trim();

                    int charId = activeChar.getId();
                    boolean handled = false;

                    if (arg.equals("") || arg.equals("refresh")) {
                        handled = true;
                    } else if (arg.equals("on") || arg.equals("enable") || arg.equals("start") || arg.equals("priest_on")) {
                        l1j.server.server.priest.PriestSettingsStore.setAutoSupport(charId, true);
                        l1j.server.server.priest.PriestSupportRunner.startFor(activeChar);
                        handled = true;
                    } else if (arg.equals("toggle") || arg.equals("toggle_support") || arg.equals("priest_toggle")) {
                        l1j.server.server.priest.PriestSettingsStore.Settings st = l1j.server.server.priest.PriestSettingsStore.get(charId);
                        boolean toOn = (st == null) ? true : !st.autoSupport;
                        l1j.server.server.priest.PriestSettingsStore.setAutoSupport(charId, toOn);
                        if (toOn) l1j.server.server.priest.PriestSupportRunner.startFor(activeChar);
                        else l1j.server.server.priest.PriestSupportRunner.stopFor(charId);
                        handled = true;
                    } else if (arg.equals("off") || arg.equals("disable") || arg.equals("stop") || arg.equals("priest_off")) {
                        l1j.server.server.priest.PriestSettingsStore.setAutoSupport(charId, false);
                        l1j.server.server.priest.PriestSupportRunner.stopFor(charId);
                        handled = true;
                    } else if (arg.equals("+10") || arg.equals("priest_plus10") || arg.equals("fanwei_01")) {
                        l1j.server.server.priest.PriestSettingsStore.deltaThreshold(charId, 10);
                        handled = true;
                    } else if (arg.equals("-10") || arg.equals("priest_minus10") || arg.equals("fanwei_02")) {
                        l1j.server.server.priest.PriestSettingsStore.deltaThreshold(charId, -10);
                        handled = true;
                    } else if (arg.startsWith("set")) {
                        String[] sp = arg.split("\\s+");
                        if (sp.length >= 2) {
                            try {
                                int v = Integer.parseInt(sp[1]);
                                l1j.server.server.priest.PriestSettingsStore.setThreshold(charId, v);
                                handled = true;
                            } catch (Exception ignore) {}
                        }
                    }

                    if (arg.equals("info")) {
                        activeChar.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(objectId, "priest_spells"));
                        return;
                    } else if (arg.equals("back_main")) {
                        l1j.server.server.priest.PriestDialogController.open(activeChar, l1j.server.server.priest.BoundPriestFinder.findFor(activeChar), 0, 50, false); return;
                    }

                    if (handled) {
                        int iq = (npc.getNpcTemplate() != null) ? npc.getNpcTemplate().get_int() : 0;
                        l1j.server.server.priest.PriestSettingsStore.Settings st = l1j.server.server.priest.PriestSettingsStore.get(charId);
                        l1j.server.server.priest.PriestSettingsStore.regenTick(charId);
                        String[] data = new String[]{ String.valueOf(iq), String.valueOf(st.healThreshold), (st.autoSupport ? "開" : "關"), String.valueOf(st.mp) };
                        l1j.server.server.priest.PriestDialogController.open(activeChar, l1j.server.server.priest.BoundPriestFinder.findFor(activeChar), 0, 50, false); return;
                    }
                }
            }
        } catch (Throwable __t) { }
        // === PRIEST DIALOG (NPCTalkAction) END ===


// === AFK HOOK BEGIN ===
try {
	String __afk_action__ = action;
	if (__afk_action__ != null) {
		String __afk_lower__ = __afk_action__.toLowerCase(java.util.Locale.ROOT);
		if (__afk_action__.startsWith("sum_") || __afk_lower__.startsWith("afk:")) {
			if (l1j.server.server.afk.AfkUiRouter.tryHandleNpcAction(activeChar, null, __afk_action__, null)) {
				return;
			}
		}
	}
} catch (Throwable t) {
	// 保守：不影響 NPC 原流程
}
// === Daily Sign-in Robust Hook (supports: "signin", "event signin", "event\nsignin", "bypass ... signin", "npc_%objectId% signin") ===
try {
    String __act = action;
    String __low = (__act == null) ? "" : __act.toLowerCase(java.util.Locale.ROOT);
    boolean __doSign = false;

    // 1) Two-string event flow: first is "event", second is "signin"
    if ("event".equals(__low)) {
        try {
            String __evt = readS(); // safe best-effort
            if ("signin".equalsIgnoreCase(__evt)) {
                __doSign = true;
            }
        } catch (Throwable __ignore) {}
    }

    // 2) Single-string variants
    if (!__doSign) {
        if ("signin".equals(__low)) {
            __doSign = true;
        } else if (__low.startsWith("event ")) {
            String __tail = __low.substring(6).trim();
            if ("signin".equals(__tail)) __doSign = true;
        } else if (__low.startsWith("bypass") && __low.contains(" signin")) {
            __doSign = true;
        } else if (__low.startsWith("npc_") && __low.contains(" signin")) {
            __doSign = true;
        }
    }

    if (__doSign) {
        try {
            l1j.server.server.model.SignInService.attemptSignIn(activeChar);
        } catch (Throwable __t) {
            // 保守：避免影響原流程
        }
        return;
    }
} catch (Throwable __tOuter) {
    // ignore
}
// === End Sign-in Hook ===

// === AFK HOOK END ===


		L1Object obj = L1World.getInstance().findObject(objectId);
		if (obj == null) {
			_log.warning("object not found, oid " + objectId);
			return;
		}

        try {
            L1NpcInstance npc = (L1NpcInstance) obj;
            // PSHOP: 攤位 NPC 或模板 ID 為 970004 時，依攤主與非攤主送出不同對話
            boolean isPShop = false;
            int ownerId = -1;
            // 判斷是否為攤位類型
            if (npc instanceof l1j.server.server.model.Instance.L1PShopNpcInstance) {
                isPShop = true;
                ownerId = ((l1j.server.server.model.Instance.L1PShopNpcInstance) npc).getOwnerId();
            } else {
                // 非 PShop 但模板 ID 可能是互動 NPC
                try {
                    int npcTmplId = npc.getNpcTemplate().get_npcId();
                    if (npcTmplId == 970004) {
                        // 嘗試取得 owner
                        try {
                            l1j.server.server.model.Instance.L1PShopNpcInstance psNpc = (l1j.server.server.model.Instance.L1PShopNpcInstance) npc;
                            ownerId = psNpc.getOwnerId();
                            isPShop = true;
                        } catch (Throwable ignore) {
                            // 若非真正攤位類型，則無特殊處理
                            isPShop = false;
                        }
                    }
                } catch (Throwable ignore) {}
            }
            if (isPShop) {
                // 記錄點擊的 NPC objectId，供後續查看清單使用
                try { activeChar.setTempID(npc.getId()); } catch (Throwable ignore) {}
                if (ownerId == activeChar.getId()) {
                    activeChar.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(npc.getId(), "pshop_owner"));
                } else {
                    activeChar.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(npc.getId(), "pshop_shop"));
                }
                return;
            }
            npc.onFinalAction(activeChar, action);
        } catch (ClassCastException e) {
        }
	}

	@Override
	public String getType() {
		return C_NPC_TALK_ACTION;
	}

}
