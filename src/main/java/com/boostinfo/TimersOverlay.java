package com.boostinfo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Potion timers (plus the other timers, if you attach them here). Default spot: top right, under the minimap.
 */
public class TimersOverlay extends OverlayPanel
{
	private final TimerEntries entries;
	private final BoostInfoConfig config;
	private final CombatTracker combat;

	@Inject
	TimersOverlay(TimerEntries entries, BoostInfoConfig config, CombatTracker combat)
	{
		this.combat = combat;
		this.entries = entries;
		this.config = config;
		setPosition(OverlayPosition.TOP_RIGHT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.preview() && config.showMode() == ShowMode.IN_COMBAT && !combat.inCombat())
		{
			return null;
		}
		List<Entry> list = entries.potionTimers();
		if (config.otherPlacement() == OtherPlacement.WITH_POTION_TIMERS)
		{
			list.addAll(entries.otherTimers());
		}
		if (list.isEmpty())
		{
			return null;
		}
		Layouts.fill(panelComponent, config, config.layout(), config.timersOrientation().resolve(config.orientation()), list, true);
		return super.render(graphics);
	}
}
