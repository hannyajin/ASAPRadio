package com.heartpirates.harthsip;

import net.sf.asap.ASAP;
import net.sf.asap.ASAPInfo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.files.FileHandle;

/**
 * Simple ASAP Radio for libGDX (Desktop and Android only)
 * 
 * Requires asap.jar -> @link http://asap.sourceforge.net/
 * license WTFPL
 * @author hannyajin
 */

public class AsapRadio {
	private ASAP asap = new ASAP();
	private AudioDevice device = null;
	private int song = 0;
	private String nowPlaying = "";

	String filePath = "data/mus/X_Ray_2.sap";

	enum AudioState {
		PLAY, STOP
	}

	private AudioState audioState = AudioState.STOP;

	public AsapRadio() {
		this.device = Gdx.audio.newAudioDevice(44100, false);
	}

	public AsapRadio(AudioDevice device) {
		this.device = device;
	}

	public AsapRadio init() {
		loadMusic("data/mus/Jatatap.sap");
		return this;
	}

	public void dispose() {
		this.device.dispose();
	}

	public void start() {
		final ASAP asap = this.asap;
		final AudioDevice device = this.device;

		Thread playbackThread = new Thread(new Runnable() {

			@Override
			public void run() {
				byte[] bytes = new byte[1024 << 2];

				int numSamples;

				while ((numSamples = asap.generate(bytes, bytes.length, 1)) > 0) {
					short[] shortPCM = getShortPCM(bytes, numSamples);
					device.writeSamples(shortPCM, 0, shortPCM.length);

					try {
						Thread.sleep(14);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		playbackThread.setPriority(Thread.NORM_PRIORITY);
		playbackThread.start();
	}

	public void tick() {
		if (device == null)
			return;

		switch (audioState) {
		case PLAY:
			statePlay();
			break;
		case STOP:
		default:
			stateStop();
			break;
		}
	}

	private short[] getShortPCM(byte[] bytes, int numSamples) {
		short[] pcm = new short[numSamples >> 1];
		for (int i = 0; i < pcm.length; i++) {
			int p = i << 1;
			int a = bytes[p] & 0xFF;
			int b = bytes[p + 1] & 0xFF;
			int full = a | b << 8;
			pcm[i] = (short) full;
		}
		return pcm;
	}

	private int statePlay() {
		byte[] bytes = new byte[1024 << 2];

		int numSamples;
		numSamples = this.asap.generate(bytes, bytes.length, 1);

		short[] shortPCM = getShortPCM(bytes, numSamples);
		this.device.writeSamples(shortPCM, 0, shortPCM.length);
		return this.device.getLatency();
	}

	private void stateStop() {
	}

	public void play() {
		Gdx.app.debug("Radio", "Play.");
		this.audioState = AudioState.PLAY;
	}

	public void stop() {
		Gdx.app.debug("Radio", "Stop.");
		this.audioState = AudioState.STOP;
	}

	private void loadMusic(String path) {
		FileHandle handle = Gdx.files.internal(path);

		if (!handle.exists()) {
			Gdx.app.debug("Radio", "Music file not found.");
		} else {
			Gdx.app.debug("Radio", "Music loaded.");
			byte[] bytes = handle.readBytes();
			loadAsapMusic(path, bytes, bytes.length);
			this.nowPlaying = path;
		}
	}

	private void loadAsapMusic(String name, byte[] bytes, int length) {
		ASAPInfo info;
		try {
			this.asap.load(name, bytes, length);
			info = this.asap.getInfo();
			this.asap.playSong(song,
					info.getLoop(song) ? -1 : info.getDuration(song));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setVolume(float percent) {
		if (this.device == null)
			return;

		this.device.setVolume(percent);
	}
}