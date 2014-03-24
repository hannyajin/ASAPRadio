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
 * Baisc ASAP music player with javax.sound.
 * 
 * @author hannyajin
 *
 */
public class ASAPRadio implements Runnable {

	public ASAP asap = new ASAP();
	SourceDataLine line = null;

	private Object lock = new Object();
	private Thread thread = null;

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

	public static void main(String[] args) {
		ASAPRadio radio = null;
		try {
			radio = new ASAPRadio();

			File dir = new File(ASAPRadio.class.getClassLoader()
					.getResource("music").getPath());

			radio.loadDirectory(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < radio.fileList.size(); i++) {
			try {
				radio.play(i);

				Thread.sleep(10000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void play(String name, byte[] bytes) throws Exception {
		synchronized (lock) {
			this.asap.load(name, bytes, bytes.length);
			this.asap.playSong(0, -1);

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

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (lock) {
					if (playing) {
						byte[] bytes = new byte[1024 << 3];

						int len = this.asap.generate(bytes, bytes.length, 1);
						this.line.write(bytes, 0, len);
					}
				}

				Thread.sleep(10);
			} catch (InterruptedException e) {
				// ignore (useless).
			}
		}
	}
}
