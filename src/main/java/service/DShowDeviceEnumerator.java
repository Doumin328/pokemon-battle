package service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DShowDeviceEnumerator {

    public static class DeviceInfo {
        public final String name;
        public final String type;

        public DeviceInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "[" + type + "] " + name;
        }
    }

    private static final String FFMPEG_COMMAND = "ffmpeg"; // PATH に通っている前提

    public static List<DeviceInfo> getDShowDevices() {
        List<DeviceInfo> devices = new ArrayList<>();

        try {
            // Try to find ffmpeg executable on PATH first (Windows uses "where")
            String ffmpegCmd = FFMPEG_COMMAND;
            try {
                ProcessBuilder wherePb = new ProcessBuilder("where", FFMPEG_COMMAND);
                wherePb.redirectErrorStream(true);
                Process whereProc = wherePb.start();
                try (BufferedReader wbr = new BufferedReader(new InputStreamReader(whereProc.getInputStream()))) {
                    String wline = wbr.readLine();
                    if (wline != null && !wline.isBlank()) {
                        ffmpegCmd = wline.trim();
                    }
                }
                whereProc.waitFor();
            } catch (Exception ignored) {
                // ignore - fallback to plain command
            }

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegCmd,
                    "-list_devices", "true",
                    "-f", "dshow",
                    "-i", "dummy"
            );
            // ffmpeg は stderr に情報を出す場合が多いので stderr を両方読む
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Read both streams fully into byte arrays
            byte[] stdoutBytes;
            byte[] stderrBytes;
            try (InputStream isOut = new BufferedInputStream(process.getInputStream());
                 InputStream isErr = new BufferedInputStream(process.getErrorStream())) {
                stdoutBytes = isOut.readAllBytes();
                stderrBytes = isErr.readAllBytes();
            }

            process.waitFor();

            // Combine stderr + stdout which ffmpeg sometimes uses interchangeably
            byte[] combined = new byte[stderrBytes.length + stdoutBytes.length];
            System.arraycopy(stderrBytes, 0, combined, 0, stderrBytes.length);
            System.arraycopy(stdoutBytes, 0, combined, stderrBytes.length, stdoutBytes.length);

            // Try decoding in multiple charsets to account for locale differences
            String outputText = null;
            Charset[] tryCharsets = new Charset[]{Charset.forName("UTF-8"), Charset.defaultCharset()};
            // On Windows, fallback to MS932 (Shift_JIS) which often appears for localized ffmpeg
            try {
                tryCharsets = new Charset[]{Charset.forName("UTF-8"), Charset.defaultCharset(), Charset.forName("MS932")};
            } catch (Exception ex) {
                // ignore if MS932 not available
                tryCharsets = new Charset[]{Charset.forName("UTF-8"), Charset.defaultCharset()};
            }

            for (Charset cs : tryCharsets) {
                try {
                    outputText = new String(combined, cs);
                    if (outputText != null && !outputText.isBlank()) break;
                } catch (Exception ignored) {
                }
            }

            if (outputText == null) outputText = "";

            // Now parse quoted strings robustly: "..."
            Pattern quotePattern = Pattern.compile("\\\"([^\\\"]+)\\\"");
            Matcher m = quotePattern.matcher(outputText);
            String currentType = null;

            // Also track lines to detect context (video/audio sections)
            String[] lines = outputText.split("\\r?\\n");
            for (String l : lines) {
                String t = l.trim();
                if (t.contains("DirectShow video devices") || t.toLowerCase().contains("video devices")) {
                    currentType = "Video";
                    continue;
                }
                if (t.contains("DirectShow audio devices") || t.toLowerCase().contains("audio devices")) {
                    currentType = "Audio";
                    continue;
                }

                // For this line, find quoted substrings
                Matcher mm = quotePattern.matcher(t);
                while (mm.find()) {
                    String name = mm.group(1).trim();
                    if (name.isEmpty() || name.startsWith("@")) continue;

                    String tail = t.substring(mm.end()).toLowerCase();
                    String type = currentType;
                    if (tail.contains("(video)") || tail.contains("video")) type = "Video";
                    else if (tail.contains("(audio)") || tail.contains("audio")) type = "Audio";

                    // If type is still null, try simple heuristics based on name
                    if (type == null) {
                        String nlow = name.toLowerCase();
                        if (nlow.contains("audio") || nlow.contains("mic") || nlow.contains("マイク") || nlow.contains("microphone")) {
                            type = "Audio";
                        } else {
                            type = "Video";
                        }
                    }

                    // Avoid duplicates (same name + type)
                    boolean dup = false;
                    for (DeviceInfo di : devices) {
                        if (di.name.equals(name) && ((di.type == null && type == null) || (di.type != null && di.type.equals(type)))) {
                            dup = true; break;
                        }
                    }
                    if (!dup) {
                        devices.add(new DeviceInfo(name, type));
                        System.out.println("[DShowDeviceEnumerator] parsed device: " + name + " -> " + type);
                    }
                }
            }

            // If still empty, dump raw output to a temp file for inspection
            if (devices.isEmpty()) {
                try {
                    Path tmp = Files.createTempFile("ffmpeg_dshow_output_", ".txt");
                    Files.writeString(tmp, outputText, Charset.defaultCharset());
                    System.out.println("[DShowDeviceEnumerator] No devices parsed; raw ffmpeg output written to: " + tmp.toAbsolutePath());
                    System.out.println("[DShowDeviceEnumerator] Raw output (first 2000 chars):\n" +
                            (outputText.length() > 2000 ? outputText.substring(0, 2000) : outputText));
                } catch (Exception ex) {
                    System.out.println("[DShowDeviceEnumerator] Failed to write raw ffmpeg output: " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return devices;
    }
}
