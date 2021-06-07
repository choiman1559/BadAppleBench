import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BadApple {

    @picocli.CommandLine.Command(name = "BadAppleJava", helpCommand = true, description = "Prints ascii-ed \"Bad Apple\" video.")
    static class Parameters {
        @picocli.CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
        public boolean helpRequested = false;

        @picocli.CommandLine.Option(names = {"-r", "--resize"}, description = "Set whether or not to resize the image")
        public boolean reSize = false;

        @picocli.CommandLine.Option(names = {"-c", "--clear"}, description = "Clear terminal when refresh frame")
        public boolean cleanTerminal = false;

        @picocli.CommandLine.Option(names = {"-d", "--delay"}, description = "Set the delay between frames (milliseconds)")
        public long delayMilliseconds = 26;

        @picocli.CommandLine.Option(names = {"-t", "--ratio"} ,description = "Ratio value when resetting frame size")
        public int ratioValueResize = 1;

        @picocli.CommandLine.Option(names = {"-a", "--audio"}, description = "Play mp4 file's audio")
        public boolean playAudio = false;

        @picocli.CommandLine.Option(names = {"-l", "--loop"}, description = "Play video by loop")
        public boolean playAsLoop = false;

        @picocli.CommandLine.Option(names = {"-s", "--sync"}, description = "Sync audio with video")
        public boolean syncAudioWithVideo = false;

        @picocli.CommandLine.Option(names = {"-f", "--file"}, paramLabel = "ARCHIVE", description = "target *.mp4 file to play")
        public File inputFile = null;
    }

    public static void grabberVideoFramer(Parameters parameters) throws IOException, InterruptedException, LineUnavailableException {
        Frame frame;
        Frame audioFrame;

        int flag = 0;
        URL resource = parameters.inputFile != null && parameters.inputFile.exists() ? parameters.inputFile.toURI().toURL() : BadApple.class.getResource("BadApple.mp4");

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
        System.out.println("Duration" + ftp / fFmpegFrameGrabber.getFrameRate() / 60);
        System.out.println("Start running video extraction frame, it takes a long time");

        Thread audioThread = null;
        if(parameters.playAudio && !parameters.syncAudioWithVideo) {
            audioThread = new Thread(() -> {
                int audioFlag = 0;
                int audioFtp = audioGrabber.getLengthInAudioFrames();

                while (audioFlag <= audioFtp) {
                    try {
                        processAudio(audioGrabber.grabSamples().samples, sampleFormat, sourceDataLine);
                    } catch (FFmpegFrameGrabber.Exception e) {
                        e.printStackTrace();
                    }
                    audioFlag++;
                }
            });
            audioThread.start();
        }

        while (flag <= ftp) {
            frame = fFmpegFrameGrabber.grabImage();

            StringBuilder textToPrint = new StringBuilder();
            if (frame != null) {
                BufferedImage originImg = FrameToBufferedImage(frame);
                BufferedImage img = parameters.reSize ? Thumbnails.of(originImg)
                        .size(originImg.getWidth() / parameters.ratioValueResize, originImg.getHeight() / parameters.ratioValueResize)
                        .keepAspectRatio(true).asBufferedImage() : originImg;
                for (int i = 0; i < img.getHeight(); i++) {
                    for (int j = 0; j < img.getWidth(); j++) {
                        Color pixcol = new Color(img.getRGB(j, i));
                        double pixval = (((pixcol.getRed() * 0.30) + (pixcol.getBlue() * 0.59) + (pixcol.getGreen() * 0.11)));
                        textToPrint.append(strChar(pixval));
                    }
                    textToPrint.append("\n");
                }
            }

            if (parameters.cleanTerminal) {
                ProcessBuilder processBuilder = System.getProperty("os.name").contains("Windows") ? new ProcessBuilder("cmd", "/c", "cls") : new ProcessBuilder("clear");
                Process process = processBuilder.inheritIO().start();
                process.waitFor();
            }

            flag++;
            Thread.sleep(parameters.delayMilliseconds);
            if (parameters.playAudio && parameters.syncAudioWithVideo) {
                audioFrame = audioGrabber.grabSamples();
                processAudio(audioFrame.samples, sampleFormat, sourceDataLine);
            }
            System.out.print(textToPrint.toString());
        }

        if(audioThread != null && parameters.playAudio && !parameters.syncAudioWithVideo) {
            while (true) {
                if (!audioThread.isAlive()) break;
            }
        }
        if(!parameters.playAsLoop) System.out.println("============End of operation============");
        fFmpegFrameGrabber.stop();
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
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getBufferedImage(frame);
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
                for (int i = 0; i < tl.length; i = i + 2) {
                    for (int j = 0; j < 2; j++) {
                        combine[j + 4 * k] = tl[i + j];
                        combine[j + 2 + 4 * k] = tr[i + j];
                    }
                    k++;
                }
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
                for (int i = 0; i < tl.length; i = i + 2) {
                    for (int j = 0; j < 2; j++) {
                        combine[j + 4 * k] = tl[i + j];
                        combine[j + 2 + 4 * k] = tr[i + j];
                    }
                    k++;
                }
                sourceDataLine.write(combine, 0, combine.length);
                break;
            default:
                JOptionPane.showMessageDialog(null, "nonsupport audio format", "nonsupport audio format", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                break;
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
        Parameters parameters = new Parameters();
        picocli.CommandLine commandLine = new picocli.CommandLine(parameters);
        commandLine.setUnmatchedArgumentsAllowed(false).parseArgs(args);
        if(parameters.inputFile != null) {
            String[] names = parameters.inputFile.getName().split("\\.");
            if(!names[names.length - 1].toLowerCase().equals("mp4")) {
                System.out.println("Not supported file: " + parameters.inputFile.getName());
                System.exit(-1);
            }
        }

        if(!parameters.helpRequested) do {
            grabberVideoFramer(parameters);
        } while (parameters.playAsLoop);
        else System.out.print(commandLine.getUsageMessage());
    }
}
