package com.boostinfo;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BoostInfoPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BoostInfoPlugin.class);
		RuneLite.main(args);
	}
}