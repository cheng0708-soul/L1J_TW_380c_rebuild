
package l1j.server.server.priest;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.templates.L1Item;

/** 道具名稱裝飾：「低階祭司[編號12345][智力+2]」 */
public final class PriestItemNameDecorator {
    private PriestItemNameDecorator() {}
    public static String decorate(L1ItemInstance item, String baseName) {
        if (item == null) return baseName;
        L1Item tpl = item.getItem();
        int id = tpl.getItemId();
        if (id < 240123 || id > 240127) return baseName; // 只裝飾祭司道具

        PriestIqDAO2.Row row = PriestIqDAO2.byItemObjId(item.getId());
        if (row == null) return baseName;

        String clean = baseName.replaceAll("\\\\[.*?\\\\]", "").trim();
        StringBuilder sb = new StringBuilder();
        sb.append(clean);
        sb.append("[編號").append(row.priestUniqueId).append("]");
        if (row.iqBonus > 0) {
            sb.append("[智力+").append(row.iqBonus).append("]");
        }
        
        String decorated = sb.toString();

        // 支援版模占位符：[%編號] / [%智力] / %Y / %6
        // 若原本 baseName 內含這些占位符，改用替換而不是附加
        String base = baseName;
        if (base.contains("[%編號]") || base.contains("%Y") || base.contains("[%智力]") || base.contains("%6")) {
            String rep = base;
            rep = rep.replace("[%編號]", String.valueOf(row.priestUniqueId));
            rep = rep.replace("%Y", String.valueOf(row.priestUniqueId));
            rep = rep.replace("[%智力]", String.valueOf(row.iqBonus));
            rep = rep.replace("%6", String.valueOf(row.iqBonus));
            return rep;
        }

        return decorated;
    }
}

