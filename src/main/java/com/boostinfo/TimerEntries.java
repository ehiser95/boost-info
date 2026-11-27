package com.boostinfo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

/**
 * Builds the rows for timers (potion timers and the other Timers and Buffs timers), shared by every list.
 */
@Singleton
class TimerEntries
{
	private final TimerTracker timers;
	private final BoostInfoConfig config;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final Map<Integer, BufferedImage> sprites = new ConcurrentHashMap<>();
	private final Client client;

	@Inject
	TimerEntries(Client client, TimerTracker timers, BoostInfoConfig config, ItemManager itemManager, SpriteManager spriteManager)
	{
		this.client = client;
		this.timers = timers;
		this.config = config;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
	}

	/** Potion timers (overload, salts, prayer regen, divines). */
	List<Entry> potionTimers()
	{
		if (config.preview())
		{
			return potionSamples();
		}
		return build(t -> t.getGroup() != TimerType.Group.OTHER);
	}

	/** The other Timers and Buffs timers (stamina, antifire, vengeance...). */
	List<Entry> otherTimers()
	{
		if (config.preview())
		{
			return otherSamples();
		}
		List<Entry> list = build(t -> t.getGroup() == TimerType.Group.OTHER);
		Entry tzhaar = tzhaarEntry();
		if (tzhaar != null)
		{
			list.add(0, tzhaar);
		}
		Entry ammo = ammoEntry();
		if (ammo != null)
		{
			list.add(ammo);
		}
		return list;
	}

	/**
	 * Your equipped ammo and how many are left (red when low). Client thread (overlays render on it).
	 */
	private Entry ammoEntry()
	{
		if (!config.otherAmmo())
		{
			return null;
		}
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return null;
		}
		Item ammo = worn.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		if (ammo == null || ammo.getId() <= 0 || ammo.getQuantity() <= 0)
		{
			return null;
		}
		int qty = ammo.getQuantity();
		Color color = config.otherAmmoLow() > 0 && qty <= config.otherAmmoLow() ? Colors.WARN : Colors.GOOD;
		String name = itemManager.getItemComposition(ammo.getId()).getName();
		return new Entry(itemManager.getImage(ammo.getId()), name, formatCount(qty), color, 1f, -1);
	}

	/**
	 * Fight Caves / Inferno run time, counting up.
	 */
	private Entry tzhaarEntry()
	{
		if (!config.otherTzhaar())
		{
			return null;
		}
		Map.Entry<TimerType, Long> elapsed = timers.tzhaarElapsed();
		if (elapsed == null)
		{
			return null;
		}
		long secs = elapsed.getValue();
		String time = secs >= 3600
			? String.format("%d:%02d:%02d", secs / 3600, (secs / 60) % 60, secs % 60)
			: String.format("%d:%02d", secs / 60, secs % 60);
		return new Entry(icon(elapsed.getKey()), elapsed.getKey().getDisplayName(), time, Colors.GOOD, 1f, -1);
	}

	private static String formatCount(int n)
	{
		if (n >= 10_000_000)
		{
			return (n / 1_000_000) + "M";
		}
		if (n >= 100_000)
		{
			return (n / 1000) + "k";
		}
		return String.format("%,d", n);
	}

	// ---------------------------------------------------------------- preview samples

	private List<Entry> potionSamples()
	{
		List<Entry> list = new ArrayList<>();
		// Overload: show the red pre-refresh warning on a short cycle so you can see it
		boolean red = config.refreshWarning() && (System.currentTimeMillis() / 600) % 25 >= 23;
		list.add(new Entry(itemManager.getImage(ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4), "Overload", "1:18",
			red ? Colors.WARN : Colors.GOOD, 1f, 0.26));
		float fade = config.fadePrayerRegen() ? Colors.pulse() : 1f;
		list.add(new Entry(itemManager.getImage(ItemID._4DOSE1PRAYER_REGENERATION), "Prayer regen", "0:24",
			Colors.LOW, fade, 0.12));
		list.add(new Entry(itemManager.getImage(ItemID._4DOSEDIVINERANGE), "Divine ranging", "3:40", Colors.GOOD, 1f, 0.73));
		return list;
	}

	private List<Entry> otherSamples()
	{
		List<Entry> list = new ArrayList<>();
		list.add(new Entry(itemManager.getImage(ItemID._4DOSESTAMINA), "Stamina", "1:45", Colors.GOOD, 1f, 0.9));
		list.add(new Entry(itemManager.getImage(ItemID._4DOSE3ANTIDRAGON), "Super antifire", "2:40", Colors.GOOD, 1f, 0.8));
		list.add(new Entry(icon(TimerType.THRALL), "Thrall", "0:42", Colors.GOOD, 1f, 0.7));
		list.add(new Entry(icon(TimerType.DEATH_CHARGE), "Death charge", "0:58", Colors.GOOD, 1f, 0.95));
		list.add(new Entry(icon(TimerType.VENGEANCE_ACTIVE), "Vengeance", "Active", Colors.GOOD, 1f, -1));
		list.add(new Entry(icon(TimerType.CANNON), "Cannon (W" + Math.max(1, client.getWorld()) + ")", "18:32", Colors.GOOD, 1f, 0.74));
		list.add(new Entry(icon(TimerType.ICE_BARRAGE), "Ice barrage", "0:12", Colors.LOW, 1f, 0.6));
		list.add(new Entry(icon(TimerType.HOME_TELEPORT), "Home teleport", "24:10", Colors.GOOD, 1f, 0.8));
		list.add(new Entry(icon(TimerType.COLOSSEUM_DOOM), "Doom", "3", Colors.GOOD, 1f, -1));
		list.add(new Entry(itemManager.getImage(ItemID.DRAGON_ARROW), "Dragon arrow",
			formatCount(config.otherAmmoLow() > 0 ? config.otherAmmoLow() : 87), config.otherAmmoLow() > 0 ? Colors.WARN : Colors.GOOD, 1f, -1));
		return list;
	}

	private List<Entry> build(Predicate<TimerType> include)
	{
		Map<TimerType, TimerTracker.ActiveTimer> active = timers.snapshot();
		List<Entry> entries = new ArrayList<>();
		for (Map.Entry<TimerType, TimerTracker.ActiveTimer> e : active.entrySet())
		{
			TimerType type = e.getKey();
			if (!include.test(type) || !shown(type) || coveredByCombinedDivine(type, active))
			{
				continue;
			}

			TimerTracker.ActiveTimer t = e.getValue();
			String name = t.suffix == null ? type.getDisplayName() : type.getDisplayName() + " (" + t.suffix + ")";
			if (TimerType.COUNTERS.contains(type))
			{
				// Stacks / damage: a number. Curse of the Moons (Blue Moon) goes red at 18
				Color c = type == TimerType.CURSE_OF_THE_MOONS_BLUE && t.count >= 18 ? Colors.WARN : Colors.GOOD;
				entries.add(new Entry(icon(type), name, String.valueOf(t.count), c, 1f, -1));
				continue;
			}
			if (t.endTick >= TimerTracker.INDICATOR)
			{
				// On/off effects with no countdown (Vengeance waiting to rebound)
				entries.add(new Entry(icon(type), type.getDisplayName(), "Active", Colors.GOOD, 1f, -1));
				continue;
			}
			double secondsLeft = timers.secondsLeft(t);
			if (config.showMode() == ShowMode.WHEN_LOW && secondsLeft > config.timersThreshold())
			{
				continue;
			}

			Color color = secondsLeft <= config.fadeThreshold() ? Colors.LOW : Colors.GOOD;
			if (type.isRefreshing() && config.refreshWarning())
			{
				int toRefresh = timers.ticksToRefresh(t);
				if (toRefresh > 0 && toRefresh <= config.refreshWarningTicks())
				{
					color = Colors.WARN;
				}
			}

			float alpha = fades(type) && secondsLeft <= config.fadeThreshold() ? Colors.pulse() : 1f;
			entries.add(new Entry(icon(type), name, Colors.formatTime(secondsLeft), color, alpha,
				timers.fractionLeft(t)));
		}
		return entries;
	}

	private BufferedImage icon(TimerType type)
	{
		if (type.getItemId() >= 0)
		{
			return itemManager.getImage(type.getItemId());
		}
		int id = type.getSpriteId();
		BufferedImage img = sprites.get(id);
		if (img == null)
		{
			spriteManager.getSpriteAsync(id, 0, loaded -> sprites.put(id, loaded));
		}
		return img;
	}

	private boolean shown(TimerType type)
	{
		switch (type.getGroup())
		{
			case OVERLOAD:
				return config.showOverload();
			case SALTS:
				return config.showSalts();
			case PRAYER_REGEN:
				return config.showPrayerRegen();
			case DIVINE:
				return config.showDivine();
			default:
				switch (type.getOtherKey())
				{
					case "stamina":
						return config.otherStamina();
					case "antifire":
						return config.otherAntifire();
					case "antipoison":
						return config.otherAntipoison();
					case "heart":
						return config.otherHeart();
					case "vengeance":
						return config.otherVengeance();
					case "spells":
						return config.otherSpells();
					case "potions":
						return config.otherPotions();
					case "thralls":
						return config.otherThralls();
					case "arceuus":
						return config.otherArceuus();
					case "cannon":
						return config.otherCannon();
					case "freezes":
						return config.otherFreezes();
					case "teleports":
						return config.otherTeleports();
					case "lunar":
						return config.otherLunar();
					case "toa":
						return config.otherToa();
					case "bosses":
						return config.otherBosses();
					case "bossBuffs":
						return config.otherBossBuffs();
					case "burn":
						return config.otherBurn();
					case "burnNext":
						return config.otherBurnNext();
					case "skilling":
						return config.otherSkilling();
					case "tzhaar":
						return config.otherTzhaar();
					default:
						return config.otherMisc();
				}
		}
	}

	private boolean fades(TimerType type)
	{
		switch (type.getGroup())
		{
			case OVERLOAD:
				return config.fadeOverload();
			case SALTS:
				return config.fadeSalts();
			case PRAYER_REGEN:
				return config.fadePrayerRegen();
			case DIVINE:
				return config.fadeDivine();
			default:
				return false;
		}
	}

	/**
	 * A divine super combat also sets the single-stat divine timers; only show the combined one.
	 */
	private static boolean coveredByCombinedDivine(TimerType type, Map<TimerType, TimerTracker.ActiveTimer> active)
	{
		if (type.getGroup() != TimerType.Group.DIVINE || type.getHeldSkills().size() != 1)
		{
			return false;
		}

		TimerTracker.ActiveTimer self = active.get(type);
		// A moonlight potion also sets the divine defence timer: show only the moonlight potion
		TimerTracker.ActiveTimer moonlight = active.get(TimerType.MOONLIGHT_POTION);
		if (type == TimerType.DIVINE_DEFENCE && moonlight != null && moonlight.endTick >= self.endTick - 1)
		{
			return true;
		}
		for (TimerType combined : new TimerType[]{TimerType.DIVINE_COMBAT, TimerType.DIVINE_BASTION, TimerType.DIVINE_BATTLEMAGE})
		{
			TimerTracker.ActiveTimer c = active.get(combined);
			if (c != null && combined.getHeldSkills().containsAll(type.getHeldSkills()) && c.endTick >= self.endTick)
			{
				return true;
			}
		}
		return false;
	}
}
