package com.boostinfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OtherPlacement
{
	OWN_LIST("Their own list"),
	WITH_POTION_TIMERS("With potion timers"),
	WITH_STAT_BOOSTS("With stat boosts");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
