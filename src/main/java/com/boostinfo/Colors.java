package com.boostinfo;

import java.awt.Color;

final class Colors
{
	static final Color GOOD = new Color(95, 217, 122);
	static final Color LOW = new Color(255, 200, 87);
	static final Color WARN = new Color(255, 90, 79);
	static final Color TEXT = new Color(232, 226, 214);

	private Colors()
	{
	}

	/**
	 * Fade-in/out opacity: smooth pulse between 0.25 and 1 every 1.2 seconds.
	 */
	static float pulse()
	{
		double phase = (System.currentTimeMillis() % 1200) / 1200.0;
		return (float) (0.625 + 0.375 * Math.cos(phase * 2 * Math.PI));
	}

	static String formatTime(double seconds)
	{
		int s = (int) Math.ceil(seconds);
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}
}
