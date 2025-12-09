package service;

import org.bytedeco.javacv.Frame;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioPlayerService {

    private SourceDataLine line;
    private AudioFormat format;
    private boolean initialized = false;

    /**
     * JavaCV の audio Frame を受け取り、リアルタイム再生
     */
    public void playAudioFrame(Frame audioFrame) {
        if (audioFrame == null || audioFrame.samples == null) return;

        // samples[0] は ShortBuffer のことが多い
        ShortBuffer sb = (ShortBuffer) audioFrame.samples[0];

        // ShortBuffer → short[]
        short[] samples = new short[sb.limit()];
        sb.get(samples);

        // 最初の一回だけ audioFormat を決める
        if (!initialized) {
            int sampleRate = audioFrame.sampleRate;
            int channels = audioFrame.audioChannels;

            format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false
            );

            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                initialized = true;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // short[] → byte[] に変換
        byte[] rawBytes = shortToBytes(samples);

        // 再生
        line.write(rawBytes, 0, rawBytes.length);
    }

    /**
     * short[] を little-endian の byte[] に変換
     */
    private byte[] shortToBytes(short[] samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : samples) {
            buffer.putShort(s);
        }
        return buffer.array();
    }

    /**
     * 音声停止
     */
    public void stop() {
        try {
            if (line != null) {
                line.flush();
                line.stop();
                line.close();
            }
        } catch (Exception ignored) {}
        initialized = false;
    }
}
