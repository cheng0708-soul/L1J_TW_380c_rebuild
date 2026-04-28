package l1j.server.server.afk;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 獨立牆後怪判定用的設定讀取層（盡量不改既有 AfkConfig）。
 * - 會嘗試從 AfkConfig 以反射讀取以下鍵；若不存在則採用預設值。
 * - 這個類別只提供靜態 getter，不持有外部相依；方便直接移除或替換。
 *
 * 參數對應（建議在 config/afk.properties 內新增，含中文註解）：
 *  Afk_walljudge_enabled=true
 *  Afk_wall_los_step_limit=20          // LOS 檢查最多步數（Bresenham 修正上限）
 *  Afk_wall_range_check=10             // 遠程有效射程內才嚴格檢查（<=10 格）
 *  Afk_wall_ignore_radius=25           // 目標距離 > 25 格時自動清除 ignore
 *  Afk_wall_recheck_ms=300             // ignore 重試間隔（毫秒）
 *  Afk_wall_clear_on_hit=true          // 當攻擊實際命中時，立即清除 ignore
 *  Afk_wall_debug=false
 */
public class AfkWallConfig {
	private static final Logger _log = Logger.getLogger(AfkWallConfig.class.getName());

	private static boolean _enabled = true;
	private static int _losStepLimit = 20;
	private static int _rangeCheck = 10;
	private static int _ignoreRadius = 25;
	private static int _recheckMs = 300;
	private static boolean _clearOnHit = true;
	private static boolean _debug = false;

	static {
		// 嘗試從 AfkConfig 讀取（存在才覆蓋）
		tryLoadFromAfkConfig();
	}

	public static void tryLoadFromAfkConfig() {
		try {
			Class<?> cfg = Class.forName("l1j.server.server.afk.AfkConfig");
			_enabled = getBoolean(cfg, "Afk_walljudge_enabled", _enabled);
			_losStepLimit = getInt(cfg, "Afk_wall_los_step_limit", _losStepLimit);
			_rangeCheck = getInt(cfg, "Afk_wall_range_check", _rangeCheck);
			_ignoreRadius = getInt(cfg, "Afk_wall_ignore_radius", _ignoreRadius);
			_recheckMs = getInt(cfg, "Afk_wall_recheck_ms", _recheckMs);
			_clearOnHit = getBoolean(cfg, "Afk_wall_clear_on_hit", _clearOnHit);
			_debug = getBoolean(cfg, "Afk_wall_debug", _debug);
		} catch (Throwable t) {
			// 若沒有 AfkConfig 就維持預設，不丟例外以保持獨立性
			_log.log(Level.FINE, "AfkWallConfig: AfkConfig not found, using defaults.", t);
		}
	}

	private static boolean getBoolean(Class<?> cfg, String field, boolean def) {
		try {
			return ((Boolean) cfg.getField(field).get(null)).booleanValue();
		} catch (Throwable t) {
			return def;
		}
	}

	private static int getInt(Class<?> cfg, String field, int def) {
		try {
			return ((Integer) cfg.getField(field).get(null)).intValue();
		} catch (Throwable t) {
			return def;
		}
	}

	// ===== getters =====
	public static boolean enabled() { return _enabled; }
	public static int losStepLimit() { return _losStepLimit; }
	public static int rangeCheck() { return _rangeCheck; }
	public static int ignoreRadius() { return _ignoreRadius; }
	public static int recheckMs() { return _recheckMs; }
	public static boolean clearOnHit() { return _clearOnHit; }
	public static boolean debug() { return _debug; }
}