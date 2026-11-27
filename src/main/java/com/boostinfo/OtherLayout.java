package com.boostinfo;

import lombok.RequiredArgsConstructor;

/**
 * Layout for the "Other timers" list: the same as the stat boosts, or its own.
 */
@RequiredArgsConstructor
public enum OtherLayout
{
	SAME("Same as Layout", null),
	CHIPS("Chips", LayoutStyle.CHIPS),
	TILES("Tiles with a time bar", LayoutStyle.TILES),
	LIST("Compact list", LayoutStyle.LIST),
	BADGES("Compact: icon badges", LayoutStyle.BADGES),
	MINI_CHIPS("Compact: mini chips", LayoutStyle.MINI_CHIPS),
	STRIP("Compact: strip", LayoutStyle.STRIP),
	;

	private final String name;
	private final LayoutStyle style;

	LayoutStyle resolve(LayoutStyle main)
	{
		return style == null ? main : style;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
