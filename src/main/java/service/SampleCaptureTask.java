package service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.bytedeco.javacv.*;
import javafx.scene.image.ImageView;

import java.nio.ShortBuffer;

public class SampleCaptureTask  implements Runnable {

    private final ImageViewUpdater updater;

    public SampleCaptureTask(ImageView imageView) {
        this.updater = new ImageViewUpdater(imageView);
    }

    @Override
    public void run() {
        FFmpegFrameGrabber grabber = null;

        try {
            // ★キャプボデバイス名（Windowsでは directshow を使う）
            grabber = new FFmpegFrameGrabber("video=Your Capture Device Name");
            grabber.setFormat("dshow");  // Windows のキャプチャーデバイス形式
            grabber.start();

            System.out.println("Video size: " + grabber.getImageWidth() + "x" + grabber.getImageHeight());
            System.out.println("Audio: " + grabber.getAudioChannels() + " ch");

            while (true) {
                Frame frame = grabber.grab();

                if (frame == null) continue;

                // 映像フレームの場合
                if (frame.image != null) {
                    Image img = JavaCVUtils.toFXImage(frame);
                    updater.update(img);
                }

                // 音声フレームの場合
                if (frame.samples != null) {
                    ShortBuffer channelSamples = (ShortBuffer) frame.samples[0];
                    // ここでは再生しない（動作確認は映像のみ）
                    // → 後で AudioPlayer を作成して同期再生可能
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (Exception ignored) {}
            }
        }
    }
}

