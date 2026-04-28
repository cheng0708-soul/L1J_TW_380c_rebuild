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
package l1j.server.server.model;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.Config;
import l1j.server.server.ActionCodes;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.IdFactory;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.model.Instance.L1DoorInstance;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.gametime.L1GameTime;
import l1j.server.server.model.gametime.L1GameTimeAdapter;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1SpawnTime;
import l1j.server.server.types.Point;
import l1j.server.server.utils.Random;
import l1j.server.server.utils.collections.Lists;
import l1j.server.server.utils.collections.Maps;

public class L1Spawn extends L1GameTimeAdapter {
	private static Logger _log = Logger.getLogger(L1Spawn.class.getName());

	private final L1Npc _template;

	private int _id; // just to find this in the spawn table

	private String _location;

	private int _maximumCount;
	
	// wave respawn
	/** 這個 spawn 原始設定的基礎數量（從 setAmount 設進來） */
	private int _baseAmount;
	/** 最高只允許到 10 倍 */
	private static final int MAX_WAVE_MULTIPLIER = 10;
	/** 目前這一輪這個 spawn 要維持的怪物數量（依當下在線人數算出） */
	private int _currentWaveTarget = 0;

	
	private int _npcid;

	private int _groupId;

	private int _locx;

	private int _locy;

	private int Randomx;

	private int Randomy;

	private int _locx1;

	private int _locy1;

	private int _locx2;

	private int _locy2;

	private int _heading;

	private int _minRespawnDelay;

	private int _maxRespawnDelay;

	private short _mapid;

	private boolean _respaenScreen;

	private int _movementDistance;

	private boolean _rest;

	private int _spawnType;

	private int _delayInterval;

	private L1SpawnTime _time;

	private Map<Integer, Point> _homePoint = null; // initでspawnした個々のオブジェクトのホームポイント

	private List<L1NpcInstance> _mobs = Lists.newList();

	private String _name;

	private class SpawnTask implements Runnable {
		private int _spawnNumber;

		private int _objectId;

		private SpawnTask(int spawnNumber, int objectId) {
			_spawnNumber = spawnNumber;
			_objectId = objectId;
		}

		@Override		
		public void run() {
			// 原生重生一隻
			doSpawn(_spawnNumber, _objectId);
			// BOSS 不複製
			if (L1Spawn.this instanceof L1BossSpawn) return;
			if (!isWaveMode()) return;
			if (!Config.ONLINE_SPAWN_MULTIPLIER_ENABLED) return;
			int online=L1World.getInstance().getAllPlayers().size();
			int threshold=Config.ONLINE_SPAWN_THRESHOLD;
			int mult=Config.ONLINE_SPAWN_MULTIPLIER;
			if (threshold<=0 || mult<=1 || online<threshold) return;
			L1Object obj=L1World.getInstance().findObject(_objectId);
			if (!(obj instanceof L1NpcInstance)) return;
			L1NpcInstance base=(L1NpcInstance)obj;
			for(int k=1;k<mult;k++) spawnCloneFrom(base);
		}
	}

	public L1Spawn(L1Npc mobTemplate) {
		_template = mobTemplate;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public short getMapId() {
		return _mapid;
	}

	public void setMapId(short _mapid) {
		this._mapid = _mapid;
	}

	public boolean isRespawnScreen() {
		return _respaenScreen;
	}

	public void setRespawnScreen(boolean flag) {
		_respaenScreen = flag;
	}

	public int getMovementDistance() {
		return _movementDistance;
	}

	public void setMovementDistance(int i) {
		_movementDistance = i;
	}

	public int getAmount() {
		return _maximumCount;
	}

	public int getGroupId() {
		return _groupId;
	}

	public int getId() {
		return _id;
	}

	public String getLocation() {
		return _location;
	}

	public int getLocX() {
		return _locx;
	}

	public int getLocY() {
		return _locy;
	}

	public int getNpcId() {
		return _npcid;
	}

	public int getHeading() {
		return _heading;
	}

	public int getRandomx() {
		return Randomx;
	}

	public int getRandomy() {
		return Randomy;
	}

	public int getLocX1() {
		return _locx1;
	}

	public int getLocY1() {
		return _locy1;
	}

	public int getLocX2() {
		return _locx2;
	}

	public int getLocY2() {
		return _locy2;
	}

	public int getMinRespawnDelay() {
		return _minRespawnDelay;
	}

	public int getMaxRespawnDelay() {
		return _maxRespawnDelay;
	}

	public void setAmount(int amount) {
		_maximumCount = amount;
		// 初始化波次制的基礎數量：用 DB 中設定的 amount 當 base
		if (_baseAmount <= 0) {
			_baseAmount = amount;
		}
	}

	public void setId(int id) {
		_id = id;
	}

	public void setGroupId(int i) {
		_groupId = i;
	}

	public void setLocation(String location) {
		_location = location;
	}

	public void setLocX(int locx) {
		_locx = locx;
	}

	public void setLocY(int locy) {
		_locy = locy;
	}

	public void setNpcid(int npcid) {
		_npcid = npcid;
	}

	public void setHeading(int heading) {
		_heading = heading;
	}

	public void setRandomx(int randomx) {
		Randomx = randomx;
	}

	public void setRandomy(int randomy) {
		Randomy = randomy;
	}

	public void setLocX1(int locx1) {
		_locx1 = locx1;
	}

	public void setLocY1(int locy1) {
		_locy1 = locy1;
	}

	public void setLocX2(int locx2) {
		_locx2 = locx2;
	}

	public void setLocY2(int locy2) {
		_locy2 = locy2;
	}

	public void setMinRespawnDelay(int i) {
		_minRespawnDelay = i;
	}

	public void setMaxRespawnDelay(int i) {
		_maxRespawnDelay = i;
	}

	private int calcRespawnDelay() {
		int respawnDelay = _minRespawnDelay * 1000;
		if (_delayInterval > 0) {
			respawnDelay += Random.nextInt(_delayInterval) * 1000;
		}
		L1GameTime currentTime = L1GameTimeClock.getInstance().currentTime();
		if ((_time != null) && !_time.getTimePeriod().includes(currentTime)) { // 指定時間外なら指定時間までの時間を足す
			long diff = (_time.getTimeStart().getTime() - currentTime.toTime().getTime());
			if (diff < 0) {
				diff += 24 * 1000L * 3600L;
			}
			diff /= 6; // real time to game time
			respawnDelay = (int) diff;
		}
		return respawnDelay;
	}

	/**
	 * SpawnTaskを起動する。
	 * 
	 * @param spawnNumber
	 *            L1Spawnで管理されている番号。ホームポイントが無ければ何を指定しても良い。
	 */
	public void executeSpawnTask(int spawnNumber, int objectId) {
		SpawnTask task = new SpawnTask(spawnNumber, objectId);
		GeneralThreadPool.getInstance().schedule(task, calcRespawnDelay());
	}

	private boolean _initSpawn = false;

	private boolean _spawnHomePoint;

	public void init() {
		if ((_time != null) && _time.isDeleteAtEndTime()) {
			// 時間外削除が指定されているなら、時間経過の通知を受ける。
			L1GameTimeClock.getInstance().addListener(this);
		}
		_delayInterval = _maxRespawnDelay - _minRespawnDelay;
		_initSpawn = true;
		// ホームポイントを持たせるか
		if (Config.SPAWN_HOME_POINT && (Config.SPAWN_HOME_POINT_COUNT <= getAmount()) && (Config.SPAWN_HOME_POINT_DELAY >= getMinRespawnDelay())
				&& isAreaSpawn()) {
			_spawnHomePoint = true;
			_homePoint = Maps.newMap();
		}

		int spawnNum = 0;
		while (spawnNum < _maximumCount) {
			// spawnNumは1～maxmumCountまで
			doSpawn(++spawnNum);
		}
		_initSpawn = false;
	}

	/**
	 * ホームポイントがある場合は、spawnNumberを基にspawnする。 それ以外の場合は、spawnNumberは未使用。
	 */
	protected void doSpawn(int spawnNumber) { // 初期配置
		// 指定時間外であれば、次spawnを予約して終わる。
		if ((_time != null) && !_time.getTimePeriod().includes(L1GameTimeClock.getInstance().currentTime())) {
			executeSpawnTask(spawnNumber, 0);
			return;
		}
		doSpawn(spawnNumber, 0);
	}

	protected void doSpawn(int spawnNumber, int objectId) { // 再出現
		L1NpcInstance mob = null;
		try {
			int newlocx = getLocX();
			int newlocy = getLocY();
			int tryCount = 0;

			mob = NpcTable.getInstance().newNpcInstance(_template);
			synchronized (_mobs) {
				_mobs.add(mob);
			}
			if (objectId == 0) {
				mob.setId(IdFactory.getInstance().nextId());
			}
			else {
				mob.setId(objectId); // オブジェクトID再利用
			}

			if ((0 <= getHeading()) && (getHeading() <= 7)) {
				mob.setHeading(getHeading());
				//TODO 隨機面向byby9001183ex
			} else if (getHeading() == 9) {
				mob.setHeading(Random.nextInt(8));
				//TODO 隨機面向by9001183ex
			} else {
				// heading値が正しくない
				mob.setHeading(5);
			}

			int npcId = mob.getNpcTemplate().get_npcId();
			if ((npcId == 45488) && (getMapId() == 9)) { // 卡士伯
				mob.setMap((short) (getMapId() + Random.nextInt(2)));
			}
			else if ((npcId == 45601) && (getMapId() == 11)) { // 死亡騎士
				mob.setMap((short) (getMapId() + Random.nextInt(3)));
			}
			else if ((npcId == 81322) && (getMapId() == 25)) { // 黑騎士副隊長
				mob.setMap((short) (getMapId() + Random.nextInt(2)));
			}
			else {
				mob.setMap(getMapId());
			}
			mob.setMovementDistance(getMovementDistance());
			mob.setRest(isRest());
			while (tryCount <= 50) {
				switch (getSpawnType()) {
					case SPAWN_TYPE_PC_AROUND: // PC周辺に湧くタイプ
						if (!_initSpawn) { // 初期配置では無条件に通常spawn
							List<L1PcInstance> players = Lists.newList();
							for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
								if (getMapId() == pc.getMapId()) {
									players.add(pc);
								}
							}
							if (players.size() > 0) {
								L1PcInstance pc = players.get(Random.nextInt(players.size()));
								L1Location loc = pc.getLocation().randomLocation(PC_AROUND_DISTANCE, false);
								newlocx = loc.getX();
								newlocy = loc.getY();
								break;
							}
						}
						// フロアにPCがいなければ通常の出現方法
					default:
						if (isAreaSpawn()) { // 座標が範囲指定されている場合
							Point pt = null;
							if (_spawnHomePoint && (null != (pt = _homePoint.get(spawnNumber)))) { // ホームポイントを元に再出現させる場合
								L1Location loc = new L1Location(pt, getMapId()).randomLocation(Config.SPAWN_HOME_POINT_RANGE, false);
								newlocx = loc.getX();
								newlocy = loc.getY();
							}
							else {
								int rangeX = getLocX2() - getLocX1();
								int rangeY = getLocY2() - getLocY1();
								newlocx = Random.nextInt(rangeX) + getLocX1();
								newlocy = Random.nextInt(rangeY) + getLocY1();
							}
							if (tryCount > 49) { // 出現位置が決まらない時はlocx,locyの値
								newlocx = getLocX();
								newlocy = getLocY();
							}
						}
						else if (isRandomSpawn()) { // 座標のランダム値が指定されている場合
							newlocx = (getLocX() + (Random.nextInt(getRandomx()) - Random.nextInt(getRandomx())));
							newlocy = (getLocY() + (Random.nextInt(getRandomy()) - Random.nextInt(getRandomy())));
						}
						else { // どちらも指定されていない場合
							newlocx = getLocX();
							newlocy = getLocY();
						}
				}
				mob.setX(newlocx);
				mob.setHomeX(newlocx);
				mob.setY(newlocy);
				mob.setHomeY(newlocy);

				if (mob.getMap().isInMap(mob.getLocation()) && mob.getMap().isPassable(mob.getLocation())) {
					if (mob instanceof L1MonsterInstance) {
						if (isRespawnScreen()) {
							break;
						}
						L1MonsterInstance mobtemp = (L1MonsterInstance) mob;
						if (L1World.getInstance().getVisiblePlayer(mobtemp).isEmpty()) {
							break;
						}
						// 画面内にPCが居て出現できない場合は、3秒後にスケジューリングしてやり直し
						SpawnTask task = new SpawnTask(spawnNumber, mob.getId());
						GeneralThreadPool.getInstance().schedule(task, 3000L);
						return;
					}
				}
				tryCount++;
			}
			if (mob instanceof L1MonsterInstance) {
				((L1MonsterInstance) mob).initHide();
			}

			mob.setSpawn(this);
			mob.setreSpawn(true);
			mob.setSpawnNumber(spawnNumber); // L1Spawnでの管理番号(ホームポイントに使用)
			if (_initSpawn && _spawnHomePoint) { // 初期配置でホームポイントを設定
				Point pt = new Point(mob.getX(), mob.getY());
				_homePoint.put(spawnNumber, pt); // ここで保存したpointを再出現時に使う
			}

			if (mob instanceof L1MonsterInstance) {
				if (mob.getMapId() == 666) {
					((L1MonsterInstance) mob).set_storeDroped(true);
				}
			}
			if ((npcId == 45573) && (mob.getMapId() == 2)) { // バフォメット
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					if (pc.getMapId() == 2) {
						L1Teleport.teleport(pc, 32664, 32797, (short) 2, 0, true);
					}
				}
			}

			if (((npcId == 46142) && (mob.getMapId() == 73)) || ((npcId == 46141) && (mob.getMapId() == 74))) {
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					if ((pc.getMapId() >= 72) && (pc.getMapId() <= 74)) {
						L1Teleport.teleport(pc, 32840, 32833, (short) 72, pc.getHeading(), true);
					}
				}
			}
			if ((npcId == 81341) && ((mob.getMapId() == 2000) || (mob.getMapId() == 2001) || (mob.getMapId() == 2002) || (mob.getMapId() == 2003))) { // 再生之祭壇
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					if ((pc.getMapId() >= 2000) && (pc.getMapId() <= 2003)) {
						L1Teleport.teleport(pc, 32933, 32988, (short) 410, 5, true);
					}
				}
			}

			doCrystalCave(npcId);

			L1World.getInstance().storeObject(mob);
			L1World.getInstance().addVisibleObject(mob);

			if (mob instanceof L1MonsterInstance) {
				L1MonsterInstance mobtemp = (L1MonsterInstance) mob;
				if (!_initSpawn && (mobtemp.getHiddenStatus() == 0)) {
					mobtemp.onNpcAI(); // モンスターのＡＩを開始
				}
			}
			if (getGroupId() != 0) {
				L1MobGroupSpawn.getInstance().doSpawn(mob, getGroupId(), isRespawnScreen(), _initSpawn);
			}
			mob.turnOnOffLight();
			mob.startChat(L1NpcInstance.CHAT_TIMING_APPEARANCE); // チャット開始
		}
		catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	public void setRest(boolean flag) {
		_rest = flag;
	}

	public boolean isRest() {
		return _rest;
	}

	private static final int SPAWN_TYPE_PC_AROUND = 1;

	private static final int PC_AROUND_DISTANCE = 30;

	private int getSpawnType() {
		return _spawnType;
	}

	public void setSpawnType(int type) {
		_spawnType = type;
	}

	private boolean isAreaSpawn() {
		return (getLocX1() != 0) && (getLocY1() != 0) && (getLocX2() != 0) && (getLocY2() != 0);
	}

	private boolean isRandomSpawn() {
		return (getRandomx() != 0) || (getRandomy() != 0);
	}

	public L1SpawnTime getTime() {
		return _time;
	}

	public void setTime(L1SpawnTime time) {
		_time = time;
	}

	@Override
	public void onMinuteChanged(L1GameTime time) {
		if (_time.getTimePeriod().includes(time)) {
			return;
		}
		synchronized (_mobs) {
			if (_mobs.isEmpty()) {
				return;
			}
			// 指定時間外になっていれば削除
			for (L1NpcInstance mob : _mobs) {
				mob.setCurrentHpDirect(0);
				mob.setDead(true);
				mob.setStatus(ActionCodes.ACTION_Die);
				mob.deleteMe();
			}
			_mobs.clear();
		}
	}

	public static void doCrystalCave(int npcId) {
		int[] npcId2 =
		{ 46143, 46144, 46145, 46146, 46147, 46148, 46149, 46150, 46151, 46152 };
		int[] doorId =
		{ 5001, 5002, 5003, 5004, 5005, 5006, 5007, 5008, 5009, 5010 };

		for (int i = 0; i < npcId2.length; i++) {
			if (npcId == npcId2[i]) {
				closeDoorInCrystalCave(doorId[i]);
			}
		}
	}

	private static void closeDoorInCrystalCave(int doorId) {
		for (L1Object object : L1World.getInstance().getObject()) {
			if (object instanceof L1DoorInstance) {
				L1DoorInstance door = (L1DoorInstance) object;
				if (door.getDoorId() == doorId) {
					door.close();
				}
			}
		}
	}


	// 決定是否在此 spawn 啟用波次制
	// 目前只在拉 3F(mapId=309) 且 spawn id 為 32868、32872 作為測試
	private boolean isWaveMode() {
		// 全域啟用波次制：所有非 BOSS 的 spawn 都會依線上人數調整數量
		// 外層已經用 L1BossSpawn 排除 BOSS，不會影響 Boss spawn
		return true;
	}

	// 根據線上人數計算此 spawn 應該存在的怪物數量
	// 1 人在線：維持 DB 原本數量
	// 2 人(含)以上：10 倍，但最多不超過 MAX_WAVE_MULTIPLIER(10 倍)
	private int calcOnlineWaveCount() {
		int base = (_baseAmount > 0) ? _baseAmount : 1;
		int online = L1World.getInstance().getAllPlayers().size();

		// 未啟用在線人數倍增時，直接回傳基礎數量
		if (!Config.ONLINE_SPAWN_MULTIPLIER_ENABLED) {
			return base;
		}

		int threshold = Config.ONLINE_SPAWN_THRESHOLD;
		int fixedCount = Config.ONLINE_SPAWN_MULTIPLIER;

		// 參數防呆：門檻或設定數量異常時，直接採用基礎數量
		if (threshold <= 0 || fixedCount <= 0) {
			return base;
		}

		// 未達門檻：在線人數小於設定值，維持原本 DB 設定的數量
		if (online < threshold) {
			return base;
		}

		// 達到門檻：
		// 伺服器設定的倍率值(ONLINE_SPAWN_MULTIPLIER)直接代表「這個 spawn 應該維持幾隻」
		// 例如設定為 10 → 殺一隻就會補到 10 隻，而不是 DB count 的 10 倍。
		return fixedCount;
	}

	// 單一 spawn 的波次制重生：
// - 每一個 spawn 有一個「目前這一輪」的目標數量 _currentWaveTarget
// - 當場上這個 spawn 生出的怪都死光時，才依當下在線人數重算下一輪的目標數量
// - 這一輪進行中途，就算在線人數變動，也不會變更這一輪的目標數量
	private void handleWaveRespawn(int spawnNumber, int objectId) {
		// 死一隻怪時的重生邏輯：
		// 1) 先使用原本的 doSpawn() 重生 1 隻，讓系統依照 DB count / 地圖上限判定是否能重生。
		// 2) 如果達成人數門檻，則再依當前倍率額外複製怪物（倍率 N → 再多生 N-1 隻）。

		// 先做一次原始重生，這一隻會遵守原本的 spawn 上限與地圖條件。
		doSpawn(spawnNumber, objectId);

		// 以下為倍率模式：只有在達成人數門檻時才會啟動。
		if (!Config.ONLINE_SPAWN_MULTIPLIER_ENABLED) {
			return;
		}

		int online = L1World.getInstance().getAllPlayers().size();
		int threshold = Config.ONLINE_SPAWN_THRESHOLD;
		int multiplier = Config.ONLINE_SPAWN_MULTIPLIER;

		// 門檻或倍率設定異常，或未達門檻時，一律維持原本「死一隻生一隻」的行為。
		if (threshold <= 0 || multiplier <= 1 || online < threshold) {
			return;
		}

		// 已達門檻：在剛剛那一隻正常重生之外，再額外複製 (multiplier - 1) 隻同樣的怪物。
		// 這些額外的 doSpawn 呼叫依然會受到原本 spawn / 地圖上限的保護，不會無限制爆量。
		for (int i = 1; i < multiplier; i++) {
			doSpawn(spawnNumber, 0);
		}
	}
	private void spawnCloneFrom(L1NpcInstance base){
		try{
			L1NpcInstance npc=NpcTable.getInstance().newNpcInstance(base.getNpcId());
			npc.setId(IdFactory.getInstance().nextId());
			npc.setMap(base.getMapId());
			npc.setX(base.getX());
			npc.setY(base.getY());
			npc.setHomeX(base.getX());
			npc.setHomeY(base.getY());
			npc.setHeading(base.getHeading());
			L1World.getInstance().storeObject(npc);
			L1World.getInstance().addVisibleObject(npc);
			npc.turnOnOffLight();
			npc.startChat(L1NpcInstance.CHAT_TIMING_APPEARANCE);
		}catch(Exception e){_log.log(Level.SEVERE,e.getMessage(),e);}
	}
}