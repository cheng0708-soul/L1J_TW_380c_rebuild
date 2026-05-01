package l1j.server.server.templates;

public class L1WeaponMagic {
	private int _weaponId;
	private int _skillId;
	private int _probability;
	private int _enchantBonus;
	private int _castType;
	private int _triggerType;

	public int getWeaponId()          { return _weaponId; }
	public void setWeaponId(int v)    { _weaponId = v; }
	public int getSkillId()           { return _skillId; }
	public void setSkillId(int v)     { _skillId = v; }
	public int getProbability()       { return _probability; }
	public void setProbability(int v) { _probability = v; }
	public int getEnchantBonus()      { return _enchantBonus; }
	public void setEnchantBonus(int v){ _enchantBonus = v; }
	public int getCastType()          { return _castType; }
	public void setCastType(int v)    { _castType = v; }
	public int getTriggerType()       { return _triggerType; }
	public void setTriggerType(int v) { _triggerType = v; }
}
