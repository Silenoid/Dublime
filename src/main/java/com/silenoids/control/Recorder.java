package com.silenoids.control;

import com.silenoids.utils.FileUtils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class Recorder implements Runnable {
    private AudioInputStream audioInputStream;
    private AudioFormat audioFormat;
    private Microphone microphone;
    private Thread thread;
    private double duration;

    public void startWithAlias(String dirPath, String fileName) {
        File inputAudioFile = FileUtils.loadDirFile(dirPath, fileName);

        try {
            AudioFileFormat inputAudioFormat = AudioSystem.getAudioFileFormat(inputAudioFile);

            setStrangeL4DFormat(inputAudioFormat);

            thread = new Thread(this);
            thread.setName("Capture Microphone");
            thread.start();
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    private void setStrangeL4DFormat(AudioFileFormat inputAudioFormat) {
        if(inputAudioFormat.getFormat().getSampleRate() == 22050) {
            audioFormat = new AudioFormat(
                    22000,
                    16,
                    1,
                    true,
                    true
            );
        } else {
            audioFormat = inputAudioFormat.getFormat();
        }
    }

    public void stop() {
        thread = null;
    }

    @Override
    public void run() {
        duration = 0;

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final TargetDataLine line = getTargetDataLineForRecord()) {
            int frameSizeInBytes = audioFormat.getFrameSize();
            int bufferLengthInFrames = line.getBufferSize() / 8;
            final int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
            buildByteOutputStream(out, line, frameSizeInBytes, bufferLengthInBytes);
            this.audioInputStream = new AudioInputStream(line);
            setAudioInputStream(convertToAudioIStream(out, frameSizeInBytes));
            audioInputStream.reset();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void buildByteOutputStream(final ByteArrayOutputStream out, final TargetDataLine line, int frameSizeInBytes, final int bufferLengthInBytes) {
        final byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead;

        line.start();
        while (thread != null) {
            if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                break;
            }
            out.write(data, 0, numBytesRead);
        }
    }

    private void setAudioInputStream(AudioInputStream aStream) {
        this.audioInputStream = aStream;
    }

    public AudioInputStream convertToAudioIStream(final ByteArrayOutputStream out, int frameSizeInBytes) {
        byte[] audioBytes = out.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream audioStream = new AudioInputStream(bais, audioFormat, audioBytes.length / frameSizeInBytes);
        long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / audioFormat.getFrameRate());
        duration = milliseconds / 1000.0;
        System.out.println("Recorded duration in seconds: " + duration);
        return audioStream;
    }

    // TODO: fix custom selected microphone, line is null
    public TargetDataLine getTargetDataLineForRecord() {
        TargetDataLine line;

        try {
            if (microphone != null) {
                line = (TargetDataLine) microphone.getLine();
            } else {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    return null;
                }
                line = (TargetDataLine) AudioSystem.getLine(info);
            }
            line.open(audioFormat, line.getBufferSize());
        } catch (final Exception ex) {
            System.out.println("Something wrong with loading the line " + microphone);
            return null;
        }

        return line;
    }

    public AudioInputStream getAudioInputStream() {
        return audioInputStream;
    }

    public Microphone getMicrophone() {
        return microphone;
    }

    public void setMicrophone(Microphone microphone) {
        this.microphone = microphone;
    }
}
