package com.boostinfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Whether you're in combat: you hit something, something hit you, you're attacking an NPC,
 * or an NPC is attacking you. You stay "in combat" for a while after the last of these.
 */
@Singleton
class CombatTracker
{
	private final Client client;
	private final BoostInfoConfig config;
	private int lastCombatTick = -1000;

	@Inject
	CombatTracker(Client client, BoostInfoConfig config)
	{
		this.client = client;
		this.config = config;
	}

	boolean inCombat()
	{
		// Timeout is in seconds; a game tick is 0.6 seconds
		int timeoutTicks = (int) Math.ceil(config.combatTimeout() / 0.6);
		return client.getTickCount() - lastCombatTick <= timeoutTicks;
	}

	void reset()
	{
		lastCombatTick = -1000;
	}

	private void mark()
	{
		lastCombatTick = client.getTickCount();
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		if (e.getActor() == client.getLocalPlayer() || e.getHitsplat().isMine())
		{
			mark();
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged e)
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return;
		}
		if ((e.getSource() == me && attackable(e.getTarget())) || (e.getTarget() == me && e.getSource() instanceof NPC))
		{
			mark();
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		// Still attacking an NPC (even if missing), or an NPC is still attacking you
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return;
		}
		if (attackable(me.getInteracting()))
		{
			mark();
		}
	}

	private static boolean attackable(Actor a)
	{
		return a instanceof NPC && ((NPC) a).getCombatLevel() > 0;
	}
}
