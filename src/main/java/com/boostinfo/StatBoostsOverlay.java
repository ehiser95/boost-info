package com.boostinfo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Compact list of boosted (and drained) stats. Default spot: bottom right, above the chatbox, next to the inventory.
 */
public class StatBoostsOverlay extends OverlayPanel
{
	private static final Set<Skill> COMBAT = ImmutableSet.of(
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER);

	private static final Set<Skill> NON_COMBAT = ImmutableSet.of(
		Skill.MINING, Skill.AGILITY, Skill.SMITHING, Skill.HERBLORE, Skill.FISHING, Skill.THIEVING,
		Skill.COOKING, Skill.CRAFTING, Skill.FIREMAKING, Skill.FLETCHING, Skill.WOODCUTTING, Skill.RUNECRAFT,
		Skill.SLAYER, Skill.FARMING, Skill.CONSTRUCTION, Skill.HUNTER, Skill.SAILING);

	private final Client client;
	private final BoostInfoConfig config;
	private final SkillIconManager skillIconManager;
	private final StatDecayTracker decay;
	private final TimerTracker timers;
	private final PotionReminder reminder;
	private final TimerEntries timerEntries;

	@Inject
	StatBoostsOverlay(Client client, BoostInfoConfig config, SkillIconManager skillIconManager,
		StatDecayTracker decay, TimerTracker timers, PotionReminder reminder, TimerEntries timerEntries)
	{
		this.reminder = reminder;
		this.timerEntries = timerEntries;
		this.client = client;
		this.config = config;
		this.skillIconManager = skillIconManager;
		this.decay = decay;
		this.timers = timers;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	private String skillName(Skill skill)
	{
		if (!config.shortNames())
		{
			return skill.getName();
		}
		switch (skill)
		{
			case ATTACK:
				return "Atk";
			case STRENGTH:
				return "Str";
			case DEFENCE:
				return "Def";
			case RANGED:
				return "Rng";
			case MAGIC:
				return "Mg";
			case PRAYER:
				return "Pray";
			default:
				return skill.getName();
		}
	}

	private Color nameColour(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return config.attackColour();
			case STRENGTH:
				return config.strengthColour();
			case DEFENCE:
				return config.defenceColour();
			case RANGED:
				return config.rangedColour();
			case MAGIC:
				return config.magicColour();
			default:
				return null;
		}
	}

	/**
	 * Sample boosts for the preview, coloured exactly like real ones would be.
	 */
	private List<Entry> previewEntries()
	{
		List<Entry> entries = new ArrayList<>();
		// Values picked to show each colour level: healthy, level 1, level 2, level 3, and a reminder / drain
		int[][] samples = {
			{Skill.ATTACK.ordinal(), 17}, {Skill.STRENGTH.ordinal(), Math.max(1, config.tier1Boost())},
			{Skill.DEFENCE.ordinal(), Math.max(1, config.tier2Boost())}, {Skill.RANGED.ordinal(), Math.max(1, config.tier3Boost())},
		};
		for (int[] sample : samples)
		{
			Skill skill = Skill.values()[sample[0]];
			int boost = sample[1];
			Color color = Colors.GOOD;
			boolean flash = false;
			if (config.tier1Enabled() && boost <= config.tier1Boost())
			{
				color = config.tier1Colour();
				flash = config.tier1Flash();
			}
			if (config.tier2Enabled() && boost <= config.tier2Boost())
			{
				color = config.tier2Colour();
				flash = config.tier2Flash();
			}
			if (config.tier3Enabled() && boost <= config.tier3Boost())
			{
				color = config.tier3Colour();
				flash = config.tier3Flash();
			}
			entries.add(sampleEntry(skill, boost, color, flash));
		}
		// Magic: below your reminder minimum (red, flashing) if the reminder is on, otherwise a drained stat
		boolean reminder = config.reminderEnabled() && config.reminderMagic() > 0;
		if (reminder || config.showDrains())
		{
			entries.add(sampleEntry(Skill.MAGIC, reminder ? 0 : -4, Colors.WARN, reminder));
		}
		return entries;
	}

	private Entry sampleEntry(Skill skill, int boost, Color color, boolean flash)
	{
		Entry e = new Entry(skillIconManager.getSkillImage(skill, true), skillName(skill),
			(boost >= 0 ? "+" : "") + boost, color, 1f, -1);
		e.flashValue = flash;
		e.nameColor = nameColour(skill);
		return e;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.preview())
		{
			List<Entry> entries = previewEntries();
			if (config.otherPlacement() == OtherPlacement.WITH_STAT_BOOSTS)
			{
				entries.addAll(timerEntries.otherTimers());
			}
			Layouts.fill(panelComponent, config.layout(), config.orientation(), entries, false);
			return super.render(graphics);
		}

		Map<TimerType, TimerTracker.ActiveTimer> active = timers.snapshot();
		List<Entry> entries = new ArrayList<>();

		for (Skill skill : Skill.values())
		{
			// Prayer only counts when boosted above its base level (low prayer points are not a drain)
			boolean combat = COMBAT.contains(skill);
			if (!combat && !(config.showNonCombat() && NON_COMBAT.contains(skill)))
			{
				continue;
			}

			int boost = client.getBoostedSkillLevel(skill) - client.getRealSkillLevel(skill);
			boolean needsPotion = reminder.below(skill);
			if ((boost == 0 && !needsPotion) || (skill == Skill.PRAYER && boost < 0))
			{
				continue;
			}

			Color color;
			boolean flash = false;
			if (boost < 0 && !needsPotion)
			{
				if (!config.showDrains())
				{
					continue;
				}
				color = Colors.WARN;
			}
			else if (boost <= 0)
			{
				// Below your minimum with no boost at all: red and flashing
				color = Colors.WARN;
				flash = true;
			}
			else
			{
				// Time until this boost is gone: held by an active potion timer, else natural decay
				TimerType holder = null;
				double held = -1;
				for (Map.Entry<TimerType, TimerTracker.ActiveTimer> e : active.entrySet())
				{
					if (e.getKey().getHeldSkills().contains(skill))
					{
						double s = timers.secondsLeft(e.getValue());
						if (s > held)
						{
							held = s;
							holder = e.getKey();
						}
					}
				}

				int decayTicks = decay.ticksUntilExpired(boost);
				double secondsLeft = Math.max(held, decayTicks < 0 ? -1 : decayTicks * 0.6);

				if (config.statsDisplayMode() == DisplayMode.WHEN_LOW
					&& (secondsLeft < 0 || secondsLeft > config.statsThreshold()))
				{
					continue;
				}

				color = holder == null && secondsLeft >= 0 && secondsLeft <= config.statsThreshold()
					? Colors.LOW : Colors.GOOD;

				// Your colour levels: the lowest one the boost has reached wins
				if (config.tier1Enabled() && boost <= config.tier1Boost())
				{
					color = config.tier1Colour();
					flash = config.tier1Flash();
				}
				if (config.tier2Enabled() && boost <= config.tier2Boost())
				{
					color = config.tier2Colour();
					flash = config.tier2Flash();
				}
				if (config.tier3Enabled() && boost <= config.tier3Boost())
				{
					color = config.tier3Colour();
					flash = config.tier3Flash();
				}
				if (needsPotion)
				{
					color = Colors.WARN;
					flash = true;
				}

				if (holder != null && holder.isRefreshing() && config.refreshWarning() && config.refreshWarningOnStats())
				{
					int toRefresh = timers.ticksToRefresh(active.get(holder));
					if (toRefresh > 0 && toRefresh <= config.refreshWarningTicks())
					{
						color = Colors.WARN;
					}
				}
			}

			Entry entry = new Entry(skillIconManager.getSkillImage(skill, true), skillName(skill),
				(boost >= 0 ? "+" : "") + boost, color, 1f, -1);
			entry.flashValue = flash;
			entry.nameColor = nameColour(skill);
			entries.add(entry);
		}

		if (config.otherPlacement() == OtherPlacement.WITH_STAT_BOOSTS)
		{
			entries.addAll(timerEntries.otherTimers());
		}
		if (entries.isEmpty())
		{
			return null;
		}
		Layouts.fill(panelComponent, config.layout(), config.orientation(), entries, false);
		return super.render(graphics);
	}
}
