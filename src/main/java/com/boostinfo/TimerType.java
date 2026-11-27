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
	WARD_OF_ARCEUUS("Ward of Arceuus", -1, SpriteID.MagicNecroOn.WARD_OF_ARCEUUS, "arceuus", true),
	CORRUPTION_COOLDOWN("Corruption cooldown", -1, SpriteID.MagicNecroOff.GREATER_CORRUPTION_DISABLED, "arceuus"),
	MAGIC_IMBUE("Magic Imbue", -1, SpriteID.LunarMagicOn.MAGIC_IMBUE, "spells"),
	CHARGE("Charge", -1, SpriteID.Magicon2.CHARGE, "spells"),
	TELEBLOCK("Teleblock", -1, SpriteID.Magicon2.TELE_BLOCK, "spells"),
	MENAPHITE_REMEDY("Menaphite remedy", ItemID._4DOSESTATRENEWAL, -1, "potions"),
	SURGE("Surge potion", ItemID._4DOSESURGE, -1, "potions"),
	GOADING("Goading potion", ItemID._4DOSEGOADING, -1, "potions"),
	SMOULDERING_HEART("Smouldering heart", ItemID.SMOULDERING_HEART, -1, "misc"),
	DFS_RECHARGE("DFS recharge", ItemID.DRAGONFIRE_SHIELD, -1, "misc"),
	SMOULDERING_GLAND("Smouldering gland", ItemID.SMOULDERING_GLAND, -1, "misc"),
	STAFF_OF_THE_DEAD("Staff of the Dead", ItemID.SOTD, -1, "misc"),
	MOONLIGHT_POTION("Moonlight potion", ItemID._4DOSEMOONLIGHTPOTION, -1, "potions"),

	// The rest of RuneLite's Timers and Buffs
	CANNON("Cannon", ItemID.TWPART3, -1, "cannon"),
	CANNON_REPAIR("Broken cannon", ItemID.MCANNONTOOLKIT, -1, "cannon"),
	BIND("Bind", -1, SpriteID.Magicon2.BIND, "freezes", true),
	SNARE("Snare", -1, SpriteID.Magicon2.SNARE, "freezes", true),
	ENTANGLE("Entangle", -1, SpriteID.Magicon2.ENTANGLE, "freezes", true),
	ICE_RUSH("Ice rush", -1, SpriteID.Magicon2.ICE_RUSH, "freezes", true),
	ICE_BURST("Ice burst", -1, SpriteID.Magicon2.ICE_BURST, "freezes", true),
	ICE_BLITZ("Ice blitz", -1, SpriteID.Magicon2.ICE_BLITZ, "freezes", true),
	ICE_BARRAGE("Ice barrage", -1, SpriteID.Magicon2.ICE_BARRAGE, "freezes", true),
	HOME_TELEPORT("Home teleport", -1, SpriteID.Magicon2.LUMBRIDGE_HOME_TELEPORT, "teleports"),
	MINIGAME_TELEPORT("Minigame teleport", -1, SpriteID.SideiconsInterface.MINIGAMES, "teleports"),
	HEAL_GROUP("Heal Group", -1, SpriteID.LunarMagicOn.HEAL_GROUP, "lunar"),
	SPELLBOOK_SWAP("Spellbook reset", -1, SpriteID.LunarMagicOn.SPELLBOOK_SWAP, "lunar"),
	SHADOW_VEIL("Shadow veil", -1, SpriteID.MagicNecroOn.SHADOW_VEIL, "arceuus", true),
	SHADOW_VEIL_COOLDOWN("Shadow veil cooldown", -1, SpriteID.MagicNecroOff.SHADOW_VEIL_DISABLED, "arceuus"),
	WARD_OF_ARCEUUS_COOLDOWN("Ward of Arceuus cooldown", -1, SpriteID.MagicNecroOff.WARD_OF_ARCEUUS_DISABLED, "arceuus"),
	MARK_OF_DARKNESS("Mark of Darkness", -1, SpriteID.MagicNecroOn.MARK_OF_DARKNESS, "arceuus", true),
	MARK_OF_DARKNESS_COOLDOWN("Mark of Darkness cooldown", -1, SpriteID.MagicNecroOff.MARK_OF_DARKNESS_DISABLED, "arceuus", true),
	LIQUID_ADRENALINE("Liquid adrenaline", ItemID.TOA_SUPPLY_ENERGY_2, -1, "toa"),
	SILK_DRESSING("Silk dressing", ItemID.TOA_SUPPLY_HEAL_OVERTIME_2, -1, "toa", true),
	BLESSED_CRYSTAL_SCARAB("Blessed crystal scarab", ItemID.TOA_SUPPLY_PRAYER_OVERTIME_2, -1, "toa", true),
	ABYSSAL_SIRE_STUN("Abyssal Sire stun", ItemID.ABYSSALSIRE_PET, -1, "bosses", true),
	GOD_WARS_ALTAR("God Wars altar", -1, SpriteID.Staticons.PRAYER, "bosses"),
	SCURRIUS_FOOD_PILE("Scurrius food pile", ItemID.CHEESE, -1, "bosses"),
	PICKPOCKET_STUN("Stunned", -1, SpriteID.Staticons.THIEVING, "skilling", true),
	FARMERS_AFFINITY("Farmer's affinity", ItemID.GRAIN, -1, "skilling"),
	FIGHT_CAVES("Fight Caves", ItemID.TZHAAR_CAPE_FIRE, -1, "tzhaar"),
	INFERNO("Inferno", ItemID.INFERNAL_CAPE, -1, "tzhaar"),

	// Counters: show a number instead of a time
	CURSE_OF_THE_MOONS_BLUE("Curse of the Moons", ItemID.FROST_MOON_HELM, -1, "bossBuffs"),
	CURSE_OF_THE_MOONS_ECLIPSE("Curse of the Moons", ItemID.ECLIPSE_MOON_HELM, -1, "bossBuffs"),
	COLOSSEUM_DOOM("Doom", -1, SpriteID.Hitmark.COLOSSEUM_DOOM, "bossBuffs"),
	STONE_OF_JAS("Stone of Jas", ItemID.WGS_STONE_OF_JAS_DUMMY_ITEM, -1, "bossBuffs"),
	BURN_DAMAGE_TOTAL("Burn damage", -1, SpriteID.Hitmark.BURN_DAMAGE, "burn"),
	BURN_DAMAGE_NEXT_HIT("Next burn hit", ItemID.BONE_CLAW, -1, "burnNext"),
	;

	/**
	 * Shown as a number (stacks, damage) rather than a time.
	 */
	public static final Set<TimerType> COUNTERS = EnumSet.of(CURSE_OF_THE_MOONS_BLUE, CURSE_OF_THE_MOONS_ECLIPSE,
		COLOSSEUM_DOOM, BURN_DAMAGE_TOTAL, BURN_DAMAGE_NEXT_HIT);

	/**
	 * Freezes, fastest to slowest.
	 */
	public static final Set<TimerType> FREEZES = EnumSet.of(BIND, SNARE, ENTANGLE, ICE_RUSH, ICE_BURST, ICE_BLITZ, ICE_BARRAGE);

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
	/** Cleared when you die. */
	private final boolean removedOnDeath;

	TimerType(String displayName, int itemId, Group group, boolean refreshing, Set<Skill> heldSkills)
	{
		this.displayName = displayName;
		this.itemId = itemId;
		this.group = group;
		this.refreshing = refreshing;
		this.heldSkills = heldSkills;
		this.spriteId = -1;
		this.otherKey = null;
		this.removedOnDeath = false;
	}

	TimerType(String displayName, int itemId, int spriteId, String otherKey)
	{
		this(displayName, itemId, spriteId, otherKey, false);
	}

	TimerType(String displayName, int itemId, int spriteId, String otherKey, boolean removedOnDeath)
	{
		this.displayName = displayName;
		this.itemId = itemId;
		this.group = Group.OTHER;
		this.refreshing = false;
		this.heldSkills = EnumSet.noneOf(Skill.class);
		this.spriteId = spriteId;
		this.otherKey = otherKey;
		this.removedOnDeath = removedOnDeath;
	}

	private static Set<Skill> combat()
	{
		return EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC);
	}
}
