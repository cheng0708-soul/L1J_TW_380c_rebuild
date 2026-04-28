package l1j.server.server.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.ConfigGuaji;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1TownLocation;
import l1j.server.server.model.Instance.L1PcInstance;

public final class ReturnTownHelper {
	private static final Logger _log = Logger.getLogger(ReturnTownHelper.class.getName());

	private ReturnTownHelper() {}

	public static boolean doReturn(final L1PcInstance pc) {
		if (pc == null) return false;
		try {
			if (ConfigGuaji.GUAJI_RETURN_USE_TOWNAPI) {
				try {
					final int townId = pc.getHomeTownId();
					if (townId > 0) {
						final int[] loc = L1TownLocation.getGetBackLoc(townId);
						if (loc != null && loc.length >= 3) {
							L1Teleport.teleport(pc, loc[0], loc[1], (short) loc[2], 5, true);
							return true;
						}
					}
				} catch (Throwable ignore) {}
			}
			L1Teleport.teleport(pc, ConfigGuaji.GUAJI_RETURN_X, ConfigGuaji.GUAJI_RETURN_Y, ConfigGuaji.GUAJI_RETURN_MAP, ConfigGuaji.GUAJI_RETURN_HEAD, true);
			return true;
		} catch (Throwable e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			return false;
		}
	}
}