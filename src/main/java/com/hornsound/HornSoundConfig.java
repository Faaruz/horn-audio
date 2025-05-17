package com.hornsound;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;


@ConfigGroup("Horn Sound")
public interface HornSoundConfig extends Config
{
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Set the horn volume (0% to 100%)",
			position = 1
	)
	@Range(
			min = 0,
			max = 100
	)
	default int volume()
	{
		return 100; // default to full volume
	}

	@ConfigItem(
			keyName = "disableEncouragedByOthers",
			name = "Disable other players horn",
			description = "Don't play sound when others encourage you with the horn",
			position = 2
	)
	default boolean disableEncouragedByOthers()
	{
		return false;
	}

}
