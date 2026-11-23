package com.boostinfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

/**
 * Works out which combat boosts are below your minimum and which potion to drink for them.
 * Methods that read the game must be called on the client thread (overlay rendering is).
 */
@Singleton
class PotionReminder
{
	private static final int COMBAT_TICKS = 10;

	static final class Need
	{
		final List<Skill> skills = new ArrayList<>();
		int potionItemId;
		String potionName;
	}

	private final Client client;
	private final BoostInfoConfig config;
	private final ItemManager itemManager;
	private int lastCombatTick = -100;
	/** When each skill first dropped below its minimum (epoch millis), for the delay. */
	private final java.util.Map<Skill, Long> belowSince = new java.util.EnumMap<>(Skill.class);

	@Inject
	PotionReminder(Client client, BoostInfoConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		Player me = client.getLocalPlayer();
		if (e.getHitsplat().isMine() || (me != null && e.getActor() == me))
		{
			lastCombatTick = client.getTickCount();
		}
	}

	boolean inCombat()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return false;
		}
		Actor target = me.getInteracting();
		return client.getTickCount() - lastCombatTick <= COMBAT_TICKS || target instanceof NPC;
	}

	int minimum(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return config.reminderAttack();
			case STRENGTH:
				return config.reminderStrength();
			case DEFENCE:
				return config.reminderDefence();
			case RANGED:
				return config.reminderRanged();
			case MAGIC:
				return config.reminderMagic();
			default:
				return 0;
		}
	}

	/**
	 * Is this skill's boost below your minimum right now (and should we be reminding)?
	 */
	boolean below(Skill skill)
	{
		boolean belowNow = belowIgnoringDelay(skill);
		synchronized (belowSince)
		{
			if (!belowNow)
			{
				belowSince.remove(skill);
				return false;
			}
			long now = System.currentTimeMillis();
			long since = belowSince.computeIfAbsent(skill, k -> now);
			// Wait out the delay first (e.g. after a brew, while you restore)
			return now - since >= config.reminderDelay() * 1000L;
		}
	}

	private boolean belowIgnoringDelay(Skill skill)
	{
		if (!config.reminderEnabled())
		{
			return false;
		}
		int min = minimum(skill);
		if (min <= 0 || (config.reminderOnlyInCombat() && !inCombat()))
		{
			return false;
		}
		int boost = client.getBoostedSkillLevel(skill) - client.getRealSkillLevel(skill);
		return boost < min;
	}

	/**
	 * Potions to drink, grouped (one per potion), for the skills that are below their minimum.
	 */
	List<Need> needs()
	{
		Map<String, Need> byPotion = new LinkedHashMap<>();
		for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC})
		{
			if (!below(skill))
			{
				continue;
			}
			int[] potion = potionFor(skill);
			String name = cleanName(itemManager.getItemComposition(potion[0]).getName());
			Need need = byPotion.computeIfAbsent(name, k ->
			{
				Need n = new Need();
				n.potionItemId = potion[0];
				n.potionName = k;
				return n;
			});
			need.skills.add(skill);
		}
		return new ArrayList<>(byPotion.values());
	}

	/**
	 * The best potion in your inventory for this skill, else a standard one. Returns {itemId}.
	 */
	private int[] potionFor(Skill skill)
	{
		String[] wanted;
		int fallback;
		switch (skill)
		{
			case RANGED:
				wanted = new String[]{"divine bastion", "bastion potion", "divine ranging", "ranging potion", "overload", "smelling salts"};
				fallback = ItemID._4DOSERANGERSPOTION;
				break;
			case MAGIC:
				wanted = new String[]{"divine battlemage", "battlemage potion", "divine magic", "magic potion", "saturated heart",
					"imbued heart", "forgotten brew", "ancient brew", "overload", "smelling salts"};
				fallback = ItemID._4DOSE1MAGIC;
				break;
			case ATTACK:
				wanted = new String[]{"divine super combat", "super combat", "overload", "smelling salts", "divine super attack",
					"super attack", "combat potion", "attack potion"};
				fallback = ItemID._4DOSE2COMBAT;
				break;
			case STRENGTH:
				wanted = new String[]{"divine super combat", "super combat", "overload", "smelling salts", "divine super strength",
					"super strength", "combat potion", "strength potion"};
				fallback = ItemID._4DOSE2COMBAT;
				break;
			default:
				wanted = new String[]{"divine super combat", "super combat", "overload", "smelling salts", "divine super defence",
					"super defence", "defence potion"};
				fallback = ItemID._4DOSE2COMBAT;
				break;
		}

		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv != null)
		{
			for (String want : wanted)
			{
				for (Item item : inv.getItems())
				{
					if (item.getId() > 0 && itemManager.getItemComposition(item.getId()).getName().toLowerCase().contains(want))
					{
						return new int[]{item.getId()};
					}
				}
			}
		}
		return new int[]{fallback};
	}

	/** "Ranging potion(4)" -> "Ranging potion" */
	private static String cleanName(String name)
	{
		return name.replaceAll("\\s*\\(\\d\\)$", "");
	}
}
