package com.boostinfo;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;

/**
 * Potion effects with a known duration.
 */
@Getter
public enum TimerType
{
	OVERLOAD("Overload", ItemID.NZONE4DOSEOVERLOADPOTION, Group.OVERLOAD, true, combat()),
	OVERLOAD_RAID("Overload", ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4, Group.OVERLOAD, true, combat()),
	BLIGHTED_OVERLOAD("Overload", ItemID.DEADMAN4DOSEOVERLOAD, Group.OVERLOAD, true, combat()),
	SMELLING_SALTS("Smelling salts", ItemID.TOA_SUPPLY_STATS_2, Group.SALTS, true, combat()),
	PRAYER_REGEN("Prayer regen", ItemID._4DOSE1PRAYER_REGENERATION, Group.PRAYER_REGEN, false, EnumSet.noneOf(Skill.class)),
	PRAYER_ENHANCE("Prayer enhance", ItemID.RAIDS_VIAL_PRAYER_WEAK_4, Group.PRAYER_REGEN, false, EnumSet.noneOf(Skill.class)),
	DIVINE_COMBAT("Divine combat", ItemID._4DOSEDIVINECOMBAT, Group.DIVINE, false, EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE)),
	DIVINE_BASTION("Divine bastion", ItemID._4DOSEDIVINEBASTION, Group.DIVINE, false, EnumSet.of(Skill.RANGED, Skill.DEFENCE)),
	DIVINE_BATTLEMAGE("Divine battlemage", ItemID._4DOSEDIVINEBATTLEMAGE, Group.DIVINE, false, EnumSet.of(Skill.MAGIC, Skill.DEFENCE)),
	DIVINE_ATTACK("Divine attack", ItemID._4DOSEDIVINEATTACK, Group.DIVINE, false, EnumSet.of(Skill.ATTACK)),
	DIVINE_STRENGTH("Divine strength", ItemID._4DOSEDIVINESTRENGTH, Group.DIVINE, false, EnumSet.of(Skill.STRENGTH)),
	DIVINE_DEFENCE("Divine defence", ItemID._4DOSEDIVINEDEFENCE, Group.DIVINE, false, EnumSet.of(Skill.DEFENCE)),
	DIVINE_RANGING("Divine ranging", ItemID._4DOSEDIVINERANGE, Group.DIVINE, false, EnumSet.of(Skill.RANGED)),
	DIVINE_MAGIC("Divine magic", ItemID._4DOSEDIVINEMAGIC, Group.DIVINE, false, EnumSet.of(Skill.MAGIC)),

	// Other timers (same sources as RuneLite's Timers and Buffs plugin)
	STAMINA("Stamina", ItemID._4DOSESTAMINA, -1, "stamina"),
	ANTIFIRE("Antifire", ItemID._4DOSE1ANTIDRAGON, -1, "antifire"),
	SUPER_ANTIFIRE("Super antifire", ItemID._4DOSE3ANTIDRAGON, -1, "antifire"),
	ANTIPOISON("Antipoison", ItemID._4DOSEANTIPOISON, -1, "antipoison"),
	ANTIVENOM("Anti-venom", ItemID.ANTIVENOM4, -1, "antipoison"),
	IMBUED_HEART("Heart cooldown", ItemID.IMBUED_HEART, -1, "heart"),
	VENGEANCE("Vengeance cooldown", -1, SpriteID.LunarMagicOn.VENGEANCE, "vengeance"),
	VENGEANCE_ACTIVE("Vengeance", -1, SpriteID.LunarMagicOn.VENGEANCE, "vengeance"),
	THRALL("Thrall", -1, SpriteID.MagicNecroOn.RESURRECT_SUPERIOR_SKELETON, "thralls"),
	THRALL_COOLDOWN("Thrall cooldown", -1, SpriteID.MagicNecroOff.RESURRECT_SUPERIOR_SKELETON_DISABLED, "thralls"),
	DEATH_CHARGE("Death charge", -1, SpriteID.MagicNecroOn.DEATH_CHARGE, "arceuus"),
	DEATH_CHARGE_COOLDOWN("Death charge cooldown", -1, SpriteID.MagicNecroOff.DEATH_CHARGE_DISABLED, "arceuus"),
	WARD_OF_ARCEUUS("Ward of Arceuus", -1, SpriteID.MagicNecroOn.WARD_OF_ARCEUUS, "arceuus"),
	CORRUPTION_COOLDOWN("Corruption cooldown", -1, SpriteID.MagicNecroOff.GREATER_CORRUPTION_DISABLED, "arceuus"),
	MAGIC_IMBUE("Magic Imbue", -1, SpriteID.LunarMagicOn.MAGIC_IMBUE, "spells"),
	CHARGE("Charge", -1, SpriteID.Magicon2.CHARGE, "spells"),
	TELEBLOCK("Teleblock", -1, SpriteID.Magicon2.TELE_BLOCK, "spells"),
	MENAPHITE_REMEDY("Menaphite remedy", ItemID._4DOSESTATRENEWAL, -1, "potions"),
	SURGE("Surge potion", ItemID._4DOSESURGE, -1, "potions"),
	GOADING("Goading potion", ItemID._4DOSEGOADING, -1, "potions"),
	SMOULDERING_HEART("Smouldering heart", ItemID.SMOULDERING_HEART, -1, "misc"),
	DFS_RECHARGE("DFS recharge", ItemID.DRAGONFIRE_SHIELD, -1, "misc"),
	;

	/**
	 * Overloads and smelling salts re-apply their boost every 25 ticks (15 seconds).
	 */
	public static final int REFRESH_TICKS = 25;

	public enum Group
	{
		OVERLOAD, SALTS, PRAYER_REGEN, DIVINE, OTHER
	}

	private final String displayName;
	private final int itemId;
	private final Group group;
	private final boolean refreshing;
	private final Set<Skill> heldSkills;
	/** Sprite icon (spells) when there's no item icon, else -1. */
	private final int spriteId;
	/** Which "Other timers" setting turns this on, or null for potion timers. */
	private final String otherKey;

	TimerType(String displayName, int itemId, Group group, boolean refreshing, Set<Skill> heldSkills)
	{
		this.displayName = displayName;
		this.itemId = itemId;
		this.group = group;
		this.refreshing = refreshing;
		this.heldSkills = heldSkills;
		this.spriteId = -1;
		this.otherKey = null;
	}

	TimerType(String displayName, int itemId, int spriteId, String otherKey)
	{
		this.displayName = displayName;
		this.itemId = itemId;
		this.group = Group.OTHER;
		this.refreshing = false;
		this.heldSkills = EnumSet.noneOf(Skill.class);
		this.spriteId = spriteId;
		this.otherKey = otherKey;
	}

	private static Set<Skill> combat()
	{
		return EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC);
	}
}
