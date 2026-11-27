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
	private static final int MINI_ICON = 12;
	private static final int MINI_PAD = 3;
	static final Color BG = new Color(42, 36, 29);

	private final Entry entry;
	private final Color bg;
	/** Compact layout B: smaller font, icon and padding. */
	private final boolean mini;

	@Setter
	private Point preferredLocation = new Point();
	@Setter
	private Dimension preferredSize = new Dimension();
	@Getter
	private final Rectangle bounds = new Rectangle();

	Chip(Entry entry, Color bg)
	{
		this(entry, bg, false);
	}

	Chip(Entry entry, Color bg, boolean mini)
	{
		this.entry = entry;
		this.bg = bg;
		this.mini = mini;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		java.awt.Font oldFont = g.getFont();
		if (mini)
		{
			g.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());
		}
		int iconSize = mini ? MINI_ICON : ICON;
		int pad = mini ? MINI_PAD : PAD;
		FontMetrics fm = g.getFontMetrics();
		int height = Math.max(iconSize, fm.getHeight()) + (mini ? 2 : 4);
		int valueWidth = fm.stringWidth(entry.value);
		int width = pad + (entry.icon != null ? iconSize + 3 : 0) + valueWidth + pad + (mini ? 1 : 0);
		int x = preferredLocation.x;
		int y = preferredLocation.y;

		Composite old = g.getComposite();
		if (entry.alpha < 1f)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entry.alpha));
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(bg);
		g.fillRoundRect(x, y, width, height, height, height);

		int textX = x + pad;
		if (entry.icon != null)
		{
			Icons.draw(g, entry.icon, textX, y + (height - iconSize) / 2, iconSize);
			textX += iconSize + 3;
		}

		TextComponent value = new TextComponent();
		value.setText(entry.value);
		value.setColor(entry.valueColor());
		value.setPosition(new Point(textX, y + (height - fm.getHeight()) / 2 + fm.getAscent()));
		value.render(g);

		g.setFont(oldFont);
		g.setComposite(old);

		Dimension d = new Dimension(width, height);
		bounds.setLocation(preferredLocation);
		bounds.setSize(d);
		return d;
	}
}
