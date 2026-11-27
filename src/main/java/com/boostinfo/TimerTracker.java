package com.boostinfo;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.util.Text;

/**
 * Tracks potion effects and every other timer and buff from RuneLite's Timers and Buffs plugin,
 * using the same game sources (varbits, chat messages, graphics). Detection rules adapted from
 * RuneLite's TimersAndBuffsPlugin (BSD-2, Copyright (c) 2017 Seth, (c) 2018 Jordan Atwood, (c) 2019 winterdaze).
 * Most times are in game ticks; cannon and teleport cooldowns use the wall clock so they survive world hops.
 */
@Slf4j
@Singleton
public class TimerTracker
{
	private static final int PRAYER_ENHANCE_TICKS = 483; // 290 seconds
	private static final int ANTIFIRE_TICKS = 30;
	private static final int SUPER_ANTIFIRE_TICKS = 20;
	private static final int POISON_TICKS = 30;
	/** Poison varp: antivenom below this value, antipoison between it and 0. */
	private static final int VENOM_CUTOFF = -38;
	private int nextPoisonTick = -1;
	private int lastDeathChargeVarb;
	/** Shown as "Active" with no countdown (e.g. Vengeance waiting to rebound). */
	static final int INDICATOR = Integer.MAX_VALUE / 2;
	private static final String PRAYER_ENHANCE_EXPIRED = "Your prayer enhance effect has worn off.";

	/** Colour codes like "@mes_hl_mag@" at the start of game messages. */
	private static final Pattern MESSAGE_CODES = Pattern.compile("@[a-z_]+@");

	private static final int FIGHT_CAVES_REGION = 9551;
	private static final int INFERNO_REGION = 9043;
	private static final int ECLIPSE_MOON_REGION = 6038;
	private static final Pattern TZHAAR_WAVE = Pattern.compile("Wave: (\\d+)");
	private static final Pattern TZHAAR_PAUSED = Pattern.compile("The (?:Inferno|Fight Cave) has been paused. You may now log out.");
	private static final String TZHAAR_START_KEY = "tzhaarStart";
	private static final String TZHAAR_PAUSED_KEY = "tzhaarPaused";

	private static final Set<Integer> STAVES_OF_THE_DEAD = new ImmutableSet.Builder<Integer>()
		.addAll(ItemVariationMapping.getVariations(ItemID.SOTD))
		.addAll(ItemVariationMapping.getVariations(ItemID.TOXIC_SOTD))
		.add(ItemID.STAFF_OF_LIGHT)
		.add(ItemID.STAFF_OF_BALANCE)
		.build();

	/** The freeze currently running, and the tick it started (moving afterwards ends it). */
	private TimerType freeze;
	private int freezeTick = -1;
	private WorldPoint lastPoint;

	public static final class ActiveTimer
	{
		int endTick;
		int nextRefreshTick = -1;
		// Longest duration seen for this effect, for the time bar
		int totalTicks;
		/** Wall-clock end (cannon, teleports), or 0 to use endTick. */
		long endMillis;
		/** Counters: the number to show. */
		int count;
		/** Extra text after the name, e.g. the cannon's world. */
		String suffix;
	}

	private final Client client;
	private final ConfigManager configManager;

	private final Map<TimerType, ActiveTimer> active = new EnumMap<>(TimerType.class);

	@Getter
	private volatile long lastTickMillis = System.currentTimeMillis();

	@Inject
	TimerTracker(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
	}

	/**
	 * Read current values when the plugin starts mid-effect. Must run on the client thread.
	 */
	void initFromVarbits()
	{
		for (int varbit : new int[]{
			VarbitID.NZONE_OVERLOAD_POTION_EFFECTS, VarbitID.RAIDS_OVERLOAD_TIMER, VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS,
			VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER, VarbitID.PRAYER_REGENERATION_POTION_TIMER,
			VarbitID.DIVINECOMBAT_POTION_TIME, VarbitID.DIVINEATTACK_POTION_TIME, VarbitID.DIVINESTRENGTH_POTION_TIME,
			VarbitID.DIVINEDEFENCE_POTION_TIME, VarbitID.DIVINERANGE_POTION_TIME, VarbitID.DIVINEMAGIC_POTION_TIME,
			VarbitID.DIVINEBASTION_POTION_TIME, VarbitID.DIVINEBATTLEMAGE_POTION_TIME,
			VarbitID.STAMINA_DURATION, VarbitID.ANTIFIRE_POTION, VarbitID.SUPER_ANTIFIRE_POTION, VarbitID.IMBUED_HEART_TIMER,
			VarbitID.MAGIC_IMBUE_ACTIVE, VarbitID.TELEBLOCK_CYCLES, VarbitID.STATRENEWAL_POTION_TIMER, VarbitID.SURGE_POTION_TIMER,
			VarbitID.GOADING_POTION_TIMER, VarbitID.WGS_SMOULDERING_HEART_TIMER, VarbitID.DRAGONFIRE_SHIELD_RECHARGE,
			VarbitID.VENGEANCE_REBOUND, VarbitID.ARCEUUS_RESURRECTION_COOLDOWN, VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN,
			VarbitID.ARCEUUS_CORRUPTION_COOLDOWN, VarbitID.WGS_SMOULDERING_GLAND_TIMER, VarbitID.MOONLIGHT_POTION_TIME,
			VarbitID.II_AFFINITY_TIME_LEFT, VarbitID.GODWARS_ALTAR_DELAY, VarbitID.RAT_BOSS_FOOD_PILE_PLAYER_EAT_DELAY,
			VarbitID.COLOSSEUM_DOOM_STACKS_CLIENT, VarbitID.WGS_STONE_OF_JAS_BOOST, VarbitID.PLAYER_OWN_BURN_DAMAGE_TOTAL,
			VarbitID.PLAYER_OWN_BURN_DAMAGE_HITSPLAT_SIZE, VarbitID.PMOON_BOSS_CONDITION})
		{
			handleVarbit(varbit, client.getVarbitValue(varbit));
		}
		handleVarp(VarPlayerID.POISON, client.getVarpValue(VarPlayerID.POISON));
		handleVarp(VarPlayerID.MAGEARENA_CHARGE, client.getVarpValue(VarPlayerID.MAGEARENA_CHARGE));
		handleVarp(VarPlayerID.AIDE_TELE_TIMER, client.getVarpValue(VarPlayerID.AIDE_TELE_TIMER));
		handleVarp(VarPlayerID.SLUG2_REGIONUID, client.getVarpValue(VarPlayerID.SLUG2_REGIONUID));
	}

	void clear()
	{
		synchronized (active)
		{
			active.clear();
		}
		freeze = null;
	}

	/**
	 * Logging out or hopping: drop tick-based timers but keep the cannon and teleport cooldowns,
	 * which run on the wall clock.
	 */
	private void clearOnLogout()
	{
		synchronized (active)
		{
			active.entrySet().removeIf(e -> e.getValue().endMillis == 0);
		}
		freeze = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOADING:
				// Doom isn't reset when you teleport out of the Colosseum
				remove(TimerType.COLOSSEUM_DOOM);
				if (!inFightCaves() && !inInferno() && tzhaarStart() != null)
				{
					resetTzhaar();
				}
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				// Logging out without pausing still pauses the Fight Caves / Inferno timer
				if (tzhaarStart() != null && tzhaarPaused() == null)
				{
					saveTzhaar(tzhaarStart(), Instant.now().toEpochMilli());
				}
				clearOnLogout();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		lastTickMillis = System.currentTimeMillis();
		int tick = client.getTickCount();
		long now = System.currentTimeMillis();
		synchronized (active)
		{
			// Timers started from chat messages have no varbit to end them: drop them when their time is up
			active.entrySet().removeIf(en ->
			{
				ActiveTimer t = en.getValue();
				if (t.endTick >= INDICATOR)
				{
					return false;
				}
				return t.endMillis > 0 ? t.endMillis < now : t.endTick < tick;
			});
		}

		// A freeze ends when you move (after the tick it started)
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return;
		}
		WorldPoint here = me.getWorldLocation();
		if (freeze != null && freezeTick != tick && !here.equals(lastPoint))
		{
			remove(freeze);
			freeze = null;
		}
		lastPoint = here;
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged e)
	{
		Actor actor = e.getActor();
		if (actor != client.getLocalPlayer())
		{
			return;
		}
		int graphic = actor.getGraphic();
		int tick = client.getTickCount();
		if (graphic == SpotanimID.BIND_IMPACT)
		{
			startFreeze(TimerType.BIND);
		}
		else if (graphic == SpotanimID.SNARE_IMPACT)
		{
			startFreeze(TimerType.SNARE);
		}
		else if (graphic == SpotanimID.ENTANGLE_IMPACT)
		{
			startFreeze(TimerType.ENTANGLE);
		}
		else if (freezeTick == tick)
		{
			// "You have been frozen!" assumes Ice Barrage: shorten it to the spell that actually hit
			TimerType actual = null;
			if (graphic == SpotanimID.ICE_RUSH_IMPACT)
			{
				actual = TimerType.ICE_RUSH;
			}
			else if (graphic == SpotanimID.ICE_BLITZ_IMPACT)
			{
				actual = TimerType.ICE_BURST;
			}
			else if (graphic == SpotanimID.ICE_BURST_IMPACT)
			{
				actual = TimerType.ICE_BLITZ;
			}
			if (actual != null)
			{
				remove(TimerType.ICE_BARRAGE);
				startFreeze(actual);
			}
		}
	}

	private void startFreeze(TimerType type)
	{
		for (TimerType f : TimerType.FREEZES)
		{
			remove(f);
		}
		setDuration(type, freezeTicks(type));
		freeze = type;
		freezeTick = client.getTickCount();
	}

	private static int freezeTicks(TimerType type)
	{
		switch (type)
		{
			case BIND:
			case ICE_RUSH:
				return 8;
			case SNARE:
			case ICE_BURST:
				return 16;
			case ENTANGLE:
			case ICE_BLITZ:
				return 24;
			default:
				return 32;
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath e)
	{
		if (e.getActor() != client.getLocalPlayer())
		{
			return;
		}
		synchronized (active)
		{
			active.keySet().removeIf(TimerType::isRemovedOnDeath);
		}
		remove(TimerType.PRAYER_ENHANCE);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() != InventoryID.WORN)
		{
			return;
		}
		ItemContainer worn = e.getItemContainer();
		Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null || !STAVES_OF_THE_DEAD.contains(weapon.getId()))
		{
			remove(TimerType.STAFF_OF_THE_DEAD);
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (e.getNpc().getId() == NpcID.ABYSSALSIRE_SIRE_PUPPET)
		{
			remove(TimerType.ABYSSAL_SIRE_STUN);
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged e)
	{
		if (e.getNpc().getId() == NpcID.ABYSSALSIRE_SIRE_PUPPET)
		{
			remove(TimerType.ABYSSAL_SIRE_STUN);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		NPC npc = e.getNpc();
		// Killing Vorkath's spawn unfreezes you
		if (npc.isDead() && (npc.getId() == NpcID.VORKATH_SPAWN_QUEST || npc.getId() == NpcID.VORKATH_SPAWN))
		{
			remove(TimerType.ICE_BARRAGE);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarbitId() != -1)
		{
			handleVarbit(e.getVarbitId(), e.getValue());
		}
		else
		{
			handleVarp(e.getVarpId(), e.getValue());
		}
	}

	private void handleVarp(int varp, int value)
	{
		if (varp == VarPlayerID.POISON)
		{
			int tick = client.getTickCount();
			if (value == 0)
			{
				nextPoisonTick = -1;
			}
			else if (nextPoisonTick - tick <= 0)
			{
				nextPoisonTick = tick + POISON_TICKS;
			}
			if (value >= 0 || value < VENOM_CUTOFF)
			{
				remove(TimerType.ANTIPOISON);
			}
			else
			{
				setDuration(TimerType.ANTIPOISON, nextPoisonTick - tick + Math.abs((value + 1) * POISON_TICKS));
			}
			if (value >= VENOM_CUTOFF)
			{
				remove(TimerType.ANTIVENOM);
			}
			else
			{
				setDuration(TimerType.ANTIVENOM, nextPoisonTick - tick + Math.abs((value + 1 - VENOM_CUTOFF) * POISON_TICKS));
			}
		}
		else if (varp == VarPlayerID.MAGEARENA_CHARGE)
		{
			setDurationOrRemove(TimerType.CHARGE, value, value * 2);
		}
		else if (varp == VarPlayerID.AIDE_TELE_TIMER)
		{
			teleportCooldown(TimerType.HOME_TELEPORT, value, 30);
		}
		else if (varp == VarPlayerID.SLUG2_REGIONUID)
		{
			teleportCooldown(TimerType.MINIGAME_TELEPORT, value, 20);
		}
	}

	/**
	 * The varp is the minute (since the epoch) you last teleported.
	 */
	private void teleportCooldown(TimerType type, int lastTeleportMinute, int cooldownMinutes)
	{
		long end = ((long) lastTeleportMinute + cooldownMinutes) * 60_000L;
		setWallClock(type, end, cooldownMinutes * 100, null);
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		// Strip <col> tags and the newer "@mes_hl_mag@"-style colour codes the game puts in front of some messages
		String message = MESSAGE_CODES.matcher(Text.removeTags(e.getMessage())).replaceAll("").trim();
		if (message.startsWith("You drink some of your") && message.contains("prayer enhance"))
		{
			setDuration(TimerType.PRAYER_ENHANCE, PRAYER_ENHANCE_TICKS);
		}
		else if (message.equals(PRAYER_ENHANCE_EXPIRED))
		{
			remove(TimerType.PRAYER_ENHANCE);
		}
		else if (message.startsWith("You resurrect a ") && message.endsWith(" thrall."))
		{
			// A thrall lasts 1 tick (0.6s) per boosted Magic level, doubled once the Master combat
			// achievements tier is complete (Grandmaster requires Master, so it's doubled there too)
			int ticks = client.getBoostedSkillLevel(Skill.MAGIC);
			if (client.getVarbitValue(VarbitID.CA_TIER_STATUS_MASTER) == 2
				|| client.getVarbitValue(VarbitID.CA_TIER_STATUS_GRANDMASTER) == 2)
			{
				ticks *= 2;
			}
			setDuration(TimerType.THRALL, ticks);
		}
		else if (message.startsWith("Your ") && message.endsWith(" thrall returns to the grave."))
		{
			remove(TimerType.THRALL);
		}
		else if (message.endsWith("Your defence against Arceuus magic has been strengthened."))
		{
			setDuration(TimerType.WARD_OF_ARCEUUS, client.getRealSkillLevel(net.runelite.api.Skill.MAGIC));
		}
		else if (message.endsWith("Your thieving abilities have been enhanced."))
		{
			setDuration(TimerType.SHADOW_VEIL, client.getRealSkillLevel(Skill.MAGIC));
		}
		else if (message.endsWith("You have placed a Mark of Darkness upon yourself."))
		{
			setDuration(TimerType.MARK_OF_DARKNESS, markOfDarknessTicks());
			setDuration(TimerType.MARK_OF_DARKNESS_COOLDOWN, 10);
		}

		// Pickpocketing
		if (message.contains("Your dodgy necklace protects you.") || message.contains("Your attempt to steal goes unnoticed."))
		{
			remove(TimerType.PICKPOCKET_STUN);
		}
		if (message.contains("You fail to pick ") && message.contains("pocket"))
		{
			// 5 seconds, 6 for heroes and elves
			setDuration(TimerType.PICKPOCKET_STUN, message.contains("hero") || message.contains("elf") ? 10 : 8);
		}

		// Cannon (25 minutes, or 10 to repair once broken)
		if (message.equals("You place the cannon base on the ground.") || message.equals("You add the stand.")
			|| message.equals("You add the barrels.") || message.equals("You add the furnace.")
			|| message.contains("You repair your cannon, restoring it to working order."))
		{
			remove(TimerType.CANNON_REPAIR);
			setWallClock(TimerType.CANNON, System.currentTimeMillis() + 25 * 60_000L, 2500, "W" + client.getWorld());
		}
		else if (message.equals("Your cannon has broken!"))
		{
			remove(TimerType.CANNON);
			setWallClock(TimerType.CANNON_REPAIR, System.currentTimeMillis() + 10 * 60_000L, 1000, "W" + client.getWorld());
		}
		else if (message.equals("You pick up the cannon. It's really heavy.") || message.equals("Your cannon has been destroyed!"))
		{
			remove(TimerType.CANNON);
			remove(TimerType.CANNON_REPAIR);
		}

		if (message.equals("The Sire has been disorientated temporarily."))
		{
			setDuration(TimerType.ABYSSAL_SIRE_STUN, 46);
		}
		if (message.contains("Spirits of deceased evildoers offer you their protection"))
		{
			setDuration(TimerType.STAFF_OF_THE_DEAD, 100);
		}
		if (message.contains("Your protection fades away"))
		{
			remove(TimerType.STAFF_OF_THE_DEAD);
		}
		if (message.endsWith("You have been frozen!"))
		{
			// Assume Ice Barrage; the spell's graphic on the same tick corrects it
			startFreeze(TimerType.ICE_BARRAGE);
		}

		// Tombs of Amascut supplies
		if (message.equals("You quickly apply the dressing to your wounds."))
		{
			setDuration(TimerType.SILK_DRESSING, 100);
		}
		if (message.equals("You crack the crystal in your hand."))
		{
			setDuration(TimerType.BLESSED_CRYSTAL_SCARAB, 40);
		}
		if (message.equals("You drink some of the potion, reducing the energy cost of your special attacks."))
		{
			setDuration(TimerType.LIQUID_ADRENALINE, 250);
		}

		tzhaarMessage(message);
	}

	private int markOfDarknessTicks()
	{
		int ticks = client.getRealSkillLevel(Skill.MAGIC) * 3;
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if (weapon != null && weapon.getId() == ItemID.PURGING_STAFF)
			{
				return ticks * 5;
			}
		}
		return ticks;
	}

	// ---------------------------------------------------------------- Fight Caves / Inferno

	private void tzhaarMessage(String message)
	{
		if (TZHAAR_PAUSED.matcher(message).find())
		{
			saveTzhaar(tzhaarStart(), Instant.now().toEpochMilli());
			return;
		}
		Matcher m = TZHAAR_WAVE.matcher(message);
		if (!m.find())
		{
			return;
		}
		long now = Instant.now().toEpochMilli();
		if (Integer.parseInt(m.group(1)) == 1)
		{
			// The Inferno's first wave message comes 6 seconds after its timer starts
			saveTzhaar(inInferno() ? now - 6000 : now, null);
		}
		else
		{
			Long start = tzhaarStart();
			Long paused = tzhaarPaused();
			if (start != null && paused != null)
			{
				// Resuming: push the start forward by how long it was paused
				saveTzhaar(start + (now - paused), null);
			}
		}
	}

	private void resetTzhaar()
	{
		saveTzhaar(null, null);
	}

	// Saved in settings so a paused run survives a restart; kept in memory for drawing
	private Long tzhaarStartMs;
	private Long tzhaarPausedMs;
	private boolean tzhaarLoaded;

	private void loadTzhaar()
	{
		if (!tzhaarLoaded)
		{
			tzhaarStartMs = readLong(TZHAAR_START_KEY);
			tzhaarPausedMs = readLong(TZHAAR_PAUSED_KEY);
			tzhaarLoaded = true;
		}
	}

	private Long readLong(String key)
	{
		String v = configManager.getConfiguration(BoostInfoConfig.GROUP, key);
		try
		{
			return v == null ? null : Long.parseLong(v);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private void saveTzhaar(Long start, Long paused)
	{
		tzhaarStartMs = start;
		tzhaarPausedMs = paused;
		tzhaarLoaded = true;
		if (start == null)
		{
			configManager.unsetConfiguration(BoostInfoConfig.GROUP, TZHAAR_START_KEY);
		}
		else
		{
			configManager.setConfiguration(BoostInfoConfig.GROUP, TZHAAR_START_KEY, String.valueOf(start));
		}
		if (paused == null)
		{
			configManager.unsetConfiguration(BoostInfoConfig.GROUP, TZHAAR_PAUSED_KEY);
		}
		else
		{
			configManager.setConfiguration(BoostInfoConfig.GROUP, TZHAAR_PAUSED_KEY, String.valueOf(paused));
		}
	}

	private Long tzhaarStart()
	{
		loadTzhaar();
		return tzhaarStartMs;
	}

	private Long tzhaarPaused()
	{
		loadTzhaar();
		return tzhaarPausedMs;
	}

	/**
	 * The Fight Caves or Inferno timer while you're in one: which, and seconds elapsed. Null otherwise.
	 */
	Map.Entry<TimerType, Long> tzhaarElapsed()
	{
		TimerType type = inFightCaves() ? TimerType.FIGHT_CAVES : inInferno() ? TimerType.INFERNO : null;
		Long start = tzhaarStart();
		if (type == null || start == null)
		{
			return null;
		}
		Long paused = tzhaarPaused();
		long end = paused != null ? paused : System.currentTimeMillis();
		return new java.util.AbstractMap.SimpleImmutableEntry<>(type, Math.max(0, (end - start) / 1000));
	}

	private boolean inRegion(int region)
	{
		int[] regions = client.getMapRegions();
		if (regions == null)
		{
			return false;
		}
		for (int r : regions)
		{
			if (r == region)
			{
				return true;
			}
		}
		return false;
	}

	private boolean inFightCaves()
	{
		return inRegion(FIGHT_CAVES_REGION);
	}

	private boolean inInferno()
	{
		return inRegion(INFERNO_REGION);
	}

	private void handleVarbit(int varbit, int value)
	{
		if (varbit == VarbitID.NZONE_OVERLOAD_POTION_EFFECTS || varbit == VarbitID.RAIDS_OVERLOAD_TIMER)
		{
			TimerType type = client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1 ? TimerType.OVERLOAD_RAID : TimerType.OVERLOAD;
			updateRefreshing(type, value);
		}
		else if (varbit == VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS)
		{
			updateRefreshing(TimerType.BLIGHTED_OVERLOAD, value);
		}
		else if (varbit == VarbitID.TOA_MIDRAIDLOOT_STATS_TIMER)
		{
			updateRefreshing(TimerType.SMELLING_SALTS, value);
		}
		else if (varbit == VarbitID.PRAYER_REGENERATION_POTION_TIMER)
		{
			setDurationOrRemove(TimerType.PRAYER_REGEN, value, value * 12);
		}
		else if (varbit == VarbitID.DIVINECOMBAT_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_COMBAT, value, value);
		}
		else if (varbit == VarbitID.DIVINEATTACK_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_ATTACK, value, value);
		}
		else if (varbit == VarbitID.DIVINESTRENGTH_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_STRENGTH, value, value);
		}
		else if (varbit == VarbitID.DIVINEDEFENCE_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_DEFENCE, value, value);
		}
		else if (varbit == VarbitID.DIVINERANGE_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_RANGING, value, value);
		}
		else if (varbit == VarbitID.DIVINEMAGIC_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_MAGIC, value, value);
		}
		else if (varbit == VarbitID.DIVINEBASTION_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_BASTION, value, value);
		}
		else if (varbit == VarbitID.DIVINEBATTLEMAGE_POTION_TIME)
		{
			setDurationOrRemove(TimerType.DIVINE_BATTLEMAGE, value, value);
		}
		else if (varbit == VarbitID.STAMINA_ACTIVE || varbit == VarbitID.STAMINA_DURATION)
		{
			int duration = client.getVarbitValue(VarbitID.STAMINA_DURATION);
			if (client.getVarbitValue(VarbitID.STAMINA_ACTIVE) == 1 && duration > 0)
			{
				setDuration(TimerType.STAMINA, duration * 10);
			}
			else
			{
				remove(TimerType.STAMINA);
			}
		}
		else if (varbit == VarbitID.ANTIFIRE_POTION)
		{
			updateCycled(TimerType.ANTIFIRE, value, ANTIFIRE_TICKS);
		}
		else if (varbit == VarbitID.SUPER_ANTIFIRE_POTION)
		{
			updateCycled(TimerType.SUPER_ANTIFIRE, value, SUPER_ANTIFIRE_TICKS);
		}
		else if (varbit == VarbitID.IMBUED_HEART_TIMER)
		{
			setDurationOrRemove(TimerType.IMBUED_HEART, value, value * 10);
		}
		else if (varbit == VarbitID.MAGIC_IMBUE_ACTIVE)
		{
			setDurationOrRemove(TimerType.MAGIC_IMBUE, value, value * 10);
		}
		else if (varbit == VarbitID.TELEBLOCK_CYCLES)
		{
			setDurationOrRemove(TimerType.TELEBLOCK, value - 100, value - 100);
		}
		else if (varbit == VarbitID.VENGEANCE_TIMELIMIT)
		{
			// A 30 second cooldown starts when you cast it
			setDurationOrRemove(TimerType.VENGEANCE, value, 50);
		}
		else if (varbit == VarbitID.VENGEANCE_REBOUND)
		{
			setIndicator(TimerType.VENGEANCE_ACTIVE, value == 1);
		}
		else if (varbit == VarbitID.ARCEUUS_RESURRECTION_ACTIVE)
		{
			if (value == 0)
			{
				remove(TimerType.THRALL);
			}
		}
		else if (varbit == VarbitID.ARCEUUS_RESURRECTION_COOLDOWN)
		{
			setDurationOrRemove(TimerType.THRALL_COOLDOWN, value, 17);
		}
		else if (varbit == VarbitID.ARCEUUS_DEATH_CHARGE_ACTIVE)
		{
			// 2 = just cast; 1 after 0 = cast again; 0 = used up
			if (value == 2 || (value == 1 && lastDeathChargeVarb == 0))
			{
				setDuration(TimerType.DEATH_CHARGE, 100);
			}
			else if (value == 0)
			{
				remove(TimerType.DEATH_CHARGE);
			}
			lastDeathChargeVarb = value;
		}
		else if (varbit == VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN)
		{
			setDurationOrRemove(TimerType.DEATH_CHARGE_COOLDOWN, value, 100);
		}
		else if (varbit == VarbitID.ARCEUUS_CORRUPTION_COOLDOWN)
		{
			setDurationOrRemove(TimerType.CORRUPTION_COOLDOWN, value, 50);
		}
		else if (varbit == VarbitID.STATRENEWAL_POTION_TIMER)
		{
			setDurationOrRemove(TimerType.MENAPHITE_REMEDY, value, value * 25);
		}
		else if (varbit == VarbitID.SURGE_POTION_TIMER)
		{
			setDurationOrRemove(TimerType.SURGE, value, value * 10);
		}
		else if (varbit == VarbitID.GOADING_POTION_TIMER)
		{
			setDurationOrRemove(TimerType.GOADING, value, value * 6);
		}
		else if (varbit == VarbitID.WGS_SMOULDERING_HEART_TIMER)
		{
			setDurationOrRemove(TimerType.SMOULDERING_HEART, value, value * 25);
		}
		else if (varbit == VarbitID.DRAGONFIRE_SHIELD_RECHARGE)
		{
			setDurationOrRemove(TimerType.DFS_RECHARGE, value, value * 8);
		}
		else if (varbit == VarbitID.WGS_SMOULDERING_GLAND_TIMER)
		{
			setDurationOrRemove(TimerType.SMOULDERING_GLAND, value, value * 4);
		}
		else if (varbit == VarbitID.MOONLIGHT_POTION_TIME)
		{
			// Re-dosing can leave divine defence one tick ahead; match it
			int ticks = client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME) == value + 1 ? value + 1 : value;
			setDurationOrRemove(TimerType.MOONLIGHT_POTION, value, ticks);
		}
		else if (varbit == VarbitID.LUNAR_SPELLBOOK_CHANGE)
		{
			setDurationOrRemove(TimerType.SPELLBOOK_SWAP, value, 200);
		}
		else if (varbit == VarbitID.HEAL_GROUP_TIMELIMIT)
		{
			setDurationOrRemove(TimerType.HEAL_GROUP, value, 150);
		}
		else if (varbit == VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN)
		{
			setDurationOrRemove(TimerType.SHADOW_VEIL_COOLDOWN, value, 50);
		}
		else if (varbit == VarbitID.ARCEUUS_WARD_COOLDOWN)
		{
			setDurationOrRemove(TimerType.WARD_OF_ARCEUUS_COOLDOWN, value, 50);
		}
		else if (varbit == VarbitID.ARCEUUS_SHADOW_VEIL_ACTIVE)
		{
			if (value == 0)
			{
				remove(TimerType.SHADOW_VEIL);
			}
		}
		else if (varbit == VarbitID.TOA_MIDRAIDLOOT_ENERGY_ACTIVE)
		{
			if (value == 0)
			{
				remove(TimerType.LIQUID_ADRENALINE);
			}
		}
		else if (varbit == VarbitID.II_AFFINITY_TIME_LEFT)
		{
			setDurationOrRemove(TimerType.FARMERS_AFFINITY, value, value * 20);
		}
		else if (varbit == VarbitID.GODWARS_ALTAR_DELAY)
		{
			setDurationOrRemove(TimerType.GOD_WARS_ALTAR, value, value * 100);
		}
		else if (varbit == VarbitID.RAT_BOSS_FOOD_PILE_PLAYER_EAT_DELAY)
		{
			setDurationOrRemove(TimerType.SCURRIUS_FOOD_PILE, value, value * 100);
		}
		else if (varbit == VarbitID.PMOON_BOSS_CONDITION)
		{
			Player me = client.getLocalPlayer();
			boolean eclipse = me != null && WorldPoint.fromLocal(client, me.getLocalLocation()).getRegionID() == ECLIPSE_MOON_REGION;
			setCount(eclipse ? TimerType.CURSE_OF_THE_MOONS_ECLIPSE : TimerType.CURSE_OF_THE_MOONS_BLUE, value);
		}
		else if (varbit == VarbitID.COLOSSEUM_DOOM_STACKS_CLIENT)
		{
			setCount(TimerType.COLOSSEUM_DOOM, value);
		}
		else if (varbit == VarbitID.WGS_STONE_OF_JAS_BOOST)
		{
			setIndicator(TimerType.STONE_OF_JAS, value > 0);
		}
		else if (varbit == VarbitID.PLAYER_OWN_BURN_DAMAGE_TOTAL)
		{
			setCount(TimerType.BURN_DAMAGE_TOTAL, value);
		}
		else if (varbit == VarbitID.PLAYER_OWN_BURN_DAMAGE_HITSPLAT_SIZE)
		{
			setCount(TimerType.BURN_DAMAGE_NEXT_HIT, value);
		}
	}

	/**
	 * Counters (stacks, burn damage): a number with no countdown. 0 removes it.
	 */
	private void setCount(TimerType type, int count)
	{
		if (count <= 0)
		{
			remove(type);
			return;
		}
		synchronized (active)
		{
			ActiveTimer t = active.computeIfAbsent(type, k -> new ActiveTimer());
			t.endTick = INDICATOR;
			t.count = count;
		}
	}

	/**
	 * Timers that run on the wall clock (survive world hops and logouts). Removed once already over.
	 */
	private void setWallClock(TimerType type, long endMillis, int totalTicks, String suffix)
	{
		if (endMillis <= System.currentTimeMillis())
		{
			remove(type);
			return;
		}
		synchronized (active)
		{
			ActiveTimer t = active.computeIfAbsent(type, k -> new ActiveTimer());
			t.endMillis = endMillis;
			t.endTick = 0;
			t.totalTicks = totalTicks;
			t.suffix = suffix;
		}
	}

	/**
	 * Overload and smelling salts: the varbit is the number of 15-second refreshes left.
	 * It goes down by one at each refresh, so a decrease marks the start of a new 25-tick cycle.
	 */
	private void updateRefreshing(TimerType type, int refreshesLeft)
	{
		if (refreshesLeft <= 0)
		{
			remove(type);
			return;
		}

		int tick = client.getTickCount();
		synchronized (active)
		{
			ActiveTimer t = active.computeIfAbsent(type, k -> new ActiveTimer());
			if (t.nextRefreshTick - tick <= 0)
			{
				t.nextRefreshTick = tick + TimerType.REFRESH_TICKS;
			}
			t.endTick = t.nextRefreshTick + (refreshesLeft - 1) * TimerType.REFRESH_TICKS;
			t.totalTicks = Math.max(t.totalTicks, t.endTick - tick);
		}
	}

	/**
	 * Timers where the varbit counts down in fixed-length cycles (antifire: 30 ticks, super antifire: 20).
	 */
	private void updateCycled(TimerType type, int cyclesLeft, int cycleTicks)
	{
		if (cyclesLeft <= 0)
		{
			remove(type);
			return;
		}
		int tick = client.getTickCount();
		synchronized (active)
		{
			ActiveTimer t = active.computeIfAbsent(type, k -> new ActiveTimer());
			if (t.nextRefreshTick - tick <= 0)
			{
				t.nextRefreshTick = tick + cycleTicks;
			}
			t.endTick = t.nextRefreshTick + (cyclesLeft - 1) * cycleTicks;
			t.totalTicks = Math.max(t.totalTicks, t.endTick - tick);
		}
	}

	private void setIndicator(TimerType type, boolean on)
	{
		if (on)
		{
			synchronized (active)
			{
				ActiveTimer t = active.computeIfAbsent(type, k -> new ActiveTimer());
				t.endTick = INDICATOR;
			}
		}
		else
		{
			remove(type);
		}
	}

	private void setDurationOrRemove(TimerType type, int value, int ticks)
	{
		if (value <= 0)
		{
			remove(type);
		}
		else
		{
			setDuration(type, ticks);
		}
	}

	private void setDuration(TimerType type, int ticks)
	{
		synchronized (active)
		{
			ActiveTimer t = active.computeIfAbsent(type, k -> new ActiveTimer());
			t.endTick = client.getTickCount() + ticks;
			t.totalTicks = Math.max(t.totalTicks, ticks);
		}
	}

	private void remove(TimerType type)
	{
		synchronized (active)
		{
			active.remove(type);
		}
	}

	/**
	 * Snapshot of active timers for rendering.
	 */
	Map<TimerType, ActiveTimer> snapshot()
	{
		synchronized (active)
		{
			Map<TimerType, ActiveTimer> copy = new EnumMap<>(TimerType.class);
			for (Map.Entry<TimerType, ActiveTimer> e : active.entrySet())
			{
				ActiveTimer t = new ActiveTimer();
				t.endTick = e.getValue().endTick;
				t.nextRefreshTick = e.getValue().nextRefreshTick;
				t.totalTicks = e.getValue().totalTicks;
				t.endMillis = e.getValue().endMillis;
				t.count = e.getValue().count;
				t.suffix = e.getValue().suffix;
				copy.put(e.getKey(), t);
			}
			return copy;
		}
	}

	/**
	 * Seconds left, smoothed between ticks.
	 */
	double secondsLeft(ActiveTimer t)
	{
		if (t.endMillis > 0)
		{
			return Math.max(0, (t.endMillis - System.currentTimeMillis()) / 1000.0);
		}
		int ticks = t.endTick - client.getTickCount();
		double sinceTick = Math.min(0.6, (System.currentTimeMillis() - lastTickMillis) / 1000.0);
		return Math.max(0, ticks * 0.6 - sinceTick);
	}

	/**
	 * Fraction of the effect left (0..1), for the time bar.
	 */
	double fractionLeft(ActiveTimer t)
	{
		return t.totalTicks <= 0 ? 0 : Math.min(1, secondsLeft(t) / (t.totalTicks * 0.6));
	}

	/**
	 * Ticks until the next 15-second refresh, or -1 for timers that don't refresh.
	 */
	int ticksToRefresh(ActiveTimer t)
	{
		if (t.nextRefreshTick < 0)
		{
			return -1;
		}
		int ticks = t.nextRefreshTick - client.getTickCount();
		// Between the expected refresh and the varbit update arriving, roll into the next cycle
		while (ticks <= 0)
		{
			ticks += TimerType.REFRESH_TICKS;
		}
		return ticks;
	}
}
