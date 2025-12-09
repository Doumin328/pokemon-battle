package service;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple audio capture service using Java Sound API.
 * - Can list available mixers
 * - Start capturing from a selected mixer (by index) and play back to system output
 */
public class AudioCaptureService {

    private TargetDataLine targetLine;
    private SourceDataLine sourceLine;
    private Thread captureThread;
    private AtomicBoolean running = new AtomicBoolean(false);

    public List<String> getAudioDeviceList() {
        List<String> devices = new ArrayList<>();
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : infos) {
            devices.add(info.getName() + " - " + info.getDescription());
        }
        return devices;
    }

    public void startCapture(int deviceIndex) {
        stopCapture();

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        if (mixers.length == 0) return;

        int idx = Math.max(0, Math.min(deviceIndex, mixers.length - 1));
        Mixer.Info selected = mixers[idx];

        try {
            Mixer mixer = AudioSystem.getMixer(selected);

            // PCM format - common values; adjust if your device uses different format
            AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);

            DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
            if (!mixer.isLineSupported(targetInfo)) {
                // try default mixer
                targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
            } else {
                targetLine = (TargetDataLine) mixer.getLine(targetInfo);
            }

            targetLine.open(format);
            targetLine.start();

            // Playback to default system output
            DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format);
            sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
            sourceLine.open(format);
            sourceLine.start();

            running.set(true);

            captureThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (running.get()) {
                    int read = targetLine.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        sourceLine.write(buffer, 0, read);
                    }
                }
            }, "Audio-Capture-Thread");
            captureThread.setDaemon(true);
            captureThread.start();

        } catch (Exception e) {
            e.printStackTrace();
            stopCapture();
        }
    }

    public void stopCapture() {
        running.set(false);
        try {
            if (captureThread != null) captureThread.join(200);
        } catch (InterruptedException ignored) {
        }
        if (targetLine != null) {
            try { targetLine.stop(); } catch (Exception ignored) {}
            try { targetLine.close(); } catch (Exception ignored) {}
            targetLine = null;
        }
        if (sourceLine != null) {
            try { sourceLine.drain(); } catch (Exception ignored) {}
            try { sourceLine.stop(); } catch (Exception ignored) {}
            try { sourceLine.close(); } catch (Exception ignored) {}
            sourceLine = null;
        }
    }
}
