package l1j.server.server.datatables;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.templates.L1Item;
import l1j.server.server.serverpackets.S_ServerMessage;

public class TreasureBoxTable {

    private static final Logger _log = Logger.getLogger(TreasureBoxTable.class.getName());
    private static TreasureBoxTable _instance;

    public static TreasureBoxTable getInstance() {
        if (_instance == null) {
            _instance = new TreasureBoxTable();
        }
        return _instance;
    }

    public enum BoxType { SPECIFIC, RANDOM }

    public static class Reward {
        public final int itemId;
        public final int baseCount;
        public final double chance;
        public final Integer min;
        public final Integer max;

        public Reward(int itemId, int baseCount, double chance, Integer min, Integer max) {
            this.itemId = itemId;
            this.baseCount = baseCount;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }

        public int rollCount() {
            if (min != null && max != null && max >= min) {
                return ThreadLocalRandom.current().nextInt(max - min + 1) + min;
            }
            return baseCount;
        }
    }

    public static class BoxDef {
        public final int boxItemId;
        public final BoxType type;
        public final List<Reward> rewards = new ArrayList<Reward>();
        public BoxDef(int boxItemId, BoxType type) {
            this.boxItemId = boxItemId;
            this.type = type;
        }
    }

    private final Map<Integer, BoxDef> _boxes = new HashMap<Integer, BoxDef>();

    private TreasureBoxTable() {
        load();
    }

    private static String getAttr(Element e, String... keys) {
        for (String k : keys) {
            if (e.hasAttribute(k)) return e.getAttribute(k);
            NamedNodeMap map = e.getAttributes();
            for (int i = 0; i < map.getLength(); i++) {
                Node n = map.item(i);
                if (n.getNodeType() == Node.ATTRIBUTE_NODE && n.getNodeName().equalsIgnoreCase(k)) {
                    return n.getNodeValue();
                }
            }
        }
        return null;
    }

    private static Integer toIntOrNull(String s) {
        try { return (s == null || s.isEmpty()) ? null : Integer.valueOf(s.trim()); }
        catch (Exception ex) { return null; }
    }

    private static int toIntOrDefault(String s, int def) {
        try { return (s == null || s.isEmpty()) ? def : Integer.parseInt(s.trim()); }
        catch (Exception ex) { return def; }
    }

    private static Double toDoubleOrNull(String s) {
        try { return (s == null || s.isEmpty()) ? null : Double.valueOf(s.trim()); }
        catch (Exception ex) { return null; }
    }

    private void load() {
        _boxes.clear();
        try {
            File f = new File("data/xml/Item/TreasureBox.xml");
            if (!f.exists()) {
                f = new File("data/xml/item/TreasureBox.xml");
            }
            if (!f.exists()) {
                _log.warning("TreasureBox.xml not found");
                return;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(f);

            NodeList list = doc.getElementsByTagName("TreasureBox");
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                Integer itemId = toIntOrNull(getAttr(e, "ItemId", "itemId", "ITEMID"));
                String typeStr = getAttr(e, "Type", "type", "TYPE");
                if (itemId == null || typeStr == null) continue;

                BoxType type = BoxType.valueOf(typeStr.trim().toUpperCase());
                BoxDef def = new BoxDef(itemId, type);

                NodeList rs = e.getElementsByTagName("Item");
                for (int j = 0; j < rs.getLength(); j++) {
                    Element r = (Element) rs.item(j);
                    Integer rid = toIntOrNull(getAttr(r, "ItemId","itemId","ITEMID"));
                    int baseCount = toIntOrDefault(getAttr(r, "Count","count","COUNT"), 1);
                    Double chanceObj = toDoubleOrNull(getAttr(r, "Chance","chance","CHANCE"));
                    double chance = (chanceObj == null) ? 0.0 : chanceObj.doubleValue();

                    Integer min = toIntOrNull(getAttr(r, "Min","min","MIN"));
                    Integer max = toIntOrNull(getAttr(r, "Max","max","MAX"));
                    String range = getAttr(r, "Range","range","RANGE");
                    if ((min == null || max == null) && range != null) {
                        String[] p = range.split("-");
                        if (p.length == 2) {
                            min = toIntOrNull(p[0]);
                            max = toIntOrNull(p[1]);
                        }
                    }

                    if (rid != null) {
                        def.rewards.add(new Reward(rid, baseCount, chance, min, max));
                    }
                }
                _boxes.put(itemId, def);
            }
        } catch (Exception ex) {
            _log.log(Level.SEVERE, "TreasureBox load failed", ex);
        }
    }

    public boolean isTreasureBox(int itemId) {
        return _boxes.containsKey(itemId);
    }

    public void openAndGive(L1PcInstance pc, L1ItemInstance boxItem) {
        BoxDef def = _boxes.get(boxItem.getItemId());
        if (def == null) {
            pc.sendPackets(new S_ServerMessage(79)); // nothing happens
            return;
        }

        if (!pc.getInventory().consumeItem(boxItem.getItemId(), 1)) {
            pc.sendPackets(new S_ServerMessage(337, "道具"));
            return;
        }

        switch (def.type) {
            case SPECIFIC:
                for (Reward r : def.rewards) {
                    int give = r.rollCount();
                    if (give > 0) {
                        pc.getInventory().storeItem(r.itemId, give);
                        sendObtainMsg(pc, r.itemId, give);
                    }
                }
                break;
            case RANDOM:
                Reward picked = pickByChance(def.rewards);
                if (picked != null) {
                    int give = picked.rollCount();
                    if (give > 0) {
                        pc.getInventory().storeItem(picked.itemId, give);
                        sendObtainMsg(pc, picked.itemId, give);
                    }
                }
                break;
        }
    }

    private void sendObtainMsg(L1PcInstance pc, int itemId, int count) {
        L1Item template = ItemTable.getInstance().getTemplate(itemId);
        String name = (template != null) ? template.getName() : String.valueOf(itemId);
        if (count <= 1) {
            // 官方常用：獲得 %0
            pc.sendPackets(new S_ServerMessage(403, name));
        } else {
            // 部分核心有「%0 (%1)」樣板；若沒有，會顯示名稱加自帶數量
            pc.sendPackets(new S_ServerMessage(403, name + " (" + count + ")"));
        }
    }

    private Reward pickByChance(List<Reward> rewards) {
        double total = 0.0;
        for (Reward r : rewards) total += r.chance;
        if (total <= 0.0) return null;
        double roll = ThreadLocalRandom.current().nextDouble() * total;
        for (Reward r : rewards) {
            roll -= r.chance;
            if (roll <= 0) return r;
        }
        return rewards.get(rewards.size() - 1);
    }
}
