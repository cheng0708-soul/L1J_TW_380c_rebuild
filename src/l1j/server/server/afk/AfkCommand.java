package l1j.server.server.afk;
public enum AfkCommand{
	TOGGLE,TELEPORT_MODE,PATROL_RANGE_NEAR,PATROL_RANGE_MID,PATROL_RANGE_FAR,SKILL_REGISTER,SKILL_LIST,SKILL_CLEAR,GO_HOME;
	public static AfkCommand fromAction(String action){
		if(action==null) return null;
		switch(action){
			case "sum_1": return TOGGLE;
			case "sum_2": return TELEPORT_MODE;
			case "sum_3": return PATROL_RANGE_NEAR;
			case "sum_4": return PATROL_RANGE_MID;
			case "sum_5": return PATROL_RANGE_FAR;
			case "sum_6": return SKILL_REGISTER;
			case "sum_7": return SKILL_LIST;
			case "sum_8": return SKILL_CLEAR;
			case "sum_9": return GO_HOME;
			default: return null;
		}
	}
}
