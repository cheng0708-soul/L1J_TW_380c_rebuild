package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.templates.L1WeaponMagic;
import l1j.server.server.utils.SQLUtil;

public class WeaponMagicTable {
	private static Logger _log = Logger.getLogger(WeaponMagicTable.class.getName());
	private static WeaponMagicTable _instance;
	private final HashMap<Integer, L1WeaponMagic> _map = new HashMap<>();

	public static WeaponMagicTable getInstance() {
		if (_instance == null) _instance = new WeaponMagicTable();
		return _instance;
	}

	public L1WeaponMagic get(int weaponId) {
		return _map.get(weaponId);
	}

	private WeaponMagicTable() {
		load();
	}

	private void load() {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT * FROM weapon_magic");
			rs = pstm.executeQuery();
			while (rs.next()) {
				L1WeaponMagic wm = new L1WeaponMagic();
				wm.setWeaponId(rs.getInt("weapon_id"));
				wm.setSkillId(rs.getInt("skill_id"));
				wm.setProbability(rs.getInt("probability"));
				wm.setEnchantBonus(rs.getInt("enchant_bonus"));
				wm.setCastType(rs.getInt("cast_type"));
				_map.put(wm.getWeaponId(), wm);
			}
		} catch (SQLException e) {
			_log.severe(e.getMessage());
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
		_log.config("weapon_magic loaded: " + _map.size() + " entries");
	}
}
