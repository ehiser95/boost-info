package com.boostinfo;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Boost Info",
	description = "Stat boosts, potion timers and every Timers & Buffs timer in one clean, compact overlay",
	tags = {"boosts", "overload", "salts", "prayer", "divine", "timers", "buffs", "potion", "thrall", "cannon"}
)
public class BoostInfoPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TimerTracker timerTracker;

	@Inject
	private StatDecayTracker statDecayTracker;

	@Inject
	private StatBoostsOverlay statBoostsOverlay;

	@Inject
	private TimersOverlay timersOverlay;

	@Inject
	private OtherTimersOverlay otherTimersOverlay;

	@Inject
	private ReminderOverlay reminderOverlay;

	@Inject
	private PotionReminder potionReminder;

	@Inject
	private CombatTracker combatTracker;

	@Inject
	private ConfigManager configManager;

	@Override
	protected void startUp()
	{
		eventBus.register(timerTracker);
		eventBus.register(statDecayTracker);
		eventBus.register(potionReminder);
		eventBus.register(combatTracker);
		migrateShowSettings();
		overlayManager.add(statBoostsOverlay);
		overlayManager.add(timersOverlay);
		overlayManager.add(otherTimersOverlay);
		overlayManager.add(reminderOverlay);

		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				timerTracker.initFromVarbits();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(statBoostsOverlay);
		overlayManager.remove(timersOverlay);
		overlayManager.remove(otherTimersOverlay);
		overlayManager.remove(reminderOverlay);
		eventBus.unregister(potionReminder);
		eventBus.unregister(combatTracker);
		combatTracker.reset();
		eventBus.unregister(timerTracker);
		eventBus.unregister(statDecayTracker);
		timerTracker.clear();
		statDecayTracker.reset();
	}

	/**
	 * The separate "Show stat boosts" / "Show potion timers" settings became one Show setting:
	 * carry over "Only when time is low" if either was using it.
	 */
	private void migrateShowSettings()
	{
		String stats = configManager.getConfiguration(BoostInfoConfig.GROUP, "statsDisplayMode");
		String timers = configManager.getConfiguration(BoostInfoConfig.GROUP, "timersDisplayMode");
		if (stats == null && timers == null)
		{
			return;
		}
		if ("WHEN_LOW".equals(stats) || "WHEN_LOW".equals(timers))
		{
			configManager.setConfiguration(BoostInfoConfig.GROUP, "showMode", ShowMode.WHEN_LOW);
		}
		configManager.unsetConfiguration(BoostInfoConfig.GROUP, "statsDisplayMode");
		configManager.unsetConfiguration(BoostInfoConfig.GROUP, "timersDisplayMode");
	}

	@Provides
	BoostInfoConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BoostInfoConfig.class);
	}
}
