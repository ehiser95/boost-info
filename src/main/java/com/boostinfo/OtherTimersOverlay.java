package com.boostinfo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The other Timers and Buffs timers (stamina, antifire, vengeance...) in their own list, unless attached elsewhere.
 */
public class OtherTimersOverlay extends OverlayPanel
{
	private final TimerEntries entries;
	private final BoostInfoConfig config;
	private final CombatTracker combat;

	@Inject
	OtherTimersOverlay(TimerEntries entries, BoostInfoConfig config, CombatTracker combat)
	{
		this.combat = combat;
		this.entries = entries;
		this.config = config;
		setPosition(OverlayPosition.TOP_RIGHT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.otherPlacement() != OtherPlacement.OWN_LIST)
		{
			return null;
		}
		if (!config.preview() && config.showMode() == ShowMode.IN_COMBAT && !combat.inCombat())
		{
			return null;
		}
		List<Entry> list = entries.otherTimers();
		if (list.isEmpty())
		{
			return null;
		}
		Layouts.fill(panelComponent, config, config.otherLayout().resolve(config.layout()),
			config.otherOrientation().resolve(config.orientation()), list, true);
		return super.render(graphics);
	}
}
