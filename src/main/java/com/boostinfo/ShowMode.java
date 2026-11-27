package com.boostinfo;

import lombok.RequiredArgsConstructor;

/**
 * When the stat boosts and timers are shown. One setting for every list.
 */
@RequiredArgsConstructor
public enum ShowMode
{
	ALWAYS("Always"),
	IN_COMBAT("In combat only"),
	WHEN_LOW("When time is running low"),
	;

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
