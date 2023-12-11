import com.diogonunes.jcolor.Attribute;
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

import static com.diogonunes.jcolor.Ansi.colorize;

public class BadApple {

    @picocli.CommandLine.Command(name = "BadAppleJava", helpCommand = true, description = "Prints ascii-ed \"Bad Apple\" video.")
    static class Parameters {
        @picocli.CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
        public boolean helpRequested = false;

        @picocli.CommandLine.Option(names = {"-r", "--resize"}, description = "Set whether or not to resize the image")
        public boolean reSize = true;

        @picocli.CommandLine.Option(names = {"-c", "--clear"}, description = "Clear terminal when refresh frame")
        public boolean cleanTerminal = false;

        @picocli.CommandLine.Option(names = {"-d", "--delay"}, description = "Set the delay between frames (milliseconds)")
        public long delayMilliseconds = 26;

        @picocli.CommandLine.Option(names = {"-dn", "--delay-nano"}, description = "Set the delay between frames (milliseconds)")
        public long delayNanoseconds = -1;

        @picocli.CommandLine.Option(names = {"-ad", "--auto-delay"}, description = "(Experimental) Automatically determines delay length")
        public boolean autoDelay = false;

        @picocli.CommandLine.Option(names = {"-t", "--ratio"}, description = "Ratio value when resetting frame size")
        public int ratioValueResize = 1;

        @picocli.CommandLine.Option(names = {"-a", "--audio"}, description = "Play mp4 file's audio")
        public boolean playAudio = false;

        @picocli.CommandLine.Option(names = {"-l", "--loop"}, description = "Play video by loop")
        public boolean playAsLoop = false;

        @picocli.CommandLine.Option(names = {"-s", "--sync"}, description = "Sync audio with video")
        public boolean syncAudioWithVideo = false;

        @picocli.CommandLine.Option(names = {"-e", "--engine"}, description = "Convert to Ascii art using my own engine")
        public boolean useInnerEngine = false;

        @picocli.CommandLine.Option(names = {"-p", "--pre-render"}, description = "(Experimental) Pre-Render the image to ascii before play the video")
        public boolean usePreRender = false;

        @picocli.CommandLine.Option(names = {"-f", "--file"}, paramLabel = "ARCHIVE", description = "target *.mp4 file to play")
        public File inputFile = null;

        @picocli.CommandLine.Option(names = {"-b", "--buffer-output"}, description = "use more buffer when print ascii")
        public boolean isBufferStream = true;

        @picocli.CommandLine.Option(names = {"-q", "--print-color"}, description = "print color as well as ascii texts")
        public boolean printColor = false;
    }

    static Parameters parameters;
    static ArrayList<String> frameList = new ArrayList<>();
    static boolean isFirstFrame = true;
    static long defaultDelayLength;
    static BufferedWriter printStream;
    static double lastDelay = 0L;

    public static void grabberVideoFramer() throws IOException, LineUnavailableException, InterruptedException {
        Frame frame;
        Frame audioFrame;

        int flag = 0;
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

        int ftp = fFmpegFrameGrabber.getLengthInFrames();
        defaultDelayLength = (long) (1000 / fFmpegFrameGrabber.getFrameRate());
        printStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(java.io.FileDescriptor.out), StandardCharsets.US_ASCII), 512);

        System.out.println("Duration" + ftp / fFmpegFrameGrabber.getFrameRate() / 60);
        System.out.println("Start running video extraction frame, it takes a long time");

        Thread audioThread = getAudioThread(audioGrabber, sampleFormat, sourceDataLine);
        if(!parameters.usePreRender && parameters.playAudio && !parameters.syncAudioWithVideo) {
            audioThread.start();
        }

        while (flag <= ftp) {
            frame = fFmpegFrameGrabber.grabImage();

            String textToPrint = "";
            if (frame != null) {
                BufferedImage originImg = FrameToBufferedImage(frame);
                if(originImg == null) continue;

                if (parameters.useInnerEngine) {
                    textToPrint = processFrameIntoString(originImg);
                } else {
                    textToPrint = processFrameIntoStringUsingEncoder(originImg);
                }
            }

            flag++;
            if(parameters.usePreRender) {
                frameList.add(textToPrint);
            } else {
                if (parameters.playAudio && parameters.syncAudioWithVideo) {
                    audioFrame = audioGrabber.grabSamples();
                    processAudio(audioFrame.samples, sampleFormat, sourceDataLine);
                }
                printAndWaitFrame(textToPrint);
            }
        }

        if(parameters.usePreRender) {
            if(parameters.playAudio && !parameters.syncAudioWithVideo) {
                audioThread.start();
            }

            for(String textToPrint: frameList) {
                printAndWaitFrame(textToPrint);
            }
        }

        if (parameters.playAudio && !parameters.syncAudioWithVideo) {
            while (true) {
                if (!audioThread.isAlive()) break;
            }
        }

        if (!parameters.playAsLoop) System.out.println("============End of operation============");
        fFmpegFrameGrabber.stop();
        printStream.close();
    }

    private static void printAndWaitFrame(String textToPrint) throws IOException, InterruptedException {
        if (parameters.cleanTerminal) {
            if (parameters.isBufferStream) {
                printStream.write("\033[H\033[2J");
                printStream.flush();
            } else {
                ProcessBuilder processBuilder = System.getProperty("os.name").contains("Windows") ? new ProcessBuilder("cmd", "/c", "cls") : new ProcessBuilder("clear");
                Process process = processBuilder.inheritIO().start();
                process.waitFor();
            }
        }

        if(parameters.autoDelay) {
            textToPrint += " Delay (ms): " + lastDelay + " Fps: " + (1000 / lastDelay) + "\n";
        }

        long printStartedTime = System.currentTimeMillis();
        if (parameters.isBufferStream) {
            printStream.write(textToPrint);
            printStream.flush();
        } else System.out.print(textToPrint);

        if(parameters.autoDelay) {
            if(isFirstFrame) {
                isFirstFrame = false;
            } else {
                double delay = defaultDelayLength - (System.currentTimeMillis() - printStartedTime);
                if(delay >= 0) {
                    delay = delay - (textToPrint.length() * 0.00003);
                    if(delay > 0) {
                        Thread.sleep((long) delay);
                    }
                }
                lastDelay = delay;
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
    }

    private static Thread getAudioThread(FFmpegFrameGrabber audioGrabber, int sampleFormat, SourceDataLine sourceDataLine) {
        return new Thread(() -> {
            int audioFlag = 0;
            int audioFtp = audioGrabber.getLengthInAudioFrames();

            while (audioFlag <= audioFtp) {
                try {
                    processAudio(audioGrabber.grabSamples().samples, sampleFormat, sourceDataLine);
                } catch (FFmpegFrameGrabber.Exception e) {
                    System.out.println("Error while processing audio:" + e.getMessage());
                }
                audioFlag++;
            }
        });
    }

    public static String processFrameIntoString(BufferedImage originImg) {
        StringBuilder textToPrint = new StringBuilder();
        final String base = "@#&$%*o!;.";
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
                if(parameters.printColor) textToPrint.append(colorize(text, Attribute.TEXT_COLOR(red, green, blue)));
                else textToPrint.append(text);
            }
            textToPrint.append("\r\n");
        }

        return textToPrint.toString();
    }

    public static String processFrameIntoStringUsingEncoder(BufferedImage originImg) throws IOException {
        StringBuilder textToPrint = new StringBuilder();
        BufferedImage img = parameters.reSize ? Thumbnails.of(originImg)
                .size(originImg.getWidth() / parameters.ratioValueResize, originImg.getHeight() / parameters.ratioValueResize)
                .keepAspectRatio(true).asBufferedImage() : originImg;

        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                Color pixelCol = new Color(img.getRGB(j, i));
                double pixelVal = (((pixelCol.getRed() * 0.30) + (pixelCol.getBlue() * 0.59) + (pixelCol.getGreen() * 0.11)));
                if(parameters.printColor) textToPrint.append(colorize(strChar(pixelVal), Attribute.TEXT_COLOR(pixelCol.getRed(), pixelCol.getGreen(), pixelCol.getBlue())));
                else textToPrint.append(strChar(pixelVal));
            }
            textToPrint.append("\n");
        }
        return textToPrint.toString();
    }

    public static String strChar(double g) {
        String str;
        if (g >= 240) {
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

    public static void main(String[] args) throws IOException, InterruptedException, LineUnavailableException {
        parameters = new Parameters();
        picocli.CommandLine commandLine = new picocli.CommandLine(parameters);
        commandLine.setUnmatchedArgumentsAllowed(false).parseArgs(args);
        if (parameters.inputFile != null) {
            String[] names = parameters.inputFile.getName().split("\\.");
            if (!names[names.length - 1].equalsIgnoreCase("mp4")) {
                System.out.println("Not supported file: " + parameters.inputFile.getName());
                System.exit(-1);
            }
        }

        if (!parameters.helpRequested) do {
            grabberVideoFramer();
        } while (parameters.playAsLoop);
        else System.out.print(commandLine.getUsageMessage());
    }
}
