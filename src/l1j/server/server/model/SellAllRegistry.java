package l1j.server.server.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * SellAllRegistry
 * 每個角色的一鍵販售設定：
 * - 已登錄的一鍵販售物品 itemId 清單（保持順序）
 * - 是否處於「登錄模式」
 * - 取消清單目前頁數
 *
 * 已登錄物品會持久化存到 ./data/sellall/{charId}.txt
 */
public final class SellAllRegistry {

    private static final String DIR = "./data/sellall";

    private static final ConcurrentMap<Integer, State> MAP = new ConcurrentHashMap<Integer, State>();

    private SellAllRegistry() {
    }

    private static State getState(L1PcInstance pc) {
        if (pc == null) {
            return null;
        }
        Integer key = Integer.valueOf(pc.getId());
        State st = MAP.get(key);
        if (st != null) {
            return st;
        }
        st = new State();
        // 從檔案載入
        loadFromFile(pc, st);
        MAP.put(key, st);
        return st;
    }

    // 取得目前已登錄的一鍵販售物品
    public static Set<Integer> getRegisteredItems(L1PcInstance pc) {
        State st = getState(pc);
        if (st == null) {
            return new LinkedHashSet<Integer>();
        }
        synchronized (st) {
            return new LinkedHashSet<Integer>(st._registered);
        }
    }

    public static boolean addItem(L1PcInstance pc, int itemId) {
        State st = getState(pc);
        if (st == null) {
            return false;
        }
        synchronized (st) {
            boolean added = st._registered.add(Integer.valueOf(itemId));
            if (added) {
                saveToFile(pc, st);
            }
            return added;
        }
    }

    public static boolean removeItem(L1PcInstance pc, int itemId) {
        State st = getState(pc);
        if (st == null) {
            return false;
        }
        synchronized (st) {
            boolean removed = st._registered.remove(Integer.valueOf(itemId));
            if (removed) {
                saveToFile(pc, st);
            }
            return removed;
        }
    }

    public static void clear(L1PcInstance pc) {
        State st = getState(pc);
        if (st == null) {
            return;
        }
        synchronized (st) {
            st._registered.clear();
            saveToFile(pc, st);
        }
    }

    // 登錄模式：背包點擊會被當作登錄
    public static boolean isRegisterMode(L1PcInstance pc) {
        State st = getState(pc);
        if (st == null) {
            return false;
        }
        synchronized (st) {
            return st._registerMode;
        }
    }

    public static void setRegisterMode(L1PcInstance pc, boolean flag) {
        State st = getState(pc);
        if (st == null) {
            return;
        }
        synchronized (st) {
            st._registerMode = flag;
        }
    }

    // 取消清單目前頁數
    public static int getCancelPage(L1PcInstance pc) {
        State st = getState(pc);
        if (st == null) {
            return 0;
        }
        synchronized (st) {
            return st._cancelPage;
        }
    }

    public static void setCancelPage(L1PcInstance pc, int page) {
        State st = getState(pc);
        if (st == null) {
            return;
        }
        synchronized (st) {
            st._cancelPage = page;
        }
    }

    // 玩家登出時呼叫，可釋放記憶體（不刪除檔案）
    public static void onLogout(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        MAP.remove(Integer.valueOf(pc.getId()));
    }

    // ----------------- 檔案 I/O -----------------

    private static File getFile(L1PcInstance pc) {
        File dir = new File(DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, pc.getId() + ".txt");
    }

    private static void loadFromFile(L1PcInstance pc, State st) {
        File f = getFile(pc);
        if (!f.exists()) {
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                try {
                    int id = Integer.parseInt(line);
                    st._registered.add(Integer.valueOf(id));
                } catch (NumberFormatException e) {
                    // ignore bad line
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void saveToFile(L1PcInstance pc, State st) {
        File f = getFile(pc);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(f, false));
            for (Integer id : st._registered) {
                bw.write(Integer.toString(id.intValue()));
                bw.newLine();
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // ----------------- 內部狀態物件 -----------------

    private static class State {
        private final LinkedHashSet<Integer> _registered = new LinkedHashSet<Integer>();
        private boolean _registerMode = false;
        private int _cancelPage = 0;
    }
}
