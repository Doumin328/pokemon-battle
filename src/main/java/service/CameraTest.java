package service;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class CameraTest {

    static {
        System.loadLibrary("opencv_java490");
    }

    public static void main(String[] args) {

        System.out.println("=== Device List ===");

        // デバイス列挙 & そのまま動作チェック
        for (int i = 0; i < 5; i++) {
            VideoCapture cap = new VideoCapture(i);
            if (cap.isOpened()) {
                System.out.println("Found device at index: " + i);

                Mat frame = new Mat();
                if (cap.read(frame)) {
                    System.out.println(" → Device " + i + " WORKS!");
                    System.out.println(" → Resolution: " + frame.width() + "x" + frame.height());
                } else {
                    System.out.println(" → Device " + i + " opened but no frame.");
                }

                cap.release();
            } else {
                System.out.println("No device at index: " + i);
            }
        }

    }
}
