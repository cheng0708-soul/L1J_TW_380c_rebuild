package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.serverpackets.S_MoveCharPacket;
import l1j.server.server.utils.MoveUtil;

public final class AfkPacketUtil {
    private AfkPacketUtil() {}

    private static void stepBroadcast(final L1PcInstance pc, final int toX, final int toY, final int heading) {
        pc.setHeading(heading);
        pc.getLocation().set(toX, toY);
        S_MoveCharPacket p = new S_MoveCharPacket(pc);
        pc.sendPackets(p);
        pc.broadcastPacket(p);
    }

    private static void safeSetPassable(final L1Map map, final int x, final int y, final boolean passable) {
        try { map.setPassable(x, y, passable); } catch (Throwable ignore) {}
        try { map.setPassable(new l1j.server.server.model.L1Location(x, y, map.getId()), passable); } catch (Throwable ignore) {}
    }

    public static void moveOutOneBroadcast(final L1PcInstance pc, final int nx, final int ny) {
        if (pc == null) return;
        final L1Map map = pc.getMap();
        final int ox = pc.getX(), oy = pc.getY();
        final int h = heading(ox, oy, nx, ny);
        safeSetPassable(map, ox, oy, true);
        safeSetPassable(map, nx, ny, false);
        stepBroadcast(pc, nx, ny, h);
    }

    public static void moveBackBroadcast(final L1PcInstance pc, final int bx, final int by, final int backHeading) {
        if (pc == null) return;
        final L1Map map = pc.getMap();
        final int cx = pc.getX(), cy = pc.getY();
        safeSetPassable(map, cx, cy, true);
        safeSetPassable(map, bx, by, false);
        stepBroadcast(pc, bx, by, backHeading);
    }

    public static int heading(final int x, final int y, final int tx, final int ty) {
        final int dx = Integer.signum(tx - x);
        final int dy = Integer.signum(ty - y);
        if (dx==0 && dy<0) return 0;
        if (dx>0 && dy<0) return 1;
        if (dx>0 && dy==0) return 2;
        if (dx>0 && dy>0) return 3;
        if (dx==0 && dy>0) return 4;
        if (dx<0 && dy>0) return 5;
        if (dx<0 && dy==0) return 6;
        if (dx<0 && dy<0) return 7;
        return 0;
    }
}
