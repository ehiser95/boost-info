package com.boostinfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Layout A: a rounded pill with the icon beside the value.
 */
class Chip implements LayoutableRenderableEntity
{
	private static final int ICON = 14;
	private static final int PAD = 5;
	private static final Color BG = new Color(42, 36, 29, 230);

	private final Entry entry;

	@Setter
	private Point preferredLocation = new Point();
	@Setter
	private Dimension preferredSize = new Dimension();
	@Getter
	private final Rectangle bounds = new Rectangle();

	Chip(Entry entry)
	{
		this.entry = entry;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		FontMetrics fm = g.getFontMetrics();
		int height = Math.max(ICON, fm.getHeight()) + 4;
		int valueWidth = fm.stringWidth(entry.value);
		int width = PAD + (entry.icon != null ? ICON + 3 : 0) + valueWidth + PAD;
		int x = preferredLocation.x;
		int y = preferredLocation.y;

		Composite old = g.getComposite();
		if (entry.alpha < 1f)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entry.alpha));
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(BG);
		g.fillRoundRect(x, y, width, height, height, height);

		int textX = x + PAD;
		if (entry.icon != null)
		{
			Icons.draw(g, entry.icon, textX, y + (height - ICON) / 2, ICON);
			textX += ICON + 3;
		}

		TextComponent value = new TextComponent();
		value.setText(entry.value);
		value.setColor(entry.valueColor());
		value.setPosition(new Point(textX, y + (height - fm.getHeight()) / 2 + fm.getAscent()));
		value.render(g);

		g.setComposite(old);

		Dimension d = new Dimension(width, height);
		bounds.setLocation(preferredLocation);
		bounds.setSize(d);
		return d;
	}
}
