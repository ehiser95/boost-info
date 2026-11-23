package com.boostinfo;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * One stat boost or timer, ready to be drawn in any layout.
 */
class Entry
{
	final BufferedImage icon;
	final String name;
	final String value;
	final Color color;
	final float alpha;
	/**
	 * Fraction of time left (0..1) for the tiles' time bar, or -1 for no bar.
	 */
	final double fraction;
	/** Colour of the name (compact list), or null for the default. */
	Color nameColor;
	/** Make the number flash. */
	boolean flashValue;

	Entry(BufferedImage icon, String name, String value, Color color, float alpha, double fraction)
	{
		this.icon = icon;
		this.name = name;
		this.value = value;
		this.color = color;
		this.alpha = alpha;
		this.fraction = fraction;
	}

	/**
	 * The number's colour this frame (fades in and out when flashing).
	 */
	Color valueColor()
	{
		if (!flashValue)
		{
			return color;
		}
		int a = Math.round(255 * Colors.pulse());
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(40, Math.min(255, a)));
	}
}
