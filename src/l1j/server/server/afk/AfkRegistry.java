package l1j.server.server.afk;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkRegistry {
	private static final ConcurrentMap<Integer, AfkAiController> MAP = new ConcurrentHashMap<Integer, AfkAiController>();
	private AfkRegistry() {}
	public static AfkAiController get(L1PcInstance pc) { return MAP.computeIfAbsent(pc.getId(), id -> new AfkAiController(pc)); }
	public static void remove(L1PcInstance pc) { MAP.remove(pc.getId()); }
}