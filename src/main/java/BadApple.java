import com.diogonunes.jcolor.Attribute;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorDeviceConfiguration;
import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.diogonunes.jcolor.Ansi.colorize;

public class BadApple {
    static final String BadApple_Version = "2.0.0";
    @picocli.CommandLine.Command(name = "BadApple", helpCommand = true, description = "Prints ascii-ed \"Bad Apple\" video.")
    static class Parameters {
        @picocli.CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
        public boolean helpRequested = false;

        @picocli.CommandLine.Option(names = {"-v", "--verbose"}, description = "Print debug log under frame while playing video")
        public boolean verbose = false;

        @picocli.CommandLine.Option(names = {"-r", "--resize"}, description = "Set whether or not to resize the image")
        public boolean reSize = true;

        @picocli.CommandLine.Option(names = {"-c", "--clear"}, description = "Clear terminal when refresh frame")
        public boolean cleanTerminal = false;

        @picocli.CommandLine.Option(names = {"-cn", "--clear-curses"}, description = "Clear terminal using ncurses")
        public boolean ncursesTerminal = false;

        @picocli.CommandLine.Option(names = {"-d", "--delay"}, description = "Set the delay between frames (milliseconds)")
        public long delayMilliseconds = 26;

        @picocli.CommandLine.Option(names = {"-dn", "--delay-nano"}, description = "Set the delay between frames (milliseconds)")
        public long delayNanoseconds = -1;

        @picocli.CommandLine.Option(names = {"-ad", "--auto-delay"}, description = "(Experimental) Automatically determines delay length")
        public boolean autoDelay = false;

        @picocli.CommandLine.Option(names = {"-ar", "--auto-ratio"}, description = "(Experimental) Automatically determines downscale ratio")
        public boolean autoRatio = false;

        @picocli.CommandLine.Option(names = {"-t", "--ratio"}, description = "Aspect ratio value to downscale frames")
        public int ratioValueResize = 1;

        @picocli.CommandLine.Option(names = {"-a", "--audio"}, description = "Play mp4 file's audio")
        public boolean playAudio = false;

        @picocli.CommandLine.Option(names = {"-l", "--loop"}, description = "Play video by loop")
        public boolean playAsLoop = false;

        @picocli.CommandLine.Option(names = {"-s", "--sync-audio"}, description = "Sync audio with video")
        public boolean syncAudioWithVideo = false;

        @picocli.CommandLine.Option(names = {"-e", "--engine"}, description = "Convert to Ascii art using my own engine")
        public boolean useInnerEngine = false;

        @picocli.CommandLine.Option(names = {"-p", "--pre-render"}, description = "(Experimental) Pre-Render all the frames to ascii before play the video (Warning: Requires a lot of memory)")
        public boolean usePreRender = false;

        @picocli.CommandLine.Option(names = {"-f", "--file"}, paramLabel = "ARCHIVE", description = "target *.mp4 file to play")
        public File inputFile = null;

        @picocli.CommandLine.Option(names = {"-b", "--buffer-output"}, description = "use more buffer when print ascii")
        public boolean isBufferStream = true;

        @picocli.CommandLine.Option(names = {"-bs", "--buffer-size"}, description = "Size of Buffer, Default is 8192 bytes.")
        public int bufferSize = 8192;

        @picocli.CommandLine.Option(names = {"-q", "--print-color"}, description = "print color as well as ascii texts")
        public boolean printColor = false;
    }

    static Terminal terminal;
    static Screen screen;
    static BufferedWriter printStream;

    static Parameters parameters;
    static ArrayList<String> frameList = new ArrayList<>();
    static Thread audioThread;
    static Exception videoProcessThreadException;
    static final AtomicBoolean audioWaitLonger = new AtomicBoolean();
    static final Object audioLock = new Object();

    static boolean isFirstFrame = true;
    static int frameWarmUpCount = 2;
    static long defaultDelayLength;
    static double lastDelay = 0L;

    static double videoTotalFrame;
    static double audioTotalFrame;

    static int videoFrameIndex;
    static int audioFrameIndex;
    static double videoFrameRate;
    static double audioFrameRate;

    static final int warmUpDelayWindowSize = 5;
    static final int mainDelayWindowSize = 8;
    static int decidedDelayWindowSize = warmUpDelayWindowSize;
    static int warmUpCompleteWindowTick = 45;
    static double[] delayTimeWindow = new double[decidedDelayWindowSize];
    static int delayWindowIndex = 0;

    static final int SYNC_AUDIO_FAST = 1;
    static final int SYNC_FIT = 0;
    static final int SYNC_VIDEO_FAST = -1;
    static int audioSyncStatement = SYNC_FIT;

    static class RegionsMonitoringObject {
        int performanceThreshold;
        int performanceCount = -1;
        int monitoredRegionsThreshold;
        int monitoredRegionsStartFrames = 0;
        boolean withinMonitoredRegion = false;

        void calculateRegion() {
            if (!withinMonitoredRegion) {
                monitoredRegionsStartFrames = videoFrameIndex;
                withinMonitoredRegion = true;
            } else if (videoFrameIndex - monitoredRegionsStartFrames < monitoredRegionsThreshold) {
                performanceCount -= 1;
            }
        }

        void resetRegion() {
            performanceCount = performanceThreshold;
            withinMonitoredRegion = false;
        }

        void checkExceedRegionAndReset() {
            if (withinMonitoredRegion && videoFrameIndex - monitoredRegionsStartFrames > monitoredRegionsThreshold) {
                resetRegion();
            }
        }
    }

    static int perfectCount;
    static int perfectCountThreshold;
    static RegionsMonitoringObject poorMonitoring = new RegionsMonitoringObject();
    static RegionsMonitoringObject goodMonitoring = new RegionsMonitoringObject();

    public static void grabberVideoFramer() throws Exception {
        Frame frame;
        URL resource = parameters.inputFile != null && parameters.inputFile.exists() ? parameters.inputFile.toURI().toURL() : BadApple.class.getResource("BadApple.mp4");
        if (resource == null) {
            System.out.println("Error: target resource uri is Null!");
            throw new NullPointerException("target resource uri is Null!");
        }

        FFmpegFrameGrabber fFmpegFrameGrabber = new FFmpegFrameGrabber(resource.openStream());
        fFmpegFrameGrabber.start();
        int sampleFormat = fFmpegFrameGrabber.getSampleFormat();

        FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(resource.openStream());
        audioGrabber.start();
        AudioFormat af = getAudioFormat(audioGrabber);
        DataLine.Info dataLineInfo;
        dataLineInfo = new DataLine.Info(SourceDataLine.class, af, AudioSystem.NOT_SPECIFIED);
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(af);
        sourceDataLine.start();

        OutputStream outputStream = new FileOutputStream(java.io.FileDescriptor.out);
        printStream = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII), parameters.bufferSize);
        terminal = new DefaultTerminalFactory(System.out, System.in, StandardCharsets.US_ASCII)
                .setTerminalEmulatorDeviceConfiguration(TerminalEmulatorDeviceConfiguration.getDefault().withCursorBlinking(false))
                .createTerminal();
        screen = new TerminalScreen(terminal);

        if (parameters.ncursesTerminal) {
            terminal.setCursorVisible(false);
            screen.startScreen();
        }

        defaultDelayLength = (long) (1000 / fFmpegFrameGrabber.getFrameRate());
        videoFrameIndex = 0;

        if (parameters.autoRatio) {
            parameters.ratioValueResize = 1;
        }

        videoTotalFrame = fFmpegFrameGrabber.getLengthInFrames();
        audioTotalFrame = audioGrabber.getLengthInAudioFrames();

        videoFrameRate = fFmpegFrameGrabber.getVideoFrameRate();
        audioFrameRate = audioGrabber.getAudioFrameRate();
        audioWaitLonger.set(false);

        poorMonitoring.monitoredRegionsThreshold = (int) (videoFrameRate * 5);
        poorMonitoring.performanceThreshold = (int) (videoFrameRate * 1.5);
        poorMonitoring.resetRegion();

        goodMonitoring.monitoredRegionsThreshold = (int) (videoFrameRate * 40);
        goodMonitoring.performanceThreshold = (int) (goodMonitoring.monitoredRegionsThreshold * 0.9);
        poorMonitoring.resetRegion();

        perfectCount = 0;
        perfectCountThreshold = (int) (fFmpegFrameGrabber.getLengthInFrames() * 0.35);

        System.out.println("Duration Video: " + videoTotalFrame / videoFrameRate + ", Audio: " + audioTotalFrame / audioFrameRate);
        System.out.println("Start running video extraction frame, it takes a long time");

        initAudioThread(audioGrabber, sampleFormat, sourceDataLine);
        if (!parameters.usePreRender && parameters.playAudio) {
            audioThread.start();
        }

        while (videoFrameIndex <= videoTotalFrame) {
            long printStartedTime = System.currentTimeMillis();
            frame = fFmpegFrameGrabber.grabImage();
            videoFrameIndex++;

            Frame finalFrame = frame;
            Thread mainProcessThread = new Thread(() -> {
                try {
                    processVideoFrame(printStartedTime, finalFrame);
                } catch (IOException | InterruptedException e) {
                    videoProcessThreadException = e;
                }
            });

            mainProcessThread.start();
            while (true) {
                if(videoProcessThreadException != null) {
                    throw videoProcessThreadException;
                }

                if(!mainProcessThread.isAlive()) {
                    break;
                }

                if(System.currentTimeMillis() - printStartedTime >= defaultDelayLength * 3) {
                    mainProcessThread.interrupt();
                    poorMonitoring.calculateRegion();
                    break;
                }
            }
        }

        if (parameters.usePreRender) {
            if (parameters.playAudio) {
                audioThread.start();
            }

            for (String textToPrint : frameList) {
                long printStartedTime = System.currentTimeMillis();
                printAndWaitFrame(textToPrint, printStartedTime);
            }
        }

        if (parameters.playAudio) {
            while (true) {
                if (!audioThread.isAlive()) break;
            }
        }

        fFmpegFrameGrabber.stop();
        if (!parameters.playAsLoop) {
            System.out.println("============End of operation============");
            printStream.close();

            if (parameters.ncursesTerminal) {
                clearScreen(true);
                screen.stopScreen();
                terminal.close();
            }
        } else {
            isFirstFrame = true;
            frameWarmUpCount = 2;
            lastDelay = 0L;

            videoFrameIndex = 0;
            audioFrameIndex = 0;
            audioSyncStatement = SYNC_FIT;

            decidedDelayWindowSize = warmUpDelayWindowSize;
            warmUpCompleteWindowTick = 50;
            delayTimeWindow = new double[decidedDelayWindowSize];
            delayWindowIndex = 0;

            poorMonitoring = new RegionsMonitoringObject();
            goodMonitoring = new RegionsMonitoringObject();
        }
    }

    private static void processVideoFrame(long printStartedTime, Frame frame) throws IOException, InterruptedException {
        String textToPrint = "";
        if (frame != null) {
            BufferedImage originImg = FrameToBufferedImage(frame);
            if (originImg != null) {
                if (parameters.useInnerEngine) {
                    textToPrint = processFrameIntoString(originImg);
                } else {
                    textToPrint = processFrameIntoStringUsingEncoder(originImg);
                }
            }

            if (parameters.usePreRender) {
                frameList.add(textToPrint);
            } else {
                printAndWaitFrame(textToPrint, printStartedTime);
            }
        }
    }

    private static void printAndWaitFrame(String textToPrint, long printStartedTime) throws IOException, InterruptedException {
        clearScreen(false);
        String[] lines = textToPrint.split(System.lineSeparator());
        String verbose = getVerbose(textToPrint, lines);

        if (parameters.ncursesTerminal) {
            TextGraphics textGraphics = screen.newTextGraphics();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                textGraphics.putCSIStyledString(0, i, line);
            }

            if (!verbose.isEmpty()) {
                String[] verboseLines = verbose.split(System.lineSeparator());
                for (int i = 0; i < verboseLines.length; i++) {
                    String line = verboseLines[i];
                    textGraphics.putString(0, i + lines.length, line);
                }
            }

            screen.refresh();
        } else {
            if (!verbose.isEmpty()) {
                textToPrint += verbose;
            }

            if (parameters.isBufferStream) {
                printStream.write(textToPrint + "\r");
                printStream.flush();
            } else {
                System.out.print(textToPrint);
            }
        }

        if (parameters.autoDelay) {
            if (isFirstFrame) {
                isFirstFrame = false;
                frameWarmUpCount -= 1;
            } else if (frameWarmUpCount > 0) {
                frameWarmUpCount -= 1;
                adjustDelay(printStartedTime, textToPrint);
            } else {
                adjustDelay(printStartedTime, textToPrint);
                if (parameters.autoRatio) {
                    adjustDownSamplingRatio();
                }
            }
        } else {
            if (parameters.delayNanoseconds < 0) {
                Thread.sleep(parameters.delayMilliseconds);
            } else {
                long start = System.nanoTime();
                long end;
                do {
                    end = System.nanoTime();
                } while ((start + parameters.delayNanoseconds) - end >= 0);
            }
        }

        if (parameters.syncAudioWithVideo) {
            final double allowableRange = 0.45;
            double currentAudioSecond = audioFrameIndex / audioFrameRate;
            double currentVideoSecond = videoFrameIndex / videoFrameRate;
            double syncDiff = currentAudioSecond - currentVideoSecond;

            if (syncDiff > allowableRange) {
                audioSyncStatement = SYNC_AUDIO_FAST;
                if (syncDiff > allowableRange * 2) synchronized (audioLock) {
                    if (!audioWaitLonger.get()) {
                        audioWaitLonger.set(true);
                        audioLock.notify();
                    }
                }
            } else if (syncDiff * -1 > allowableRange) {
                audioSyncStatement = SYNC_VIDEO_FAST;
                Thread.sleep((long) ((currentVideoSecond - currentAudioSecond) * 3));
            } else if (syncDiff <= allowableRange) {
                audioSyncStatement = SYNC_FIT;
                if (audioWaitLonger.get()) synchronized (audioLock) {
                    audioWaitLonger.set(false);
                    audioLock.notify();
                }
            }
        }
    }

    private static String getVerbose(String textToPrint, String[] lines) {
        String verbose = "";
        if (parameters.autoDelay && parameters.verbose) {
            double videoInSecond = videoFrameIndex / videoFrameRate;
            double audioInSecond = audioFrameIndex / audioFrameRate;

            double sum = 0.0;
            for (double time : delayTimeWindow) {
                sum += time;
            }
            double averageProcessingTime = sum / delayTimeWindow.length;

            verbose = String.format(" Delay (ms): %.5f, Ratio: %s, Fps: %.5f\n Lines: %s, Single: %s, Total: %s\n Video (s): %.2f, Audio (s): %.2f, Diff (s): %.2f\n Avg: %.2f, Window: %d, Poor: (Start: %s, Count: %s), Good: (Start: %s, Count: %s), Perfect: (Threshold: %s, Count: %s)\n",
                    lastDelay, parameters.ratioValueResize, (1000 / lastDelay),
                    lines.length, lines[0].length(), textToPrint.length(),
                    videoInSecond, audioInSecond, audioInSecond - videoInSecond,

                    averageProcessingTime, delayTimeWindow.length, poorMonitoring.monitoredRegionsStartFrames, poorMonitoring.performanceCount,
                    goodMonitoring.monitoredRegionsStartFrames, goodMonitoring.performanceCount, perfectCountThreshold, perfectCount
            );
        }
        return verbose;
    }

    private static void adjustDelay(long printStartedTime, String textToPrint) throws InterruptedException {
        double delay = defaultDelayLength - (System.currentTimeMillis() - printStartedTime);
        if (delay >= 0) {
            double delayMargin = (textToPrint.length() * (parameters.printColor ? 0.000006 : 0.000003));
            if (delay > delayMargin) switch (audioSyncStatement) {
                case SYNC_AUDIO_FAST:
                    delay -= delayMargin;
                    break;

                case SYNC_VIDEO_FAST:
                    delay += delayMargin;
                    break;

                case SYNC_FIT:
                    //Do Nothing
                    break;
            }
            Thread.sleep((long) delay);
        }
        lastDelay = delay;
    }

    private static void adjustDownSamplingRatio() throws IOException, InterruptedException {
        if (decidedDelayWindowSize == mainDelayWindowSize) {
            perfectCount += 1;
            if (perfectCount >= perfectCountThreshold) {
                parameters.autoRatio = false;
            }
        } else if (warmUpCompleteWindowTick > 0) {
            warmUpCompleteWindowTick -= 1;
        } else {
            decidedDelayWindowSize = mainDelayWindowSize;
            delayWindowIndex = 0;

            double[] arrToCopy = delayTimeWindow;
            delayTimeWindow = new double[decidedDelayWindowSize];
            System.arraycopy(arrToCopy, 0, delayTimeWindow, 0, arrToCopy.length);
            return;
        }

        delayTimeWindow[delayWindowIndex] = lastDelay;
        delayWindowIndex = (delayWindowIndex + 1) % decidedDelayWindowSize;

        double sum = 0.0;
        for (double time : delayTimeWindow) {
            sum += time;
        }
        double averageProcessingTime = sum / delayTimeWindow.length;

        if (lastDelay < 0 || averageProcessingTime < defaultDelayLength - defaultDelayLength * 0.99) {
            if (decidedDelayWindowSize == mainDelayWindowSize) {
                poorMonitoring.calculateRegion();
                goodMonitoring.resetRegion();

                if (poorMonitoring.performanceCount <= 0) {
                    delayTimeWindow = new double[decidedDelayWindowSize];
                    delayWindowIndex = 0;
                    poorMonitoring.resetRegion();
                    perfectCount = 0;

                    parameters.ratioValueResize += 1;
                    clearScreen(true);
                }
            } else {
                delayTimeWindow = new double[decidedDelayWindowSize];
                delayWindowIndex = 0;

                parameters.ratioValueResize += 1;
                clearScreen(true);
            }
        } else if (parameters.ratioValueResize > 1 && averageProcessingTime < defaultDelayLength - defaultDelayLength * 0.3) {
            if (decidedDelayWindowSize == mainDelayWindowSize) {
                goodMonitoring.calculateRegion();
                poorMonitoring.resetRegion();

                if (goodMonitoring.performanceCount <= 0) {
                    delayTimeWindow = new double[decidedDelayWindowSize];
                    delayWindowIndex = 0;

                    parameters.ratioValueResize -= 1;
                    clearScreen(true);
                    goodMonitoring.resetRegion();
                    perfectCount = 0;
                }
            }
        } else if (decidedDelayWindowSize == mainDelayWindowSize) {
            poorMonitoring.checkExceedRegionAndReset();
            goodMonitoring.checkExceedRegionAndReset();
        }
    }

    private static void clearScreen(boolean needClearCurses) throws IOException, InterruptedException {
        if (parameters.ncursesTerminal && needClearCurses) {
            screen.clear();
            screen.refresh();
            return;
        }

        if (!parameters.ncursesTerminal && parameters.cleanTerminal) {
            if (parameters.isBufferStream) {
                printStream.write("\033[H\033[2J");
                printStream.flush();
            } else {
                ProcessBuilder processBuilder = System.getProperty("os.name").contains("Windows") ? new ProcessBuilder("cmd", "/c", "cls") : new ProcessBuilder("clear");
                Process process = processBuilder.inheritIO().start();
                process.waitFor();
            }
        }
    }

    private static void initAudioThread(FFmpegFrameGrabber audioGrabber, int sampleFormat, SourceDataLine sourceDataLine) {
        if (audioThread == null || !audioThread.isAlive()) audioThread = new Thread(() -> {
            audioFrameIndex = 0;
            int audioFtp = audioGrabber.getLengthInAudioFrames();

            while (audioFrameIndex <= audioFtp) {
                synchronized (audioLock) {
                    try {
                        if (audioWaitLonger.get()) {
                            audioLock.wait();
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Error Occurred while stopping audio thread: " + e.getMessage());
                        continue;
                    }
                }

                try {
                    Frame sample = audioGrabber.grabSamples();
                    if(sample != null) {
                        processAudio(sample.samples, sampleFormat, sourceDataLine);
                    }
                } catch (FFmpegFrameGrabber.Exception e) {
                    System.out.println("Error while processing audio:" + e.getMessage());
                }
                audioFrameIndex++;
            }
        });
    }

    public static String processFrameIntoString(BufferedImage originImg) {
        StringBuilder textToPrint = new StringBuilder();
        final String base = parameters.printColor ? " " : "@#&$%*o!;.";
        int ratioForIndex = parameters.ratioValueResize * 4;
        for (int index = 0; index < originImg.getHeight(); index += ratioForIndex) {
            for (int j = 0; j < originImg.getWidth(); j += parameters.ratioValueResize) {
                int pixel = originImg.getRGB(j, index);
                int red = (pixel & 0xff0000) >> 16;
                int green = (pixel & 0xff00) >> 8;
                int blue = (pixel & 0xff);
                float gray = 0.299f * red + 0.578f * green + 0.114f * blue;
                int indexBase = Math.round(gray * (base.length() + 1) / 255);

                String text = indexBase >= base.length() ? " " : String.valueOf(base.charAt(indexBase));
                if (parameters.printColor) textToPrint.append(colorize(text, Attribute.BACK_COLOR(red, green, blue)));
                else textToPrint.append(text);
            }
            textToPrint.append("\r\n");
        }

        return textToPrint.toString();
    }

    public static String processFrameIntoStringUsingEncoder(BufferedImage originImg) throws IOException {
        StringBuilder textToPrint = new StringBuilder();
        BufferedImage img = parameters.reSize ? Thumbnails.of(originImg)
                .size(originImg.getWidth() / parameters.ratioValueResize, originImg.getHeight() / (parameters.ratioValueResize * 4))
                .keepAspectRatio(false)
                .asBufferedImage() : originImg;

        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                Color pixelCol = new Color(img.getRGB(j, i));
                double pixelVal = (((pixelCol.getRed() * 0.30) + (pixelCol.getBlue() * 0.59) + (pixelCol.getGreen() * 0.11)));
                if (parameters.printColor)
                    textToPrint.append(colorize(strChar(pixelVal), Attribute.BACK_COLOR(pixelCol.getRed(), pixelCol.getGreen(), pixelCol.getBlue())));
                else textToPrint.append(strChar(pixelVal));
            }
            textToPrint.append("\n");
        }
        return textToPrint.toString();
    }

    public static String strChar(double g) {
        String str;
        if (parameters.printColor || g >= 240) {
            str = " ";
        } else if (g >= 210) {
            str = ".";
        } else if (g >= 190) {
            str = "*";
        } else if (g >= 170) {
            str = "+";
        } else if (g >= 120) {
            str = "^";
        } else if (g >= 110) {
            str = "&";
        } else if (g >= 80) {
            str = "8";
        } else if (g >= 60) {
            str = "#";
        } else {
            str = "@";
        }
        return str;
    }

    public static BufferedImage FrameToBufferedImage(Frame frame) {
        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            return converter.getBufferedImage(frame);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return null;
    }

    public static void processAudio(Buffer[] samples, int sampleFormat, SourceDataLine sourceDataLine) {
        int k;
        FloatBuffer leftData, rightData;
        ShortBuffer ILData, IRData;
        ByteBuffer TLData, TRData;
        float vol = 1;
        byte[] tl, tr;
        byte[] combine;
        switch (sampleFormat) {
            case avutil.AV_SAMPLE_FMT_FLTP:
                leftData = (FloatBuffer) samples[0];
                TLData = floatToByteValue(leftData, vol);
                rightData = (FloatBuffer) samples[1];
                TRData = floatToByteValue(rightData, vol);
                tl = TLData.array();
                tr = TRData.array();
                combine = new byte[tl.length + tr.length];
                k = 0;
                combineByteArray(k, tl, tr, combine);
                sourceDataLine.write(combine, 0, combine.length);
                break;
            case avutil.AV_SAMPLE_FMT_S16:
                ILData = (ShortBuffer) samples[0];
                TLData = shortToByteValue(ILData, vol);
                tl = TLData.array();
                sourceDataLine.write(tl, 0, tl.length);
                break;
            case avutil.AV_SAMPLE_FMT_FLT:
                leftData = (FloatBuffer) samples[0];
                TLData = floatToByteValue(leftData, vol);
                tl = TLData.array();
                sourceDataLine.write(tl, 0, tl.length);
                break;
            case avutil.AV_SAMPLE_FMT_S16P:
                ILData = (ShortBuffer) samples[0];
                IRData = (ShortBuffer) samples[1];
                TLData = shortToByteValue(ILData, vol);
                TRData = shortToByteValue(IRData, vol);
                tl = TLData.array();
                tr = TRData.array();
                combine = new byte[tl.length + tr.length];
                k = 0;
                combineByteArray(k, tl, tr, combine);
                sourceDataLine.write(combine, 0, combine.length);
                break;
            default:
                JOptionPane.showMessageDialog(null, "nonsupport audio format", "nonsupport audio format", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                break;
        }
    }

    private static void combineByteArray(int k, byte[] tl, byte[] tr, byte[] combine) {
        for (int i = 0; i < tl.length; i = i + 2) {
            for (int j = 0; j < 2; j++) {
                combine[j + 4 * k] = tl[i + j];
                combine[j + 2 + 4 * k] = tr[i + j];
            }
            k++;
        }
    }

    public static ByteBuffer shortToByteValue(ShortBuffer arr, float vol) {
        int len = arr.capacity();
        ByteBuffer bb = ByteBuffer.allocate(len * 2);
        for (int i = 0; i < len; i++) {
            bb.putShort(i * 2, (short) ((float) arr.get(i) * vol));
        }
        return bb;
    }

    public static ByteBuffer floatToByteValue(FloatBuffer arr, float vol) {
        int len = arr.capacity();
        float f;
        float v;
        ByteBuffer res = ByteBuffer.allocate(len * 2);
        v = 32768.0f * vol;
        for (int i = 0; i < len; i++) {
            f = arr.get(i) * v;
            if (f > v) f = v;
            if (f < -v) f = v;
            res.putShort(i * 2, (short) f);
        }
        return res;
    }

    public static AudioFormat getAudioFormat(FFmpegFrameGrabber fg) {
        AudioFormat af = null;
        switch (fg.getSampleFormat()) {
            case avutil.AV_SAMPLE_FMT_U8:
            case avutil.AV_SAMPLE_FMT_S32:
            case avutil.AV_SAMPLE_FMT_DBLP:
            case avutil.AV_SAMPLE_FMT_U8P:
            case avutil.AV_SAMPLE_FMT_DBL:
            case avutil.AV_SAMPLE_FMT_S64:
            case avutil.AV_SAMPLE_FMT_S64P:
                break;
            case avutil.AV_SAMPLE_FMT_S16:
            case avutil.AV_SAMPLE_FMT_FLTP:
            case avutil.AV_SAMPLE_FMT_S16P:
            case avutil.AV_SAMPLE_FMT_FLT:
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 16, fg.getAudioChannels(),
                        fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            case avutil.AV_SAMPLE_FMT_S32P:
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 32, fg.getAudioChannels(),
                        fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            default:
                System.out.println("unsupported");
                System.exit(0);
        }
        return af;
    }

    public static void main(String[] args) throws Exception {
        parameters = new Parameters();
        picocli.CommandLine commandLine = new picocli.CommandLine(parameters);
        commandLine.setUnmatchedArgumentsAllowed(false).parseArgs(args);
        if (parameters.inputFile != null) {
            if (!parameters.inputFile.canRead()) {
                System.out.println("File Cannot be read: " + parameters.inputFile.getName());
                System.exit(-1);
            }

            String[] names = parameters.inputFile.getName().split("\\.");
            if (!names[names.length - 1].equalsIgnoreCase("mp4")) {
                System.out.println("Not supported file: " + parameters.inputFile.getName() + " Only supports MPEG-4 format!");
                System.exit(-1);
            }
        }

        if (!parameters.helpRequested) do {
            grabberVideoFramer();
        } while (parameters.playAsLoop);
        else {
            String helpMessage = String.format("BadApple-Java, Written By.Choiman1559, Version: %s\n", BadApple_Version);
            helpMessage += commandLine.getUsageMessage();

            System.out.print(helpMessage);
        }
    }
}
