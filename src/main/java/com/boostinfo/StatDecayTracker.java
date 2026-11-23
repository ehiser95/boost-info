package com.boostinfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Works out when boosted stats next drop by one, using the same approach as RuneLite's Boosts plugin:
 * boosts fall by 1 level every 100 ticks (60s), or every 150 ticks (90s) with Preserve.
 */
@Singleton
public class StatDecayTracker
{
	private final Client client;

	private final int[] lastLevels = new int[Skill.values().length];
	private volatile int lastChangeDown = -1;
	private volatile boolean preserveBeenActive;

	@Inject
	StatDecayTracker(Client client)
	{
		this.client = client;
		reset();
	}

	void reset()
	{
		java.util.Arrays.fill(lastLevels, -1);
		lastChangeDown = -1;
		preserveBeenActive = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				reset();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		Skill skill = e.getSkill();
		int idx = skill.ordinal();
		int last = lastLevels[idx];
		int cur = client.getBoostedSkillLevel(skill);
		if (last != -1 && cur == last - 1 && cur >= client.getRealSkillLevel(skill))
		{
			// A boosted stat just ticked down by one
			lastChangeDown = client.getTickCount();
		}
		lastLevels[idx] = cur;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		// Keep the 60s cycle going between drops so the next one stays predictable
		if (lastChangeDown != -1 && ticksToNextDrop() <= 0)
		{
			lastChangeDown = client.getTickCount();
		}
	}

	/**
	 * Ticks until boosted stats next drop by one, or -1 if not known yet (no drop seen since login).
	 */
	int ticksToNextDrop()
	{
		if (lastChangeDown == -1)
		{
			return -1;
		}

		int since = client.getTickCount() - lastChangeDown;
		boolean preserve = client.isPrayerActive(Prayer.PRESERVE);
		if ((preserve && (since < 75 || preserveBeenActive)) || since > 125)
		{
			preserveBeenActive = true;
			return 150 - since;
		}

		preserveBeenActive = false;
		return since > 100 ? 125 - since : 100 - since;
	}

	int dropInterval()
	{
		return preserveBeenActive ? 150 : 100;
	}

	/**
	 * Estimated ticks until a boost of this size is completely gone, or -1 if unknown.
	 */
	int ticksUntilExpired(int boost)
	{
		int next = ticksToNextDrop();
		if (boost <= 0 || next < 0)
		{
			return -1;
		}
		return next + (boost - 1) * dropInterval();
	}
}
