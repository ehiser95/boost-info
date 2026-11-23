package com.boostinfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DisplayMode
{
	ALWAYS("Always"),
	WHEN_LOW("Only when time is low"),
	;

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
