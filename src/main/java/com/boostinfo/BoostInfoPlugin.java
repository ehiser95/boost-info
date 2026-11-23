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
	description = "A clean list of stat boosts and potion timers, with refresh warnings and fading",
	tags = {"boosts", "overload", "salts", "prayer", "divine", "timers", "potion"}
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

	@Override
	protected void startUp()
	{
		eventBus.register(timerTracker);
		eventBus.register(statDecayTracker);
		eventBus.register(potionReminder);
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
		eventBus.unregister(timerTracker);
		eventBus.unregister(statDecayTracker);
		timerTracker.clear();
		statDecayTracker.reset();
	}

	@Provides
	BoostInfoConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BoostInfoConfig.class);
	}
}
