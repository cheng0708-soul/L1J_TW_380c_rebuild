/**
 * License THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). THE WORK IS PROTECTED
 * BY COPYRIGHT AND/OR OTHER APPLICABLE LAW. ANY USE OF THE WORK OTHER THAN AS
 * AUTHORIZED UNDER THIS LICENSE OR COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND AGREE TO
 * BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE MAY BE
 * CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */

package l1j.server.server.clientpackets;

import static l1j.server.server.model.skill.L1SkillId.AWAKEN_ANTHARAS;
import static l1j.server.server.model.skill.L1SkillId.AWAKEN_FAFURION;
import static l1j.server.server.model.skill.L1SkillId.AWAKEN_VALAKAS;
import static l1j.server.server.model.skill.L1SkillId.BLESSED_ARMOR;
import static l1j.server.server.model.skill.L1SkillId.CANCELLATION;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_BLESS_OF_CRAY;
import static l1j.server.server.model.skill.L1SkillId.EFFECT_BLESS_OF_SAELL;
import static l1j.server.server.model.skill.L1SkillId.ELEMENTAL_PROTECTION;
import static l1j.server.server.model.skill.L1SkillId.ENCHANT_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.SHAPE_CHANGE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CURSE_BARLOG;
import static l1j.server.server.model.skill.L1SkillId.STATUS_CURSE_YAHEE;
import static l1j.server.server.model.skill.L1SkillId.STATUS_HASTE;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ClientThread;
import l1j.server.server.HomeTownTimeController;
import l1j.server.server.WarTimeController;
import l1j.server.server.datatables.CastleTable;
import l1j.server.server.datatables.DailyQuestTable;
import l1j.server.server.datatables.DoorTable;
import l1j.server.server.datatables.ExpTable;
import l1j.server.server.datatables.HouseTable;
import l1j.server.server.datatables.InnKeyTable;
import l1j.server.server.datatables.InnTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.NpcActionTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.datatables.PolyTable;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.datatables.TownTable;
import l1j.server.server.datatables.UBTable;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.L1HauntedHouse;
import l1j.server.server.model.L1HouseLocation;
import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1PcInventory;
import l1j.server.server.model.L1PetMatch;
import l1j.server.server.model.L1PolyMorph;
import l1j.server.server.model.L1Quest;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1TownLocation;
import l1j.server.server.model.L1UltimateBattle;
import l1j.server.server.model.L1World;
import l1j.server.server.model.PortableShop;
import l1j.server.server.model.SellAllService;
import l1j.server.server.model.SignInService;
import l1j.server.server.model.Instance.L1DoorInstance;
import l1j.server.server.model.Instance.L1HousekeeperInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1MerchantInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.game.L1PolyRace;
import l1j.server.server.model.identity.L1ItemId;
import l1j.server.server.model.npc.L1NpcHtml;
import l1j.server.server.model.npc.action.L1NpcAction;
import l1j.server.server.model.skill.L1BuffUtil;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_ApplyAuction;
import l1j.server.server.serverpackets.S_AuctionBoardRead;
import l1j.server.server.serverpackets.S_CharReset;
import l1j.server.server.serverpackets.S_DelSkill;
import l1j.server.server.serverpackets.S_Deposit;
import l1j.server.server.serverpackets.S_Drawal;
import l1j.server.server.serverpackets.S_HPUpdate;
import l1j.server.server.serverpackets.S_HouseMap;
import l1j.server.server.serverpackets.S_HowManyKey;
import l1j.server.server.serverpackets.S_ItemName;
import l1j.server.server.serverpackets.S_MPUpdate;
import l1j.server.server.serverpackets.S_Message_YN;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_PetCtrlMenu;
import l1j.server.server.serverpackets.S_PetList;
import l1j.server.server.serverpackets.S_PledgeWarehouseHistory;
import l1j.server.server.serverpackets.S_RetrieveElfList;
import l1j.server.server.serverpackets.S_RetrieveList;
import l1j.server.server.serverpackets.S_RetrievePledgeList;
import l1j.server.server.serverpackets.S_SelectTarget;
import l1j.server.server.serverpackets.S_SellHouse;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_ShopBuyList;
import l1j.server.server.serverpackets.S_ShopSellList;
import l1j.server.server.serverpackets.S_SkillHaste;
import l1j.server.server.serverpackets.S_SkillIconAura;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.S_TaxRate;
import l1j.server.server.templates.L1Castle;
import l1j.server.server.templates.L1House;
import l1j.server.server.templates.L1Inn;
import l1j.server.server.templates.L1Item;
import l1j.server.server.templates.L1Skills;
import l1j.server.server.templates.L1Town;
import l1j.server.server.utils.Random;

/**
 * TODO: 翻譯好 客端NPC
 */
public class C_NPCAction extends ClientBasePacket {
    // Mute PSHOP TRACE messages (UI spam)
    private static final boolean MUTE_PSHOP_TRACE = true;



	private static final String C_NPC_ACTION = "[C] C_NPCAction";

	private static Logger _log = Logger.getLogger(C_NPCAction.class.getName());

	public C_NPCAction(byte abyte0[], ClientThread client) throws Exception {
		super(abyte0);
		
		L1PcInstance pc = client.getActiveChar();
		if (pc == null) {
			return;
		}
		
		int objid = readD();
		String s = readS();
		// /who 話 bypass  (from who_main)
		if (s != null) {
			String cmd = s.trim();
			//  bypass -h , 實
			if (cmd.startsWith("bypass")) {
				int sp = cmd.indexOf(' ');
				if (sp > 0 && sp < cmd.length() - 1) {
					cmd = cmd.substring(sp + 1).trim();
					if (cmd.startsWith("-h")) {
						sp = cmd.indexOf(' ');
						if (sp > 0 && sp < cmd.length() - 1) {
							cmd = cmd.substring(sp + 1).trim();
						}
					}
				}
			}
			if (cmd.startsWith("npc_")) {
				cmd = cmd.substring(4).trim();
			}
						String lower = cmd.toLowerCase(java.util.Locale.ROOT);
						// Sell-all dialog from sell_all.html
						if (lower.equals("sellall_reg")) {
							SellAllService.handleRegister(pc);
							return;
						} else if (lower.equals("sellall_cancel")) {
							SellAllService.handleCancel(pc);
							return;
						} else if (lower.startsWith("sellall_cancel_select")) {
							SellAllService.handleCancelSelect(pc, cmd);
							return;
						} else if (lower.equals("sellall_page_prev")) {
							SellAllService.handlePagePrev(pc);
							return;
						} else if (lower.equals("sellall_page_next")) {
							SellAllService.handlePageNext(pc);
							return;
						} else if (lower.equals("sellall_exec")) {
							SellAllService.handleExecute(pc);
							return;
						} else if (lower.equals("sellall_back")) {
							SellAllService.openMainDialog(pc);
							return;
						}

						// Boss status dialog (boss_status.html)
						if (lower.equals("boss_page_prev") || lower.equals("boss_prev")) {
							l1j.server.server.model.BossStatusService.pagePrev(pc);
							return;
						} else if (lower.equals("boss_page_next") || lower.equals("boss_next")) {
							l1j.server.server.model.BossStatusService.pageNext(pc);
							return;
						}


			if (lower.startsWith("who ")) {
				String arg = lower.substring(4).trim();
				if (arg.equals("online")) {
					int amount = l1j.server.server.model.L1World.getInstance().getAllPlayers().size();
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("人: " + amount));
				} else if (arg.equals("droprate")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("寶: " + l1j.server.Config.RATE_DROP_ITEMS + " : " + l1j.server.Config.RATE_DROP_ADENA + " "));
				} else if (arg.equals("exprate")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage(": " + l1j.server.Config.RATE_XP + " "));
				} else if (arg.equals("friendly")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("好: " + l1j.server.Config.RATE_KARMA + " 義: " + l1j.server.Config.RATE_LA + " "));
				} else if (arg.equals("restart")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("伺諬網說"));
				} else if (arg.equals("nowtime")) {
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String now = sdf.format(new java.util.Date());
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage(": " + now));
				} else if (arg.equals("personaldrop")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("佮人寶 " + l1j.server.Config.RATE_DROP_ITEMS + " "));
				} else if (arg.equals("enchant")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("武強: " + l1j.server.Config.ENCHANT_CHANCE_WEAPON + "%Ｒ強: " + l1j.server.Config.ENCHANT_CHANCE_ARMOR + "%"));
				} else if (arg.equals("attr")) {
					pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("武屬軸: " + l1j.server.Config.ATTR_ENCHANT_CHANCE + "%"));
				}
				return;
			}
		}

// === PSHOP TRACE ROUTE BEGIN ===
try {
    String __cmd = s;
    if (__cmd != null) {
        __cmd = __cmd.trim();
        if (__cmd.startsWith("bypass")) {
            int sp = __cmd.indexOf(' ');
            if (sp > 0 && sp < __cmd.length()-1) {
                __cmd = __cmd.substring(sp + 1).trim();
                if (__cmd.startsWith("-h")) {
                    sp = __cmd.indexOf(' ');
                    if (sp > 0 && sp < __cmd.length()-1) {
                        __cmd = __cmd.substring(sp + 1).trim();
                    }
                }
            }
        }
        if (__cmd.startsWith("npc_")) { __cmd = __cmd.substring(4); }
        // show what we parsed to the player
        if (!MUTE_PSHOP_TRACE) try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("PSHOP TRACE: cmd=" + __cmd)); } catch (Throwable __t2) {}
        if (__cmd.startsWith("pshop_")) {
            if (!MUTE_PSHOP_TRACE) try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("PSHOP TRACE: routing to C_PShopBypass")); } catch (Throwable __t3) {}
            l1j.server.server.clientpackets.C_PShopBypass.handle(pc, __cmd);
            return;
        }
    } else {
        if (!MUTE_PSHOP_TRACE) try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("PSHOP TRACE: cmd is null")); } catch (Throwable __t4) {}
    }
} catch (Throwable __t) {
    if (!MUTE_PSHOP_TRACE) try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("PSHOP TRACE: route err " + __t)); } catch (Throwable __t5) {}
}
// === PSHOP TRACE ROUTE END ===

// === AFK HOOK BEGIN ===
try {
	String __afk_action__ = s;
	if (__afk_action__ != null) {
		String __afk_lower__ = __afk_action__.toLowerCase(java.util.Locale.ROOT);
		if (__afk_action__.startsWith("sum_") || __afk_lower__.startsWith("afk:") || __afk_lower__.startsWith("follow_") || __afk_lower__.startsWith("coop_")) {
			if (l1j.server.server.afk.AfkUiRouter.tryHandleNpcAction(pc, null, __afk_action__, null)) {
				return;
			}
		}
	}
} catch (Throwable t) {
	// 影 NPC 
}
// === AFK HOOK END ===

        // === Daily Sign-in Hook (NPCAction path) ===
        try {
            String __s = s;
            String __low = (__s == null) ? "" : __s.toLowerCase(java.util.Locale.ROOT);
            boolean __doSign = false;
            // direct
            if ("signin".equals(__low)) __doSign = true;
            // event merged string
            else if (__low.startsWith("event ")) {
                String __tail = __low.substring(6).trim();
                if ("signin".equals(__tail)) __doSign = true;
            }
            // bypass or npc_ patterns
            else if (__low.startsWith("bypass") && __low.contains(" signin")) __doSign = true;
            else if (__low.startsWith("npc_") && __low.contains(" signin")) __doSign = true;

            if (__doSign) {
                try { SignInService.attemptSignIn(pc); } catch (Throwable __t) {}
                return;
            }
        } catch (Throwable __tOuter) { }
        // === End Sign-in Hook ===

// === Daily Quest Hook ===
try {
    if (s != null) {
        String dqCmd = s.trim().toLowerCase(java.util.Locale.ROOT);
        if (dqCmd.startsWith("dailyquest_accept") || dqCmd.startsWith("dailyquest_complete")) {
            l1j.server.server.model.L1Object dqObj = l1j.server.server.model.L1World.getInstance().findObject(objid);
            if (dqObj instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                l1j.server.server.model.Instance.L1NpcInstance dqNpc = (l1j.server.server.model.Instance.L1NpcInstance) dqObj;
                int dqNpcId = (dqNpc.getNpcTemplate() != null) ? dqNpc.getNpcTemplate().get_npcId() : 0;
                if (dqNpcId == 970010) { // 毥任 NPC ID
                    if (dqCmd.startsWith("dailyquest_accept")) {
                        DailyQuestTable.getInstance().attemptAccept(pc);
                    } else {
                        DailyQuestTable.getInstance().attemptComplete(pc);
                    }
                    return;
                }
            }
        }
    }
} catch (Exception ex) {
    try {
        pc.sendPackets(new S_SystemMessage("\u6bcf\u65e5\u4efb\u52d9\u8655\u7406\u767c\u751f\u932f\u8aa4\u3002"));
    } catch (Throwable ignore) {}
}
// === End Daily Quest Hook ===




		String s2 = null;
		if (s.equalsIgnoreCase("select") // 賬
				|| s.equalsIgnoreCase("map") // 置確
				|| s.equalsIgnoreCase("apply")) { // 
			s2 = readS();
		} else if (s.equalsIgnoreCase("ent")) {
			L1Object obj = L1World.getInstance().findObject(objid);
			if ((obj != null) && (obj instanceof L1NpcInstance)) {
				if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80088) {
					s2 = readS();
				}
			}
		}

		int[] materials = null;
		int[] counts = null;
		int[] createitem = null;
		int[] createcount = null;
                        // === PRIEST COMPANION DIALOG BEGIN ===

        try {
            l1j.server.server.model.L1Object obj = l1j.server.server.model.L1World.getInstance().findObject(objid);
            if (obj instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                l1j.server.server.model.Instance.L1NpcInstance npc = (l1j.server.server.model.Instance.L1NpcInstance) obj;
                int nid = (npc.getNpcTemplate() != null) ? npc.getNpcTemplate().get_npcId() : 0;
                if (nid==961123 || nid==961124 || nid==961125 || nid==961126 || nid==961127) {
                    String cmd = (s != null) ? s.trim() : "";
                    String lower = cmd.toLowerCase(java.util.Locale.ROOT);
                    String arg = lower.replace("talk npc", "").replace("talk pc", "").trim();
                    if (arg.startsWith(".priest")) arg = arg.substring(7).trim();
                    if (arg.startsWith("priest")) arg = arg.substring(6).trim();
                    if (arg.startsWith("_")) arg = arg.substring(1).trim();

                    int charId = pc.getId();
                    boolean handled = false;

                    if (arg.equals("") || arg.equals("refresh")) {
                        handled = true;
                    } else if (arg.equals("on") || arg.equals("enable") || arg.equals("start") || arg.equals("priest_on")) {
                        l1j.server.server.priest.PriestSettingsStore.setAutoSupport(charId, true);
                        l1j.server.server.priest.PriestSupportRunner.startFor(pc);
                        handled = true;
                    } else if (arg.equals("toggle") || arg.equals("toggle_support") || arg.equals("priest_toggle")) {
                        l1j.server.server.priest.PriestSettingsStore.Settings st = l1j.server.server.priest.PriestSettingsStore.get(charId);
                        boolean toOn = (st == null) ? true : !st.autoSupport;
                        l1j.server.server.priest.PriestSettingsStore.setAutoSupport(charId, toOn);
                        if (toOn) l1j.server.server.priest.PriestSupportRunner.startFor(pc);
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
                        pc.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(objid, "priest_spells"));
                        return;
                    } else if (arg.equals("back_main")) {
                        l1j.server.server.priest.PriestDialogController.open(pc,
                        l1j.server.server.priest.BoundPriestFinder.findFor(pc), 0, 50, false);
                        return;
                    }

                    
                        if (handled) {
                            l1j.server.server.model.Instance.L1ItemInstance __bound =
                                l1j.server.server.priest.BoundPriestFinder.findFor(pc);
                            l1j.server.server.priest.PriestIqDAO2.Row __row =
                                (__bound != null)
                                    ? l1j.server.server.priest.PriestIqDAO2.byItemObjId(__bound.getId())
                                    : null;

                            int iq = 0;
                            if (__row != null && __row.iqTotal > 0) {
                                // 已 priest DB Ｔ使 iq_total
                                iq = __row.iqTotal;
                            } else {
                                int __baseInt = 0;
                                if (__bound != null) {
                                    String __name = __bound.getItem().getName();
                                    if (__name != null) {
                                        if (__name.contains("")) __baseInt = 10;
                                        else if (__name.contains("中")) __baseInt = 12;
                                        else if (__name.contains("")) __baseInt = 14;
                                        else if (__name.contains("")) __baseInt = 16;
                                        else if (__name.contains("")) __baseInt = 18;
                                    }
                                    if (__baseInt <= 0) {
                                        __baseInt = l1j.server.server.priest.NpcIntelResolver.getBaseIntByItemAndName(
                                            __bound.getItemId(), __name);
                                    }
                                }
                                int __bonus = (__row != null) ? __row.iqBonus : 0;
                                iq = Math.max(0, __baseInt + __bonus);
                            }

                            int __mpInit = (npc.getNpcTemplate() != null) ? npc.getNpcTemplate().get_mp() : 0;
                            int __regen = 0;
                            switch (nid) {
                                case 961123: __regen = 3; break;
                                case 961124: __regen = 5; break;
                                case 961125: __regen = 7; break;
                                case 961126: __regen = 9; break;
                                case 961127: __regen = 12; break;
                            }
                            l1j.server.server.priest.PriestSettingsStore.Settings st =
                                l1j.server.server.priest.PriestSettingsStore.ensureLoaded(charId, __mpInit, __regen);
                            l1j.server.server.priest.PriestSettingsStore.regenTick(charId);
                            String[] data = new String[]{
                                String.valueOf(iq),
                                String.valueOf(st.healThreshold),
                                (st.autoSupport ? "" : ""),
                                String.valueOf(st.mp)
                            };
                            pc.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(objid, "priest_doll", data));
                            return;
                        }
                }
            }
        } catch (Throwable t) {
        }

// === PRIEST COMPANION DIALOG END ===// === PRIEST COMPANION DIALOG END ===// === PRIEST COMPANION DIALOG END ===
		String htmlid = null;
		String success_htmlid = null;
		String failure_htmlid = null;
		String[] htmldata = null;

		int questid = 0;
		int questvalue = 0;
		int contribution = 0;
		
		L1PcInstance target;
		L1Object obj = L1World.getInstance().findObject(objid);
		/* Portable shop from item (portable.html) */
		if (s.equalsIgnoreCase("portable_buy")) {
			PortableShop.openBuy(pc);
			return;
		} else if (s.equalsIgnoreCase("portable_sell")) {
			PortableShop.openSell(pc);
			return;
		}

		// One-click sell dialog (sell_all.html)
		if (s.equalsIgnoreCase("sellall_reg")) {
			SellAllService.handleRegister(pc);
			return;
		} else if (s.equalsIgnoreCase("sellall_cancel")) {
			SellAllService.handleCancel(pc);
			return;
		} else if (s.startsWith("sellall_cancel_select")) {
			SellAllService.handleCancelSelect(pc, s);
			return;
		} else if (s.equalsIgnoreCase("sellall_exec")) {
			SellAllService.handleExecute(pc);
			return;
		} else if (s.equalsIgnoreCase("sellall_back")) {
			SellAllService.openMainDialog(pc);
			return;
		}

			if (obj != null) {
			if (obj instanceof L1NpcInstance) {
				L1NpcInstance npc = (L1NpcInstance) obj;
        // PSHOP: NPC (81003) from action
        if (npc.getNpcId() == 81003) {
            pc.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(npc.getId(), "pshop_owner"));
            return;
        }
        // PSHOP: NPC 話 (action)
        if (npc.getNpcId() == 970004) {
            pc.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(npc.getId(), "pshop_owner"));
            return;
        }
				int difflocx = Math.abs(pc.getX() - npc.getX());
				int difflocy = Math.abs(pc.getY() - npc.getY());
				if (!(obj instanceof L1PetInstance) && !(obj instanceof L1SummonInstance)) {
					if ((difflocx > 5) || (difflocy > 5)) { // 5以跢話
						return;
					}
				}
				npc.onFinalAction(pc, s);
			} else if (obj instanceof L1PcInstance) {
				// Allow player-id dialogs (for example item-opened tracy1) to execute XML actions.
				L1NpcAction playerAction = NpcActionTable.getInstance().get(s, pc, obj);
				if (playerAction != null) {
					playerAction.execute(s, pc, obj, new byte[0]);
					return;
				}

				target = (L1PcInstance) obj;
				int awakeSkillId = target.getAwakeSkillId();
				if ((awakeSkillId == AWAKEN_ANTHARAS)
						|| (awakeSkillId == AWAKEN_FAFURION)
						|| (awakeSkillId == AWAKEN_VALAKAS)) {
					target.sendPackets(new S_ServerMessage(1384)); // 身
					return;
				}
				if (target.isShapeChange()) {
					L1PolyMorph.handleCommands(target, s);
					target.setShapeChange(false);
				} else {
					L1PolyMorph poly = PolyTable.getInstance().getTemplate(s);
					if ((poly != null) || s.equals("none")) {
						if (target.getInventory().checkItem(40088) && usePolyScroll(target, 40088, s)) {
						}
						if (target.getInventory().checkItem(40096) && usePolyScroll(target, 40096, s)) {
						}
						if (target.getInventory().checkItem(140088) && usePolyScroll(target, 140088, s)) {
						}
					}
				}
				return;
			}
		} else {
			// _log.warning("object not found, oid " + i);
		}

		// XML
		L1NpcAction action = NpcActionTable.getInstance().get(s, pc, obj);
		if (action != null) {
			L1NpcHtml result = action.execute(s, pc, obj, readByte());
			if (result != null) {
				pc.sendPackets(new S_NPCTalkReturn(obj.getId(), result));
			}
			return;
		}

		/*
		 * 
		 */
		if (s.equalsIgnoreCase("buy")) {
			L1NpcInstance npc = (L1NpcInstance) obj;
			// sell 該 NPC 檢
			if (isNpcSellOnly(npc)) {
				return;
			}

			// 販賣
			pc.sendPackets(new S_ShopSellList(objid, pc));
		} else if (s.equalsIgnoreCase("sell")) {
			int npcid = ((L1NpcInstance) obj).getNpcTemplate().get_npcId();
			if ((npcid == 70523) || (npcid == 70805)) { //  or 
				htmlid = "ladar2";
			} else if ((npcid == 70537) || (npcid == 70807)) { //  or 
				htmlid = "farlin2";
			} else if ((npcid == 70525) || (npcid == 70804)) { //  or 
				htmlid = "lien2";
			} else if ((npcid == 50527) || (npcid == 50505) || (npcid == 50519)
					|| (npcid == 50545) || (npcid == 50531) || (npcid == 50529)
					|| (npcid == 50516) || (npcid == 50538) || (npcid == 50518)
					|| (npcid == 50509) || (npcid == 50536) || (npcid == 50520)
					|| (npcid == 50543) || (npcid == 50526) || (npcid == 50512)
					|| (npcid == 50510) || (npcid == 50504) || (npcid == 50525)
					|| (npcid == 50534) || (npcid == 50540) || (npcid == 50515)
					|| (npcid == 50513) || (npcid == 50528) || (npcid == 50533)
					|| (npcid == 50542) || (npcid == 50511) || (npcid == 50501)
					|| (npcid == 50503) || (npcid == 50508) || (npcid == 50514)
					|| (npcid == 50532) || (npcid == 50544) || (npcid == 50524)
					|| (npcid == 50535) || (npcid == 50521) || (npcid == 50517)
					|| (npcid == 50537) || (npcid == 50539) || (npcid == 50507)
					|| (npcid == 50530) || (npcid == 50502) || (npcid == 50506)
					|| (npcid == 50522) || (npcid == 50541) || (npcid == 50523)
					|| (npcid == 50620) || (npcid == 50623) || (npcid == 50619)
					|| (npcid == 50621) || (npcid == 50622) || (npcid == 50624)
					|| (npcid == 50617) || (npcid == 50614) || (npcid == 50618)
					|| (npcid == 50616) || (npcid == 50615) || (npcid == 50626)
					|| (npcid == 50627) || (npcid == 50628) || (npcid == 50629)
					|| (npcid == 50630) || (npcid == 50631)) { // NPC
				String sellHouseMessage = sellHouse(pc, objid, npcid);
				if (sellHouseMessage != null) {
					htmlid = sellHouseMessage;
				}
			} else { // 丬

				// 以買
				if (!hasAnyBuybackSmart(pc, objid)) { pc.sendPackets(new S_NPCTalkReturn(objid, "nosell")); return; } if (!hasAnyBuybackSmart(pc, objid)) { pc.sendPackets(new S_NPCTalkReturn(objid, "nosell")); return; } if (!hasAnyBuybackSmart(pc, objid)) { pc.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(objid, "nosell")); return; } if (!hasAnyBuybackSmart(pc, objid)) { pc.sendPackets(new l1j.server.server.serverpackets.S_NPCTalkReturn(objid, "nosell")); return; } 
// william 置檢沯賣就 nosell 話串 UI
try {
    if (!hasAnyWilliamSellable(pc)) {
        int talkId = (obj instanceof l1j.server.server.model.Instance.L1NpcInstance)
            ? ((l1j.server.server.model.Instance.L1NpcInstance) obj).getId() : objid;
        // 話示並UI
pc.sendPackets(new l1j.server.server.serverpackets.S_ServerMessage(79)); // 沯
pc.sendPackets(new l1j.server.server.serverpackets.S_ShopBuyList(objid, pc)); // 空維UI穩
return;

    }
} catch (Throwable ignore) {}
pc.sendPackets(new S_ShopBuyList(objid, pc));
			}
		} else if ((((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 91002 // 寵競NPC編
				)
				&& s.equalsIgnoreCase("ent")) {
			L1PolyRace.getInstance().enterGame(pc);
		} else if (s.equalsIgnoreCase("retrieve")) { // 人庫
			if (pc.getLevel() >= 5) {
				if (client.getAccount().getWarePassword() > 0) {
					pc.sendPackets(new S_ServerMessage(834));
				} else {
					pc.sendPackets(new S_RetrieveList(objid, pc));
				}
			}
		} else if (s.equalsIgnoreCase("retrieve-elven")) { // 精庫
			if ((pc.getLevel() >= 5) && pc.isElf()) {
				if (pc.isElf() && (pc.getLevel() > 4)) {
					if (client.getAccount().getWarePassword() > 0) {
						pc.sendPackets(new S_ServerMessage(834));
					} else {
						pc.sendPackets(new S_RetrieveElfList(objid, pc));
					}
				}
			}
		} else if (s.equalsIgnoreCase("retrieve-pledge")) { // 庫
			if (pc.getLevel() >= 5) {
				if (pc.getClanid() == 0) {
					// \f1庫使
					pc.sendPackets(new S_ServerMessage(208));
					return;
				}
				int rank = pc.getClanRank();
				if ((rank != L1Clan.CLAN_RANK_PROBATION)
						&& (rank != L1Clan.CLAN_RANK_GUARDIAN)
						&& (rank != L1Clan.CLAN_RANK_PRINCE)
						&& (rank != L1Clan.CLAN_RANK_LEAGUE_PUBLIC)
						&& (rank != L1Clan.CLAN_RANK_LEAGUE_PROBATION)
						&& (rank != L1Clan.CLAN_RANK_LEAGUE_GUARDIAN)
						&& (rank != L1Clan.CLAN_RANK_LEAGUE_VICEPRINCE)
						&& (rank != L1Clan.CLAN_RANK_LEAGUE_PRINCE)) {
					// 衡衡庫
					pc.sendPackets(new S_ServerMessage(728));
					return;
				}
				if (client.getAccount().getWarePassword() > 0) {
					pc.sendPackets(new S_ServerMessage(834));
				} else {
					pc.sendPackets(new S_RetrievePledgeList(objid, pc));
				}
			}
		} else if(s.equalsIgnoreCase("history")){ // 確庫使 
			pc.sendPackets(new S_PledgeWarehouseHistory(pc.getClanid()));
		} else if (s.equalsIgnoreCase("get")) {
			L1NpcInstance npc = (L1NpcInstance) obj;
			int npcId = npc.getNpcTemplate().get_npcId();
			//  or 
			if ((npcId == 70099) || (npcId == 70796)) {
				L1ItemInstance item = pc.getInventory().storeItem(20081, 1); // 
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				pc.getQuest().set_end(L1Quest.QUEST_OILSKINMANT);
				htmlid = ""; // 
			}
			// HomeTown 管 禩
			else if ((npcId == 70528) || (npcId == 70546) || (npcId == 70567)
					|| (npcId == 70594) || (npcId == 70654) || (npcId == 70748)
					|| (npcId == 70774) || (npcId == 70799) || (npcId == 70815)
					|| (npcId == 70860)) {

				int townId = pc.getHomeTownId();
				int pay = pc.getPay();
				int cb = pc.getContribution(); // 貢度
				htmlid = "";
				if (pay < 1) {
					pc.sendPackets(new S_ServerMessage(767));// 沯費諨
				} else if ((pay > 0) && (cb < 500)) {
					pc.sendPackets(new S_ServerMessage(766));// 貢度足徰
				} else if (townId > 0) {
					double payBonus = 1.0; // cb > 499 && cb < 1000
					boolean isLeader = TownTable.getInstance().isLeader(pc,
							townId); // 
					L1ItemInstance item = pc.getInventory().findItemId(
							L1ItemId.ADENA);
					if ((cb > 999) && (cb < 1500)) {
						payBonus = 1.5;
					} else if ((cb > 1499) && (cb < 2000)) {
						payBonus = 2.0;
					} else if ((cb > 1999) && (cb < 2500)) {
						payBonus = 2.5;
					} else if ((cb > 2499) && (cb < 3000)) {
						payBonus = 3.0;
					} else if (cb > 2999) {
						payBonus = 4.0;
					}
					if (isLeader) {
						payBonus++;
					}
					if ((item != null)
							&& (item.getCount() + pay * payBonus > 2000000000)) {
						pc.sendPackets(new S_ServerMessage(166,"2,000,000,000"));
						htmlid = "";
					} else if ((item != null)
							&& (item.getCount() + pay * payBonus < 2000000001)) {
						pay = (int) (HomeTownTimeController.getPay(pc.getId()) * payBonus);
						pc.getInventory().storeItem(L1ItemId.ADENA, pay);
						pc.sendPackets(new S_ServerMessage(761, "" + pay));
						pc.setPay(0);
					}
				}
			}
		} else if (s.equalsIgnoreCase("townscore")) {// 確貢度
			L1NpcInstance npc = (L1NpcInstance) obj;
			int npcId = npc.getNpcTemplate().get_npcId();
			if ((npcId == 70528) || (npcId == 70546) || (npcId == 70567)
					|| (npcId == 70594) || (npcId == 70654) || (npcId == 70748)
					|| (npcId == 70774) || (npcId == 70799) || (npcId == 70815)
					|| (npcId == 70860)) {
				if (pc.getHomeTownId() > 0) {
					pc.sendPackets(new S_ServerMessage(1569, String.valueOf(pc
							.getContribution())));
				}
			}
		} else if (s.equalsIgnoreCase("fix")) { // 武修

		} else if (s.equalsIgnoreCase("room")) { // 秿
			L1NpcInstance npc = (L1NpcInstance) obj;
			int npcId = npc.getNpcTemplate().get_npcId();
			boolean canRent = false;
			boolean findRoom = false;
			boolean isRent = false;
			boolean isHall = false;
			int roomNumber = 0;
			byte roomCount = 0;
			for (int i = 0; i < 16; i++) {
				L1Inn inn = InnTable.getInstance().getTemplate(npcId, i);
				if (inn != null) { // 此館NPC为空
					Timestamp dueTime = inn.getDueTime();
					Calendar cal = Calendar.getInstance();
					long checkDueTime = (cal.getTimeInMillis() - dueTime.getTime()) / 1000;
					if (inn.getLodgerId() == pc.getId() && checkDueTime < 0) { // 秨人
						if (inn.isHall()) { // 秨議室
							isHall = true;
						}
						isRent = true; // 已
						break;
					} else if (!findRoom && !isRent) { // 尪
						if (checkDueTime >= 0) { // 秨已
							canRent = true;
							findRoom = true;
							roomNumber = inn.getRoomNumber();
						} else { // 箺
							if (!inn.isHall()) { // 丬
								roomCount++;
							}
						}
					}
				}
			}

			if (isRent) {
				if (isHall) {
					htmlid = "inn15"; // 已議廳
				} else {
					htmlid = "inn5"; // 起已秿
				}
			} else if (roomCount >= 12) {
				htmlid = "inn6"; // 好＾
			} else if (canRent) {
				pc.setInnRoomNumber(roomNumber); // 編
				pc.setHall(false); // 丬
				pc.sendPackets(new S_HowManyKey(npc, 300, 1, 8, "inn2"));
			}
		} else if (s.equalsIgnoreCase("hall")
				&& (obj instanceof L1MerchantInstance)) { // 議廳
			if (pc.isCrown()) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				int npcId = npc.getNpcTemplate().get_npcId();
				boolean canRent = false;
				boolean findRoom = false;
				boolean isRent = false;
				boolean isHall = false;
				int roomNumber = 0;
				byte roomCount = 0;
				for (int i = 0; i < 16; i++) {
					L1Inn inn = InnTable.getInstance().getTemplate(npcId, i);
					if (inn != null) { // 此館NPC为空
						Timestamp dueTime = inn.getDueTime();
						Calendar cal = Calendar.getInstance();
						long checkDueTime = (cal.getTimeInMillis() - dueTime
								.getTime()) / 1000;
						if (inn.getLodgerId() == pc.getId() && checkDueTime < 0) { // 秨人
							if (inn.isHall()) { // 秨議室
								isHall = true;
							}
							isRent = true; // 已
							break;
						} else if (!findRoom && !isRent) { // 尪
							if (checkDueTime >= 0) { // 秨已
								canRent = true;
								findRoom = true;
								roomNumber = inn.getRoomNumber();
							} else { // 箺
								if (inn.isHall()) { // 議室
									roomCount++;
								}
							}
						}
					}
				}

				if (isRent) {
					if (isHall) {
						htmlid = "inn15"; // 已議廳
					} else {
						htmlid = "inn5"; // 起已秿
					}
				} else if (roomCount >= 4) {
					htmlid = "inn16"; // 好Ｎ好空議廳
				} else if (canRent) {
					pc.setInnRoomNumber(roomNumber); // 編
					pc.setHall(true); // 議室
					pc.sendPackets(new S_HowManyKey(npc, 300, 1, 8, "inn12"));
				}
			} else {
				// 孬主議廳
				htmlid = "inn10";
			}
		} else if (s.equalsIgnoreCase("return")) { // 
			L1NpcInstance npc = (L1NpcInstance) obj;
			int npcId = npc.getNpcTemplate().get_npcId();
			int price = 0;
			boolean isBreak = false;
			// 秤
			for (int i = 0; i < 16; i++) {
				L1Inn inn = InnTable.getInstance().getTemplate(npcId, i);
				if (inn != null) { // 此館NPC为空
					if (inn.getLodgerId() == pc.getId()) { // 欲秨人
						Timestamp dueTime = inn.getDueTime();
						if (dueTime != null) { // 为空
							Calendar cal = Calendar.getInstance();
							if (((cal.getTimeInMillis() - dueTime.getTime()) / 1000) < 0) { // 秨
								isBreak = true;
								price += 60; //  20%
							}
						}
						Timestamp ts = new Timestamp(System.currentTimeMillis()); // 
						inn.setDueTime(ts); // 
						inn.setLodgerId(0); // 秨人
						inn.setKeyId(0); // 館
						inn.setHall(false);
						// DB
						InnTable.getInstance().updateInn(inn);
						break;
					}
				}
			}
			// 
			for (L1ItemInstance item : pc.getInventory().getItems()) {
				if (item.getInnNpcId() == npcId) { // NPC符
					price += 20 * item.getCount(); //  20 * 
					InnKeyTable.DeleteKey(item); // 
					pc.getInventory().removeItem(item); // 
					isBreak = true;
				}
			}

			if (isBreak) {
				htmldata = new String[] { npc.getName(), String.valueOf(price) };
				htmlid = "inn20";
				pc.getInventory().storeItem(L1ItemId.ADENA, price); // 
			} else {
				htmlid = "";
			}
		} else if (s.equalsIgnoreCase("enter")) { // 議廳
			L1NpcInstance npc = (L1NpcInstance) obj;
			int npcId = npc.getNpcTemplate().get_npcId();

			for (L1ItemInstance item : pc.getInventory().getItems()) {
				if (item.getInnNpcId() == npcId) { // NPC符
					for (int i = 0; i < 16; i++) {
						L1Inn inn = InnTable.getInstance()
								.getTemplate(npcId, i);
						if (inn.getKeyId() == item.getKeyId()) {
							Timestamp dueTime = item.getDueTime();
							if (dueTime != null) { // 为空
								Calendar cal = Calendar.getInstance();
								if (((cal.getTimeInMillis() - dueTime.getTime()) / 1000) < 0) { // 秨
									int[] data = null;
									switch (npcId) {
									case 70012: // 說話 - 
										data = new int[] { 32745, 32803, 16384,
												32743, 32808, 16896 };
										break;
									case 70019: // 魯 - 義
										data = new int[] { 32743, 32803, 17408,
												32744, 32807, 17920 };
										break;
									case 70031: //  - 
										data = new int[] { 32744, 32803, 18432,
												32744, 32807, 18944 };
										break;
									case 70065: //  - 
										data = new int[] { 32744, 32803, 19456,
												32744, 32807, 19968 };
										break;
									case 70070: // 風 - 維
										data = new int[] { 32744, 32803, 20480,
												32744, 32807, 20992 };
										break;
									case 70075: //  - 米德
										data = new int[] { 32744, 32803, 21504,
												32744, 32807, 22016 };
										break;
									case 70084: // 海 - 
										data = new int[] { 32744, 32803, 22528,
												32744, 32807, 23040 };
										break;
									default:
										break;
									}

									pc.setInnKeyId(item.getKeyId()); // 編

									if (!item.checkRoomOrHall()) { // 
										L1Teleport.teleport(pc, data[0],
												data[1], (short) data[2], 6,
												false);
									} else { // 議室
										L1Teleport.teleport(pc, data[3],
												data[4], (short) data[5], 6,
												false);
										break;
									}
								}
							}
						}
					}
				}
			}
		} else if (s.equalsIgnoreCase("openigate")) { //  / 
			L1NpcInstance npc = (L1NpcInstance) obj;
			openCloseGate(pc, npc.getNpcTemplate().get_npcId(), true);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("closeigate")) { //  / 
			L1NpcInstance npc = (L1NpcInstance) obj;
			openCloseGate(pc, npc.getNpcTemplate().get_npcId(), false);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("askwartime")) { //  / 次
			L1NpcInstance npc = (L1NpcInstance) obj;
			if (npc.getNpcTemplate().get_npcId() == 60514) { // 
				htmldata = makeWarTimeStrings(L1CastleLocation.KENT_CASTLE_ID);
				htmlid = "ktguard7";
			} else if (npc.getNpcTemplate().get_npcId() == 60560) { // 
				htmldata = makeWarTimeStrings(L1CastleLocation.OT_CASTLE_ID);
				htmlid = "orcguard7";
			} else if (npc.getNpcTemplate().get_npcId() == 60552) { // 
				htmldata = makeWarTimeStrings(L1CastleLocation.WW_CASTLE_ID);
				htmlid = "wdguard7";
			} else if ((npc.getNpcTemplate().get_npcId() == 60524) || // ()
					(npc.getNpcTemplate().get_npcId() == 60525) || // 
					(npc.getNpcTemplate().get_npcId() == 60529)) { // 
				htmldata = makeWarTimeStrings(L1CastleLocation.GIRAN_CASTLE_ID);
				htmlid = "grguard7";
			} else if (npc.getNpcTemplate().get_npcId() == 70857) { // 
				htmldata = makeWarTimeStrings(L1CastleLocation.HEINE_CASTLE_ID);
				htmlid = "heguard7";
			} else if ((npc.getNpcTemplate().get_npcId() == 60530) || // 
					(npc.getNpcTemplate().get_npcId() == 60531)) {
				htmldata = makeWarTimeStrings(L1CastleLocation.DOWA_CASTLE_ID);
				htmlid = "dcguard7";
			} else if ((npc.getNpcTemplate().get_npcId() == 60533) || // 
																		// 
					(npc.getNpcTemplate().get_npcId() == 60534)) {
				htmldata = makeWarTimeStrings(L1CastleLocation.ADEN_CASTLE_ID);
				htmlid = "adguard7";
			} else if (npc.getNpcTemplate().get_npcId() == 81156) { // 
				htmldata = makeWarTimeStrings(L1CastleLocation.DIAD_CASTLE_ID);
				htmlid = "dfguard3";
			}
		} else if (s.equalsIgnoreCase("inex")) { // /
			// 表示
			// 
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				int castle_id = clan.getCastleId();
				if (castle_id != 0) { // 主
					L1Castle l1castle = CastleTable.getInstance()
							.getCastleTable(castle_id);
					pc.sendPackets(new S_ServerMessage(309, // %0精%1
							l1castle.getName(), String.valueOf(l1castle.getPublicMoney())));
					htmlid = ""; // 
				}
			}
		} else if (s.equalsIgnoreCase("tax")) { // 調
			pc.sendPackets(new S_TaxRate(pc.getId()));
		} else if (s.equalsIgnoreCase("withdrawal")) { // 强
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				int castle_id = clan.getCastleId();
				if (castle_id != 0) { // 主
					L1Castle l1castle = CastleTable.getInstance().getCastleTable(castle_id);
					pc.sendPackets(new S_Drawal(pc.getId(), l1castle.getPublicMoney()));
				}
			}
		} else if (s.equalsIgnoreCase("cdeposit")) { // 賥
			pc.sendPackets(new S_Deposit(pc.getId()));
		} else if (s.equalsIgnoreCase("employ")) { // 

		} else if (s.equalsIgnoreCase("arrange")) { // 

		} else if (s.equalsIgnoreCase("arrange")) { // 置箭

		} else if (s.equalsIgnoreCase("castlegate")) { // 管
			repairGate(pc);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("encw")) { // 武 / 武強
			if (pc.getWeapon() == null) {
				pc.sendPackets(new S_ServerMessage(79));
			} else {
				for (L1ItemInstance item : pc.getInventory().getItems()) {
					if (pc.getWeapon().equals(item)) {
						L1SkillUse l1skilluse = new L1SkillUse();
						l1skilluse.handleCommands(pc, ENCHANT_WEAPON,
								item.getId(), 0, 0, null, 0,
								L1SkillUse.TYPE_SPELLSC);
						break;
					}
				}
			}
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("enca")) { //  / 強
			L1ItemInstance item = pc.getInventory().getItemEquipped(2, 2);
			if (item != null) {
				L1SkillUse l1skilluse = new L1SkillUse();
				l1skilluse.handleCommands(pc, BLESSED_ARMOR, item.getId(), 0,
						0, null, 0, L1SkillUse.TYPE_SPELLSC);
			} else {
				pc.sendPackets(new S_ServerMessage(79));
			}
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("depositnpc")) { // 寵
			for (L1NpcInstance petNpc : pc.getPetList().values()) {
				if (petNpc instanceof L1PetInstance) { // 
					L1PetInstance pet = (L1PetInstance) petNpc;
					pc.sendPackets(new S_PetCtrlMenu(pc, petNpc, false));// 寵形
					// 止飽度
					pet.stopFoodTimer(pet);
					pet.collect(true);
					pc.getPetList().remove(pet.getId());
					pet.deleteMe();
				}
			}
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("withdrawnpc")) { // 寵
			pc.sendPackets(new S_PetList(objid, pc));
		} else if (s.equalsIgnoreCase("aggressive")) { // 
			if (obj instanceof L1PetInstance) {
				L1PetInstance l1pet = (L1PetInstance) obj;
				l1pet.setCurrentPetStatus(1);
			}
		} else if (s.equalsIgnoreCase("defensive")) { // 禦
			if (obj instanceof L1PetInstance) {
				L1PetInstance l1pet = (L1PetInstance) obj;
				l1pet.setCurrentPetStatus(2);
			}
		} else if (s.equalsIgnoreCase("stay")) { // 
			if (obj instanceof L1PetInstance) {
				L1PetInstance l1pet = (L1PetInstance) obj;
				l1pet.setCurrentPetStatus(3);
			}
		} else if (s.equalsIgnoreCase("extend")) { // 
			if (obj instanceof L1PetInstance) {
				L1PetInstance l1pet = (L1PetInstance) obj;
				l1pet.setCurrentPetStatus(4);
			}
		} else if (s.equalsIgnoreCase("alert")) { // 警
			if (obj instanceof L1PetInstance) {
				L1PetInstance l1pet = (L1PetInstance) obj;
				l1pet.setCurrentPetStatus(5);
			}
		} else if (s.equalsIgnoreCase("dismiss")) { // 
			if (obj instanceof L1PetInstance) {
				L1PetInstance l1pet = (L1PetInstance) obj;
				l1pet.setCurrentPetStatus(6);
			}
		} else if (s.equalsIgnoreCase("changename")) { // 決
			pc.setTempID(objid); // ID学
			pc.sendPackets(new S_Message_YN(325, "")); // 決
		} else if (s.equalsIgnoreCase("attackchr")) {
			if (obj instanceof L1Character) {
				L1Character cha = (L1Character) obj;
				pc.sendPackets(new S_SelectTarget(cha.getId()));
			}
		} else if (s.equalsIgnoreCase("select")) { // 競売示
			pc.sendPackets(new S_AuctionBoardRead(objid, s2));
		} else if (s.equalsIgnoreCase("map")) { // 置確
			pc.sendPackets(new S_HouseMap(objid, s2));
		} else if (s.equalsIgnoreCase("apply")) { // 競売
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				if (pc.isCrown() && (pc.getId() == clan.getLeaderId())) { // 主
					if (pc.getLevel() >= 15) {
						if (clan.getHouseId() == 0) {
							pc.sendPackets(new S_ApplyAuction(objid, s2));
						} else {
							pc.sendPackets(new S_ServerMessage(521)); // 家
							htmlid = ""; // 
						}
					} else {
						pc.sendPackets(new S_ServerMessage(519)); // 15主競売
						htmlid = ""; // 
					}
				} else {
					pc.sendPackets(new S_ServerMessage(518)); // 令主
					htmlid = ""; // 
				}
			} else {
				pc.sendPackets(new S_ServerMessage(518)); // 令主
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("open") // 
				|| s.equalsIgnoreCase("close")) { // 
			L1NpcInstance npc = (L1NpcInstance) obj;
			openCloseDoor(pc, npc, s);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("expel")) { // 夨人追
			L1NpcInstance npc = (L1NpcInstance) obj;
			expelOtherClan(pc, npc.getNpcTemplate().get_npcId());
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("pay")) { // 
			L1NpcInstance npc = (L1NpcInstance) obj;
			htmldata = makeHouseTaxStrings(pc, npc);
			htmlid = "agpay";
		} else if (s.equalsIgnoreCase("payfee")) { // 
			L1NpcInstance npc = (L1NpcInstance) obj;
			htmldata = new String[] { npc.getNpcTemplate().get_name(), "2000" };
			htmlid = "";
			if (payFee(pc, npc))
				htmlid = "agpayfee";
		} else if (s.equalsIgnoreCase("name")) { // 家決
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				int houseId = clan.getHouseId();
				if (houseId != 0) {
					L1House house = HouseTable.getInstance().getHouseTable(
							houseId);
					int keeperId = house.getKeeperId();
					L1NpcInstance npc = (L1NpcInstance) obj;
					if (npc.getNpcTemplate().get_npcId() == keeperId) {
						pc.setTempID(houseId); // ID学
						pc.sendPackets(new S_Message_YN(512, "")); // 家
					}
				}
			}
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("rem")) { // 家中家
		} else if (s.equalsIgnoreCase("tel0") // ()
				|| s.equalsIgnoreCase("tel1") // (管)
				|| s.equalsIgnoreCase("tel2") // (罪使)
				|| s.equalsIgnoreCase("tel3")) { // ()
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				int houseId = clan.getHouseId();
				if (houseId != 0) {
					L1House house = HouseTable.getInstance().getHouseTable(
							houseId);
					int keeperId = house.getKeeperId();
					L1NpcInstance npc = (L1NpcInstance) obj;
					if (npc.getNpcTemplate().get_npcId() == keeperId) {
						int[] loc = new int[3];
						if (s.equalsIgnoreCase("tel0")) {
							loc = L1HouseLocation.getHouseTeleportLoc(houseId,
									0);
						} else if (s.equalsIgnoreCase("tel1")) {
							loc = L1HouseLocation.getHouseTeleportLoc(houseId,
									1);
						} else if (s.equalsIgnoreCase("tel2")) {
							loc = L1HouseLocation.getHouseTeleportLoc(houseId,
									2);
						} else if (s.equalsIgnoreCase("tel3")) {
							loc = L1HouseLocation.getHouseTeleportLoc(houseId,
									3);
						}
						L1Teleport.teleport(pc, loc[0], loc[1], (short) loc[2],
								5, true);
					}
				}
			}
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("upgrade")) { // 
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				int houseId = clan.getHouseId();
				if (houseId != 0) {
					L1House house = HouseTable.getInstance().getHouseTable(
							houseId);
					int keeperId = house.getKeeperId();
					L1NpcInstance npc = (L1NpcInstance) obj;
					if (npc.getNpcTemplate().get_npcId() == keeperId) {
						if (pc.isCrown() && (pc.getId() == clan.getLeaderId())) { // 主
							if (house.isPurchaseBasement()) {
								// 
								pc.sendPackets(new S_ServerMessage(1135));
							} else {
								if (pc.getInventory().consumeItem(
										L1ItemId.ADENA, 5000000)) {
									house.setPurchaseBasement(true);
									HouseTable.getInstance().updateHouse(house); // DB込
									// 
									pc.sendPackets(new S_ServerMessage(1099));
								} else {
									// \f1足
									pc.sendPackets(new S_ServerMessage(189));
								}
							}
						} else {
							// 令主
							pc.sendPackets(new S_ServerMessage(518));
						}
					}
				}
			}
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("hall")
				&& (obj instanceof L1HousekeeperInstance)) { // 
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				int houseId = clan.getHouseId();
				if (houseId != 0) {
					L1House house = HouseTable.getInstance().getHouseTable(
							houseId);
					int keeperId = house.getKeeperId();
					L1NpcInstance npc = (L1NpcInstance) obj;
					if (npc.getNpcTemplate().get_npcId() == keeperId) {
						if (house.isPurchaseBasement()) {
							int[] loc = new int[3];
							loc = L1HouseLocation.getBasementLoc(houseId);
							L1Teleport.teleport(pc, loc[0], loc[1],
									(short) (loc[2]), 5, true);
						} else {
							// 
							pc.sendPackets(new S_ServerMessage(1098));
						}
					}
				}
			}
			htmlid = ""; // 
		}

		// ElfAttr:0.,1.,2.,4.水,8.風
		else if (s.equalsIgnoreCase("fire")) // 系
		{
			if (pc.isElf()) {
				if (pc.getElfAttr() != 0) {
					return;
				}
				pc.setElfAttr(2);
				pc.save(); // DB込
				pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_ELF, 1)); // 忽身滿
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("water")) { // 水系
			if (pc.isElf()) {
				if (pc.getElfAttr() != 0) {
					return;
				}
				pc.setElfAttr(4);
				pc.save(); // DB込
				pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_ELF, 2)); // 忽身滿水
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("air")) { // 風系
			if (pc.isElf()) {
				if (pc.getElfAttr() != 0) {
					return;
				}
				pc.setElfAttr(8);
				pc.save(); // DB込
				pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_ELF, 3)); // 忽身滿風
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("earth")) { // 系
			if (pc.isElf()) {
				if (pc.getElfAttr() != 0) {
					return;
				}
				pc.setElfAttr(1);
				pc.save(); // DB込
				pc.sendPackets(new S_PacketBox(S_PacketBox.MSG_ELF, 4)); // 忽身滿
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("init")) { // 精
			if (pc.isElf()) {
				if (pc.getElfAttr() == 0) {
					return;
				}
				for (int cnt = 129; cnt <= 176; cnt++) // 泧
				{
					L1Skills l1skills1 = SkillsTable.getInstance().getTemplate(
							cnt);
					int skill_attr = l1skills1.getAttr();
					if (skill_attr != 0) // 以DB
					{
						SkillsTable.getInstance().spellLost(pc.getId(),
								l1skills1.getSkillId());
					}
				}
				// 屧御
				if (pc.hasSkillEffect(ELEMENTAL_PROTECTION)) {
					pc.removeSkillEffect(ELEMENTAL_PROTECTION);
				}
				pc.sendPackets(new S_DelSkill(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 248, 252, 252, 255, 0, 0, 0, 0, 0,
						0)); // 以泦
				pc.setElfAttr(0);
				pc.save(); // DB込
				pc.sendPackets(new S_ServerMessage(678));
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("exp")) { // 騤復
			if (pc.getExpRes() == 1) {
				int cost = 0;
				int level = pc.getLevel();
				int lawful = pc.getLawful();
				if (level < 45) {
					cost = level * level * 100;
				} else {
					cost = level * level * 200;
				}
				if (lawful >= 0) {
					cost = (cost / 2);
				}
				pc.sendPackets(new S_Message_YN(738, String.valueOf(cost))); // 騤復%0覧騤復
			} else {
				pc.sendPackets(new S_ServerMessage(739)); // 仯騤復
				htmlid = ""; // 
			}
		} else if (s.equalsIgnoreCase("pk")) { // 罪
			if (pc.getLawful() < 30000) {
				pc.sendPackets(new S_ServerMessage(559)); // \f1罪衦
			} else if (pc.get_PKcount() < 5) {
				pc.sendPackets(new S_ServerMessage(560)); // \f1罪覯
			} else {
				if (pc.getInventory().consumeItem(L1ItemId.ADENA, 700000)) {
					pc.set_PKcount(pc.get_PKcount() - 5);
					pc.sendPackets(new S_ServerMessage(561, String.valueOf(pc
							.get_PKcount()))); // PK%0
				} else {
					pc.sendPackets(new S_ServerMessage(189)); // \f1足
				}
			}
			// 
			htmlid = "";
		} else if (s.equalsIgnoreCase("ent")) {
			// 屷
			//  
			// 観覧
			// 
			int npcId = ((L1NpcInstance) obj).getNpcId();
			if (npcId == 80085) {
				htmlid = enterHauntedHouse(pc);
			} else if (npcId == 80088) {
				htmlid = enterPetMatch(pc, Integer.valueOf(s2));
			} else if ((npcId == 50038) || (npcId == 50042) || (npcId == 50029)
					|| (npcId == 50019) || (npcId == 50062)) { // 管人観
				htmlid = watchUb(pc, npcId);
			} else if (npcId == 71251) { // 
				if (!pc.getInventory().checkItem(49142)) { // 帮
					pc.sendPackets(new S_ServerMessage(1290)); // 親
					return;
				}
				L1SkillUse l1skilluse = new L1SkillUse();
				l1skilluse.handleCommands(pc, CANCELLATION, pc.getId(),
						pc.getX(), pc.getY(), null, 0, L1SkillUse.TYPE_LOGIN);
				pc.getInventory().takeoffEquip(945); // polyId
				L1Teleport.teleport(pc, 32737, 32789, (short) 997, 4, false);
				int initStatusPoint = 75 + pc.getElixirStats();
				int pcStatusPoint = pc.getBaseStr() + pc.getBaseInt()
						+ pc.getBaseWis() + pc.getBaseDex() + pc.getBaseCon()
						+ pc.getBaseCha();
				if (pc.getLevel() > 50) {
					pcStatusPoint += (pc.getLevel() - 50 - pc.getBonusStats());
				}
				int diff = pcStatusPoint - initStatusPoint;
				/**
				 * [50以]
				 * 
				 *  -  = 人 - 50 -> 人 = 50 + ( - )
				 */
				int maxLevel = 1;

				if (diff > 0) {
					// 99:就?丯
					maxLevel = Math.min(50 + diff, 99);
				} else {
					maxLevel = pc.getLevel();
				}

				pc.setTempMaxLevel(maxLevel);
				pc.setTempLevel(1);
				pc.setInCharReset(true);
				pc.sendPackets(new S_CharReset(pc));
			} else {
				htmlid = enterUb(pc, npcId);
			}
		} else if (s.equalsIgnoreCase("par")) { // UB  管人
			htmlid = enterUb(pc, ((L1NpcInstance) obj).getNpcId());
		} else if (s.equalsIgnoreCase("info")) { // 確競確
			htmlid = "colos2";
		} else if (s.equalsIgnoreCase("sco")) { // UB徹覧確
			htmldata = new String[10];
			htmlid = "colos3";
		}

		else if (s.equalsIgnoreCase("haste")) { // 
			L1NpcInstance l1npcinstance = (L1NpcInstance) obj;
			int npcid = l1npcinstance.getNpcTemplate().get_npcId();
			if (npcid == 70514) {
				pc.sendPackets(new S_ServerMessage(183));
				pc.sendPackets(new S_SkillHaste(pc.getId(), 1, 1600));
				pc.broadcastPacket(new S_SkillHaste(pc.getId(), 1, 0));
				pc.sendPackets(new S_SkillSound(pc.getId(), 755));
				pc.broadcastPacket(new S_SkillSound(pc.getId(), 755));
				pc.setMoveSpeed(1);
				pc.setSkillEffect(STATUS_HASTE, 1600 * 1000);
				htmlid = ""; // 
			}
		}
		// 身
		else if (s.equalsIgnoreCase("skeleton nbmorph")) {
			poly(client, 2374);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("lycanthrope nbmorph")) {
			poly(client, 3874);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("shelob nbmorph")) {
			poly(client, 95);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("ghoul nbmorph")) {
			poly(client, 3873);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("ghast nbmorph")) {
			poly(client, 3875);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("atuba orc nbmorph")) {
			poly(client, 3868);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("skeleton axeman nbmorph")) {
			poly(client, 2376);
			htmlid = ""; // 
		} else if (s.equalsIgnoreCase("troll nbmorph")) {
			poly(client, 3878);
			htmlid = ""; // 
		}

		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71095) { // 墮
			if (s.equalsIgnoreCase("teleport evil-dungeon")) { // 循念
				boolean find = false;
				for (Object objs : L1World.getInstance().getVisibleObjects(306).values()) {
					if (objs instanceof L1PcInstance) {
						L1PcInstance _pc = (L1PcInstance) objs;
						if (_pc != null) {
							find = true;
							htmlid = "csoulqn"; // 佪念
							break;
						}
					}
				}
				if (!find) {
					L1Quest quest = pc.getQuest();
					int lv50_step = quest.get_step(L1Quest.QUEST_LEVEL50);
					if (lv50_step == L1Quest.QUEST_END) {
						htmlid = "csoulq3";
					} else if (lv50_step >= 3) {
						L1Teleport.teleport(pc, 32747, 32799, (short) 306, 6, true);
					} else {
						htmlid = "csoulq2";
					}
				}
			}
		}
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81279) { // 
																					// -
																					// 
			if (s.equalsIgnoreCase("a")) {
				// 已身
				L1BuffUtil.effectBlessOfDragonSlayer(pc, EFFECT_BLESS_OF_CRAY,
						2400, 7681);
				htmlid = "grayknight2";
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81292) { // 巫女
																					// -
																					// 
			if (s.equalsIgnoreCase("a")) {
				// 巫女繴身
				L1BuffUtil.effectBlessOfDragonSlayer(pc, EFFECT_BLESS_OF_SAELL,
						2400, 7680);
				htmlid = "";
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71038) {
			// 
			if (s.equalsIgnoreCase("A")) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				L1ItemInstance item = pc.getInventory().storeItem(41060, 1); // 
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				htmlid = "orcfnoname9";
			}
			// 調
			else if (s.equalsIgnoreCase("Z")) {
				if (pc.getInventory().consumeItem(41060, 1)) {
					htmlid = "orcfnoname11";
				}
			}
		}
		// - 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71039) {
			// 
			if (s.equalsIgnoreCase("teleportURL")) {
				htmlid = "orcfbuwoo2";
			}
		}
		// 調  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71040) {
			// 
			if (s.equalsIgnoreCase("A")) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				L1ItemInstance item = pc.getInventory().storeItem(41065, 1); // 調証
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				htmlid = "orcfnoa4";
			}
			// 調
			else if (s.equalsIgnoreCase("Z")) {
				if (pc.getInventory().consumeItem(41065, 1)) {
					htmlid = "orcfnoa7";
				}
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71041) {
			// 調
			if (s.equalsIgnoreCase("A")) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				L1ItemInstance item = pc.getInventory().storeItem(41064, 1); // 調証
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				htmlid = "orcfhuwoomo4";
			}
			// 調
			else if (s.equalsIgnoreCase("Z")) {
				if (pc.getInventory().consumeItem(41064, 1)) {
					htmlid = "orcfhuwoomo6";
				}
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71042) {
			// 調
			if (s.equalsIgnoreCase("A")) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				L1ItemInstance item = pc.getInventory().storeItem(41062, 1); // 調証
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				htmlid = "orcfbakumo4";
			}
			// 調
			else if (s.equalsIgnoreCase("Z")) {
				if (pc.getInventory().consumeItem(41062, 1)) {
					htmlid = "orcfbakumo6";
				}
			}
		}
		// - 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71043) {
			// 調
			if (s.equalsIgnoreCase("A")) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				L1ItemInstance item = pc.getInventory().storeItem(41063, 1); // 調証
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				htmlid = "orcfbuka4";
			}
			// 調
			else if (s.equalsIgnoreCase("Z")) {
				if (pc.getInventory().consumeItem(41063, 1)) {
					htmlid = "orcfbuka6";
				}
			}
		}
		// - 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71044) {
			// 調
			if (s.equalsIgnoreCase("A")) {
				L1NpcInstance npc = (L1NpcInstance) obj;
				L1ItemInstance item = pc.getInventory().storeItem(41061, 1); // 調証
				String npcName = npc.getNpcTemplate().get_name();
				String itemName = item.getItem().getName();
				pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
				htmlid = "orcfkame4";
			}
			// 調
			else if (s.equalsIgnoreCase("Z")) {
				if (pc.getInventory().consumeItem(41061, 1)) {
					htmlid = "orcfkame6";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71078) {
			// 
			if (s.equalsIgnoreCase("teleportURL")) {
				htmlid = "usender2";
			}
		}
		// 治
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71080) {
			// 伾
			if (s.equalsIgnoreCase("teleportURL")) {
				htmlid = "amisoo2";
			}
		}
		// 空歪
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80048) {
			// 
			if (s.equalsIgnoreCase("2")) {
				htmlid = ""; // 
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80049) {
			// 迥
			if (s.equalsIgnoreCase("1")) {
				if (pc.getKarma() <= -10000000) {
					pc.setKarma(1000000);
					// 声強
					pc.sendPackets(new S_ServerMessage(1078));
					htmlid = "betray13";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80050) {
			// 秮魯槸
			if (s.equalsIgnoreCase("1")) {
				htmlid = "meet105";
			}
			// 秮魦槫誾
			else if (s.equalsIgnoreCase("2")) {
				if (pc.getInventory().checkItem(40718)) { // 
					htmlid = "meet106";
				} else {
					htmlid = "meet110";
				}
			}
			// 1
			else if (s.equalsIgnoreCase("a")) {
				if (pc.getInventory().consumeItem(40718, 1)) {
					pc.addKarma((int) (-100 * Config.RATE_KARMA));
					// 姿迫
					pc.sendPackets(new S_ServerMessage(1079));
					htmlid = "meet107";
				} else {
					htmlid = "meet104";
				}
			}
			// 10
			else if (s.equalsIgnoreCase("b")) {
				if (pc.getInventory().consumeItem(40718, 10)) {
					pc.addKarma((int) (-1000 * Config.RATE_KARMA));
					// 姿迫
					pc.sendPackets(new S_ServerMessage(1079));
					htmlid = "meet108";
				} else {
					htmlid = "meet104";
				}
			}
			// 100
			else if (s.equalsIgnoreCase("c")) {
				if (pc.getInventory().consumeItem(40718, 100)) {
					pc.addKarma((int) (-10000 * Config.RATE_KARMA));
					// 姿迫
					pc.sendPackets(new S_ServerMessage(1079));
					htmlid = "meet109";
				} else {
					htmlid = "meet104";
				}
			}
			// 槫
			else if (s.equalsIgnoreCase("d")) {
				if (pc.getInventory().checkItem(40615) // 影2
						|| pc.getInventory().checkItem(40616)) { // 影3
					htmlid = "";
				} else {
					L1Teleport.teleport(pc, 32683, 32895, (short) 608, 5, true);
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80052) { // 影
			if (s.equalsIgnoreCase("a")) { // 給
				if (pc.hasSkillEffect(STATUS_CURSE_BARLOG)) { // 影
					pc.killSkillEffectTimer(STATUS_CURSE_BARLOG);
				}
				pc.sendPackets(new S_SkillSound(pc.getId(), 750));
				pc.broadcastPacket(new S_SkillSound(pc.getId(), 750));
				pc.sendPackets(new S_SkillIconAura(221, 1020, 2)); // 影
				pc.setSkillEffect(STATUS_CURSE_BARLOG, 1020 * 1000);
				pc.sendPackets(new S_ServerMessage(1127));
				htmlid = "";
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80053) {
			// 
			if (s.equalsIgnoreCase("a")) {
				//   / 
				int aliceMaterialId = 0;
				int karmaLevel = 0;
				int[] material = null;
				int[] count = null;
				int createItem = 0;
				String successHtmlId = null;
				String htmlId = null;

				int[] aliceMaterialIdList = { 40991, 196, 197, 198, 199, 200,
						201, 202 };
				int[] karmaLevelList = { -1, -2, -3, -4, -5, -6, -7, -8 };
				int[][] materialsList = { { 40995, 40718, 40991 },
						{ 40997, 40718, 196 }, { 40990, 40718, 197 },
						{ 40994, 40718, 198 }, { 40993, 40718, 199 },
						{ 40998, 40718, 200 }, { 40996, 40718, 201 },
						{ 40992, 40718, 202 } };
				int[][] countList = { { 100, 100, 1 }, { 100, 100, 1 },
						{ 100, 100, 1 }, { 50, 100, 1 }, { 50, 100, 1 },
						{ 50, 100, 1 }, { 10, 100, 1 }, { 10, 100, 1 } };
				int[] createItemList = { 196, 197, 198, 199, 200, 201, 202, 203 };
				String[] successHtmlIdList = { "alice_1", "alice_2", "alice_3",
						"alice_4", "alice_5", "alice_6", "alice_7", "alice_8" };
				String[] htmlIdList = { "aliceyet", "alice_1", "alice_2",
						"alice_3", "alice_4", "alice_5", "alice_5", "alice_7" };

				for (int i = 0; i < aliceMaterialIdList.length; i++) {
					if (pc.getInventory().checkItem(aliceMaterialIdList[i])) {
						aliceMaterialId = aliceMaterialIdList[i];
						karmaLevel = karmaLevelList[i];
						material = materialsList[i];
						count = countList[i];
						createItem = createItemList[i];
						successHtmlId = successHtmlIdList[i];
						htmlId = htmlIdList[i];
						break;
					}
				}

				if (aliceMaterialId == 0) {
					htmlid = "alice_no";
				} else if (aliceMaterialId == 203) {
					htmlid = "alice_8";
				} else {
					if (pc.getKarmaLevel() <= karmaLevel) {
						materials = material;
						counts = count;
						createitem = new int[] { createItem };
						createcount = new int[] { 1 };
						success_htmlid = successHtmlId;
						failure_htmlid = "alice_no";
					} else {
						htmlid = htmlId;
					}
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80055) {
			L1NpcInstance npc = (L1NpcInstance) obj;
			htmlid = getYaheeAmulet(pc, npc, s);
		}
		// 業管
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80056) {
			L1NpcInstance npc = (L1NpcInstance) obj;
			if (pc.getKarma() <= -10000000) {
				getBloodCrystalByKarma(pc, npc, s);
			}
			htmlid = "";
		}
		// 次()
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80063) {
			// 中
			if (s.equalsIgnoreCase("a")) {
				if (pc.getInventory().checkItem(40921)) { // 紮
					L1Teleport.teleport(pc, 32674, 32832, (short) 603, 2, true);
				} else {
					htmlid = "gpass02";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80064) {
			// 秮永主
			if (s.equalsIgnoreCase("1")) {
				htmlid = "meet005";
			}
			// 秮魦誾
			else if (s.equalsIgnoreCase("2")) {
				if (pc.getInventory().checkItem(40678)) { // 
					htmlid = "meet006";
				} else {
					htmlid = "meet010";
				}
			}
			// 1
			else if (s.equalsIgnoreCase("a")) {
				if (pc.getInventory().consumeItem(40678, 1)) {
					pc.addKarma((int) (100 * Config.RATE_KARMA));
					// 声強
					pc.sendPackets(new S_ServerMessage(1078));
					htmlid = "meet007";
				} else {
					htmlid = "meet004";
				}
			}
			// 10
			else if (s.equalsIgnoreCase("b")) {
				if (pc.getInventory().consumeItem(40678, 10)) {
					pc.addKarma((int) (1000 * Config.RATE_KARMA));
					// 声強
					pc.sendPackets(new S_ServerMessage(1078));
					htmlid = "meet008";
				} else {
					htmlid = "meet004";
				}
			}
			// 100
			else if (s.equalsIgnoreCase("c")) {
				if (pc.getInventory().consumeItem(40678, 100)) {
					pc.addKarma((int) (10000 * Config.RATE_KARMA));
					// 声強
					pc.sendPackets(new S_ServerMessage(1078));
					htmlid = "meet009";
				} else {
					htmlid = "meet004";
				}
			}
			// 
			else if (s.equalsIgnoreCase("d")) {
				if (pc.getInventory().checkItem(40909) // 
						|| pc.getInventory().checkItem(40910) // 水
						|| pc.getInventory().checkItem(40911) // 
						|| pc.getInventory().checkItem(40912) // 風
						|| pc.getInventory().checkItem(40913) // 
						|| pc.getInventory().checkItem(40914) // 水
						|| pc.getInventory().checkItem(40915) // 
						|| pc.getInventory().checkItem(40916) // 風
						|| pc.getInventory().checkItem(40917) // 
						|| pc.getInventory().checkItem(40918) // 水
						|| pc.getInventory().checkItem(40919) // 
						|| pc.getInventory().checkItem(40920) // 風
						|| pc.getInventory().checkItem(40921)) { // 紮
					htmlid = "";
				} else {
					L1Teleport.teleport(pc, 32674, 32832, (short) 602, 2, true);
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80066) {
			// 忥
			if (s.equalsIgnoreCase("1")) {
				if (pc.getKarma() >= 10000000) {
					pc.setKarma(-1000000);
					// 姿迫
					pc.sendPackets(new S_ServerMessage(1079));
					htmlid = "betray03";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80071) {
			L1NpcInstance npc = (L1NpcInstance) obj;
			htmlid = getBarlogEarring(pc, npc, s);
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80073) { // 
			if (s.equalsIgnoreCase("a")) { // 給
				if (pc.hasSkillEffect(STATUS_CURSE_YAHEE)) { // 
					pc.killSkillEffectTimer(STATUS_CURSE_YAHEE);
				}
				pc.sendPackets(new S_SkillSound(pc.getId(), 750));
				pc.broadcastPacket(new S_SkillSound(pc.getId(), 750));
				pc.sendPackets(new S_SkillIconAura(221, 1020, 1)); // 
				pc.setSkillEffect(STATUS_CURSE_YAHEE, 1020 * 1000);
				pc.sendPackets(new S_ServerMessage(1127));
				htmlid = "";
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80072) {
			String sEquals = null;
			int karmaLevel = 0;
			int[] material = null;
			int[] count = null;
			int createItem = 0;
			String failureHtmlId = null;
			String htmlId = null;

			String[] sEqualsList = { "0", "1", "2", "3", "4", "5", "6", "7",
					"8", "a", "b", "c", "d", "e", "f", "g", "h" };
			String[] htmlIdList = { "lsmitha", "lsmithb", "lsmithc", "lsmithd",
					"lsmithe", "", "lsmithf", "lsmithg", "lsmithh" };
			int[] karmaLevelList = { 1, 2, 3, 4, 5, 6, 7, 8 };
			int[][] materialsList = { { 20158, 40669, 40678 },
					{ 20144, 40672, 40678 }, { 20075, 40671, 40678 },
					{ 20183, 40674, 40678 }, { 20190, 40674, 40678 },
					{ 20078, 40674, 40678 }, { 20078, 40670, 40678 },
					{ 40719, 40673, 40678 } };
			int[][] countList = { { 1, 50, 100 }, { 1, 50, 100 },
					{ 1, 50, 100 }, { 1, 20, 100 }, { 1, 40, 100 },
					{ 1, 5, 100 }, { 1, 1, 100 }, { 1, 1, 100 } };
			int[] createItemList = { 20083, 20131, 20069, 20179, 20209, 20290,
					20261, 20031 };
			String[] failureHtmlIdList = { "lsmithaa", "lsmithbb", "lsmithcc",
					"lsmithdd", "lsmithee", "lsmithff", "lsmithgg", "lsmithhh" };

			for (int i = 0; i < sEqualsList.length; i++) {
				if (s.equalsIgnoreCase(sEqualsList[i])) {
					sEquals = sEqualsList[i];
					if (i <= 8) {
						htmlId = htmlIdList[i];
					} else if (i > 8) {
						karmaLevel = karmaLevelList[i - 9];
						material = materialsList[i - 9];
						count = countList[i - 9];
						createItem = createItemList[i - 9];
						failureHtmlId = failureHtmlIdList[i - 9];
					}
					break;
				}
			}
			if (s.equalsIgnoreCase(sEquals)) {
				if ((karmaLevel != 0) && (pc.getKarmaLevel() >= karmaLevel)) {
					materials = material;
					counts = count;
					createitem = new int[] { createItem };
					createcount = new int[] { 1 };
					success_htmlid = "";
					failure_htmlid = failureHtmlId;
				} else {
					htmlid = htmlId;
				}
			}
		}
		// 業管
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80074) {
			L1NpcInstance npc = (L1NpcInstance) obj;
			if (pc.getKarma() >= 10000000) {
				getSoulCrystalByKarma(pc, npc, s);
			}
			htmlid = "";
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80057) {
			htmlid = karmaLevelToHtmlId(pc.getKarmaLevel());
			htmldata = new String[] { String.valueOf(pc.getKarmaPercent()) };
		}
		// 次(風水)
		else if ((((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80059)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80060)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80061)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80062)) {
			htmlid = talkToDimensionDoor(pc, (L1NpcInstance) obj, s);
		}
		//   
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81124) {
			if (s.equalsIgnoreCase("1")) {
				poly(client, 4002);
				htmlid = ""; // 
			} else if (s.equalsIgnoreCase("2")) {
				poly(client, 4004);
				htmlid = ""; // 
			} else if (s.equalsIgnoreCase("3")) {
				poly(client, 4950);
				htmlid = ""; // 
			}
		}

		// 
		// 丬 / 
		else if (s.equalsIgnoreCase("contract1")) {
			pc.getQuest().set_step(L1Quest.QUEST_LYRA, 1);
			htmlid = "lyraev2";
		} else if (s.equalsIgnoreCase("contract1yes") || //  Yes
				s.equalsIgnoreCase("contract1no")) { //  No

			if (s.equalsIgnoreCase("contract1yes")) {
				htmlid = "lyraev5";
			} else if (s.equalsIgnoreCase("contract1no")) {
				pc.getQuest().set_step(L1Quest.QUEST_LYRA, 0);
				htmlid = "lyraev4";
			}
			int totem = 0;
			if (pc.getInventory().checkItem(40131)) {
				totem++;
			}
			if (pc.getInventory().checkItem(40132)) {
				totem++;
			}
			if (pc.getInventory().checkItem(40133)) {
				totem++;
			}
			if (pc.getInventory().checkItem(40134)) {
				totem++;
			}
			if (pc.getInventory().checkItem(40135)) {
				totem++;
			}
			if (totem != 0) {
				materials = new int[totem];
				counts = new int[totem];
				createitem = new int[totem];
				createcount = new int[totem];

				totem = 0;
				if (pc.getInventory().checkItem(40131)) {
					L1ItemInstance l1iteminstance = pc.getInventory()
							.findItemId(40131);
					int i1 = l1iteminstance.getCount();
					materials[totem] = 40131;
					counts[totem] = i1;
					createitem[totem] = L1ItemId.ADENA;
					createcount[totem] = i1 * 50;
					totem++;
				}
				if (pc.getInventory().checkItem(40132)) {
					L1ItemInstance l1iteminstance = pc.getInventory()
							.findItemId(40132);
					int i1 = l1iteminstance.getCount();
					materials[totem] = 40132;
					counts[totem] = i1;
					createitem[totem] = L1ItemId.ADENA;
					createcount[totem] = i1 * 100;
					totem++;
				}
				if (pc.getInventory().checkItem(40133)) {
					L1ItemInstance l1iteminstance = pc.getInventory()
							.findItemId(40133);
					int i1 = l1iteminstance.getCount();
					materials[totem] = 40133;
					counts[totem] = i1;
					createitem[totem] = L1ItemId.ADENA;
					createcount[totem] = i1 * 50;
					totem++;
				}
				if (pc.getInventory().checkItem(40134)) {
					L1ItemInstance l1iteminstance = pc.getInventory()
							.findItemId(40134);
					int i1 = l1iteminstance.getCount();
					materials[totem] = 40134;
					counts[totem] = i1;
					createitem[totem] = L1ItemId.ADENA;
					createcount[totem] = i1 * 30;
					totem++;
				}
				if (pc.getInventory().checkItem(40135)) {
					L1ItemInstance l1iteminstance = pc.getInventory()
							.findItemId(40135);
					int i1 = l1iteminstance.getCount();
					materials[totem] = 40135;
					counts[totem] = i1;
					createitem[totem] = L1ItemId.ADENA;
					createcount[totem] = i1 * 200;
					totem++;
				}
			}
		}
		// 迩
		else if (s.equalsIgnoreCase("pandora6")     // 潵(說話 貨)
				|| s.equalsIgnoreCase("cold6")      // 庫德(說話 )
				|| s.equalsIgnoreCase("balsim3")    // 巴(說話 )
				|| s.equalsIgnoreCase("arieh6")     // 70015: ( )
				|| s.equalsIgnoreCase("andyn3")     // 70016: ( 武)
				|| s.equalsIgnoreCase("ysorya3")    // 70018: 索( 貨)
				|| s.equalsIgnoreCase("luth3")      // 70021: 西(魯 貨)
				|| s.equalsIgnoreCase("catty3")     // 70024: (魯 武)
				|| s.equalsIgnoreCase("mayer3")     // 70030: ( 貨)
				|| s.equalsIgnoreCase("vergil3")    // 70032: ( )
				|| s.equalsIgnoreCase("stella6")    // 70036: ( )
				|| s.equalsIgnoreCase("ralf6")      // 70044: ( 武)
				|| s.equalsIgnoreCase("berry6")     // 70045: ( 貨)
				|| s.equalsIgnoreCase("jin6")       // 70046: ( )
				|| s.equalsIgnoreCase("defman3")    // 70047: 夫( 武)
				|| s.equalsIgnoreCase("mellisa3")   // 70052: 馬( 貨)
				|| s.equalsIgnoreCase("mandra3")    // 70061: 德( 武)
				|| s.equalsIgnoreCase("bius3")      // 70063: ( 貨)
				|| s.equalsIgnoreCase("momo6")      // 70069: (風 )
				|| s.equalsIgnoreCase("ashurEv7")   // 70071: ( 貨)
				|| s.equalsIgnoreCase("elmina3")    // 70072: 米(風 貨)
				|| s.equalsIgnoreCase("glen3")      // 70073: ( 武)
				|| s.equalsIgnoreCase("mellin3")    // 70074: ( 貨)
				|| s.equalsIgnoreCase("orcm6")      // 70078: ( )
				|| s.equalsIgnoreCase("jackson3")   // 70079: ( 貨)
				|| s.equalsIgnoreCase("britt3")     // 70082: (海 貨)
				|| s.equalsIgnoreCase("old6")       // 70085: (海 )
				|| s.equalsIgnoreCase("shivan3")) { // 70083: (海 武)
			htmlid = s;
			int npcid = ((L1NpcInstance) obj).getNpcTemplate().get_npcId();
			int taxRatesCastle = L1CastleLocation
					.getCastleTaxRateByNpcId(npcid);
			htmldata = new String[] { String.valueOf(taxRatesCastle) };
		}
		// 氫
		else if (s.equalsIgnoreCase("set")) {
			if (obj instanceof L1NpcInstance) {
				int npcid = ((L1NpcInstance) obj).getNpcTemplate().get_npcId();
				int town_id = L1TownLocation.getTownIdByNpcid(npcid);

				if ((town_id >= 1) && (town_id <= 10)) {
					if (pc.getHomeTownId() == -1) {
						// \f1氻衪置
						pc.sendPackets(new S_ServerMessage(759));
						htmlid = "";
					} else if (pc.getHomeTownId() > 0) {
						// 
						if (pc.getHomeTownId() != town_id) {
							L1Town town = TownTable.getInstance().getTownTable(
									pc.getHomeTownId());
							if (town != null) {
								// 氻%0
								pc.sendPackets(new S_ServerMessage(758, town
										.get_name()));
							}
							htmlid = "";
						} else {
							// 
							htmlid = "";
						}
					} else if (pc.getHomeTownId() == 0) {
						// 
						if (pc.getLevel() < 10) {
							// \f1氻10以
							pc.sendPackets(new S_ServerMessage(757));
							htmlid = "";
						} else {
							int level = pc.getLevel();
							int cost = level * level * 10;
							if (pc.getInventory().consumeItem(L1ItemId.ADENA,
									cost)) {
								pc.setHomeTownId(town_id);
								pc.setContribution(0); // 念
								pc.save();
							} else {
								// 足
								pc.sendPackets(new S_ServerMessage(337, "$4"));
							}
							htmlid = "";
						}
					}
				}
			}
		}
		// 氻
		else if (s.equalsIgnoreCase("clear")) {
			if (obj instanceof L1NpcInstance) {
				int npcid = ((L1NpcInstance) obj).getNpcTemplate().get_npcId();
				int town_id = L1TownLocation.getTownIdByNpcid(npcid);
				if (town_id > 0) {
					if (pc.getHomeTownId() > 0) {
						if (pc.getHomeTownId() == town_id) {
							pc.setHomeTownId(-1);
							pc.setContribution(0); // 貢度
							pc.save();
						} else {
							// \f1氧
							pc.sendPackets(new S_ServerMessage(756));
						}
					}
					htmlid = "";
				}
			}
		}
		// 誰
		else if (s.equalsIgnoreCase("ask")) {
			if (obj instanceof L1NpcInstance) {
				int npcid = ((L1NpcInstance) obj).getNpcTemplate().get_npcId();
				int town_id = L1TownLocation.getTownIdByNpcid(npcid);

				if ((town_id >= 1) && (town_id <= 10)) {
					L1Town town = TownTable.getInstance().getTownTable(town_id);
					String leader = town.get_leader_name();
					if ((leader != null) && (leader.length() != 0)) {
						htmlid = "owner";
						htmldata = new String[] { leader };
					} else {
						htmlid = "noowner";
					}
				}
			}
		}
		// HomeTown   (涯 for 3.3C)
		else if ((((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70534)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70556)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70572)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70631)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70663)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70761)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70788)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70806)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70830)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70876)) {
			// 
			if (s.equalsIgnoreCase("r")) {
			}
			// 头
			else if (s.equalsIgnoreCase("t")) {

			}
			// 
			else if (s.equalsIgnoreCase("c")) {

			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70997) {
			// 竡
			if (s.equalsIgnoreCase("0")) {
				final int[] item_ids = { 41146, 4, 20322, 173, 40743, };
				final int[] item_amounts = { 1, 1, 1, 1, 500, };
				for (int i = 0; i < item_ids.length; i++) {
					L1ItemInstance item = pc.getInventory().storeItem(
							item_ids[i], item_amounts[i]);
					pc.sendPackets(new S_ServerMessage(143,
							((L1NpcInstance) obj).getNpcTemplate().get_name(),
							item.getLogName()));
				}
				pc.getQuest().set_step(L1Quest.QUEST_DOROMOND, 1);
				htmlid = "jpe0015";
			}
		}
		// ()
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70999) {
			// 紹件渡
			if (s.equalsIgnoreCase("1")) {
				if (pc.getInventory().consumeItem(41146, 1)) {
					final int[] item_ids = { 23, 20219, 20193, };
					final int[] item_amounts = { 1, 1, 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getLogName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_DOROMOND, 2);
					htmlid = "";
				}
			} else if (s.equalsIgnoreCase("2")) {
				L1ItemInstance item = pc.getInventory().storeItem(41227, 1); // 紹
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getLogName()));
				pc.getQuest().set_step(L1Quest.QUEST_AREX, L1Quest.QUEST_END);
				htmlid = "";
			}
		}
		// ()
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71005) {
			// 
			if (s.equalsIgnoreCase("0")) {
				if (!pc.getInventory().checkItem(41209)) {
					L1ItemInstance item = pc.getInventory().storeItem(41209, 1);
					pc.sendPackets(new S_ServerMessage(143,
							((L1NpcInstance) obj).getNpcTemplate().get_name(),
							item.getItem().getName()));
					htmlid = ""; // 
				}
			}
			// 
			else if (s.equalsIgnoreCase("1")) {
				if (pc.getInventory().consumeItem(41213, 1)) {
					L1ItemInstance item = pc.getInventory()
							.storeItem(40029, 20);
					pc.sendPackets(new S_ServerMessage(143,
							((L1NpcInstance) obj).getNpcTemplate().get_name(),
							item.getItem().getName() + " (" + 20 + ")"));
					htmlid = ""; // 
				}
			}
		}
		// ()
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71006) {
			if (s.equalsIgnoreCase("0")) {
				if (pc.getLevel() > 25) {
					htmlid = "jpe0057";
				} else if (pc.getInventory().checkItem(41213)) { // 
					htmlid = "jpe0056";
				} else if (pc.getInventory().checkItem(41210)
						|| pc.getInventory().checkItem(41211)) { // 磨
					htmlid = "jpe0055";
				} else if (pc.getInventory().checkItem(41209)) { // 
					htmlid = "jpe0054";
				} else if (pc.getInventory().checkItem(41212)) { // 製
					htmlid = "jpe0056";
					materials = new int[] { 41212 }; // 製
					counts = new int[] { 1 };
					createitem = new int[] { 41213 }; // 
					createcount = new int[] { 1 };
				} else {
					htmlid = "jpe0057";
				}
			}
		}
		// 治師島中ＨＰ復
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70512) {
			// 治("fullheal")
			if (s.equalsIgnoreCase("0") || s.equalsIgnoreCase("fullheal")) {
				int hp = Random.nextInt(21) + 70;
				pc.setCurrentHp(pc.getCurrentHp() + hp);
				pc.sendPackets(new S_ServerMessage(77));
				pc.sendPackets(new S_SkillSound(pc.getId(), 830));
				pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
				htmlid = ""; // 
			}
		}
		// 治師練HPMP復
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71037) {
			if (s.equalsIgnoreCase("0")) {
				pc.setCurrentHp(pc.getMaxHp());
				pc.setCurrentMp(pc.getMaxMp());
				pc.sendPackets(new S_ServerMessage(77));
				pc.sendPackets(new S_SkillSound(pc.getId(), 830));
				pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
				pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
			}
		}
		// 治師西
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71030) {
			if (s.equalsIgnoreCase("fullheal")) {
				if (pc.getInventory().checkItem(L1ItemId.ADENA, 5)) { // check
					pc.getInventory().consumeItem(L1ItemId.ADENA, 5); // del
					pc.setCurrentHp(pc.getMaxHp());
					pc.setCurrentMp(pc.getMaxMp());
					pc.sendPackets(new S_ServerMessage(77));
					pc.sendPackets(new S_SkillSound(pc.getId(), 830));
					pc.sendPackets(new S_HPUpdate(pc.getCurrentHp(), pc.getMaxHp()));
					pc.sendPackets(new S_MPUpdate(pc.getCurrentMp(), pc.getMaxMp()));
					if (pc.isInParty()) { // 中
						pc.getParty().updateMiniHP(pc);
					}
				} else {
					pc.sendPackets(new S_ServerMessage(337, "$4")); // 足
				}
			}
		}
		// 師
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71002) {
			// 泦
			if (s.equalsIgnoreCase("0")) {
				if (pc.getLevel() <= 13) {
					L1SkillUse skillUse = new L1SkillUse();
					skillUse.handleCommands(pc, CANCELLATION, pc.getId(),
							pc.getX(), pc.getY(), null, 0,
							L1SkillUse.TYPE_NPCBUFF, (L1NpcInstance) obj);
					htmlid = ""; // 
				}
			}
		}
		// ()
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71025) {
			if (s.equalsIgnoreCase("0")) {
				L1ItemInstance item = pc.getInventory().storeItem(41225, 1); // 注
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
				htmlid = "jpe0083";
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71055) {
			// 
			if (s.equalsIgnoreCase("0")) {
				L1ItemInstance item = pc.getInventory().storeItem(40701, 1); // 尪
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
				pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 1);
				htmlid = "lukein8";
			} else if (s.equalsIgnoreCase("2")) {
				htmlid = "lukein12";
				pc.getQuest().set_step(L1Quest.QUEST_RESTA, 3);
			}
		}
		// 尪箱-1
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71063) {
			if (s.equalsIgnoreCase("0")) {
				materials = new int[] { 40701 }; // 尪
				counts = new int[] { 1 };
				createitem = new int[] { 40702 }; // 尪
				createcount = new int[] { 1 };
				htmlid = "maptbox1";
				pc.getQuest().set_end(L1Quest.QUEST_TBOX1);
				int[] nextbox = { 1, 2, 3 };
				int pid = Random.nextInt(nextbox.length);
				int nb = nextbox[pid];
				if (nb == 1) { // b
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 2);
				} else if (nb == 2) { // c
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 3);
				} else if (nb == 3) { // d
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 4);
				}
			}
		}
		// 尪箱-2
		else if ((((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71064)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71065)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71066)) {
			if (s.equalsIgnoreCase("0")) {
				materials = new int[] { 40701 }; // 尪
				counts = new int[] { 1 };
				createitem = new int[] { 40702 }; // 尪
				createcount = new int[] { 1 };
				htmlid = "maptbox1";
				pc.getQuest().set_end(L1Quest.QUEST_TBOX2);
				int[] nextbox2 = { 1, 2, 3, 4, 5, 6 };
				int pid = Random.nextInt(nextbox2.length);
				int nb2 = nextbox2[pid];
				if (nb2 == 1) { // e
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 5);
				} else if (nb2 == 2) { // f
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 6);
				} else if (nb2 == 3) { // g
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 7);
				} else if (nb2 == 4) { // h
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 8);
				} else if (nb2 == 5) { // i
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 9);
				} else if (nb2 == 6) { // j
					pc.getQuest().set_step(L1Quest.QUEST_LUKEIN1, 10);
				}
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71056) {
			// 
			if (s.equalsIgnoreCase("a")) {
				pc.getQuest().set_step(L1Quest.QUEST_SIMIZZ, 1);
				htmlid = "SIMIZZ7";
			} else if (s.equalsIgnoreCase("b")) {
				if (pc.getInventory().checkItem(40661)
						&& pc.getInventory().checkItem(40662)
						&& pc.getInventory().checkItem(40663)) {
					htmlid = "SIMIZZ8";
					pc.getQuest().set_step(L1Quest.QUEST_SIMIZZ, 2);
					materials = new int[] { 40661, 40662, 40663 };
					counts = new int[] { 1, 1, 1 };
					createitem = new int[] { 20044 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "SIMIZZ9";
				}
			} else if (s.equalsIgnoreCase("d")) {
				htmlid = "SIMIZZ12";
				pc.getQuest().set_step(L1Quest.QUEST_SIMIZZ, L1Quest.QUEST_END);
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71057) {
			// 
			if (s.equalsIgnoreCase("3")) {
				htmlid = "doil4";
			} else if (s.equalsIgnoreCase("6")) {
				htmlid = "doil6";
			} else if (s.equalsIgnoreCase("1")) {
				if (pc.getInventory().checkItem(40714)) {
					htmlid = "doil8";
					materials = new int[] { 40714 };
					counts = new int[] { 1 };
					createitem = new int[] { 40647 };
					createcount = new int[] { 1 };
					pc.getQuest().set_step(L1Quest.QUEST_DOIL,
							L1Quest.QUEST_END);
				} else {
					htmlid = "doil7";
				}
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71059) {
			// 
			if (s.equalsIgnoreCase("A")) {
				htmlid = "rudian6";
				L1ItemInstance item = pc.getInventory().storeItem(40700, 1);
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
				pc.getQuest().set_step(L1Quest.QUEST_RUDIAN, 1);
			} else if (s.equalsIgnoreCase("B")) {
				if (pc.getInventory().checkItem(40710)) {
					htmlid = "rudian8";
					materials = new int[] { 40700, 40710 };
					counts = new int[] { 1, 1 };
					createitem = new int[] { 40647 };
					createcount = new int[] { 1 };
					pc.getQuest().set_step(L1Quest.QUEST_RUDIAN,
							L1Quest.QUEST_END);
				} else {
					htmlid = "rudian9";
				}
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71060) {
			// 仲
			if (s.equalsIgnoreCase("A")) {
				if (pc.getQuest().get_step(L1Quest.QUEST_RUDIAN) == L1Quest.QUEST_END) {
					htmlid = "resta6";
				} else {
					htmlid = "resta4";
				}
			} else if (s.equalsIgnoreCase("B")) {
				htmlid = "resta10";
				pc.getQuest().set_step(L1Quest.QUEST_RESTA, 2);
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71061) {
			// 絿
			if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(40647, 3)) {
					htmlid = "cadmus6";
					pc.getInventory().consumeItem(40647, 3);
					pc.getQuest().set_step(L1Quest.QUEST_CADMUS, 2);
				} else {
					htmlid = "cadmus5";
					pc.getQuest().set_step(L1Quest.QUEST_CADMUS, 1);
				}
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71036) {
			if (s.equalsIgnoreCase("a")) {
				htmlid = "kamyla7";
				pc.getQuest().set_step(L1Quest.QUEST_KAMYLA, 1);
			} else if (s.equalsIgnoreCase("c")) {
				htmlid = "kamyla10";
				pc.getInventory().consumeItem(40644, 1);
				pc.getQuest().set_step(L1Quest.QUEST_KAMYLA, 3);
			} else if (s.equalsIgnoreCase("e")) {
				htmlid = "kamyla13";
				pc.getInventory().consumeItem(40630, 1);
				pc.getQuest().set_step(L1Quest.QUEST_KAMYLA, 4);
			} else if (s.equalsIgnoreCase("i")) {
				htmlid = "kamyla25";
			} else if (s.equalsIgnoreCase("b")) { // 迷宮
				if (pc.getQuest().get_step(L1Quest.QUEST_KAMYLA) == 1) {
					L1Teleport.teleport(pc, 32679, 32742, (short) 482, 5, true);
				}
			} else if (s.equalsIgnoreCase("d")) { // 
				if (pc.getQuest().get_step(L1Quest.QUEST_KAMYLA) == 3) {
					L1Teleport.teleport(pc, 32736, 32800, (short) 483, 5, true);
				}
			} else if (s.equalsIgnoreCase("f")) { // 
				if (pc.getQuest().get_step(L1Quest.QUEST_KAMYLA) == 4) {
					L1Teleport.teleport(pc, 32746, 32807, (short) 484, 5, true);
				}
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71089) {
			// 証
			if (s.equalsIgnoreCase("a")) {
				htmlid = "francu10";
				L1ItemInstance item = pc.getInventory().storeItem(40644, 1);
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
				pc.getQuest().set_step(L1Quest.QUEST_KAMYLA, 2);
			}
		}
		// 試練2(海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71090) {
			// 武
			if (s.equalsIgnoreCase("a")) {
				htmlid = "";
				final int[] item_ids = { 246, 247, 248, 249, 40660 };
				final int[] item_amounts = { 1, 1, 1, 1, 5 };
				for (int i = 0; i < item_ids.length; i++) {
					L1ItemInstance item = pc.getInventory().storeItem(
							item_ids[i], item_amounts[i]);
					pc.sendPackets(new S_ServerMessage(143,
							((L1NpcInstance) obj).getNpcTemplate().get_name(),
							item.getItem().getName()));
					pc.getQuest().set_step(L1Quest.QUEST_CRYSTAL, 1);
				}
			} else if (s.equalsIgnoreCase("b")) {
				if (pc.getInventory().checkEquipped(246)
						|| pc.getInventory().checkEquipped(247)
						|| pc.getInventory().checkEquipped(248)
						|| pc.getInventory().checkEquipped(249)) {
					htmlid = "jcrystal5";
				} else if (pc.getInventory().checkItem(40660)) {
					htmlid = "jcrystal4";
				} else {
					pc.getInventory().consumeItem(246, 1);
					pc.getInventory().consumeItem(247, 1);
					pc.getInventory().consumeItem(248, 1);
					pc.getInventory().consumeItem(249, 1);
					pc.getInventory().consumeItem(40620, 1);
					pc.getQuest().set_step(L1Quest.QUEST_CRYSTAL, 2);
					L1Teleport.teleport(pc, 32801, 32895, (short) 483, 4, true);
				}
			} else if (s.equalsIgnoreCase("c")) {
				if (pc.getInventory().checkEquipped(246)
						|| pc.getInventory().checkEquipped(247)
						|| pc.getInventory().checkEquipped(248)
						|| pc.getInventory().checkEquipped(249)) {
					htmlid = "jcrystal5";
				} else {
					pc.getInventory().checkItem(40660);
					L1ItemInstance l1iteminstance = pc.getInventory()
							.findItemId(40660);
					int sc = l1iteminstance.getCount();
					if (sc > 0) {
						pc.getInventory().consumeItem(40660, sc);
					} else {
					}
					pc.getInventory().consumeItem(246, 1);
					pc.getInventory().consumeItem(247, 1);
					pc.getInventory().consumeItem(248, 1);
					pc.getInventory().consumeItem(249, 1);
					pc.getInventory().consumeItem(40620, 1);
					pc.getQuest().set_step(L1Quest.QUEST_CRYSTAL, 0);
					L1Teleport.teleport(pc, 32736, 32800, (short) 483, 4, true);
				}
			}
		}
		// 試練2(海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71091) {
			// 
			if (s.equalsIgnoreCase("a")) {
				htmlid = "";
				pc.getInventory().consumeItem(40654, 1);
				pc.getQuest()
						.set_step(L1Quest.QUEST_CRYSTAL, L1Quest.QUEST_END);
				L1Teleport.teleport(pc, 32744, 32927, (short) 483, 4, true);
			}
		}
		// (海)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71074) {
			// 士
			if (s.equalsIgnoreCase("A")) {
				htmlid = "lelder5";
				pc.getQuest().set_step(L1Quest.QUEST_LIZARD, 1);
				// 宻
			} else if (s.equalsIgnoreCase("B")) {
				htmlid = "lelder10";
				pc.getInventory().consumeItem(40633, 1);
				pc.getQuest().set_step(L1Quest.QUEST_LIZARD, 3);
			} else if (s.equalsIgnoreCase("C")) {
				htmlid = "lelder13";
				if (pc.getQuest().get_step(L1Quest.QUEST_LIZARD) == L1Quest.QUEST_END) {
				}
				materials = new int[] { 40634 };
				counts = new int[] { 1 };
				createitem = new int[] { 20167 }; // 
				createcount = new int[] { 1 };
				pc.getQuest().set_step(L1Quest.QUEST_LIZARD, L1Quest.QUEST_END);
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71198) {
			if (s.equalsIgnoreCase("A")) {
				if ((pc.getQuest().get_step(71198) != 0)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(41339, 5)) { // 亡
					L1ItemInstance item = ItemTable.getInstance().createItem(
							41340); // 
									// 紹
					if (item != null) {
						if (pc.getInventory().checkAddItem(item, 1) == 0) {
							pc.getInventory().storeItem(item);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
											.get_name(), item.getItem()
											.getName())); // \f1%0%1
						}
					}
					pc.getQuest().set_step(71198, 1);
					htmlid = "tion4";
				} else {
					htmlid = "tion9";
				}
			} else if (s.equalsIgnoreCase("B")) {
				if ((pc.getQuest().get_step(71198) != 1)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(41341, 1)) { // 
					pc.getQuest().set_step(71198, 2);
					htmlid = "tion5";
				} else {
					htmlid = "tion10";
				}
			} else if (s.equalsIgnoreCase("C")) {
				if ((pc.getQuest().get_step(71198) != 2)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(41343, 1)) { // 
					L1ItemInstance item = ItemTable.getInstance().createItem(
							21057); // 練士1
					if (item != null) {
						if (pc.getInventory().checkAddItem(item, 1) == 0) {
							pc.getInventory().storeItem(item);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
											.get_name(), item.getItem()
											.getName())); // \f1%0%1
						}
					}
					pc.getQuest().set_step(71198, 3);
					htmlid = "tion6";
				} else {
					htmlid = "tion12";
				}
			} else if (s.equalsIgnoreCase("D")) {
				if ((pc.getQuest().get_step(71198) != 3)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(41344, 1)) { // 水精
					L1ItemInstance item = ItemTable.getInstance().createItem(
							21058); // 練士2
					if (item != null) {
						pc.getInventory().consumeItem(21057, 1); // 練士1
						if (pc.getInventory().checkAddItem(item, 1) == 0) {
							pc.getInventory().storeItem(item);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
											.get_name(), item.getItem()
											.getName())); // \f1%0%1
						}
					}
					pc.getQuest().set_step(71198, 4);
					htmlid = "tion7";
				} else {
					htmlid = "tion13";
				}
			} else if (s.equalsIgnoreCase("E")) {
				if ((pc.getQuest().get_step(71198) != 4)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(41345, 1)) { // 乳
					L1ItemInstance item = ItemTable.getInstance().createItem(
							21059); // 
									// 
									// 
					if (item != null) {
						pc.getInventory().consumeItem(21058, 1); // 練士2
						if (pc.getInventory().checkAddItem(item, 1) == 0) {
							pc.getInventory().storeItem(item);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
											.get_name(), item.getItem()
											.getName())); // \f1%0%1
						}
					}
					pc.getQuest().set_step(71198, 0);
					pc.getQuest().set_step(71199, 0);
					htmlid = "tion8";
				} else {
					htmlid = "tion15";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71199) {
			if (s.equalsIgnoreCase("A")) {
				if ((pc.getQuest().get_step(71199) != 0)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().checkItem(41340, 1)) { //  紹
					pc.getQuest().set_step(71199, 1);
					htmlid = "jeron2";
				} else {
					htmlid = "jeron10";
				}
			} else if (s.equalsIgnoreCase("B")) {
				if ((pc.getQuest().get_step(71199) != 1)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(L1ItemId.ADENA, 1000000)) {
					L1ItemInstance item = ItemTable.getInstance().createItem(
							41341); // 
					if (item != null) {
						if (pc.getInventory().checkAddItem(item, 1) == 0) {
							pc.getInventory().storeItem(item);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
											.get_name(), item.getItem()
											.getName())); // \f1%0%1
						}
					}
					pc.getInventory().consumeItem(41340, 1);
					pc.getQuest().set_step(71199, 255);
					htmlid = "jeron6";
				} else {
					htmlid = "jeron8";
				}
			} else if (s.equalsIgnoreCase("C")) {
				if ((pc.getQuest().get_step(71199) != 1)
						|| pc.getInventory().checkItem(21059, 1)) {
					return;
				}
				if (pc.getInventory().consumeItem(41342, 1)) { // 
					L1ItemInstance item = ItemTable.getInstance().createItem(
							41341); // 
					if (item != null) {
						if (pc.getInventory().checkAddItem(item, 1) == 0) {
							pc.getInventory().storeItem(item);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
											.get_name(), item.getItem()
											.getName())); // \f1%0%1
						}
					}
					pc.getInventory().consumeItem(41340, 1);
					pc.getQuest().set_step(71199, 255);
					htmlid = "jeron5";
				} else {
					htmlid = "jeron9";
				}
			}
		}
		// 師
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80079) {
			// 
			if (s.equalsIgnoreCase("0")) {
				if (!pc.getInventory().checkItem(41312)) { // 師壺
					L1ItemInstance item = pc.getInventory().storeItem(41312, 1);
					if (item != null) {
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName())); // \f1%0%1
						pc.getQuest().set_step(L1Quest.QUEST_KEPLISHA,
								L1Quest.QUEST_END);
					}
					htmlid = "keplisha7";
				}
			}
			// 
			else if (s.equalsIgnoreCase("1")) {
				if (!pc.getInventory().checkItem(41314)) { // 師
					if (pc.getInventory().checkItem(L1ItemId.ADENA, 1000)) {
						materials = new int[] { L1ItemId.ADENA, 41313 }; // 師
						counts = new int[] { 1000, 1 };
						createitem = new int[] { 41314 }; // 師
						createcount = new int[] { 1 };
						int htmlA = Random.nextInt(3) + 1;
						int htmlB = Random.nextInt(100) + 1;
						switch (htmlA) {
						case 1:
							htmlid = "horosa" + htmlB; // horosa1 ~
														// horosa100
							break;
						case 2:
							htmlid = "horosb" + htmlB; // horosb1 ~
														// horosb100
							break;
						case 3:
							htmlid = "horosc" + htmlB; // horosc1 ~
														// horosc100
							break;
						default:
							break;
						}
					} else {
						htmlid = "keplisha8";
					}
				}
			}
			// 
			else if (s.equalsIgnoreCase("2")) {
				if (pc.getTempCharGfx() != pc.getClassId()) {
					htmlid = "keplisha9";
				} else {
					if (pc.getInventory().checkItem(41314)) { // 師
						pc.getInventory().consumeItem(41314, 1); // 師
						int html = Random.nextInt(9) + 1;
						int PolyId = 6180 + Random.nextInt(64);
						polyByKeplisha(client, PolyId);
						switch (html) {
						case 1:
							htmlid = "horomon11";
							break;
						case 2:
							htmlid = "horomon12";
							break;
						case 3:
							htmlid = "horomon13";
							break;
						case 4:
							htmlid = "horomon21";
							break;
						case 5:
							htmlid = "horomon22";
							break;
						case 6:
							htmlid = "horomon23";
							break;
						case 7:
							htmlid = "horomon31";
							break;
						case 8:
							htmlid = "horomon32";
							break;
						case 9:
							htmlid = "horomon33";
							break;
						default:
							break;
						}
					}
				}
			}
			// 壺紴
			else if (s.equalsIgnoreCase("3")) {
				if (pc.getInventory().checkItem(41312)) { // 師壺
					pc.getInventory().consumeItem(41312, 1);
					htmlid = "";
				}
				if (pc.getInventory().checkItem(41313)) { // 師
					pc.getInventory().consumeItem(41313, 1);
					htmlid = "";
				}
				if (pc.getInventory().checkItem(41314)) { // 師
					pc.getInventory().consumeItem(41314, 1);
					htmlid = "";
				}
			}
		}
		//  波 ()
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80082) {
			if (s.equalsIgnoreCase("a")) {
				if (pc.getLevel() < 15) {
					htmlid = "fk_in_lv"; // 汪15以家
				} else if (pc.getInventory().consumeItem(L1ItemId.ADENA, 1000)) {
					L1PolyMorph.undoPoly(pc);
					L1Teleport
							.teleport(pc, 32742, 32799, (short) 5300, 4, true);
				} else {
					htmlid = "fk_in_0";
				}
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80084) {
			// 溪
			if (s.equalsIgnoreCase("q")) {
				if (pc.getInventory().checkItem(41356, 1)) {
					htmlid = "rparum4";
				} else {
					L1ItemInstance item = pc.getInventory().storeItem(41356, 1);
					if (item != null) {
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName())); // \f1%0%1
					}
					htmlid = "rparum3";
				}
			}
		}
		// 馬
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80105) {
			// 
			if (s.equalsIgnoreCase("c")) {
				if (pc.isCrown()) {
					if (pc.getInventory().checkItem(20383, 1)) {
						if (pc.getInventory().checkItem(L1ItemId.ADENA, 100000)) {
							L1ItemInstance item = pc.getInventory().findItemId(
									20383);
							if ((item != null) && (item.getChargeCount() != 50)) {
								item.setChargeCount(50);
								pc.getInventory().updateItem(item, L1PcInventory.COL_CHARGE_COUNT);
								pc.getInventory().consumeItem(L1ItemId.ADENA,
										100000);
								htmlid = "";
							}
						} else {
							pc.sendPackets(new S_ServerMessage(337, "$4")); // 足
						}
					}
				}
			}
		}
		// 室
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71126) {
			// 秾
			if (s.equalsIgnoreCase("B")) {
				if (pc.getInventory().checkItem(41007, 1)) { // 令魮
					htmlid = "eris10";
				} else {
					L1NpcInstance npc = (L1NpcInstance) obj;
					L1ItemInstance item = pc.getInventory().storeItem(41007, 1);
					String npcName = npc.getNpcTemplate().get_name();
					String itemName = item.getItem().getName();
					pc.sendPackets(new S_ServerMessage(143, npcName, itemName));
					htmlid = "eris6";
				}
			} else if (s.equalsIgnoreCase("C")) {
				if (pc.getInventory().checkItem(41009, 1)) { // 令
					htmlid = "eris10";
				} else {
					L1NpcInstance npc = (L1NpcInstance) obj;
					L1ItemInstance item = pc.getInventory().storeItem(41009, 1);
					String npcName = npc.getNpcTemplate().get_name();
					String itemName = item.getItem().getName();
					pc.sendPackets(new S_ServerMessage(143, npcName, itemName));
					htmlid = "eris8";
				}
			} else if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(41007, 1)) { // 令魮
					if (pc.getInventory().checkItem(40969, 20)) { // 魮
						htmlid = "eris18";
						materials = new int[] { 40969, 41007 };
						counts = new int[] { 20, 1 };
						createitem = new int[] { 41008 }; // 
						createcount = new int[] { 1 };
					} else {
						htmlid = "eris5";
					}
				} else {
					htmlid = "eris2";
				}
			} else if (s.equalsIgnoreCase("E")) {
				if (pc.getInventory().checkItem(41010, 1)) { // 
					htmlid = "eris19";
				} else {
					htmlid = "eris7";
				}
			} else if (s.equalsIgnoreCase("D")) {
				if (pc.getInventory().checkItem(41010, 1)) { // 
					htmlid = "eris19";
				} else {
					if (pc.getInventory().checkItem(41009, 1)) { // 令
						if (pc.getInventory().checkItem(40959, 1)) { // 軮
							htmlid = "eris17";
							materials = new int[] { 40959, 41009 }; // 軮
							counts = new int[] { 1, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40960, 1)) { // 軮
							htmlid = "eris16";
							materials = new int[] { 40960, 41009 }; // 軮
							counts = new int[] { 1, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40961, 1)) { // 軮
							htmlid = "eris15";
							materials = new int[] { 40961, 41009 }; // 軮
							counts = new int[] { 1, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40962, 1)) { // 殺
							htmlid = "eris14";
							materials = new int[] { 40962, 41009 }; // 殺
							counts = new int[] { 1, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40635, 10)) { // 軮
							htmlid = "eris12";
							materials = new int[] { 40635, 41009 }; // 軮
							counts = new int[] { 10, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40638, 10)) { // 軮
							htmlid = "eris11";
							materials = new int[] { 40638, 41009 }; // 軮
							counts = new int[] { 10, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40642, 10)) { // 軮
							htmlid = "eris13";
							materials = new int[] { 40642, 41009 }; // 軮
							counts = new int[] { 10, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else if (pc.getInventory().checkItem(40667, 10)) { // 殺
							htmlid = "eris13";
							materials = new int[] { 40667, 41009 }; // 殺
							counts = new int[] { 10, 1 };
							createitem = new int[] { 41010 }; // 
							createcount = new int[] { 1 };
						} else {
							htmlid = "eris8";
						}
					} else {
						htmlid = "eris7";
					}
				}
			}
		}
		// 海
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80076) {
			if (s.equalsIgnoreCase("A")) {
				int[] diaryno = { 49082, 49083 };
				int pid = Random.nextInt(diaryno.length);
				int di = diaryno[pid];
				if (di == 49082) { // 奰
					htmlid = "voyager6a";
					L1NpcInstance npc = (L1NpcInstance) obj;
					L1ItemInstance item = pc.getInventory().storeItem(di, 1);
					String npcName = npc.getNpcTemplate().get_name();
					String itemName = item.getItem().getName();
					pc.sendPackets(new S_ServerMessage(143, npcName, itemName));
				} else if (di == 49083) { // 
					htmlid = "voyager6b";
					L1NpcInstance npc = (L1NpcInstance) obj;
					L1ItemInstance item = pc.getInventory().storeItem(di, 1);
					String npcName = npc.getNpcTemplate().get_name();
					String itemName = item.getItem().getName();
					pc.sendPackets(new S_ServerMessage(143, npcName, itemName));
				}
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71128) {
			if (s.equals("A")) {
				if (pc.getInventory().checkItem(41010, 1)) { // 
					htmlid = "perita2";
				} else {
					htmlid = "perita3";
				}
			} else if (s.equals("p")) {
				// 
				if (pc.getInventory().checkItem(40987, 1) // 
						&& pc.getInventory().checkItem(40988, 1) // 
						&& pc.getInventory().checkItem(40989, 1)) { // 
					htmlid = "perita43";
				} else if (pc.getInventory().checkItem(40987, 1) // 
						&& pc.getInventory().checkItem(40989, 1)) { // 
					htmlid = "perita44";
				} else if (pc.getInventory().checkItem(40987, 1) // 
						&& pc.getInventory().checkItem(40988, 1)) { // 
					htmlid = "perita45";
				} else if (pc.getInventory().checkItem(40988, 1) // 
						&& pc.getInventory().checkItem(40989, 1)) { // 
					htmlid = "perita47";
				} else if (pc.getInventory().checkItem(40987, 1)) { // 
					htmlid = "perita46";
				} else if (pc.getInventory().checkItem(40988, 1)) { // 
					htmlid = "perita49";
				} else if (pc.getInventory().checkItem(40987, 1)) { // 
					htmlid = "perita48";
				} else {
					htmlid = "perita50";
				}
			} else if (s.equals("q")) {
				// 
				if (pc.getInventory().checkItem(41173, 1) // 
						&& pc.getInventory().checkItem(41174, 1) // 
						&& pc.getInventory().checkItem(41175, 1)) { // 
					htmlid = "perita54";
				} else if (pc.getInventory().checkItem(41173, 1) // 
						&& pc.getInventory().checkItem(41175, 1)) { // 
					htmlid = "perita55";
				} else if (pc.getInventory().checkItem(41173, 1) // 
						&& pc.getInventory().checkItem(41174, 1)) { // 
					htmlid = "perita56";
				} else if (pc.getInventory().checkItem(41174, 1) // 
						&& pc.getInventory().checkItem(41175, 1)) { // 
					htmlid = "perita58";
				} else if (pc.getInventory().checkItem(41174, 1)) { // 
					htmlid = "perita57";
				} else if (pc.getInventory().checkItem(41175, 1)) { // 
					htmlid = "perita60";
				} else if (pc.getInventory().checkItem(41176, 1)) { // 
					htmlid = "perita59";
				} else {
					htmlid = "perita61";
				}
			} else if (s.equals("s")) {
				//  
				if (pc.getInventory().checkItem(41161, 1) // 
						&& pc.getInventory().checkItem(41162, 1) // 
						&& pc.getInventory().checkItem(41163, 1)) { // 
					htmlid = "perita62";
				} else if (pc.getInventory().checkItem(41161, 1) // 
						&& pc.getInventory().checkItem(41163, 1)) { // 
					htmlid = "perita63";
				} else if (pc.getInventory().checkItem(41161, 1) // 
						&& pc.getInventory().checkItem(41162, 1)) { // 
					htmlid = "perita64";
				} else if (pc.getInventory().checkItem(41162, 1) // 
						&& pc.getInventory().checkItem(41163, 1)) { // 
					htmlid = "perita66";
				} else if (pc.getInventory().checkItem(41161, 1)) { // 
					htmlid = "perita65";
				} else if (pc.getInventory().checkItem(41162, 1)) { // 
					htmlid = "perita68";
				} else if (pc.getInventory().checkItem(41163, 1)) { // 
					htmlid = "perita67";
				} else {
					htmlid = "perita69";
				}
			} else if (s.equals("B")) {
				// 浮
				if (pc.getInventory().checkItem(40651, 10) // 
						&& pc.getInventory().checkItem(40643, 10) // 水
						&& pc.getInventory().checkItem(40618, 10) // 大
						&& pc.getInventory().checkItem(40645, 10) // 風
						&& pc.getInventory().checkItem(40676, 10) // 
						&& pc.getInventory().checkItem(40442, 5) // 
						&& pc.getInventory().checkItem(40051, 1)) { // 紨
					htmlid = "perita7";
					materials = new int[] { 40651, 40643, 40618, 40645, 40676,
							40442, 40051 };
					counts = new int[] { 10, 10, 10, 10, 20, 5, 1 };
					createitem = new int[] { 40925 }; // 浮
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita8";
				}
			} else if (s.equals("G") || s.equals("h") || s.equals("i")) {
				//  段
				if (pc.getInventory().checkItem(40651, 5) // 
						&& pc.getInventory().checkItem(40643, 5) // 水
						&& pc.getInventory().checkItem(40618, 5) // 大
						&& pc.getInventory().checkItem(40645, 5) // 風
						&& pc.getInventory().checkItem(40676, 5) // 
						&& pc.getInventory().checkItem(40675, 5) // 
						&& pc.getInventory().checkItem(40049, 3) // 紫
						&& pc.getInventory().checkItem(40051, 1)) { // 紨
					htmlid = "perita27";
					materials = new int[] { 40651, 40643, 40618, 40645, 40676,
							40675, 40049, 40051 };
					counts = new int[] { 5, 5, 5, 5, 10, 10, 3, 1 };
					createitem = new int[] { 40926 }; // 段
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita28";
				}
			} else if (s.equals("H") || s.equals("j") || s.equals("k")) {
				//  段
				if (pc.getInventory().checkItem(40651, 10) // 
						&& pc.getInventory().checkItem(40643, 10) // 水
						&& pc.getInventory().checkItem(40618, 10) // 大
						&& pc.getInventory().checkItem(40645, 10) // 風
						&& pc.getInventory().checkItem(40676, 20) // 
						&& pc.getInventory().checkItem(40675, 10) // 
						&& pc.getInventory().checkItem(40048, 3) // 紤
						&& pc.getInventory().checkItem(40051, 1)) { // 紨
					htmlid = "perita29";
					materials = new int[] { 40651, 40643, 40618, 40645, 40676,
							40675, 40048, 40051 };
					counts = new int[] { 10, 10, 10, 10, 20, 10, 3, 1 };
					createitem = new int[] { 40927 }; // 段
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita30";
				}
			} else if (s.equals("I") || s.equals("l") || s.equals("m")) {
				//  段
				if (pc.getInventory().checkItem(40651, 20) // 
						&& pc.getInventory().checkItem(40643, 20) // 水
						&& pc.getInventory().checkItem(40618, 20) // 大
						&& pc.getInventory().checkItem(40645, 20) // 風
						&& pc.getInventory().checkItem(40676, 30) // 
						&& pc.getInventory().checkItem(40675, 10) // 
						&& pc.getInventory().checkItem(40050, 3) // 紵
						&& pc.getInventory().checkItem(40051, 1)) { // 紨
					htmlid = "perita31";
					materials = new int[] { 40651, 40643, 40618, 40645, 40676,
							40675, 40050, 40051 };
					counts = new int[] { 20, 20, 20, 20, 30, 10, 3, 1 };
					createitem = new int[] { 40928 }; // 段
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita32";
				}
			} else if (s.equals("J") || s.equals("n") || s.equals("o")) {
				//  段
				if (pc.getInventory().checkItem(40651, 30) // 
						&& pc.getInventory().checkItem(40643, 30) // 水
						&& pc.getInventory().checkItem(40618, 30) // 大
						&& pc.getInventory().checkItem(40645, 30) // 風
						&& pc.getInventory().checkItem(40676, 30) // 
						&& pc.getInventory().checkItem(40675, 20) // 
						&& pc.getInventory().checkItem(40052, 1) // 紤
						&& pc.getInventory().checkItem(40051, 1)) { // 紨
					htmlid = "perita33";
					materials = new int[] { 40651, 40643, 40618, 40645, 40676,
							40675, 40052, 40051 };
					counts = new int[] { 30, 30, 30, 30, 30, 20, 1, 1 };
					createitem = new int[] { 40928 }; // 段
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita34";
				}
			} else if (s.equals("K")) { // 段(魮)
				int earinga = 0;
				int earingb = 0;
				if (pc.getInventory().checkEquipped(21014)
						|| pc.getInventory().checkEquipped(21006)
						|| pc.getInventory().checkEquipped(21007)) {
					htmlid = "perita36";
				} else if (pc.getInventory().checkItem(21014, 1)) { // 
					earinga = 21014;
					earingb = 41176;
				} else if (pc.getInventory().checkItem(21006, 1)) { // 
					earinga = 21006;
					earingb = 41177;
				} else if (pc.getInventory().checkItem(21007, 1)) { // 
					earinga = 21007;
					earingb = 41178;
				} else {
					htmlid = "perita36";
				}
				if (earinga > 0) {
					materials = new int[] { earinga };
					counts = new int[] { 1 };
					createitem = new int[] { earingb };
					createcount = new int[] { 1 };
				}
			} else if (s.equals("L")) { // 段()
				if (pc.getInventory().checkEquipped(21015)) {
					htmlid = "perita22";
				} else if (pc.getInventory().checkItem(21015, 1)) {
					materials = new int[] { 21015 };
					counts = new int[] { 1 };
					createitem = new int[] { 41179 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita22";
				}
			} else if (s.equals("M")) { // 段(宮)
				if (pc.getInventory().checkEquipped(21016)) {
					htmlid = "perita26";
				} else if (pc.getInventory().checkItem(21016, 1)) {
					materials = new int[] { 21016 };
					counts = new int[] { 1 };
					createitem = new int[] { 41182 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita26";
				}
			} else if (s.equals("b")) { // 段()
				if (pc.getInventory().checkEquipped(21009)) {
					htmlid = "perita39";
				} else if (pc.getInventory().checkItem(21009, 1)) {
					materials = new int[] { 21009 };
					counts = new int[] { 1 };
					createitem = new int[] { 41180 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita39";
				}
			} else if (s.equals("d")) { // 段(誮)
				if (pc.getInventory().checkEquipped(21012)) {
					htmlid = "perita41";
				} else if (pc.getInventory().checkItem(21012, 1)) {
					materials = new int[] { 21012 };
					counts = new int[] { 1 };
					createitem = new int[] { 41183 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita41";
				}
			} else if (s.equals("a")) { // 段()
				if (pc.getInventory().checkEquipped(21008)) {
					htmlid = "perita38";
				} else if (pc.getInventory().checkItem(21008, 1)) {
					materials = new int[] { 21008 };
					counts = new int[] { 1 };
					createitem = new int[] { 41181 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita38";
				}
			} else if (s.equals("c")) { // 段()
				if (pc.getInventory().checkEquipped(21010)) {
					htmlid = "perita40";
				} else if (pc.getInventory().checkItem(21010, 1)) {
					materials = new int[] { 21010 };
					counts = new int[] { 1 };
					createitem = new int[] { 41184 };
					createcount = new int[] { 1 };
				} else {
					htmlid = "perita40";
				}
			}
		}
		// 害細工師 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71129) {
			if (s.equals("Z")) {
				htmlid = "rumtis2";
			} else if (s.equals("Y")) {
				if (pc.getInventory().checkItem(41010, 1)) { // 
					htmlid = "rumtis3";
				} else {
					htmlid = "rumtis4";
				}
			} else if (s.equals("q")) {
				htmlid = "rumtis92";
			} else if (s.equals("A")) {
				if (pc.getInventory().checkItem(41161, 1)) {
					// 
					htmlid = "rumtis6";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("B")) {
				if (pc.getInventory().checkItem(41164, 1)) {
					// 
					htmlid = "rumtis7";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("C")) {
				if (pc.getInventory().checkItem(41167, 1)) {
					// 
					htmlid = "rumtis8";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("T")) {
				if (pc.getInventory().checkItem(41167, 1)) {
					// 
					htmlid = "rumtis9";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("w")) {
				if (pc.getInventory().checkItem(41162, 1)) {
					// 
					htmlid = "rumtis14";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("x")) {
				if (pc.getInventory().checkItem(41165, 1)) {
					// 
					htmlid = "rumtis15";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("y")) {
				if (pc.getInventory().checkItem(41168, 1)) {
					// 
					htmlid = "rumtis16";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("z")) {
				if (pc.getInventory().checkItem(41171, 1)) {
					// 
					htmlid = "rumtis17";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("U")) {
				if (pc.getInventory().checkItem(41163, 1)) {
					// 
					htmlid = "rumtis10";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("V")) {
				if (pc.getInventory().checkItem(41166, 1)) {
					// 
					htmlid = "rumtis11";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("W")) {
				if (pc.getInventory().checkItem(41169, 1)) {
					// 
					htmlid = "rumtis12";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("X")) {
				if (pc.getInventory().checkItem(41172, 1)) {
					// 
					htmlid = "rumtis13";
				} else {
					htmlid = "rumtis101";
				}
			} else if (s.equals("D") || s.equals("E") || s.equals("F")
					|| s.equals("G")) {
				int insn = 0;
				int bacn = 0;
				int me = 0;
				int mr = 0;
				int mj = 0;
				int an = 0;
				int men = 0;
				int mrn = 0;
				int mjn = 0;
				int ann = 0;
				if (pc.getInventory().checkItem(40959, 1) // 軮
						&& pc.getInventory().checkItem(40960, 1) // 軮
						&& pc.getInventory().checkItem(40961, 1) // 軮
						&& pc.getInventory().checkItem(40962, 1)) { // 殺
					insn = 1;
					me = 40959;
					mr = 40960;
					mj = 40961;
					an = 40962;
					men = 1;
					mrn = 1;
					mjn = 1;
					ann = 1;
				} else if (pc.getInventory().checkItem(40642, 10) // 軮
						&& pc.getInventory().checkItem(40635, 10) // 軮
						&& pc.getInventory().checkItem(40638, 10) // 軮
						&& pc.getInventory().checkItem(40667, 10)) { // 殺
					bacn = 1;
					me = 40642;
					mr = 40635;
					mj = 40638;
					an = 40667;
					men = 10;
					mrn = 10;
					mjn = 10;
					ann = 10;
				}
				if (pc.getInventory().checkItem(40046, 1) // 
						&& pc.getInventory().checkItem(40618, 5) // 大
						&& pc.getInventory().checkItem(40643, 5) // 水
						&& pc.getInventory().checkItem(40645, 5) // 風
						&& pc.getInventory().checkItem(40651, 5) // 
						&& pc.getInventory().checkItem(40676, 5)) { // 
					if ((insn == 1) || (bacn == 1)) {
						htmlid = "rumtis60";
						materials = new int[] { me, mr, mj, an, 40046, 40618,
								40643, 40651, 40676 };
						counts = new int[] { men, mrn, mjn, ann, 1, 5, 5, 5, 5,
								5 };
						createitem = new int[] { 40926 }; // 工段
						createcount = new int[] { 1 };
					} else {
						htmlid = "rumtis18";
					}
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71119) {
			// 歴18竾渡
			if (s.equalsIgnoreCase("request las history book")) {
				materials = new int[] { 41019, 41020, 41021, 41022, 41023,
						41024, 41025, 41026 };
				counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1 };
				createitem = new int[] { 41027 };
				createcount = new int[] { 1 };
				htmlid = "";
			}
		}
		// 衡
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71170) {
			// 歴渡
			if (s.equalsIgnoreCase("request las weapon manual")) {
				materials = new int[] { 41027 };
				counts = new int[] { 1 };
				createitem = new int[] { 40965 };
				createcount = new int[] { 1 };
				htmlid = "";
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71168) {
			// 
			if (s.equalsIgnoreCase("a")) {
				if (pc.getInventory().checkItem(41028, 1)) {
					L1Teleport.teleport(pc, 32648, 32921, (short) 535, 6, true);
					pc.getInventory().consumeItem(41028, 1);
				}
			}
		}
		// 諱(欲)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80067) {
			// 諾
			if (s.equalsIgnoreCase("n")) {
				htmlid = "";
				poly(client, 6034);
				final int[] item_ids = { 41132, 41133, 41134 };
				final int[] item_amounts = { 1, 1, 1 };
				for (int i = 0; i < item_ids.length; i++) {
					L1ItemInstance item = pc.getInventory().storeItem(
							item_ids[i], item_amounts[i]);
					pc.sendPackets(new S_ServerMessage(143,
							((L1NpcInstance) obj).getNpcTemplate().get_name(),
							item.getItem().getName()));
					pc.getQuest().set_step(L1Quest.QUEST_DESIRE, 1);
				}
				// 任
			} else if (s.equalsIgnoreCase("d")) {
				htmlid = "minicod09";
				pc.getInventory().consumeItem(41130, 1);
				pc.getInventory().consumeItem(41131, 1);
				// 
			} else if (s.equalsIgnoreCase("k")) {
				htmlid = "";
				pc.getInventory().consumeItem(41132, 1); // 衮
				pc.getInventory().consumeItem(41133, 1); // 衮
				pc.getInventory().consumeItem(41134, 1); // 衮
				pc.getInventory().consumeItem(41135, 1); // 精
				pc.getInventory().consumeItem(41136, 1); // 精
				pc.getInventory().consumeItem(41137, 1); // 精
				pc.getInventory().consumeItem(41138, 1); // 精
				pc.getQuest().set_step(L1Quest.QUEST_DESIRE, 0);
				// 精渡
			} else if (s.equalsIgnoreCase("e")) {
				if ((pc.getQuest().get_step(L1Quest.QUEST_DESIRE) == L1Quest.QUEST_END)
						|| (pc.getKarmaLevel() >= 1)) {
					htmlid = "";
				} else {
					if (pc.getInventory().checkItem(41138)) {
						htmlid = "";
						pc.addKarma((int) (1600 * Config.RATE_KARMA));
						pc.getInventory().consumeItem(41130, 1); // 衮
						pc.getInventory().consumeItem(41131, 1); // 衮令
						pc.getInventory().consumeItem(41138, 1); // 精
						pc.getQuest().set_step(L1Quest.QUEST_DESIRE,
								L1Quest.QUEST_END);
					} else {
						htmlid = "minicod04";
					}
				}
				// 
			} else if (s.equalsIgnoreCase("g")) {
				htmlid = "";
				L1ItemInstance item = pc.getInventory().storeItem(41130, 1); // 衮
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
			}
		}
		// 諱(影殿)
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81202) {
			// 諾
			if (s.equalsIgnoreCase("n")) {
				htmlid = "";
				poly(client, 6035);
				final int[] item_ids = { 41123, 41124, 41125 };
				final int[] item_amounts = { 1, 1, 1 };
				for (int i = 0; i < item_ids.length; i++) {
					L1ItemInstance item = pc.getInventory().storeItem(
							item_ids[i], item_amounts[i]);
					pc.sendPackets(new S_ServerMessage(143,
							((L1NpcInstance) obj).getNpcTemplate().get_name(),
							item.getItem().getName()));
					pc.getQuest().set_step(L1Quest.QUEST_SHADOWS, 1);
				}
				// 任
			} else if (s.equalsIgnoreCase("d")) {
				htmlid = "minitos09";
				pc.getInventory().consumeItem(41121, 1);
				pc.getInventory().consumeItem(41122, 1);
				// 
			} else if (s.equalsIgnoreCase("k")) {
				htmlid = "";
				pc.getInventory().consumeItem(41123, 1); // 
				pc.getInventory().consumeItem(41124, 1); // 
				pc.getInventory().consumeItem(41125, 1); // 
				pc.getInventory().consumeItem(41126, 1); // 衮精
				pc.getInventory().consumeItem(41127, 1); // 衮精
				pc.getInventory().consumeItem(41128, 1); // 衮精
				pc.getInventory().consumeItem(41129, 1); // 衮精
				pc.getQuest().set_step(L1Quest.QUEST_SHADOWS, 0);
				// 精渡
			} else if (s.equalsIgnoreCase("e")) {
				if ((pc.getQuest().get_step(L1Quest.QUEST_SHADOWS) == L1Quest.QUEST_END)
						|| (pc.getKarmaLevel() >= 1)) {
					htmlid = "";
				} else {
					if (pc.getInventory().checkItem(41129)) {
						htmlid = "";
						pc.addKarma((int) (-1600 * Config.RATE_KARMA));
						pc.getInventory().consumeItem(41121, 1); // 
						pc.getInventory().consumeItem(41122, 1); // 令
						pc.getInventory().consumeItem(41129, 1); // 衮精
						pc.getQuest().set_step(L1Quest.QUEST_SHADOWS,
								L1Quest.QUEST_END);
					} else {
						htmlid = "minitos04";
					}
				}
				// 紩
			} else if (s.equalsIgnoreCase("g")) {
				htmlid = "";
				L1ItemInstance item = pc.getInventory().storeItem(41121, 1); // 
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71252) {
			int weapon1 = 0;
			int weapon2 = 0;
			int newWeapon = 0;
			if (s.equalsIgnoreCase("A")) {
				weapon1 = 5; // +7
				weapon2 = 6; // +7
				newWeapon = 259; // 
				htmlid = "joegolem9";
			} else if (s.equalsIgnoreCase("B")) {
				weapon1 = 145; // +7
				weapon2 = 148; // +7
				newWeapon = 260; // 
				htmlid = "joegolem10";
			} else if (s.equalsIgnoreCase("C")) {
				weapon1 = 52; // +7
				weapon2 = 64; // +7
				newWeapon = 262; // 
				htmlid = "joegolem11";
			} else if (s.equalsIgnoreCase("D")) {
				weapon1 = 125; // +7
				weapon2 = 129; // +7
				newWeapon = 261; // 
				htmlid = "joegolem12";
			} else if (s.equalsIgnoreCase("E")) {
				weapon1 = 99; // +7
				weapon2 = 104; // +7
				newWeapon = 263; // 
				htmlid = "joegolem13";
			} else if (s.equalsIgnoreCase("F")) {
				weapon1 = 32; // +7
				weapon2 = 42; // +7
				newWeapon = 264; // 
				htmlid = "joegolem14";
			}
			if (pc.getInventory().checkEnchantItem(weapon1, 7, 1)
					&& pc.getInventory().checkEnchantItem(weapon2, 7, 1)
					&& pc.getInventory().checkItem(41246, 1000) // 絶
					&& pc.getInventory().checkItem(49143, 10)) { // 氮
				pc.getInventory().consumeEnchantItem(weapon1, 7, 1);
				pc.getInventory().consumeEnchantItem(weapon2, 7, 1);
				pc.getInventory().consumeItem(41246, 1000);
				pc.getInventory().consumeItem(49143, 10);
				L1ItemInstance item = pc.getInventory().storeItem(newWeapon, 1);
				pc.sendPackets(new S_ServerMessage(143, ((L1NpcInstance) obj)
						.getNpcTemplate().get_name(), item.getItem().getName()));
			} else {
				htmlid = "joegolem15";
				if (!pc.getInventory().checkEnchantItem(weapon1, 7, 1)) {
					pc.sendPackets(new S_ServerMessage(337, "+7 "
							+ ItemTable.getInstance().getTemplate(weapon1)
									.getName())); // \f1%0足
				}
				if (!pc.getInventory().checkEnchantItem(weapon2, 7, 1)) {
					pc.sendPackets(new S_ServerMessage(337, "+7 "
							+ ItemTable.getInstance().getTemplate(weapon2)
									.getName())); // \f1%0足
				}
				if (!pc.getInventory().checkItem(41246, 1000)) {
					int itemCount = 0;
					itemCount = 1000 - pc.getInventory().countItems(41246);
					pc.sendPackets(new S_ServerMessage(337, ItemTable
							.getInstance().getTemplate(41246).getName()
							+ "(" + itemCount + ")")); // \f1%0足
				}
				if (!pc.getInventory().checkItem(49143, 10)) {
					int itemCount = 0;
					itemCount = 10 - pc.getInventory().countItems(49143);
					pc.sendPackets(new S_ServerMessage(337, ItemTable
							.getInstance().getTemplate(49143).getName()
							+ "(" + itemCount + ")")); // \f1%0足
				}
			}
		}
		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71253) {
			// 歪
			if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(49101, 100)) {
					materials = new int[] { 49101 };
					counts = new int[] { 100 };
					createitem = new int[] { 49092 };
					createcount = new int[] { 1 };
					htmlid = "joegolem18";
				} else {
					htmlid = "joegolem19";
				}
			} else if (s.equalsIgnoreCase("B")) {
				if (pc.getInventory().checkItem(49101, 1)) {
					pc.getInventory().consumeItem(49101, 1);
					L1Teleport.teleport(pc, 33966, 33253, (short) 4, 5, true);
					htmlid = "";
				} else {
					htmlid = "joegolem20";
				}
			}
		}
		//  祭壮
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71255) {
			// 祭壮祭士
			if (s.equalsIgnoreCase("e")) {
				if (pc.getInventory().checkItem(49242, 1)) { // (20人/歪2h30)
					pc.getInventory().consumeItem(49242, 1);
					L1Teleport.teleport(pc, 32735, 32831, (short) 782, 2, true);
					htmlid = "";
				} else {
					htmlid = "tebegate3";
					// 人
					// htmlid = "tebegate4";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71256) {
			if (s.equalsIgnoreCase("E")) {
				if ((pc.getQuest().get_step(L1Quest.QUEST_MOONOFLONGBOW) == 8)
						&& pc.getInventory().checkItem(40491, 30)
						&& pc.getInventory().checkItem(40495, 40)
						&& pc.getInventory().checkItem(100, 1)
						&& pc.getInventory().checkItem(40509, 12)
						&& pc.getInventory().checkItem(40052, 1)
						&& pc.getInventory().checkItem(40053, 1)
						&& pc.getInventory().checkItem(40054, 1)
						&& pc.getInventory().checkItem(40055, 1)
						&& pc.getInventory().checkItem(41347, 1)
						&& pc.getInventory().checkItem(41350, 1)) {
					pc.getInventory().consumeItem(40491, 30);
					pc.getInventory().consumeItem(40495, 40);
					pc.getInventory().consumeItem(100, 1);
					pc.getInventory().consumeItem(40509, 12);
					pc.getInventory().consumeItem(40052, 1);
					pc.getInventory().consumeItem(40053, 1);
					pc.getInventory().consumeItem(40054, 1);
					pc.getInventory().consumeItem(40055, 1);
					pc.getInventory().consumeItem(41347, 1);
					pc.getInventory().consumeItem(41350, 1);
					htmlid = "robinhood12";
					pc.getInventory().storeItem(205, 1);
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW,
							L1Quest.QUEST_END);
				}
			} else if (s.equalsIgnoreCase("C")) {
				if (pc.getQuest().get_step(L1Quest.QUEST_MOONOFLONGBOW) == 7) {
					if (pc.getInventory().checkItem(41352, 4)
							&& pc.getInventory().checkItem(40618, 30)
							&& pc.getInventory().checkItem(40643, 30)
							&& pc.getInventory().checkItem(40645, 30)
							&& pc.getInventory().checkItem(40651, 30)
							&& pc.getInventory().checkItem(40676, 30)
							&& pc.getInventory().checkItem(40514, 20)
							&& pc.getInventory().checkItem(41351, 1)
							&& pc.getInventory().checkItem(41346, 1)) {
						pc.getInventory().consumeItem(41352, 4);
						pc.getInventory().consumeItem(40618, 30);
						pc.getInventory().consumeItem(40643, 30);
						pc.getInventory().consumeItem(40645, 30);
						pc.getInventory().consumeItem(40651, 30);
						pc.getInventory().consumeItem(40676, 30);
						pc.getInventory().consumeItem(40514, 20);
						pc.getInventory().consumeItem(41351, 1);
						pc.getInventory().consumeItem(41346, 1);
						pc.getInventory().storeItem(41347, 1);
						pc.getInventory().storeItem(41350, 1);
						htmlid = "robinhood10";
						pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 8);
					}
				}
			} else if (s.equalsIgnoreCase("B")) {
				if (pc.getInventory().checkItem(41348)
						&& pc.getInventory().checkItem(41346)) {
					htmlid = "robinhood13";
				} else {
					pc.getInventory().storeItem(41348, 1);
					pc.getInventory().storeItem(41346, 1);
					htmlid = "robinhood13";
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 2);
				}
			} else if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(40028)) {
					pc.getInventory().consumeItem(40028, 1);
					htmlid = "robinhood4";
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 1);
				} else {
					htmlid = "robinhood19";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71257) {
			if (s.equalsIgnoreCase("D")) {
				if (pc.getInventory().checkItem(41349)) {
					htmlid = "zybril10";
					pc.getInventory().storeItem(41351, 1);
					pc.getInventory().consumeItem(41349, 1);
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 7);
				} else {
					htmlid = "zybril14";
				}
			} else if (s.equalsIgnoreCase("C")) {
				if (pc.getInventory().checkItem(40514, 10)
						&& pc.getInventory().checkItem(41353)) {
					pc.getInventory().consumeItem(40514, 10);
					pc.getInventory().consumeItem(41353, 1);
					pc.getInventory().storeItem(41354, 1);
					htmlid = "zybril9";
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 6);
				}
			} else if (pc.getInventory().checkItem(41353)
					&& pc.getInventory().checkItem(40514, 10)) {
				htmlid = "zybril8";
			} else if (s.equalsIgnoreCase("B")) {
				if (pc.getInventory().checkItem(40048, 10)
						&& pc.getInventory().checkItem(40049, 10)
						&& pc.getInventory().checkItem(40050, 10)
						&& pc.getInventory().checkItem(40051, 10)) {
					pc.getInventory().consumeItem(40048, 10);
					pc.getInventory().consumeItem(40049, 10);
					pc.getInventory().consumeItem(40050, 10);
					pc.getInventory().consumeItem(40051, 10);
					pc.getInventory().storeItem(41353, 1);
					htmlid = "zybril15";
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 5);
				} else {
					htmlid = "zybril12";
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 4);
				}
			} else if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(41348)
						&& pc.getInventory().checkItem(41346)) {
					htmlid = "zybril3";
					pc.getQuest().set_step(L1Quest.QUEST_MOONOFLONGBOW, 3);
				} else {
					htmlid = "zybril11";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71258) {
			if (pc.getInventory().checkItem(40665)) {
				htmlid = "marba17";
				if (s.equalsIgnoreCase("B")) {
					htmlid = "marba7";
					if (pc.getInventory().checkItem(214)
							&& pc.getInventory().checkItem(20389)
							&& pc.getInventory().checkItem(20393)
							&& pc.getInventory().checkItem(20401)
							&& pc.getInventory().checkItem(20406)
							&& pc.getInventory().checkItem(20409)) {
						htmlid = "marba15";
					}
				}
			} else if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(40637)) {
					htmlid = "marba20";
				} else {
					L1NpcInstance npc = (L1NpcInstance) obj;
					L1ItemInstance item = pc.getInventory().storeItem(40637, 1);
					String npcName = npc.getNpcTemplate().get_name();
					String itemName = item.getItem().getName();
					pc.sendPackets(new S_ServerMessage(143, npcName, itemName));
					htmlid = "marba6";
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 71259) {
			if (pc.getInventory().checkItem(40665)) {
				htmlid = "aras8";
			} else if (pc.getInventory().checkItem(40637)) {
				htmlid = "aras1";
				if (s.equalsIgnoreCase("A")) {
					if (pc.getInventory().checkItem(40664)) {
						htmlid = "aras6";
						if (pc.getInventory().checkItem(40679)
								|| pc.getInventory().checkItem(40680)
								|| pc.getInventory().checkItem(40681)
								|| pc.getInventory().checkItem(40682)
								|| pc.getInventory().checkItem(40683)
								|| pc.getInventory().checkItem(40684)
								|| pc.getInventory().checkItem(40693)
								|| pc.getInventory().checkItem(40694)
								|| pc.getInventory().checkItem(40695)
								|| pc.getInventory().checkItem(40697)
								|| pc.getInventory().checkItem(40698)
								|| pc.getInventory().checkItem(40699)) {
							htmlid = "aras3";
						} else {
							htmlid = "aras6";
						}
					} else {
						L1NpcInstance npc = (L1NpcInstance) obj;
						L1ItemInstance item = pc.getInventory().storeItem(
								40664, 1);
						String npcName = npc.getNpcTemplate().get_name();
						String itemName = item.getItem().getName();
						pc.sendPackets(new S_ServerMessage(143, npcName,
								itemName));
						htmlid = "aras6";
					}
				} else if (s.equalsIgnoreCase("B")) {
					if (pc.getInventory().checkItem(40664)) {
						pc.getInventory().consumeItem(40664, 1);
						L1NpcInstance npc = (L1NpcInstance) obj;
						L1ItemInstance item = pc.getInventory().storeItem(
								40665, 1);
						String npcName = npc.getNpcTemplate().get_name();
						String itemName = item.getItem().getName();
						pc.sendPackets(new S_ServerMessage(143, npcName,
								itemName));
						htmlid = "aras13";
					} else {
						htmlid = "aras14";
						L1NpcInstance npc = (L1NpcInstance) obj;
						L1ItemInstance item = pc.getInventory().storeItem(
								40665, 1);
						String npcName = npc.getNpcTemplate().get_name();
						String itemName = item.getItem().getName();
						pc.sendPackets(new S_ServerMessage(143, npcName,
								itemName));
					}
				} else {
					if (s.equalsIgnoreCase("7")) {
						if (pc.getInventory().checkItem(40693)
								&& pc.getInventory().checkItem(40694)
								&& pc.getInventory().checkItem(40695)
								&& pc.getInventory().checkItem(40697)
								&& pc.getInventory().checkItem(40698)
								&& pc.getInventory().checkItem(40699)) {
							htmlid = "aras10";
						} else {
							htmlid = "aras9";
						}
					}
				}
			} else {
				htmlid = "aras7";
			}
		}
		// 治
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80099) {
			if (s.equalsIgnoreCase("A")) {
				if (pc.getInventory().checkItem(L1ItemId.ADENA, 300)) {
					pc.getInventory().consumeItem(L1ItemId.ADENA, 300);
					pc.getInventory().storeItem(41315, 1);
					pc.getQuest().set_step(
							L1Quest.QUEST_GENERALHAMELOFRESENTMENT, 1);
					htmlid = "rarson16";
				} else if (!pc.getInventory().checkItem(L1ItemId.ADENA, 300)) {
					htmlid = "rarson7";
				}
			} else if (s.equalsIgnoreCase("B")) {
				if ((pc.getQuest().get_step(
						L1Quest.QUEST_GENERALHAMELOFRESENTMENT) == 1)
						&& (pc.getInventory().checkItem(41325, 1))) {
					pc.getInventory().consumeItem(41325, 1);
					pc.getInventory().storeItem(L1ItemId.ADENA, 2000);
					pc.getInventory().storeItem(41317, 1);
					pc.getQuest().set_step(
							L1Quest.QUEST_GENERALHAMELOFRESENTMENT, 2);
					htmlid = "rarson9";
				} else {
					htmlid = "rarson10";
				}
			} else if (s.equalsIgnoreCase("C")) {
				if ((pc.getQuest().get_step(
						L1Quest.QUEST_GENERALHAMELOFRESENTMENT) == 4)
						&& (pc.getInventory().checkItem(41326, 1))) {
					pc.getInventory().storeItem(L1ItemId.ADENA, 30000);
					pc.getInventory().consumeItem(41326, 1);
					htmlid = "rarson12";
					pc.getQuest().set_step(
							L1Quest.QUEST_GENERALHAMELOFRESENTMENT, 5);
				} else {
					htmlid = "rarson17";
				}
			} else if (s.equalsIgnoreCase("D")) {
				if ((pc.getQuest().get_step(
						L1Quest.QUEST_GENERALHAMELOFRESENTMENT) <= 1)
						|| (pc.getQuest().get_step(
								L1Quest.QUEST_GENERALHAMELOFRESENTMENT) == 5)) {
					if (pc.getInventory().checkItem(L1ItemId.ADENA, 300)) {
						pc.getInventory().consumeItem(L1ItemId.ADENA, 300);
						pc.getInventory().storeItem(41315, 1);
						pc.getQuest().set_step(
								L1Quest.QUEST_GENERALHAMELOFRESENTMENT, 1);
						htmlid = "rarson16";
					} else if (!pc.getInventory().checkItem(L1ItemId.ADENA, 300)) {
						htmlid = "rarson7";
					}
				} else if ((pc.getQuest().get_step(
						L1Quest.QUEST_GENERALHAMELOFRESENTMENT) >= 2)
						&& (pc.getQuest().get_step(
								L1Quest.QUEST_GENERALHAMELOFRESENTMENT) <= 4)) {
					if (pc.getInventory().checkItem(L1ItemId.ADENA, 300)) {
						pc.getInventory().consumeItem(L1ItemId.ADENA, 300);
						pc.getInventory().storeItem(41315, 1);
						htmlid = "rarson16";
					} else if (!pc.getInventory().checkItem(L1ItemId.ADENA, 300)) {
						htmlid = "rarson7";
					}
				}
			}
		}
		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80101) {
			if (s.equalsIgnoreCase("request letter of kuen")) {
				if ((pc.getQuest().get_step(
						L1Quest.QUEST_GENERALHAMELOFRESENTMENT) == 2)
						&& (pc.getInventory().checkItem(41317, 1))) {
					pc.getInventory().consumeItem(41317, 1);
					pc.getInventory().storeItem(41318, 1);
					pc.getQuest().set_step(
							L1Quest.QUEST_GENERALHAMELOFRESENTMENT, 3);
					htmlid = "";
				} else {
					htmlid = "";
				}
			} else if (s.equalsIgnoreCase("request holy mithril dust")) {
				if ((pc.getQuest().get_step(
						L1Quest.QUEST_GENERALHAMELOFRESENTMENT) == 3)
						&& (pc.getInventory().checkItem(41315, 1))
						&& pc.getInventory().checkItem(40494, 30)
						&& pc.getInventory().checkItem(41318, 1)) {
					pc.getInventory().consumeItem(41315, 1);
					pc.getInventory().consumeItem(41318, 1);
					pc.getInventory().consumeItem(40494, 30);
					pc.getInventory().storeItem(41316, 1);
					pc.getQuest().set_step(
							L1Quest.QUEST_GENERALHAMELOFRESENTMENT, 4);
					htmlid = "";
				} else {
					htmlid = "";
				}
			}
		}

		//  
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80136) {
			int lv15_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL15);
			int lv30_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL30);
			int lv45_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL45);
			int lv50_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL50);
			if (pc.isDragonKnight()) {
				// 課
				if (s.equalsIgnoreCase("a") && (lv15_step == 0)) {
					L1NpcInstance npc = (L1NpcInstance) obj;
					L1ItemInstance item = pc.getInventory().storeItem(49210, 1); // 第次令
					String npcName = npc.getNpcTemplate().get_name();
					String itemName = item.getItem().getName();
					pc.sendPackets(new S_ServerMessage(143, npcName, itemName)); // \f1%0%1
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL15, 1);
					htmlid = "prokel3";
				// 第次課
				} else if (s.equalsIgnoreCase("c") && (lv30_step == 0)) {
					final int[] item_ids = { 49211, 49215, }; // 第次令礦
					final int[] item_amounts = { 1, 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL30, 1);
					htmlid = "prokel9";
				// 覮礦
				} else if (s.equalsIgnoreCase("e")) {
					if (pc.getInventory().checkItem(49215, 1)) {
						htmlid = "prokel35";
					} else {
						L1NpcInstance npc = (L1NpcInstance) obj;
						L1ItemInstance item = pc.getInventory().storeItem(
								49215, 1); // 礦
						String npcName = npc.getNpcTemplate().get_name();
						String itemName = item.getItem().getName();
						pc.sendPackets(new S_ServerMessage(143, npcName,
								itemName)); // \f1%0%1
						htmlid = "prokel13";
					}
				// 第次課
				} else if (s.equalsIgnoreCase("f") && (lv45_step == 0)) {
					final int[] item_ids = { 49209, 49212, 49226, }; // 信件第次令絬移軸
					final int[] item_amounts = { 1, 1, 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL45, 1);
					htmlid = "prokel16";
				// 第次課
				} else if (s.equalsIgnoreCase("h") && (lv50_step == 0)) {
					final int[] item_ids = { 49287, }; // 第次令
					final int[] item_amounts = { 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 1);
					htmlid = "prokel22";
				// 空念碮護身
				} else if (s.equalsIgnoreCase("k") && (lv50_step >= 2)) {
					if (pc.getInventory().checkItem(49202, 1)
							|| pc.getInventory().checkItem(49216, 1)) {
						htmlid = "prokel29";
					} else {
						final int[] item_ids = { 49202, 49216, };
						final int[] item_amounts = { 1, 1, };
						for (int i = 0; i < item_ids.length; i++) {
							L1ItemInstance item = pc.getInventory().storeItem(
									item_ids[i], item_amounts[i]);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
									.get_name(), item.getItem().getName()));
						}
						htmlid = "prokel28";
					}
				}
			}
		}

		/*
		 * //   else if (((L1NpcInstance)
		 * obj).getNpcTemplate().get_npcId() == 80145) {// 併 幻 試 if
		 * (pc.isDragonKnight()) { int lv45_step =
		 * pc.getQuest().get_step(L1Quest.QUEST_LEVEL45); // 渡 if
		 * (s.equalsIgnoreCase("l") && (lv45_step == 1)) { if
		 * (pc.getInventory().checkItem(49209, 1)) { // check
		 * pc.getInventory().consumeItem(49209, 1); // del
		 * pc.getQuest().set_step(L1Quest.QUEST_LEVEL45, 2); htmlid =
		 * "silrein38"; } } else if (s.equalsIgnoreCase("m") && (lv45_step ==
		 * 2)) { pc.getQuest().set_step(L1Quest.QUEST_LEVEL45, 3); htmlid =
		 * "silrein39"; } } }
		 */

		// 
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80135) {
			if (pc.isDragonKnight()) {
				// 使身
				if (s.equalsIgnoreCase("a")) {
					if (pc.getInventory().checkItem(49220, 1)) {
						htmlid = "elas5";
					} else {
						L1NpcInstance npc = (L1NpcInstance) obj;
						L1ItemInstance item = pc.getInventory().storeItem(
								49220, 1); // 使身
						String npcName = npc.getNpcTemplate().get_name();
						String itemName = item.getItem().getName();
						pc.sendPackets(new S_ServerMessage(143, npcName,
								itemName)); // \f1%0%1
						htmlid = "elas4";
					}
				}
			}
		}

		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81245) { // (HC3)
			if (pc.isDragonKnight()) {
				if (s.equalsIgnoreCase("request flute of spy")) {
					if (pc.getInventory().checkItem(49223, 1)) { // check
						pc.getInventory().consumeItem(49223, 1); // del
						L1NpcInstance npc = (L1NpcInstance) obj;
						L1ItemInstance item = pc.getInventory().storeItem(
								49222, 1); // 使
						String npcName = npc.getNpcTemplate().get_name();
						String itemName = item.getItem().getName();
						pc.sendPackets(new S_ServerMessage(143, npcName,
								itemName)); // \f1%0%1
						htmlid = "";
					} else {
						htmlid = "";
					}
				}
			}
		}

		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81246) { // 
			if (s.equalsIgnoreCase("0")) {
				materials = new int[] { L1ItemId.ADENA };
				counts = new int[] { 2500 };
				if (pc.getLevel() < 30) {
					htmlid = "sharna4";
				} else if ((pc.getLevel() >= 30) && (pc.getLevel() <= 39)) {
					createitem = new int[] { 49149 }; // 身30
					createcount = new int[] { 1 };
				} else if ((pc.getLevel() >= 40) && (pc.getLevel() <= 51)) {
					createitem = new int[] { 49150 }; // 身40
					createcount = new int[] { 1 };
				} else if ((pc.getLevel() >= 52) && (pc.getLevel() <= 54)) {
					createitem = new int[] { 49151 }; // 身52
					createcount = new int[] { 1 };
				} else if ((pc.getLevel() >= 55) && (pc.getLevel() <= 59)) {
					createitem = new int[] { 49152 }; // 身55
					createcount = new int[] { 1 };
				} else if ((pc.getLevel() >= 60) && (pc.getLevel() <= 64)) {
					createitem = new int[] { 49153 }; // 身60
					createcount = new int[] { 1 };
				} else if ((pc.getLevel() >= 65) && (pc.getLevel() <= 69)) {
					createitem = new int[] { 49154 }; // 身65
					createcount = new int[] { 1 };
				} else if (pc.getLevel() >= 70) {
					createitem = new int[] { 49155 }; // 身70
					createcount = new int[] { 1 };
				}
				success_htmlid = "sharna3";
				failure_htmlid = "sharna5";
			}
		} else if ((((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70035)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70041)
				|| (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70042)) { // 管人
			if (s.equalsIgnoreCase("status")) {// status
				htmldata = new String[15];
				for (int i = 0; i < 5; i++) {
					htmldata[i * 3] = (NpcTable.getInstance().getTemplate(
							l1j.server.server.model.game.L1BugBearRace.getInstance()
									.getRunner(i).getNpcId()).get_nameid());
					String condition;// 610 
					if (l1j.server.server.model.game.L1BugBearRace.getInstance()
							.getCondition(i) == 0) {
						condition = "$610";
					} else {
						if (l1j.server.server.model.game.L1BugBearRace.getInstance()
								.getCondition(i) > 0) {// 368
														// 
							condition = "$368";
						} else {// 370 
							condition = "$370";
						}
					}
					htmldata[i * 3 + 1] = condition;
					htmldata[i * 3 + 2] = String
							.valueOf(l1j.server.server.model.game.L1BugBearRace
									.getInstance().getWinningAverage(i));
				}
				htmlid = "maeno4";
			}
		}
		// 寵
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70077 // 德尼
				|| ((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81290) { // 
			int consumeItem = 0;
			int consumeItemCount = 0;
			int petNpcId = 0;
			int petItemId = 0;// 40314 寵
			int upLv = 0; // 
			long lvExp = 0; // LV.upLv 
			String msg = "";
			if (s.equalsIgnoreCase("buy 1")) {
				petNpcId = 45042;// 
				consumeItem = L1ItemId.ADENA;
				consumeItemCount = 50000;
				petItemId = 40314;
				upLv = 5;
				lvExp = ExpTable.getExpByLevel(upLv);
				msg = "";
			} else if (s.equalsIgnoreCase("buy 2")) {
				petNpcId = 45034;// 
				consumeItem = L1ItemId.ADENA;
				consumeItemCount = 50000;
				petItemId = 40314;
				upLv = 5;
				lvExp = ExpTable.getExpByLevel(upLv);
				msg = "";
			} else if (s.equalsIgnoreCase("buy 3")) {
				petNpcId = 45046;// 尵
				consumeItem = L1ItemId.ADENA;
				consumeItemCount = 50000;
				petItemId = 40314;
				upLv = 5;
				lvExp = ExpTable.getExpByLevel(upLv);
				msg = "";
			} else if (s.equalsIgnoreCase("buy 4")) {
				petNpcId = 45047;// 伯
				consumeItem = L1ItemId.ADENA;
				consumeItemCount = 50000;
				petItemId = 40314;
				upLv = 5;
				lvExp = ExpTable.getExpByLevel(upLv);
				msg = "";
			} else if (s.equalsIgnoreCase("buy 7")) {
				petNpcId = 97023;// 
				consumeItem = 47011;
				consumeItemCount = 1;
				petItemId = 40314;
				upLv = 5;
				lvExp = ExpTable.getExpByLevel(upLv);
				msg = "幼";
			} else if (s.equalsIgnoreCase("buy 8")) {
				petNpcId = 97022;// 
				consumeItem = 47012;
				consumeItemCount = 1;
				petItemId = 40314;
				upLv = 5;
				lvExp = ExpTable.getExpByLevel(upLv);
				msg = "幼";
			}
			if (petNpcId > 0) {
				if (!pc.getInventory().checkItem(consumeItem, consumeItemCount)) {
					pc.sendPackets(new S_ServerMessage(337, msg));
				} else if (pc.getInventory().getSize() > 180) {
					pc.sendPackets(new S_ServerMessage(337, "身空"));
				} else if (pc.getInventory().checkItem(consumeItem,
						consumeItemCount)) {
					pc.getInventory()
							.consumeItem(consumeItem, consumeItemCount);
					L1PcInventory inv = pc.getInventory();
					L1ItemInstance petamu = inv.storeItem(petItemId, 1);
					if (petamu != null) {
						PetTable.getInstance()
								.buyNewPet(petNpcId, petamu.getId() + 1,
										petamu.getId(), upLv, lvExp);
						pc.sendPackets(new S_ItemName(petamu));
						pc.sendPackets(new S_ServerMessage(403, petamu
								.getName()));
					}
				}
			} else {
				pc.sendPackets(new S_SystemMessage("話符輴"));
			}
			htmlid = "";
		}

		// 幻 試練任
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 80145) {//  帮
			int lv15_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL15);
			int lv30_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL30);
			int lv45_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL45);
			int lv50_step = pc.getQuest().get_step(L1Quest.QUEST_LEVEL50);
			if (pc.isDragonKnight()) {
				if (s.equalsIgnoreCase("l") && (lv45_step == 1)) {
					if (pc.getInventory().checkItem(49209, 1)) { // check
						pc.getInventory().consumeItem(49209, 1); // del
						pc.getQuest().set_step(L1Quest.QUEST_LEVEL45, 2);
						htmlid = "silrein38";
					}
				} else if (s.equalsIgnoreCase("m") && (lv45_step == 2)) {
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL45, 3);
					htmlid = "silrein39";
				}
			}
			if (pc.isIllusionist()) {
				// 帮第次課
				if (s.equalsIgnoreCase("a") && (lv15_step == 0)) {
					final int[] item_ids = { 49172, 49182, }; // 帮第次信件精森移軸
					final int[] item_amounts = { 1, 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL15, 1);
					htmlid = "silrein3";
				// 帮第課
				} else if (s.equalsIgnoreCase("c") && (lv30_step == 0)) {
					final int[] item_ids = { 49173, 49179, }; // 帮第次信件帮
																// 歬移軸
					final int[] item_amounts = { 1, 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL30, 1);
					htmlid = "silrein12";
				// 
				} else if (s.equalsIgnoreCase("o") && (lv30_step == 1)) {
					if (pc.getInventory().checkItem(49186, 1)
							|| pc.getInventory().checkItem(49179, 1)) {
						htmlid = "silrein17";// 已 帮袽 丯
					} else {
						L1ItemInstance item = pc.getInventory().storeItem(
								49186, 1); // 
						pc.sendPackets(new S_ServerMessage(143, item.getItem()
								.getName()));
						htmlid = "silrein16";
					}
				// 帮第課
				} else if (s.equalsIgnoreCase("e") && (lv45_step == 0)) {
					final int[] item_ids = { 49174, 49180, }; // 帮第次信件帮
																// 風移軸空水(
																// 3)
					final int[] item_amounts = { 1, 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL45, 1);
					htmlid = "silrein19";
				// 帮第課
				} else if (s.equalsIgnoreCase("h") && (lv50_step == 0)) {
					final int[] item_ids = { 49176, }; // 帮第次信
					final int[] item_amounts = { 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 1);
					htmlid = "silrein28";
				// 空念帮護身
				} else if (s.equalsIgnoreCase("k") && (lv50_step >= 2)) {
					if (pc.getInventory().checkItem(49202, 1)
							|| pc.getInventory().checkItem(49178, 1)) {
						htmlid = "silrein32";
					} else {
						final int[] item_ids = { 49202, 49178, };
						final int[] item_amounts = { 1, 1, };
						for (int i = 0; i < item_ids.length; i++) {
							L1ItemInstance item = pc.getInventory().storeItem(
									item_ids[i], item_amounts[i]);
							pc.sendPackets(new S_ServerMessage(143,
									((L1NpcInstance) obj).getNpcTemplate()
									.get_name(), item.getItem().getName()));
						}
						htmlid = "silrein32";
					}
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 70739) { // 迪
			if (pc.isCrown()) {
				if (s.equalsIgnoreCase("e")) {
					if (pc.getInventory().checkItem(49159, 1)) {
						htmlid = "dicardingp5";
						pc.getInventory().consumeItem(49159, 1);
						pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 2);
					} else {
						htmlid = "dicardingp4a";
					}
				} else if (s.equalsIgnoreCase("d")) {
					htmlid = "dicardingp7";
					L1PolyMorph.doPoly(pc, 6035, 900, 1, true);
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 3);
				} else if (s.equalsIgnoreCase("c")) {
					htmlid = "dicardingp9";
					L1PolyMorph.undoPoly(pc);
					L1PolyMorph.doPoly(pc, 6035, 900, 1, true);
				} else if (s.equalsIgnoreCase("b")) {
					htmlid = "dicardingp12";
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 4);
					if (pc.getInventory().checkItem(49165)) {
						pc.getInventory().consumeItem(49165, pc.getInventory().countItems(49165));
					} 
					if (pc.getInventory().checkItem(49166)) {
						pc.getInventory().consumeItem(49166, pc.getInventory().countItems(49166));
					} 
					if (pc.getInventory().checkItem(49167)) {
						pc.getInventory().consumeItem(49167, pc.getInventory().countItems(49167));
					} 
					if (pc.getInventory().checkItem(49168)) {
						pc.getInventory().consumeItem(49168, pc.getInventory().countItems(49168));
					} 
					if (pc.getInventory().checkItem(49239)) {
						pc.getInventory().consumeItem(49239, pc.getInventory().countItems(49239));
					}
				}
			}
			if (pc.isKnight()) {
				if (s.equalsIgnoreCase("h")) {
					if (pc.getInventory().checkItem(49160, 1)) {
						htmlid = "dicardingk5";
						pc.getInventory().consumeItem(49160, 1);
						pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 2);
					}
				} else if (s.equalsIgnoreCase("j")) {
					htmlid = "dicardingk10";
					pc.getInventory().consumeItem(49161, 10);
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 4);
				} else if (s.equalsIgnoreCase("k")) {
					htmlid = "dicardingk13";
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 4);
					if (pc.getInventory().checkItem(49165)) {
						pc.getInventory().consumeItem(49165, pc.getInventory().countItems(49165));
					} 
					if (pc.getInventory().checkItem(49166)) {
						pc.getInventory().consumeItem(49166, pc.getInventory().countItems(49166));
					} 
					if (pc.getInventory().checkItem(49167)) {
						pc.getInventory().consumeItem(49167, pc.getInventory().countItems(49167));
					} 
					if (pc.getInventory().checkItem(49168)) {
						pc.getInventory().consumeItem(49168, pc.getInventory().countItems(49168));
					} 
					if (pc.getInventory().checkItem(49239)) {
						pc.getInventory().consumeItem(49239, pc.getInventory().countItems(49239));
					}
				}
			}
			if (pc.isElf()) {
				if (s.equalsIgnoreCase("n")) {
					if (pc.getInventory().checkItem(49162, 1)) {
						htmlid = "dicardinge5";
						pc.getInventory().consumeItem(49162, 1);
						pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 2);
					}
				} else if (s.equalsIgnoreCase("p")) {
					htmlid = "dicardinge10";
					pc.getInventory().consumeItem(49163, 1);
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 5);
				} else if (s.equalsIgnoreCase("q")) {
					htmlid = "dicardinge14";
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 5);
					if (pc.getInventory().checkItem(49165)) {
						pc.getInventory().consumeItem(49165, pc.getInventory().countItems(49165));
					} 
					if (pc.getInventory().checkItem(49166)) {
						pc.getInventory().consumeItem(49166, pc.getInventory().countItems(49166));
					} 
					if (pc.getInventory().checkItem(49167)) {
						pc.getInventory().consumeItem(49167, pc.getInventory().countItems(49167));
					} 
					if (pc.getInventory().checkItem(49168)) {
						pc.getInventory().consumeItem(49168, pc.getInventory().countItems(49168));
					} 
					if (pc.getInventory().checkItem(49239)) {
						pc.getInventory().consumeItem(49239, pc.getInventory().countItems(49239));
					}
				}
			}
			if (pc.isWizard()) {
				if (s.equalsIgnoreCase("u")) {
					if (pc.getInventory().checkItem(49164, 1)) {
						htmlid = "dicardingw6";
						pc.getInventory().consumeItem(49164, 1);
						pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 3);
					}
				} else if (s.equalsIgnoreCase("w")) {
					htmlid = "dicardingw12";
					pc.getQuest().set_step(L1Quest.QUEST_LEVEL50, 4);
					if (pc.getInventory().checkItem(49165)) {
						pc.getInventory().consumeItem(49165, pc.getInventory().countItems(49165));
					} 
					if (pc.getInventory().checkItem(49166)) {
						pc.getInventory().consumeItem(49166, pc.getInventory().countItems(49166));
					} 
					if (pc.getInventory().checkItem(49167)) {
						pc.getInventory().consumeItem(49167, pc.getInventory().countItems(49167));
					} 
					if (pc.getInventory().checkItem(49168)) {
						pc.getInventory().consumeItem(49168, pc.getInventory().countItems(49168));
					} 
					if (pc.getInventory().checkItem(49239)) {
						pc.getInventory().consumeItem(49239, pc.getInventory().countItems(49239));
					}
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81334) { // 被
			if (s.equalsIgnoreCase("a")) {
				if (pc.getInventory().checkItem(49239, 1)) {
					htmlid = "rtf06";
				} else {
					final int[] item_ids = { 49239, };
					final int[] item_amounts = { 1, };
					for (int i = 0; i < item_ids.length; i++) {
						L1ItemInstance item = pc.getInventory().storeItem(
								item_ids[i], item_amounts[i]);
						pc.sendPackets(new S_ServerMessage(143,
								((L1NpcInstance) obj).getNpcTemplate()
										.get_name(), item.getItem().getName()));
					}
				}
			}
		} else if ((((L1NpcInstance) obj).getNpcTemplate().get_npcId() >= 81353)
				&& (((L1NpcInstance) obj).getNpcTemplate().get_npcId() <= 81363)) { // - 仿正設   
			int[] skills = new int[10];
			char s1 = s.charAt(0);
			switch(s1){
			case 'b':
				skills = new int[] {43, 79, 151, 158, 160, 206, 211, 216, 115, 149};                     
				break;
			case 'a':
				skills = new int[] {43, 79, 151, 158, 160, 206, 211, 216, 115, 148};
				break;
			}
			if (s.equalsIgnoreCase("a") || s.equalsIgnoreCase("b")){
				if(pc.getInventory().consumeItem(L1ItemId.ADENA,3000)){
					L1SkillUse l1skilluse = new L1SkillUse();
					for (int i = 0; i < skills.length; i++) {
						l1skilluse.handleCommands(pc, 
								skills[i], pc.getId(), pc.getX(), pc.getY(), null, 0, L1SkillUse.TYPE_GMBUFF);
					}
					htmlid = "bs_done";           
				} else {
					htmlid = "bs_adena";
				}
			}
			if (s.equalsIgnoreCase("0")) {
				htmlid = "bs_01";                 
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 50016) {// 
			if (s.equalsIgnoreCase("0")) {
				if (pc.getLevel() < 13) {// lv < 13 
					L1Teleport
							.teleport(pc, 32682, 32874, (short) 2005, 2, true);
				} else {
					htmlid = "zeno1";
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 50065) {// 魯
			if (s.equalsIgnoreCase("teleport valley-in")) {
				if (pc.getLevel() < 13) {// lv < 13 
					L1Teleport
							.teleport(pc, 32682, 32874, (short) 2005, 2, true);
				} else {
					htmlid = "";
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 50055) {// 德
			if (s.equalsIgnoreCase("teleport hidden-valley")) {
				if (pc.getLevel() < 13) {// lv < 13 
					L1Teleport
							.teleport(pc, 32682, 32874, (short) 2005, 2, true);
				} else {
					htmlid = "drist1";
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81255) {// 
			@SuppressWarnings("unused")
			int quest_step = pc.getQuest().get_step(L1Quest.QUEST_TUTOR);// 任編
			int level = pc.getLevel();// 觲
			char s1 = s.charAt(0);
			if (level < 13) {
				switch (s1) {
				case 'A':
				case 'a':// isCrown
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutorp1";// 
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutorp2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutorp3";// 
					} else if ((level > 9) && (level < 12)) {// lv10 ~ lv11
						htmlid = "tutorp4";// 
					} else if ((level > 11) && (level < 13)) {// lv12
						htmlid = "tutorp5";// 
					} else if (level > 12) {// lv13
						htmlid = "tutorp6";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'B':
				case 'b':// isKnight
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutork1";// 幫
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutork2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutork3";// 
					} else if ((level > 9) && (level < 13)) {// lv10 ~ lv12
						htmlid = "tutork4";// 
					} else if (level > 12) {// lv13
						htmlid = "tutork5";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'C':
				case 'c':// isElf
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutore1";// 幫
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutore2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutore3";// 
					} else if ((level > 9) && (level < 12)) {// lv10 ~ lv11
						htmlid = "tutore4";// 
					} else if ((level > 11) && (level < 13)) {// lv12
						htmlid = "tutore5";// 
					} else if (level > 12) {// lv13
						htmlid = "tutore6";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'D':
				case 'd':// isWizard
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutorm1";// 幫
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutorm2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutorm3";// 
					} else if ((level > 9) && (level < 12)) {// lv10 ~ lv11
						htmlid = "tutorm4";// 
					} else if ((level > 11) && (level < 13)) {// lv12
						htmlid = "tutorm5";// 
					} else if (level > 12) {// lv13
						htmlid = "tutorm6";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'E':
				case 'e':// isDarkelf
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutord1";// 幫
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutord2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutord3";// 
					} else if ((level > 9) && (level < 12)) {// lv10 ~ lv11
						htmlid = "tutord4";// 
					} else if ((level > 11) && (level < 13)) {// lv12
						htmlid = "tutord5";// 
					} else if (level > 12) {// lv13
						htmlid = "tutord6";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'F':
				case 'f':// isDragonKnight
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutordk1";// 幫
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutordk2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutordk3";// 
					} else if ((level > 9) && (level < 13)) {// lv10 ~ lv12
						htmlid = "tutordk4";// 
					} else if (level > 12) {// lv13
						htmlid = "tutordk5";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'G':
				case 'g':// isIllusionist
					if ((level > 1) && (level < 5)) {// lv2 ~ lv4
						htmlid = "tutori1";// 幫
					} else if ((level > 4) && (level < 8)) {// lv5 ~ lv7
						htmlid = "tutori2";// 
					} else if ((level > 7) && (level < 10)) {// lv8 ~ lv9
						htmlid = "tutori3";// 
					} else if ((level > 9) && (level < 13)) {// lv10 ~ lv12
						htmlid = "tutori4";// 
					} else if (level > 12) {// lv13
						htmlid = "tutori5";// 
					} else {
						htmlid = "tutorend";
					}
					break;
				case 'H':
				case 'h':
					L1Teleport.teleport(pc, 32575, 32945, (short) 0, 5, true); // 說話島庫管
					htmlid = "";
					break;
				case 'I':
				case 'i':
					L1Teleport.teleport(pc, 32579, 32923, (short) 0, 5, true); // 衷
					htmlid = "";
					break;
				case 'J':
				case 'j':
					createitem = new int[] { 42099 };
					createcount = new int[] { 1 };
					L1Teleport
							.teleport(pc, 32676, 32813, (short) 2005, 5, true); // 谷
					htmlid = "";
					break;
				case 'K':
				case 'k':
					L1Teleport.teleport(pc, 32562, 33082, (short) 0, 5, true); // 師
					htmlid = "";
					break;
				case 'L':
				case 'l':
					L1Teleport.teleport(pc, 32792, 32820, (short) 75, 5, true); // 象
					htmlid = "";
					break;
				case 'M':
				case 'm':
					L1Teleport.teleport(pc, 32877, 32904, (short) 304, 5, true); // 師賽
					htmlid = "";
					break;
				case 'N':
				case 'n':
					L1Teleport
							.teleport(pc, 32759, 32884, (short) 1000, 5, true); // 幻士
					htmlid = "";
					break;
				case 'O':
				case 'o':
					L1Teleport
							.teleport(pc, 32605, 32837, (short) 2005, 5, true); // 西
					htmlid = "";
					break;
				case 'P':
				case 'p':
					L1Teleport
							.teleport(pc, 32733, 32902, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				case 'Q':
				case 'q':
					L1Teleport
							.teleport(pc, 32559, 32843, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				case 'R':
				case 'r':
					L1Teleport
							.teleport(pc, 32677, 32982, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				case 'S':
				case 's':
					L1Teleport
							.teleport(pc, 32781, 32854, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				case 'T':
				case 't':
					L1Teleport
							.teleport(pc, 32674, 32739, (short) 2005, 5, true); // 西
					htmlid = "";
					break;
				case 'U':
				case 'u':
					L1Teleport
							.teleport(pc, 32578, 32737, (short) 2005, 5, true); // 西
					htmlid = "";
					break;
				case 'V':
				case 'v':
					L1Teleport
							.teleport(pc, 32542, 32996, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				case 'W':
				case 'w':
					L1Teleport
							.teleport(pc, 32794, 32973, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				case 'X':
				case 'x':
					L1Teleport
							.teleport(pc, 32803, 32789, (short) 2005, 5, true); // 
					htmlid = "";
					break;
				default:
					break;
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81256) {// 修練管
			int quest_step = pc.getQuest().get_step(L1Quest.QUEST_TUTOR2);// 任編
			int level = pc.getLevel();// 觲
			@SuppressWarnings("unused")
			boolean isOK = false;
			if (s.equalsIgnoreCase("A")) {
				if ((level > 4) && (quest_step == 2)) {
					createitem = new int[] { 20028, 20126, 20173, 20206, 20232,
							40029, 40030, 40098, 40099, 42099 }; // 
					createcount = new int[] { 1, 1, 1, 1, 1, 50, 5, 20, 30, 5 };
					questid = L1Quest.QUEST_TUTOR2;
					questvalue = 3;
				}
			}
			htmlid = "";
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81257) {// 人諮詢
			int level = pc.getLevel();// 觲
			char s1 = s.charAt(0);
			if (level < 46) {
				switch (s1) {
				case 'A':
				case 'a':
					L1Teleport.teleport(pc, 32562, 33082, (short) 0, 5, true); // 師
					htmlid = "";
					break;
				case 'B':
				case 'b':
					L1Teleport.teleport(pc, 33119, 32933, (short) 4, 5, true); // 義
					htmlid = "";
					break;
				case 'C':
				case 'c':
					L1Teleport.teleport(pc, 32887, 32652, (short) 4, 5, true); // 
					htmlid = "";
					break;
				case 'D':
				case 'd':
					L1Teleport.teleport(pc, 32792, 32820, (short) 75, 5, true); // 販精精泳
					htmlid = "";
					break;
				case 'E':
				case 'e':
					L1Teleport.teleport(pc, 32789, 32851, (short) 76, 5, true); // 象精修
					htmlid = "";
					break;
				case 'F':
				case 'f':
					L1Teleport.teleport(pc, 32750, 32847, (short) 76, 5, true); // 象塾溫
					htmlid = "";
					break;
				case 'G':
				case 'g':
					if (pc.isDarkelf()) {
						L1Teleport.teleport(pc, 32877, 32904, (short) 304, 5,
								true); // 師賽
						htmlid = "";
					} else {
						htmlid = "lowlv40";
					}
					break;
				case 'H':
				case 'h':
					if (pc.isDragonKnight()) {
						L1Teleport.teleport(pc, 32811, 32873, (short) 1001, 5,
								true); // 販士森
						htmlid = "";
					} else {
						htmlid = "lowlv41";
					}
					break;
				case 'I':
				case 'i':
					if (pc.isIllusionist()) {
						L1Teleport.teleport(pc, 32759, 32884, (short) 1000, 5,
								true); // 販幻士泲
						htmlid = "";
					} else {
						htmlid = "lowlv42";
					}
					break;
				case 'J':
				case 'j':
					L1Teleport.teleport(pc, 32509, 32867, (short) 0, 5, true); // 說話島
					htmlid = "";
					break;
				case 'K':
				case 'k':
					if ((level > 34)) {
						createitem = new int[] { 20282, 21139 }; // 象飾
						createcount = new int[] { 0, 0 };
						boolean isOK = false;
						for (int i = 0; i < createitem.length; i++) {
							if (!pc.getInventory().checkItem(createitem[i], 1)) { // check
								createcount[i] = 1;
								isOK = true;
							}
						}
						if (isOK) {
							success_htmlid = "lowlv43";
						} else {
							htmlid = "lowlv45";
						}
					} else {
						htmlid = "lowlv44";
					}
					break;
				case '0':
					if (level < 13) {
						htmlid = "lowlvS1";
					} else if ((level > 12) && (level < 46)) {
						htmlid = "lowlvS2";
					} else {
						htmlid = "lowlvno";
					}
					break;
				case '1':
					if (level < 13) {
						htmlid = "lowlv14";
					} else if ((level > 12) && (level < 46)) {
						htmlid = "lowlv15";
					} else {
						htmlid = "lowlvno";
					}
					break;
				case '2':
					createitem = new int[] { 20028, 20126, 20173, 20206, 20232,
							21138, 49310 }; // 象
					createcount = new int[] { 0, 0, 0, 0, 0, 0, 0 };
					boolean isOK = false;
					for (int i = 0; i < createitem.length; i++) {
						if (createitem[i] == 49310) {
							L1ItemInstance item = pc.getInventory().findItemId(
									createitem[i]);
							if (item != null) {
								if (item.getCount() < 1000) {
									createcount[i] = 1000 - item.getCount();
									isOK = true;
								}
							} else {
								createcount[i] = 1000;
								isOK = true;
							}
						} else if (!pc.getInventory().checkItem(createitem[i],
								1)) { // check
							createcount[i] = 1;
							isOK = true;
						}
					}
					if (isOK) {
						success_htmlid = "lowlv16";
					} else {
						htmlid = "lowlv17";
					}
					break;
				case '6':
					if (!pc.getInventory().checkItem(49313, 1)
							&& !pc.getInventory().checkItem(49314, 1)) {
						createitem = new int[] { 49313 }; // 象
						createcount = new int[] { 2 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 2000 };
						success_htmlid = "lowlv22";
						failure_htmlid = "lowlv20";
					} else if (pc.getInventory().checkItem(49313, 1)
							|| pc.getInventory().checkItem(49314, 1)) {
						htmlid = "lowlv23";
					} else {
						htmlid = "lowlvno";
					}
					break;
				default:
					break;
				}
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81260) {// 禩
			int townid = pc.getHomeTownId();// 觲屬
			char s1 = s.charAt(0);
			if ((pc.getLevel() > 9) && (townid > 0) && (townid < 11)) {
				switch (s1) {
				case '0':
					createitem = new int[] { 49305 }; // 製 禩水
														// addContribution + 2
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40014 };
					counts = new int[] { 1000, 3 };
					contribution = 2;
					htmlid = "";
					break;
				case '1':
					createitem = new int[] { 49304 }; // 製 禩森水
														// addContribution + 4
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40068 };
					counts = new int[] { 1000, 3 };
					contribution = 4;
					htmlid = "";
					break;
				case '2':
					createitem = new int[] { 49307 }; // 製 禩水
														// addContribution + 2
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40016 };
					counts = new int[] { 500, 3 };
					contribution = 2;
					htmlid = "";
					break;
				case '3':
					createitem = new int[] { 49306 }; // 製 禩水
														// addContribution + 2
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40015 };
					counts = new int[] { 1000, 3 };
					contribution = 2;
					htmlid = "";
					break;
				case '4':
					createitem = new int[] { 49302 }; // 製 禩水
														// addContribution + 1
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40013 };
					counts = new int[] { 500, 3 };
					contribution = 1;
					htmlid = "";
					break;
				case '5':
					createitem = new int[] { 49303 }; // 製 禩水
														// addContribution + 1
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40032 };
					counts = new int[] { 500, 3 };
					contribution = 1;
					htmlid = "";
					break;
				case '6':
					createitem = new int[] { 49308 }; // 製 禩形水
														// addContribution + 3
					createcount = new int[] { 1 };
					materials = new int[] { L1ItemId.ADENA, 40088 };
					counts = new int[] { 1000, 3 };
					contribution = 3;
					htmlid = "";
					break;
				case 'A':
				case 'a':
					switch (townid) {
					case 1:
						createitem = new int[] { 49292 }; // 購買 禩軸說話
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 2:
						createitem = new int[] { 49297 }; // 購買 禩軸
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 3:
						createitem = new int[] { 49293 }; // 購買 禩軸Ｄ魯
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 4:
						createitem = new int[] { 49296 }; // 購買 禩軸
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 5:
						createitem = new int[] { 49295 }; // 購買 禩軸風
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 6:
						createitem = new int[] { 49294 }; // 購買 禩軸Ｏ
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 7:
						createitem = new int[] { 49298 }; // 購買 禩軸
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 8:
						createitem = new int[] { 49299 }; // 購買 禩軸海
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 9:
						createitem = new int[] { 49301 }; // 購買 禩軸
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					case 10:
						createitem = new int[] { 49300 }; // 購買 禩軸
						createcount = new int[] { 1 };
						materials = new int[] { L1ItemId.ADENA };
						counts = new int[] { 400 };
						htmlid = "";
						break;
					default:
						break;
					}
					break;
				default:
					break;
				}
			}
		}
		// 魯
		else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81278) { // 魯
			if (s.equalsIgnoreCase("0")) {
				if (pc.getInventory().checkItem(46000, 1)) { // 檢身丯魯
					htmlid = "veil3"; // 已
				} else if (pc.getInventory().checkItem(L1ItemId.ADENA, 1000000)) { // 檢身幯足
					pc.getInventory().consumeItem(L1ItemId.ADENA, 1000000);
					pc.getInventory().storeItem(46000, 1);
					htmlid = "veil7"; // 購買顯示
				} else if (!pc.getInventory().checkItem(L1ItemId.ADENA, 1000000)) { // 檢身幯足
					htmlid = "veil4"; // 顯示 
				}
			} else if (s.equalsIgnoreCase("1")) {
				htmlid = "veil9"; // 建議
			}
		} else if (((L1NpcInstance) obj).getNpcTemplate().get_npcId() == 81277) { // 巨谷
			int level = pc.getLevel();// 觲
			char s1 = s.charAt(0);
			if (s.equalsIgnoreCase("0")) {
				if (level >= 30 && level <= 51) {
					L1Teleport
							.teleport(pc, 32820, 32904, (short) 1002, 5, true); // 侨
					htmlid = "";
				} else {
					htmlid = "dsecret3";
				}
			} else if (level >= 52) {
				switch (s1) {
				case '1':
					L1Teleport
							.teleport(pc, 32904, 32627, (short) 1002, 5, true); // ()
					break;
				case '2':
					L1Teleport
							.teleport(pc, 32793, 32593, (short) 1002, 5, true); // ()
					break;
				case '3':
					L1Teleport
							.teleport(pc, 32874, 32785, (short) 1002, 5, true); // ()
					break;
				case '4':
					L1Teleport
							.teleport(pc, 32993, 32716, (short) 1002, 4, true); // ()
					break;
				case '5':
					L1Teleport
							.teleport(pc, 32698, 32664, (short) 1002, 6, true); // ()
					break;
				case '6':
					L1Teleport
							.teleport(pc, 32710, 32759, (short) 1002, 6, true); // ()
					break;
				case '7':
					L1Teleport
							.teleport(pc, 32986, 32630, (short) 1002, 4, true); // 徼空
					break;
				}
				htmlid = "";
			} else {
				htmlid = "dsecret3";
			}
		}

		// else System.out.println("C_NpcAction: " + s);
		if ((htmlid != null) && htmlid.equalsIgnoreCase("colos2")) {
			htmldata = makeUbInfoStrings(((L1NpcInstance) obj).getNpcTemplate()
					.get_npcId());
		}
		if (createitem != null) { // 精製
			boolean isCreate = true;
			if (materials != null) {
				for (int j = 0; j < materials.length; j++) {
					if (!pc.getInventory().checkItemNotEquipped(materials[j],
							counts[j])) {
						L1Item temp = ItemTable.getInstance().getTemplate(
								materials[j]);
						pc.sendPackets(new S_ServerMessage(337, temp.getName())); // \f1%0足
						isCreate = false;
					}
				}
			}

			if (isCreate) {
				// 容
				int create_count = 0; // 纾1
				int create_weight = 0;
				for (int k = 0; k < createitem.length; k++) {
					if ((createitem[k] > 0) && (createcount[k] > 0)) {
						L1Item temp = ItemTable.getInstance().getTemplate(
								createitem[k]);
						if (temp != null) {
							if (temp.isStackable()) {
								if (!pc.getInventory().checkItem(createitem[k])) {
									create_count += 1;
								}
							} else {
								create_count += createcount[k];
							}
							create_weight += temp.getWeight() * createcount[k]
									/ 1000;
						}
					}
				}
				// 容確
				if (pc.getInventory().getSize() + create_count > 180) {
					pc.sendPackets(new S_ServerMessage(263)); // \f1人歩180
					return;
				}
				// 確
				if (pc.getMaxWeight() < pc.getInventory().getWeight()
						+ create_weight) {
					pc.sendPackets(new S_ServerMessage(82)); // 以
					return;
				}

				if (materials != null) {
					for (int j = 0; j < materials.length; j++) {
						// 
						pc.getInventory().consumeItem(materials[j], counts[j]);
					}
				}
				for (int k = 0; k < createitem.length; k++) {
					if ((createitem[k] > 0) && (createcount[k] > 0)) {
						L1ItemInstance item = pc.getInventory().storeItem(
								createitem[k], createcount[k]);
						if (item != null) {
							String itemName = ItemTable.getInstance()
									.getTemplate(createitem[k]).getName();
							String createrName = "";
							if (obj instanceof L1NpcInstance) {
								createrName = ((L1NpcInstance) obj)
										.getNpcTemplate().get_name();
							}
							if (createcount[k] > 1) {
								pc.sendPackets(new S_ServerMessage(143,
										createrName, itemName + " ("
												+ createcount[k] + ")")); // \f1%0%1
							} else {
								pc.sendPackets(new S_ServerMessage(143,
										createrName, itemName)); // \f1%0%1
							}
						}
					}
				}
				if (success_htmlid != null) { // html宴表
					pc.sendPackets(new S_NPCTalkReturn(objid, success_htmlid,
							htmldata));
				}
				if (questid > 0) {
					pc.getQuest().set_step(questid, questvalue);
				}
				if (contribution > 0) {
					pc.addContribution(contribution);
				}
			} else { // 精製失
				if (failure_htmlid != null) { // html宴表
					pc.sendPackets(new S_NPCTalkReturn(objid, failure_htmlid,
							htmldata));
				}
			}
		}


        // === Priest IQ: intercept any priest_doll* html ids and render via controller ===
        if (failure_htmlid != null && failure_htmlid.startsWith("priest_doll")) {
            l1j.server.server.priest.PriestDialogController.open(pc,
                l1j.server.server.priest.BoundPriestFinder.findFor(pc), 0, 50, false);
            return;
        }
        if (htmlid != null && htmlid.startsWith("priest_doll")) {
            l1j.server.server.model.Instance.L1ItemInstance __bound =
                l1j.server.server.priest.BoundPriestFinder.findFor(pc);
            l1j.server.server.priest.PriestHtmlHook.tryOpen(pc, null, __bound);
            return;
    }

		if (htmlid != null) { // html宴表
			pc.sendPackets(new S_NPCTalkReturn(objid, htmlid, htmldata));
		}
	}

	private String karmaLevelToHtmlId(int level) {
		if ((level == 0) || (level < -7) || (7 < level)) {
			return "";
		}
		String htmlid = "";
		if (0 < level) {
			htmlid = "vbk" + level;
		} else if (level < 0) {
			htmlid = "vyk" + Math.abs(level);
		}
		return htmlid;
	}

	private String watchUb(L1PcInstance pc, int npcId) {
		L1UltimateBattle ub = UBTable.getInstance().getUbForNpcId(npcId);
		L1Location loc = ub.getLocation();
		if (pc.getInventory().consumeItem(L1ItemId.ADENA, 100)) {
			try {
				pc.save();
				pc.beginGhost(loc.getX(), loc.getY(), (short) loc.getMapId(), true);
			} catch (Exception e) {
				_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		} else {
			pc.sendPackets(new S_ServerMessage(189)); // \f1足
		}
		return "";
	}

	private String enterUb(L1PcInstance pc, int npcId) {
		L1UltimateBattle ub = UBTable.getInstance().getUbForNpcId(npcId);
		if (!ub.isActive() || !ub.canPcEnter(pc)) { // 
			return "colos2";
		}
		if (ub.isNowUb()) { // 競
			return "colos1";
		}
		if (ub.getMembersCount() >= ub.getMaxPlayer()) { // 审
			return "colos4";
		}

		ub.addMember(pc); // 追
		L1Location loc = ub.getLocation().randomLocation(10, false);
		L1Teleport.teleport(pc, loc.getX(), loc.getY(), ub.getMapId(), 5, true);
		return "";
	}

	private String enterHauntedHouse(L1PcInstance pc) {
		if (L1HauntedHouse.getInstance().getHauntedHouseStatus() == L1HauntedHouse.STATUS_PLAYING) { // 競
			pc.sendPackets(new S_ServerMessage(1182)); // 
			return "";
		}
		if (L1HauntedHouse.getInstance().getMembersCount() >= 10) { // 审
			pc.sendPackets(new S_ServerMessage(1184)); // 屷人
			return "";
		}

		L1HauntedHouse.getInstance().addMember(pc); // 追
		L1Teleport.teleport(pc, 32722, 32830, (short) 5140, 2, true);
		return "";
	}

	private String enterPetMatch(L1PcInstance pc, int objid2) {
		if (pc.getPetList().values().size() > 0) {
			pc.sendPackets(new S_ServerMessage(1187)); // 使中
			return "";
		}
		if (!L1PetMatch.getInstance().enterPetMatch(pc, objid2)) {
			pc.sendPackets(new S_ServerMessage(1182)); // 
		}
		return "";
	}

	private void poly(ClientThread clientthread, int polyId) {
		L1PcInstance pc = clientthread.getActiveChar();
		int awakeSkillId = pc.getAwakeSkillId();
		if ((awakeSkillId == AWAKEN_ANTHARAS)
				|| (awakeSkillId == AWAKEN_FAFURION)
				|| (awakeSkillId == AWAKEN_VALAKAS)) {
			pc.sendPackets(new S_ServerMessage(1384)); // 身
			return;
		}

		if (pc.getInventory().checkItem(L1ItemId.ADENA, 100)) { // check
			pc.getInventory().consumeItem(L1ItemId.ADENA, 100); // del

			L1PolyMorph.doPoly(pc, polyId, 1800, L1PolyMorph.MORPH_BY_NPC);
		} else {
			pc.sendPackets(new S_ServerMessage(337, "$4")); // 足
		}
	}

	private void polyByKeplisha(ClientThread clientthread, int polyId) {
		L1PcInstance pc = clientthread.getActiveChar();
		int awakeSkillId = pc.getAwakeSkillId();
		if ((awakeSkillId == AWAKEN_ANTHARAS)
				|| (awakeSkillId == AWAKEN_FAFURION)
				|| (awakeSkillId == AWAKEN_VALAKAS)) {
			pc.sendPackets(new S_ServerMessage(1384)); // 身
			return;
		}

		if (pc.getInventory().checkItem(L1ItemId.ADENA, 100)) { // check
			pc.getInventory().consumeItem(L1ItemId.ADENA, 100); // del

			L1PolyMorph.doPoly(pc, polyId, 1800, L1PolyMorph.MORPH_BY_KEPLISHA);
		} else {
			pc.sendPackets(new S_ServerMessage(337, "$4")); // 足
		}
	}

	private String sellHouse(L1PcInstance pc, int objectId, int npcId) {
		L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
		if (clan == null) {
			return ""; // 
		}
		int houseId = clan.getHouseId();
		if (houseId == 0) {
			return ""; // 
		}
		L1House house = HouseTable.getInstance().getHouseTable(houseId);
		int keeperId = house.getKeeperId();
		if (npcId != keeperId) {
			return ""; // 
		}
		if (!pc.isCrown()) {
			pc.sendPackets(new S_ServerMessage(518)); // 令主
			return ""; // 
		}
		if (pc.getId() != clan.getLeaderId()) {
			pc.sendPackets(new S_ServerMessage(518)); // 令主
			return ""; // 
		}
		if (house.isOnSale()) {
			return "agonsale";
		}

		pc.sendPackets(new S_SellHouse(objectId, String.valueOf(houseId)));
		return null;
	}

	private void openCloseDoor(L1PcInstance pc, L1NpcInstance npc, String s) {
		L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
		if (clan != null) {
			int houseId = clan.getHouseId();
			if (houseId != 0) {
				L1House house = HouseTable.getInstance().getHouseTable(houseId);
				int keeperId = house.getKeeperId();
				if (npc.getNpcTemplate().get_npcId() == keeperId) {
					L1DoorInstance door1 = null;
					L1DoorInstance door2 = null;
					L1DoorInstance door3 = null;
					L1DoorInstance door4 = null;
					for (L1DoorInstance door : DoorTable.getInstance()
							.getDoorList()) {
						if (door.getKeeperId() == keeperId) {
							if (door1 == null) {
								door1 = door;
								continue;
							}
							if (door2 == null) {
								door2 = door;
								continue;
							}
							if (door3 == null) {
								door3 = door;
								continue;
							}
							if (door4 == null) {
								door4 = door;
								break;
							}
						}
					}
					if (door1 != null) {
						if (s.equalsIgnoreCase("open")) {
							door1.open();
						} else if (s.equalsIgnoreCase("close")) {
							door1.close();
						}
					}
					if (door2 != null) {
						if (s.equalsIgnoreCase("open")) {
							door2.open();
						} else if (s.equalsIgnoreCase("close")) {
							door2.close();
						}
					}
					if (door3 != null) {
						if (s.equalsIgnoreCase("open")) {
							door3.open();
						} else if (s.equalsIgnoreCase("close")) {
							door3.close();
						}
					}
					if (door4 != null) {
						if (s.equalsIgnoreCase("open")) {
							door4.open();
						} else if (s.equalsIgnoreCase("close")) {
							door4.close();
						}
					}
				}
			}
		}
	}

	private void openCloseGate(L1PcInstance pc, int keeperId, boolean isOpen) {
		boolean isNowWar = false;
		int pcCastleId = 0;
		if (pc.getClanid() != 0) {
			L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
			if (clan != null) {
				pcCastleId = clan.getCastleId();
			}
		}
		if ((keeperId == 70656) || (keeperId == 70549) || (keeperId == 70985)) { // 
			if (isExistDefenseClan(L1CastleLocation.KENT_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.KENT_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.KENT_CASTLE_ID);
		} else if (keeperId == 70600) { // OT
			if (isExistDefenseClan(L1CastleLocation.OT_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.OT_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.OT_CASTLE_ID);
		} else if ((keeperId == 70778) || (keeperId == 70987)
				|| (keeperId == 70687)) { // WW
			if (isExistDefenseClan(L1CastleLocation.WW_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.WW_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.WW_CASTLE_ID);
		} else if ((keeperId == 70817) || (keeperId == 70800)
				|| (keeperId == 70988) || (keeperId == 70990)
				|| (keeperId == 70989) || (keeperId == 70991)) { // 
			if (isExistDefenseClan(L1CastleLocation.GIRAN_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.GIRAN_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.GIRAN_CASTLE_ID);
		} else if ((keeperId == 70863) || (keeperId == 70992)
				|| (keeperId == 70862)) { // 
			if (isExistDefenseClan(L1CastleLocation.HEINE_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.HEINE_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.HEINE_CASTLE_ID);
		} else if ((keeperId == 70995) || (keeperId == 70994)
				|| (keeperId == 70993)) { // 
			if (isExistDefenseClan(L1CastleLocation.DOWA_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.DOWA_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.DOWA_CASTLE_ID);
		} else if (keeperId == 70996) { // 
			if (isExistDefenseClan(L1CastleLocation.ADEN_CASTLE_ID)) {
				if (pcCastleId != L1CastleLocation.ADEN_CASTLE_ID) {
					return;
				}
			}
			isNowWar = WarTimeController.getInstance().isNowWar(
					L1CastleLocation.ADEN_CASTLE_ID);
		}

		for (L1DoorInstance door : DoorTable.getInstance().getDoorList()) {
			if (door.getKeeperId() == keeperId) {
				if (isNowWar && (door.getMaxHp() > 1)) { // 中
				} else {
					if (isOpen) { // 
						door.open();
					} else { // 
						door.close();
					}
				}
			}
		}
	}

	private boolean isExistDefenseClan(int castleId) {
		boolean isExistDefenseClan = false;
		for (L1Clan clan : L1World.getInstance().getAllClans()) {
			if (castleId == clan.getCastleId()) {
				isExistDefenseClan = true;
				break;
			}
		}
		return isExistDefenseClan;
	}

	private void expelOtherClan(L1PcInstance clanPc, int keeperId) {
		int houseId = 0;
		for (L1House house : HouseTable.getInstance().getHouseTableList()) {
			if (house.getKeeperId() == keeperId) {
				houseId = house.getHouseId();
			}
		}
		if (houseId == 0) {
			return;
		}

		int[] loc = new int[3];
		for (L1Object object : L1World.getInstance().getObject()) {
			if (object instanceof L1PcInstance) {
				L1PcInstance pc = (L1PcInstance) object;
				if (L1HouseLocation.isInHouseLoc(houseId, pc.getX(), pc.getY(),
						pc.getMapId())
						&& (clanPc.getClanid() != pc.getClanid())) {
					loc = L1HouseLocation.getHouseTeleportLoc(houseId, 0);
					if (pc != null) {
						L1Teleport.teleport(pc, loc[0], loc[1], (short) loc[2],
								5, true);
					}
				}
			}
		}
	}

	private void repairGate(L1PcInstance pc) {
		L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
		if (clan != null) {
			int castleId = clan.getCastleId();
			if (castleId != 0) { // 主
				if (!WarTimeController.getInstance().isNowWar(castleId)) {
					// 
					for (L1DoorInstance door : DoorTable.getInstance()
							.getDoorList()) {
						if (L1CastleLocation.checkInWarArea(castleId, door)) {
							door.repairGate();
						}
					}
					pc.sendPackets(new S_ServerMessage(990)); // 修令
				} else {
					pc.sendPackets(new S_ServerMessage(991)); // 修令涾
				}
			}
		}
	}

	private boolean payFee(L1PcInstance pc, L1NpcInstance npc) {
		L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
		if (clan != null) {
			int houseId = clan.getHouseId();
			if (houseId != 0) {
				L1House house = HouseTable.getInstance().getHouseTable(houseId);
				int keeperId = house.getKeeperId();
				if (npc.getNpcTemplate().get_npcId() == keeperId) {
					TimeZone tz = TimeZone.getTimeZone(Config.TIME_ZONE);
					Calendar cal = Calendar.getInstance(tz); // 
					Calendar deadlineCal = house.getTaxDeadline(); // 屰

					int remainingTime = (int) ((deadlineCal.getTimeInMillis() - cal.getTimeInMillis()) / (1000 * 60 * 60 * 24));
					// 秩大 丨繳
					if (remainingTime >= Config.HOUSE_TAX_INTERVAL / 2)
						return true;
					else if (pc.getInventory().checkItem(L1ItemId.ADENA, 2000)) {
						pc.getInventory().consumeItem(L1ItemId.ADENA, 2000);
						//  deadline延
						deadlineCal.add(Calendar.DATE,Config.HOUSE_TAX_INTERVAL);
						deadlineCal.set(Calendar.MINUTE, 0); // 积
						deadlineCal.set(Calendar.SECOND, 0);
						house.setTaxDeadline(deadlineCal);
						HouseTable.getInstance().updateHouse(house); // DB込
						return true;
					} else {
						pc.sendPackets(new S_ServerMessage(189)); // \f1足
					}
				}
			}
		}
		return false;
	}

	private String[] makeHouseTaxStrings(L1PcInstance pc, L1NpcInstance npc) {
		String name = npc.getNpcTemplate().get_name();
		String[] result;
		result = new String[] { name, "2000", "1", "1", "00" };
		L1Clan clan = L1World.getInstance().getClan(pc.getClanname());
		if (clan != null) {
			int houseId = clan.getHouseId();
			if (houseId != 0) {
				L1House house = HouseTable.getInstance().getHouseTable(houseId);
				int keeperId = house.getKeeperId();
				if (npc.getNpcTemplate().get_npcId() == keeperId) {
					Calendar cal = house.getTaxDeadline();
					int month = cal.get(Calendar.MONTH) + 1;
					int day = cal.get(Calendar.DATE);
					int hour = cal.get(Calendar.HOUR_OF_DAY);
					result = new String[] { name, "2000",
							String.valueOf(month), String.valueOf(day),
							String.valueOf(hour) };
				}
			}
		}
		return result;
	}

	private String[] makeWarTimeStrings(int castleId) {
		L1Castle castle = CastleTable.getInstance().getCastleTable(castleId);
		if (castle == null) {
			return null;
		}
		Calendar warTime = castle.getWarTime();
		int year = warTime.get(Calendar.YEAR);
		int month = warTime.get(Calendar.MONTH) + 1;
		int day = warTime.get(Calendar.DATE);
		int hour = warTime.get(Calendar.HOUR_OF_DAY);
		int minute = warTime.get(Calendar.MINUTE);
		String[] result;
		if (castleId == L1CastleLocation.OT_CASTLE_ID) {
			result = new String[] { String.valueOf(year),
					String.valueOf(month), String.valueOf(day),
					String.valueOf(hour), String.valueOf(minute) };
		} else {
			result = new String[] { "", String.valueOf(year),
					String.valueOf(month), String.valueOf(day),
					String.valueOf(hour), String.valueOf(minute) };
		}
		return result;
	}

	private String getYaheeAmulet(L1PcInstance pc, L1NpcInstance npc, String s) {
		int[] amuletIdList = { 20358, 20359, 20360, 20361, 20362, 20363, 20364,
				20365 };
		int amuletId = 0;
		L1ItemInstance item = null;
		String htmlid = null;
		if (s.equalsIgnoreCase("1")) {
			amuletId = amuletIdList[0];
		} else if (s.equalsIgnoreCase("2")) {
			amuletId = amuletIdList[1];
		} else if (s.equalsIgnoreCase("3")) {
			amuletId = amuletIdList[2];
		} else if (s.equalsIgnoreCase("4")) {
			amuletId = amuletIdList[3];
		} else if (s.equalsIgnoreCase("5")) {
			amuletId = amuletIdList[4];
		} else if (s.equalsIgnoreCase("6")) {
			amuletId = amuletIdList[5];
		} else if (s.equalsIgnoreCase("7")) {
			amuletId = amuletIdList[6];
		} else if (s.equalsIgnoreCase("8")) {
			amuletId = amuletIdList[7];
		}
		if (amuletId != 0) {
			item = pc.getInventory().storeItem(amuletId, 1);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			for (int id : amuletIdList) {
				if (id == amuletId) {
					break;
				}
				if (pc.getInventory().checkItem(id)) {
					pc.getInventory().consumeItem(id, 1);
				}
			}
			htmlid = "";
		}
		return htmlid;
	}

	private String getBarlogEarring(L1PcInstance pc, L1NpcInstance npc, String s) {
		int[] earringIdList = { 21020, 21021, 21022, 21023, 21024, 21025,
				21026, 21027 };
		int earringId = 0;
		L1ItemInstance item = null;
		String htmlid = null;
		if (s.equalsIgnoreCase("1")) {
			earringId = earringIdList[0];
		} else if (s.equalsIgnoreCase("2")) {
			earringId = earringIdList[1];
		} else if (s.equalsIgnoreCase("3")) {
			earringId = earringIdList[2];
		} else if (s.equalsIgnoreCase("4")) {
			earringId = earringIdList[3];
		} else if (s.equalsIgnoreCase("5")) {
			earringId = earringIdList[4];
		} else if (s.equalsIgnoreCase("6")) {
			earringId = earringIdList[5];
		} else if (s.equalsIgnoreCase("7")) {
			earringId = earringIdList[6];
		} else if (s.equalsIgnoreCase("8")) {
			earringId = earringIdList[7];
		}
		if (earringId != 0) {
			item = pc.getInventory().storeItem(earringId, 1);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			for (int id : earringIdList) {
				if (id == earringId) {
					break;
				}
				if (pc.getInventory().checkItem(id)) {
					pc.getInventory().consumeItem(id, 1);
				}
			}
			htmlid = "";
		}
		return htmlid;
	}

	private String[] makeUbInfoStrings(int npcId) {
		L1UltimateBattle ub = UBTable.getInstance().getUbForNpcId(npcId);
		return ub.makeUbInfoStrings();
	}

	private String talkToDimensionDoor(L1PcInstance pc, L1NpcInstance npc,
			String s) {
		String htmlid = "";
		int protectionId = 0;
		int sealId = 0;
		int locX = 0;
		int locY = 0;
		short mapId = 0;
		if (npc.getNpcTemplate().get_npcId() == 80059) { // 次()
			protectionId = 40909;
			sealId = 40913;
			locX = 32773;
			locY = 32835;
			mapId = 607;
		} else if (npc.getNpcTemplate().get_npcId() == 80060) { // 次()
			protectionId = 40912;
			sealId = 40916;
			locX = 32757;
			locY = 32842;
			mapId = 606;
		} else if (npc.getNpcTemplate().get_npcId() == 80061) { // 次()
			protectionId = 40910;
			sealId = 40914;
			locX = 32830;
			locY = 32822;
			mapId = 604;
		} else if (npc.getNpcTemplate().get_npcId() == 80062) { // 次()
			protectionId = 40911;
			sealId = 40915;
			locX = 32835;
			locY = 32822;
			mapId = 605;
		}

		// 中紮迥証使
		if (s.equalsIgnoreCase("a")) {
			L1Teleport.teleport(pc, locX, locY, mapId, 5, true);
			htmlid = "";
		}
		// 絵窺
		else if (s.equalsIgnoreCase("b")) {
			L1ItemInstance item = pc.getInventory().storeItem(protectionId, 1);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			htmlid = "";
		}
		// 証
		else if (s.equalsIgnoreCase("c")) {
			htmlid = "wpass07";
		}
		// 
		else if (s.equalsIgnoreCase("d")) {
			if (pc.getInventory().checkItem(sealId)) { // 
				L1ItemInstance item = pc.getInventory().findItemId(sealId);
				pc.getInventory().consumeItem(sealId, item.getCount());
			}
		}
		// 
		else if (s.equalsIgnoreCase("e")) {
			htmlid = "";
		}
		// 涫
		else if (s.equalsIgnoreCase("f")) {
			if (pc.getInventory().checkItem(protectionId)) { // 
				pc.getInventory().consumeItem(protectionId, 1);
			}
			if (pc.getInventory().checkItem(sealId)) { // 
				L1ItemInstance item = pc.getInventory().findItemId(sealId);
				pc.getInventory().consumeItem(sealId, item.getCount());
			}
			htmlid = "";
		}
		return htmlid;
	}

	private boolean isNpcSellOnly(L1NpcInstance npc) {
		int npcId = npc.getNpcTemplate().get_npcId();
		String npcName = npc.getNpcTemplate().get_name();
		if ((npcId == 70027 // 
				)
				|| "".equals(npcName)) {
			return true;
		}
		return false;
	}

	private void getBloodCrystalByKarma(L1PcInstance pc, L1NpcInstance npc,
			String s) {
		L1ItemInstance item = null;

		// 1
		if (s.equalsIgnoreCase("1")) {
			pc.addKarma((int) (500 * Config.RATE_KARMA));
			item = pc.getInventory().storeItem(40718, 1);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			// 姿訶
			pc.sendPackets(new S_ServerMessage(1081));
		}
		// 10
		else if (s.equalsIgnoreCase("2")) {
			pc.addKarma((int) (5000 * Config.RATE_KARMA));
			item = pc.getInventory().storeItem(40718, 10);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			// 姿訶
			pc.sendPackets(new S_ServerMessage(1081));
		}
		// 100
		else if (s.equalsIgnoreCase("3")) {
			pc.addKarma((int) (50000 * Config.RATE_KARMA));
			item = pc.getInventory().storeItem(40718, 100);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			// 姿訶
			pc.sendPackets(new S_ServerMessage(1081));
		}
	}

	private void getSoulCrystalByKarma(L1PcInstance pc, L1NpcInstance npc,
			String s) {
		L1ItemInstance item = null;

		// 1
		if (s.equalsIgnoreCase("1")) {
			pc.addKarma((int) (-500 * Config.RATE_KARMA));
			item = pc.getInventory().storeItem(40678, 1);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			// 走
			pc.sendPackets(new S_ServerMessage(1080));
		}
		// 10
		else if (s.equalsIgnoreCase("2")) {
			pc.addKarma((int) (-5000 * Config.RATE_KARMA));
			item = pc.getInventory().storeItem(40678, 10);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			// 走
			pc.sendPackets(new S_ServerMessage(1080));
		}
		// 100
		else if (s.equalsIgnoreCase("3")) {
			pc.addKarma((int) (-50000 * Config.RATE_KARMA));
			item = pc.getInventory().storeItem(40678, 100);
			if (item != null) {
				pc.sendPackets(new S_ServerMessage(143, npc.getNpcTemplate()
						.get_name(), item.getLogName())); // \f1%0%1
			}
			// 走
			pc.sendPackets(new S_ServerMessage(1080));
		}
	}
	
	private boolean usePolyScroll(L1PcInstance pc, int itemId, String s) {
		int time = 0;
		if ((itemId == 40088) || (itemId == 40096)) { // 身象身
			time = 1800;
		} else if (itemId == 140088) { // 身
			time = 2100;
		}

		L1PolyMorph poly = PolyTable.getInstance().getTemplate(s);
		L1ItemInstance item = pc.getInventory().findItemId(itemId);
		boolean isUseItem = false;
		if ((poly != null) || s.equals("none")) {
			if (s.equals("none")) {
				if ((pc.getTempCharGfx() == 6034)
						|| (pc.getTempCharGfx() == 6035)) {
					isUseItem = true;
				} else {
					pc.removeSkillEffect(SHAPE_CHANGE);
					isUseItem = true;
				}
			} else if ((poly.getMinLevel() <= pc.getLevel()) || pc.isGm()) {
				L1PolyMorph.doPoly(pc, poly.getPolyId(), time,
						L1PolyMorph.MORPH_BY_ITEMMAGIC);
				isUseItem = true;
			}
		}
		if (isUseItem) {
			pc.getInventory().removeItem(item, 1);
		} else {
			pc.sendPackets(new S_ServerMessage(181)); // \f1身
		}
		return isUseItem;
	}

	@Override
	public String getType() {
		return C_NPC_ACTION;
	}


// === hasAnyBuybackSmart: 賣 ===
private boolean hasAnyBuybackSmart(final L1PcInstance pc, final int objid) {
    if (l1j.server.Config.ALL_ITEM_SELL) {
        for (l1j.server.server.model.Instance.L1ItemInstance itm : pc.getInventory().getItems()) {
            if (itm == null) continue;
            if (itm.getCount() <= 0) continue;
            if (itm.isEquipped()) continue;
            try { if (!itm.getItem().isTradable()) continue; } catch (Throwable t) { continue; }
            int bless = itm.getBless();
            if (bless >= 128 && bless <= 131) continue;
            try {
                int ref = l1j.william.L1WilliamItemPrice.getItemId(itm.getItem().getItemId());
                if (ref == 0) continue;
            } catch (Throwable t) { continue; }
            return true;
        }
        return false;
    }
    l1j.server.server.model.L1Object object = l1j.server.server.model.L1World.getInstance().findObject(objid);
    if (!(object instanceof l1j.server.server.model.Instance.L1NpcInstance)) return false;
    int npcId = ((l1j.server.server.model.Instance.L1NpcInstance) object).getNpcTemplate().get_npcId();
    l1j.server.server.model.shop.L1Shop shop = l1j.server.server.datatables.ShopTable.getInstance().get(npcId);
    if (shop == null) return false;
    try {
        java.util.List<l1j.server.server.model.shop.L1AssessedItem> assessed =
                shop.assessItems(pc.getInventory());
        return assessed != null && !assessed.isEmpty();
    } catch (Throwable t) { return false; }
}


// === william_item_price 檢觲Ｊ sell  ===
private static boolean hasAnyWilliamSellable(final l1j.server.server.model.Instance.L1PcInstance pc) {
    try {
        if (pc == null || pc.getInventory() == null) return false;
        for (l1j.server.server.model.Instance.L1ItemInstance itm : pc.getInventory().getItems()) {
            if (itm == null) continue;
            if (itm.getCount() <= 0) continue;
            if (itm.isEquipped()) continue;
            try { if (!itm.getItem().isTradable()) continue; } catch (Throwable t) { continue; }
            int bless = 0;
            try { bless = itm.getBless(); } catch (Throwable ignore) {}
            if (bless >= 128 && bless <= 131) continue;
            int itemId = 0;
            try { itemId = itm.getItem().getItemId(); } catch (Throwable ignore) {}
            if (itemId <= 0) continue;
            try {
                int ref = l1j.william.L1WilliamItemPrice.getItemId(itemId);
                if (ref != 0) return true;
            } catch (Throwable ignore) {}
        }
    } catch (Throwable e) {}
    return false;
}

}
