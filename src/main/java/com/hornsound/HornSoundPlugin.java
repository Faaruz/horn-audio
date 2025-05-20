package com.hornsound;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@PluginDescriptor(
		name = "Horn Sound"
)
public class HornSoundPlugin extends Plugin
{
	private static final File PLUGIN_DIR = new File(RuneLite.RUNELITE_DIR, "horn-sound");
	private static final String HORN_FILE_NAME = "horn.wav";

	@Inject
	private Client client;

	@Inject
	private HornSoundConfig config;

	private Clip clip;

	@Override
	protected void startUp() throws Exception
	{
		try
		{
			if (!PLUGIN_DIR.exists() && !PLUGIN_DIR.mkdirs())
			{
				log.warn("Failed to create plugin directory: {}", PLUGIN_DIR.getAbsolutePath());
				return;
			}

			File hornFile = new File(PLUGIN_DIR, HORN_FILE_NAME);

			if (!hornFile.exists())
			{
				copyDefaultHornToFile(hornFile);
			}

			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(hornFile);
			clip = AudioSystem.getClip();
			clip.open(audioInputStream);
		}
		catch (Exception e)
		{
			log.warn("Failed to load horn sound", e);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		if (clip != null)
		{
			if (clip.isRunning())
			{
				clip.stop();
			}
			clip.close();
			clip = null;
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
			playSound();
		}
		else if (message.contains("encourages you with") && !config.disableEncouragedByOthers())
		{
			playSound();
		}
	}

	private void playSound()
	{
		if (clip == null)
		{
			return;
		}

		if (clip.isRunning())
		{
			clip.stop();
		}

		clip.setFramePosition(0);

		try
		{
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float volume = config.volume();

			final float minDb = -20.0f;
			final float maxDb = 6.0f;

			float dB = (volume == 0)
					? gainControl.getMinimum()
					: minDb + (volume / 100.0f) * (maxDb - minDb);

			gainControl.setValue(dB);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Volume control not supported", e);
		}

		clip.start();
	}

	private void copyDefaultHornToFile(File destinationFile)
	{
		try (InputStream in = getClass().getResourceAsStream("/horn.wav"))
		{
			if (in == null)
			{
				log.warn("Default horn.wav resource not found!");
				return;
			}

			Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			log.info("Copied default horn.wav to {}", destinationFile.getAbsolutePath());
		}
		catch (IOException e)
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
