package com.example.audio_streams;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaSync;
import android.media.MediaSyncEvent;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import io.flutter.plugin.common.EventChannel;

import static android.media.AudioRecord.READ_BLOCKING;

interface Callback {
    void callback(short buffer[]);
}

public class Recorder {
    AudioRecord _recorder;
    boolean _recording = false;
    static int BUFFER_SIZE = 2048 * 2;
    Handler _mainThread = new Handler(Looper.getMainLooper());
    public Recorder() {
        _recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100,
                1,
                AudioFormat.ENCODING_PCM_FLOAT,
                BUFFER_SIZE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void start(Boolean denoiseEnabled,final EventChannel.EventSink _sink) {

        _recorder.startRecording();

        if(denoiseEnabled) {
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor effect = NoiseSuppressor.create(_recorder.getAudioSessionId());
                effect.setEnabled(true);
                Log.i("Audio Streaming","Opened Noise Suppressor...");
            }

            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler effect = AcousticEchoCanceler.create(_recorder.getAudioSessionId());
                effect.setEnabled(true);
                Log.i("Audio Streaming","Opened Acoustic Echo Canceler...");
            }
        }
        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Log.d("Audio Streaming", "Starting Recording ...");
                while(true) {
                    if(!_recording) {
                        break;
                    }
                     float buffer[] = new float[BUFFER_SIZE];
                     int readed = _recorder.read(buffer, 0, BUFFER_SIZE, READ_BLOCKING);
                     if(readed < 1) {
                         continue;
                     }
                    final double converted[] = new double[readed];
                    for(int i = 0; i<readed;i++) {
                        converted[i] = buffer[i];
                    }
                    _mainThread.post(new Runnable() {
                        @Override
                        public void run() {
                            _sink.success(converted);
                        }
                    });
                }
                Log.d("Audio Streaming", "Stopped Recording ...");
            }
        });
        _recording = true;
        streamThread.start();
    }

    void stop() {
        _recorder.stop();
        _recording = false;
    }


}
