package com.boostinfo;

import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Tracks potion effect timers from the game's own varbits (same sources as RuneLite's Timers and Buffs plugin).
 * All times are in game ticks; render code converts to seconds.
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

	public static final class ActiveTimer
	{
		int endTick;
		int nextRefreshTick = -1;
		// Longest duration seen for this effect, for the time bar
		int totalTicks;
	}

	private final Client client;

	private final Map<TimerType, ActiveTimer> active = new EnumMap<>(TimerType.class);

	@Getter
	private volatile long lastTickMillis = System.currentTimeMillis();

	@Inject
	TimerTracker(Client client)
	{
		this.client = client;
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
			VarbitID.ARCEUUS_CORRUPTION_COOLDOWN})
		{
			handleVarbit(varbit, client.getVarbitValue(varbit));
		}
		handleVarp(VarPlayerID.POISON, client.getVarpValue(VarPlayerID.POISON));
		handleVarp(VarPlayerID.MAGEARENA_CHARGE, client.getVarpValue(VarPlayerID.MAGEARENA_CHARGE));
	}

	void clear()
	{
		synchronized (active)
		{
			active.clear();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				clear();
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
		synchronized (active)
		{
			// Timers started from chat messages have no varbit to end them: drop them when their time is up
			for (TimerType type : new TimerType[]{TimerType.PRAYER_ENHANCE, TimerType.THRALL, TimerType.WARD_OF_ARCEUUS})
			{
				ActiveTimer t = active.get(type);
				if (t != null && t.endTick < tick)
				{
					active.remove(type);
				}
			}
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
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = Text.removeTags(e.getMessage());
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
			// A thrall lasts 1 tick per (boosted) Magic level, doubled with the Master combat achievements tier
			int ticks = client.getBoostedSkillLevel(net.runelite.api.Skill.MAGIC);
			if (client.getVarbitValue(VarbitID.CA_TIER_STATUS_MASTER) == 2)
			{
				ticks += ticks;
			}
			setDuration(TimerType.THRALL, ticks);
		}
		else if (message.endsWith("Your defence against Arceuus magic has been strengthened."))
		{
			setDuration(TimerType.WARD_OF_ARCEUUS, client.getRealSkillLevel(net.runelite.api.Skill.MAGIC));
		}
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
