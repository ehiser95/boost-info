package com.boostinfo;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BoostInfoConfig.GROUP)
public interface BoostInfoConfig extends Config
{
	String GROUP = "boost-info-list";

	@ConfigSection(name = "Stat boosts", description = "The list of boosted stats.", position = 0)
	String SECTION_STATS = "stats";

	@ConfigSection(name = "Potion timers", description = "The list of potion timers.", position = 1)
	String SECTION_TIMERS = "timers";

	@ConfigSection(name = "Warnings", description = "Red refresh warning and fading.", position = 2)
	String SECTION_WARNINGS = "warnings";

	@ConfigSection(name = "Boost level colours", description = "Change the colour of a boost as it wears down.", position = 3)
	String SECTION_LEVELS = "levels";

	@ConfigSection(name = "Potion reminder", description = "Flash and show an alert when a boost drops below your minimum.", position = 4)
	String SECTION_REMINDER = "reminder";

	@ConfigSection(name = "Other timers", description = "More timers from Timers and Buffs, in your chosen layout.", position = 5)
	String SECTION_OTHER = "other";

	@ConfigSection(name = "Skill names", description = "Names and colours in the compact list.", position = 6, closedByDefault = true)
	String SECTION_NAMES = "names";

	@ConfigItem(keyName = "preview", name = "Preview", position = -3,
		description = "Show sample boosts, timers and the potion alert so you can see how your settings look "
			+ "without drinking anything. Turn it off when you're done.")
	default boolean preview()
	{
		return false;
	}

	@ConfigItem(keyName = "orientation", name = "Orientation", position = -1,
		description = "Lay the lists out top to bottom (vertical) or side by side (horizontal).")
	default Orientation orientation()
	{
		return Orientation.VERTICAL;
	}

	@ConfigItem(keyName = "layout", name = "Layout", position = -1,
		description = "Chips, tiles with a time bar, or a compact list. Applies to both the stat boosts and the potion timers.")
	default LayoutStyle layout()
	{
		return LayoutStyle.LIST;
	}

	// Stat boosts

	@ConfigItem(keyName = "statsDisplayMode", name = "Show stat boosts", position = 0, section = SECTION_STATS,
		description = "Always, or only once a boost has this many seconds or less left.")
	default DisplayMode statsDisplayMode()
	{
		return DisplayMode.ALWAYS;
	}

	@ConfigItem(keyName = "statsThreshold", name = " ↳ Time left", position = 1, section = SECTION_STATS,
		description = "With 'Only when time is low': show a boost once it has this long left. Also when the number turns amber.")
	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	default int statsThreshold()
	{
		return 60;
	}

	@ConfigItem(keyName = "showNonCombat", name = "Include non-combat skills", position = 2, section = SECTION_STATS,
		description = "Also list boosts to skills like Mining or Herblore.")
	default boolean showNonCombat()
	{
		return false;
	}

	@ConfigItem(keyName = "showDrains", name = "Show drained stats", position = 3, section = SECTION_STATS,
		description = "Also list stats that are lowered, in red.")
	default boolean showDrains()
	{
		return true;
	}

	// Potion timers

	@ConfigItem(keyName = "timersDisplayMode", name = "Show potion timers", position = 0, section = SECTION_TIMERS,
		description = "Always, or only once a timer has this many seconds or less left.")
	default DisplayMode timersDisplayMode()
	{
		return DisplayMode.ALWAYS;
	}

	@ConfigItem(keyName = "timersThreshold", name = " ↳ Time left", position = 1, section = SECTION_TIMERS,
		description = "With 'Only when time is low': show a timer once it has this long left.")
	@Units(Units.SECONDS)
	@Range(min = 1, max = 900)
	default int timersThreshold()
	{
		return 60;
	}

	@ConfigItem(keyName = "showOverload", name = "Overload", position = 2, section = SECTION_TIMERS, description = "")
	default boolean showOverload()
	{
		return true;
	}

	@ConfigItem(keyName = "showSalts", name = "Smelling salts", position = 3, section = SECTION_TIMERS, description = "")
	default boolean showSalts()
	{
		return true;
	}

	@ConfigItem(keyName = "showPrayerRegen", name = "Prayer regen / enhance", position = 4, section = SECTION_TIMERS,
		description = "Prayer regeneration potion and Chambers of Xeric prayer enhance.")
	default boolean showPrayerRegen()
	{
		return true;
	}

	@ConfigItem(keyName = "showDivine", name = "Divine potions", position = 5, section = SECTION_TIMERS, description = "")
	default boolean showDivine()
	{
		return true;
	}

	// Warnings

	@ConfigItem(keyName = "refreshWarning", name = "Red before refresh", position = 0, section = SECTION_WARNINGS,
		description = "Overload and smelling salts: turn the number red just before each 15-second refresh.")
	default boolean refreshWarning()
	{
		return true;
	}

	@ConfigItem(keyName = "refreshWarningTicks", name = " ↳ Ticks before", position = 1, section = SECTION_WARNINGS,
		description = "How many game ticks before the refresh the number turns red.")
	@Range(min = 1, max = 5)
	default int refreshWarningTicks()
	{
		return 2;
	}

	@ConfigItem(keyName = "refreshWarningOnStats", name = " ↳ Also on stat numbers", position = 2, section = SECTION_WARNINGS,
		description = "Also turn the boosted stat numbers red, not just the timer.")
	default boolean refreshWarningOnStats()
	{
		return true;
	}

	@ConfigItem(keyName = "fadeThreshold", name = "Fade when time left", position = 3, section = SECTION_WARNINGS,
		description = "Ticked timers below start fading in and out when they have this long left.")
	@Units(Units.SECONDS)
	@Range(min = 1, max = 300)
	default int fadeThreshold()
	{
		return 30;
	}

	@ConfigItem(keyName = "fadeOverload", name = " ↳ Fade overload", position = 4, section = SECTION_WARNINGS, description = "")
	default boolean fadeOverload()
	{
		return true;
	}

	@ConfigItem(keyName = "fadeSalts", name = " ↳ Fade smelling salts", position = 5, section = SECTION_WARNINGS, description = "")
	default boolean fadeSalts()
	{
		return true;
	}

	@ConfigItem(keyName = "fadePrayerRegen", name = " ↳ Fade prayer regen / enhance", position = 6, section = SECTION_WARNINGS, description = "")
	default boolean fadePrayerRegen()
	{
		return true;
	}

	@ConfigItem(keyName = "fadeDivine", name = " ↳ Fade divine potions", position = 7, section = SECTION_WARNINGS, description = "")
	default boolean fadeDivine()
	{
		return false;
	}

	// Boost level colours

	@ConfigItem(keyName = "tier1Enabled", name = "Level 1", position = 0, section = SECTION_LEVELS,
		description = "Change colour when a boost is at or below this many levels.")
	default boolean tier1Enabled()
	{
		return true;
	}

	@ConfigItem(keyName = "tier1Boost", name = " ↳ At or below", position = 1, section = SECTION_LEVELS, description = "Boost levels left.")
	@Range(min = 0, max = 30)
	default int tier1Boost()
	{
		return 8;
	}

	@Alpha
	@ConfigItem(keyName = "tier1Colour", name = " ↳ Colour", position = 2, section = SECTION_LEVELS, description = "")
	default Color tier1Colour()
	{
		return new Color(255, 220, 60);
	}

	@ConfigItem(keyName = "tier1Flash", name = " ↳ Flash", position = 3, section = SECTION_LEVELS, description = "Make the number flash.")
	default boolean tier1Flash()
	{
		return false;
	}

	@ConfigItem(keyName = "tier2Enabled", name = "Level 2", position = 4, section = SECTION_LEVELS,
		description = "Change colour when a boost is at or below this many levels.")
	default boolean tier2Enabled()
	{
		return true;
	}

	@ConfigItem(keyName = "tier2Boost", name = " ↳ At or below", position = 5, section = SECTION_LEVELS, description = "Boost levels left.")
	@Range(min = 0, max = 30)
	default int tier2Boost()
	{
		return 5;
	}

	@Alpha
	@ConfigItem(keyName = "tier2Colour", name = " ↳ Colour", position = 6, section = SECTION_LEVELS, description = "")
	default Color tier2Colour()
	{
		return new Color(255, 140, 0);
	}

	@ConfigItem(keyName = "tier2Flash", name = " ↳ Flash", position = 7, section = SECTION_LEVELS, description = "Make the number flash.")
	default boolean tier2Flash()
	{
		return false;
	}

	@ConfigItem(keyName = "tier3Enabled", name = "Level 3", position = 8, section = SECTION_LEVELS,
		description = "Change colour when a boost is at or below this many levels.")
	default boolean tier3Enabled()
	{
		return true;
	}

	@ConfigItem(keyName = "tier3Boost", name = " ↳ At or below", position = 9, section = SECTION_LEVELS, description = "Boost levels left.")
	@Range(min = 0, max = 30)
	default int tier3Boost()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(keyName = "tier3Colour", name = " ↳ Colour", position = 10, section = SECTION_LEVELS, description = "")
	default Color tier3Colour()
	{
		return new Color(255, 60, 50);
	}

	@ConfigItem(keyName = "tier3Flash", name = " ↳ Flash", position = 11, section = SECTION_LEVELS, description = "Make the number flash.")
	default boolean tier3Flash()
	{
		return true;
	}

	// Potion reminder

	@ConfigItem(keyName = "reminderEnabled", name = "Potion reminder", position = 0, section = SECTION_REMINDER,
		description = "When a boost drops below your minimum for that skill, flash it red and show a 'Use potion' alert.")
	default boolean reminderEnabled()
	{
		return false;
	}

	@ConfigItem(keyName = "reminderAttack", name = " ↳ Attack minimum", position = 1, section = SECTION_REMINDER,
		description = "Minimum Attack boost (0 = off).")
	@Range(min = 0, max = 30)
	default int reminderAttack()
	{
		return 0;
	}

	@ConfigItem(keyName = "reminderStrength", name = " ↳ Strength minimum", position = 2, section = SECTION_REMINDER,
		description = "Minimum Strength boost (0 = off).")
	@Range(min = 0, max = 30)
	default int reminderStrength()
	{
		return 0;
	}

	@ConfigItem(keyName = "reminderDefence", name = " ↳ Defence minimum", position = 3, section = SECTION_REMINDER,
		description = "Minimum Defence boost (0 = off).")
	@Range(min = 0, max = 30)
	default int reminderDefence()
	{
		return 0;
	}

	@ConfigItem(keyName = "reminderRanged", name = " ↳ Ranged minimum", position = 4, section = SECTION_REMINDER,
		description = "Minimum Ranged boost (0 = off).")
	@Range(min = 0, max = 30)
	default int reminderRanged()
	{
		return 0;
	}

	@ConfigItem(keyName = "reminderMagic", name = " ↳ Magic minimum", position = 5, section = SECTION_REMINDER,
		description = "Minimum Magic boost (0 = off).")
	@Range(min = 0, max = 30)
	default int reminderMagic()
	{
		return 0;
	}

	@ConfigItem(keyName = "reminderOnlyInCombat", name = " ↳ Only in combat", position = 6, section = SECTION_REMINDER,
		description = "Only remind you while you're fighting (attacking or being hit in the last few seconds).")
	default boolean reminderOnlyInCombat()
	{
		return true;
	}

	@ConfigItem(keyName = "reminderScale", name = " ↳ Alert size", position = 9, section = SECTION_REMINDER,
		description = "Make the 'Use [potion name]' alert bigger or smaller. To move it, hold Alt and drag it "
			+ "(turn on Preview to see it).")
	@Units(Units.PERCENT)
	@Range(min = 50, max = 300)
	default int reminderScale()
	{
		return 100;
	}

	@ConfigItem(keyName = "reminderDelay", name = " ↳ Delay", position = 8, section = SECTION_REMINDER,
		description = "Only remind you once a boost has stayed below your minimum this long, so drinking a brew "
			+ "(which lowers your stats) gives you time to restore and re-pot first.")
	@Units(Units.SECONDS)
	@Range(min = 0, max = 120)
	default int reminderDelay()
	{
		return 15;
	}

	@ConfigItem(keyName = "reminderAlert", name = " ↳ Center-screen alert", position = 7, section = SECTION_REMINDER,
		description = "Show the potion's picture and 'Use [potion name]' in the middle of the screen, for example 'Use Ranging potion'.")
	default boolean reminderAlert()
	{
		return true;
	}

	// Other timers

	@ConfigItem(keyName = "otherPlacement", name = "Show other timers in", position = 0, section = SECTION_OTHER,
		description = "Their own list (default), or attached to the potion timers or the stat boosts.")
	default OtherPlacement otherPlacement()
	{
		return OtherPlacement.OWN_LIST;
	}

	@ConfigItem(keyName = "otherStamina", name = "Stamina", position = 1, section = SECTION_OTHER, description = "")
	default boolean otherStamina()
	{
		return true;
	}

	@ConfigItem(keyName = "otherAntifire", name = "Antifire / super antifire", position = 2, section = SECTION_OTHER, description = "")
	default boolean otherAntifire()
	{
		return true;
	}

	@ConfigItem(keyName = "otherAntipoison", name = "Antipoison / anti-venom", position = 3, section = SECTION_OTHER, description = "")
	default boolean otherAntipoison()
	{
		return true;
	}

	@ConfigItem(keyName = "otherHeart", name = "Imbued / saturated heart cooldown", position = 4, section = SECTION_OTHER, description = "")
	default boolean otherHeart()
	{
		return true;
	}

	@ConfigItem(keyName = "otherVengeance", name = "Vengeance", position = 5, section = SECTION_OTHER,
		description = "Vengeance active (waiting to rebound) and its cooldown.")
	default boolean otherVengeance()
	{
		return true;
	}

	@ConfigItem(keyName = "otherThralls", name = "Thralls", position = 9, section = SECTION_OTHER,
		description = "Thrall time left and the resurrection cooldown.")
	default boolean otherThralls()
	{
		return true;
	}

	@ConfigItem(keyName = "otherArceuus", name = "Death charge / Ward / Corruption", position = 10, section = SECTION_OTHER,
		description = "Arceuus spells: Death charge (and cooldown), Ward of Arceuus, Corruption cooldown.")
	default boolean otherArceuus()
	{
		return true;
	}

	@ConfigItem(keyName = "otherAmmo", name = "Equipped ammo", position = 11, section = SECTION_OTHER,
		description = "Your ammo slot and how much is left.")
	default boolean otherAmmo()
	{
		return true;
	}

	@ConfigItem(keyName = "otherAmmoLow", name = " ↳ Low ammo warning", position = 12, section = SECTION_OTHER,
		description = "Show the ammo count in red at or below this many (0 = off).")
	@Range(min = 0, max = 10000)
	default int otherAmmoLow()
	{
		return 100;
	}

	@ConfigItem(keyName = "otherSpells", name = "Magic Imbue / Charge / Teleblock", position = 6, section = SECTION_OTHER, description = "")
	default boolean otherSpells()
	{
		return true;
	}

	@ConfigItem(keyName = "otherPotions", name = "Remedy / surge / goading", position = 7, section = SECTION_OTHER,
		description = "Menaphite remedy, surge potion and goading potion.")
	default boolean otherPotions()
	{
		return true;
	}

	@ConfigItem(keyName = "otherMisc", name = "Smouldering heart / DFS recharge", position = 8, section = SECTION_OTHER, description = "")
	default boolean otherMisc()
	{
		return true;
	}

	// Skill names

	@ConfigItem(keyName = "shortNames", name = "Short skill names", position = 0, section = SECTION_NAMES,
		description = "Atk, Str, Def, Rng and Mg in the compact list.")
	default boolean shortNames()
	{
		return false;
	}

	@Alpha
	@ConfigItem(keyName = "attackColour", name = "Attack name colour", position = 1, section = SECTION_NAMES, description = "")
	default Color attackColour()
	{
		return new Color(255, 160, 60);
	}

	@Alpha
	@ConfigItem(keyName = "strengthColour", name = "Strength name colour", position = 2, section = SECTION_NAMES, description = "")
	default Color strengthColour()
	{
		return new Color(255, 160, 60);
	}

	@Alpha
	@ConfigItem(keyName = "defenceColour", name = "Defence name colour", position = 3, section = SECTION_NAMES, description = "")
	default Color defenceColour()
	{
		return new Color(255, 220, 60);
	}

	@Alpha
	@ConfigItem(keyName = "rangedColour", name = "Ranged name colour", position = 4, section = SECTION_NAMES, description = "")
	default Color rangedColour()
	{
		return new Color(95, 217, 122);
	}

	@Alpha
	@ConfigItem(keyName = "magicColour", name = "Magic name colour", position = 5, section = SECTION_NAMES, description = "")
	default Color magicColour()
	{
		return new Color(90, 160, 255);
	}
}
