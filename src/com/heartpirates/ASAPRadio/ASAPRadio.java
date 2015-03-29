package com.heartpirates.ASAPRadio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import net.sf.asap.ASAP;
import net.sf.asap.ASAPInfo;

/**
 * Basic ASAP music player with javax.sound.
 * 
 * @author hannyajin
 * @link http://asap.sourceforge.net/
 */
public class ASAPRadio implements Runnable {

	public ASAP asap = new ASAP();
	SourceDataLine line = null;

	private Object lock = new Object();
	private Thread thread = null;
	
	private int trackLength = 0;

	private boolean playing = false;

	List<File> fileList = new ArrayList<File>();
	private float fVolume = .9f;

	public void loadDirectory(File dir) throws IOException {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			throw new IOException("Directory [" + dir.getPath()
					+ "] Not found.");
		}

		File[] files = dir.listFiles();
		for (File file : files) {
			this.fileList.add(file);
			System.out.println(file.getName());
		}

		System.out.println("Success.");
	}

	/**
	 * Sample program. Plays all music files in music/ directory for 8 seconds.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ASAPRadio radio = null;
		try {
			radio = new ASAPRadio();

			File dir = new File(ASAPRadio.class.getClassLoader().getResource(args.length < 1 ? "music" : args[0]).getPath());

			radio.loadDirectory(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}

		/**
		try {
			radio.play(5);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		/**/
		
		
		/**/
		for (int i = 0; i < radio.fileList.size(); i++) {
			try {
				System.out.println();
				radio.play(i);

				Thread.sleep(8000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/**/
	}

	/**
	 * 
	 * @param name
	 *            must include '.' separated file extension (music format)
	 * @param bytes
	 * @throws Exception
	 */
	public void play(String name, byte[] bytes) throws Exception {
		synchronized (lock) {
			this.asap.load(name, bytes, bytes.length);
			this.asap.playSong(0, -1); // 0 - seek value (start), -1 = looping

			trackLength = bytes.length;
			
			ASAPInfo info = asap.getInfo();

			if (line != null) {
				this.line.close();
			}

			this.line = ((SourceDataLine) AudioSystem
					.getLine(new DataLine.Info(SourceDataLine.class,
							new AudioFormat(44100.0f, 16, info.getChannels(),
									true, false))));
			this.line.open();
			setVolume(fVolume);
			this.line.start();

			if (thread == null) {
				thread = new Thread(this);
				thread.start();
			} else {
				// interrupt to prepare for new music
				thread.interrupt();
			}

			System.out.println("Playing song: " + name);
			playing = true;
		}

	}

	public void play(int num) throws Exception {
		if (num < 0)
			num = 0;
		if (num >= fileList.size())
			num = fileList.size() - 1;

		File file = fileList.get(num);
		InputStream is = file.toURI().toURL().openStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (is.available() > 0) {
			baos.write(is.read());
		}
		is.close();

		byte[] bytes = baos.toByteArray();

		play(file.getName(), bytes);
	}

	public void setVolume(float percent) {
		FloatControl fc = (FloatControl) this.line
				.getControl(FloatControl.Type.MASTER_GAIN);

		float delta = fc.getMaximum() - fc.getMinimum();
		float f = (float) (fc.getMaximum() - delta * (1 - percent));

		if (f < fc.getMinimum())
			f = fc.getMinimum();
		if (f > fc.getMaximum())
			f = fc.getMaximum();

		fVolume = percent;
		fc.setValue(f);
	}

	/**
	 * Stop and reset music position.
	 */
	public void stop() {
		playing = false;
		reset();
	}

	/**
	 * Reset music playback position.
	 */
	public void reset() {
		seek(0);
	}

	public void seek(int len) {
		if (len < 0)
			len = 0;
		synchronized (lock) {
			try {
				this.asap.seek(len);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Start music playback from previous position.
	 */
	public void start() {
		playing = true;
	}

	/**
	 * Pause music playback.
	 */
	public void pause() {
		playing = false;
	}

	/**
	 * Close ASAPRadio and free used memory
	 */
	public void close() {
		synchronized (lock) {
			if (this.line != null)
				this.line.close();
			isRunning = false;
			if (this.thread != null)
				thread.interrupt();
		}
	}

	private boolean isRunning = true;

	private int lastpp = 0;
	@Override
	public void run() {
		int count = 0;
		while (isRunning) {
			try {
				synchronized (lock) {
					if (playing) {
						byte[] bytes = new byte[1024 << 3];

						int len = this.asap.generate(bytes, bytes.length, 1);
						this.line.write(bytes, 0, len);
						count += len;

						
						if (false) {
							System.out.println("Replaying music.");
							try {
								this.asap.seek(0);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						int pp = this.asap.getPosition() / 1000;
						int tt = (trackLength * 32) / 1000;
						if (pp != lastpp) {
							System.out.print("\r                                 ");
							System.out.print("\rPos: " + pp + " / " + tt);
						}
						//System.out.println("Blocks: " + this.asap.getBlocksPlayed());
						lastpp = pp;
						int n = 0;
						this.asap.detectSilence(n);
						//System.out.println("n: " + n);
					}
				}

				Thread.sleep(10);
			} catch (InterruptedException e) {
				// ignore (useless and harmless).
			}
		}
	}
}
