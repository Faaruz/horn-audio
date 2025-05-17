package com.hornsound;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "Horn Sound"
)
public class HornSoundPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private HornSoundConfig config;

	private Clip clip = null;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Horn Sound started!");

		try
		{
			File pluginDir = new File(System.getProperty("user.home"), ".runelite/horn-sound");
			File customHorn = new File(pluginDir, "horn.wav");

			if (!pluginDir.exists() && pluginDir.mkdirs())
			{
				log.info("Created horn-sound plugin directory at {}", pluginDir.getAbsolutePath());
			}

			// Optional: copy default horn if file doesn't exist
			if (!customHorn.exists())
			{
				copyDefaultHornToFolder(customHorn);
			}

			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(customHorn);
			clip = AudioSystem.getClip();
			clip.open(audioInputStream);

			log.info("Horn loaded from {}", customHorn.getAbsolutePath());
		}
		catch (Exception e)
		{
			log.warn("Failed to load horn sound", e);
		}
	}


	@Override
	protected void shutDown() throws Exception
	{
		if (clip != null && clip.isRunning())
		{
			clip.stop();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = event.getMessage();

		if (message.contains("You encourage nearby allies"))
		{
			log.info("Horn activated (you encouraged others) - playing sound!");
			playSound();
		}
		else if (message.contains("encourages you with") && !config.disableEncouragedByOthers())
		{
			log.info("Horn activated (you were encouraged) - playing sound!");
			playSound();
		}
	}

	private void playSound()
	{
		if (clip != null)
		{
			if (clip.isRunning())
			{
				clip.stop();
			}
			clip.setFramePosition(0);

			try
			{
				FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float volume = config.volume();

				float minDb = -20.0f;
				float maxDb = 6.0f;

				float dB = (volume == 0)
						? gainControl.getMinimum()
						: minDb + (volume / 100.0f) * (maxDb - minDb);

				gainControl.setValue(dB);
			}
			catch (IllegalArgumentException e)
			{
				log.warn("Volume control not supported on this system/clip", e);
			}

			clip.start();
		}
	}

	private void copyDefaultHornToFolder(File destinationFile)
	{
		try (var in = getClass().getResourceAsStream("/horn.wav"))
		{
			if (in == null)
			{
				log.warn("Default horn.wav resource not found!");
				return;
			}

			Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			log.info("Default horn.wav copied to {}", destinationFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			log.warn("Failed to copy default horn.wav to plugin folder", e);
		}
	}

	@Provides
	HornSoundConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HornSoundConfig.class);
	}
}
