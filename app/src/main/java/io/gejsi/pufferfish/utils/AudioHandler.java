package io.gejsi.pufferfish.utils;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;

import io.gejsi.pufferfish.controllers.MapsActivity;

public class AudioHandler {
  private boolean audioPermissionGranted;
  private boolean isRecording = false;
  private AudioRecord audioRecord;
  // measurement data
  double[] data;

  public void setAudioPermissionGranted(boolean locationPermissionGranted) {
    this.audioPermissionGranted = locationPermissionGranted;
  }

  private MapsActivity activity;
  private GoogleMap map;

  public AudioHandler(MapsActivity activity, GoogleMap googleMap) {
    this.activity = activity;
    this.map = googleMap;
  }

  @SuppressLint("MissingPermission")
  public void start() {
    if (!audioPermissionGranted) {
      return;
    }

    int sampleRate = 44100;
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channelConfig, audioFormat, minBufferSize);
    audioRecord.startRecording();
    isRecording = true;

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    String averagePref = sharedPreferences.getString("average", "");
    int averageLength = averagePref.length() == 0 ? 10 : Integer.parseInt(averagePref);

    data = new double[averageLength];
    // 16-bit audio data
    short[] buffer = new short[minBufferSize];

    new Thread(() -> {
      for (int n = 0; isRecording; n++) {
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

        data[n % averageLength] = db;
      }
    }).start();
  }

  public void stop() {
    isRecording = false;
    if (audioRecord != null) audioRecord.stop();
  }

  public double getData() {
    double sum = 0;

    for (int i = 0; i < data.length; i++) {
      if (data[i] != 0) sum += data[i];
    }

    return sum / data.length;
  }
}
