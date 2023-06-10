package io.gejsi.pufferfish.handlers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

import io.gejsi.pufferfish.models.MeasurementSampler;

public class AudioHandler extends MeasurementSampler {
  private AudioRecord audioRecord;

  public AudioHandler(Context context) {
    super(context);
  }

  @Override
  @SuppressLint("MissingPermission")
  public void start() {
    int sampleRate = 44100;
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    // 16-bit audio data
    short[] buffer = new short[minBufferSize];
    audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channelConfig, audioFormat, minBufferSize);
    audioRecord.startRecording();

    this.setRecording(true);
    int averageLength = this.getAverageLengthPreference();
    this.setData(new double[averageLength]);

    new Thread(() -> {
      for (int n = 0; this.isRecording(); n++) {
        int numRead = audioRecord.read(buffer, 0, buffer.length);
        long sum = 0;
        for (int i = 0; i < numRead; i++) {
          sum += buffer[i] * buffer[i];
        }
        // root-mean-square (RMS) value of the audio data,
        // which is a measure of the average power of the signal
        double rms = numRead != 0 ? Math.sqrt(sum / numRead) : 0;

        float airPressure = 20.0f;
        // decibels
        double db = rms != 0 ? 20 * Math.log10(rms / airPressure) : 0;

        double[] data = this.getData();
        data[n % averageLength] = db;
        Log.d("Test", "start: " + db);
        Log.d("Test", "start: " + Arrays.toString(data));
      }
    }).start();
  }

  @Override
  public void stop() {
    this.setRecording(false);
    if (audioRecord != null) audioRecord.stop();
  }
}
