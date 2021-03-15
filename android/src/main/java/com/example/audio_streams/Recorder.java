package com.example.audio_streams;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaSync;
import android.media.MediaSyncEvent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import io.flutter.plugin.common.EventChannel;

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
                AudioFormat.ENCODING_PCM_8BIT,
                BUFFER_SIZE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void start(final EventChannel.EventSink _sink) {

        _recorder.startRecording();
        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Log.d("Audio Streaming", "Starting Recording ...");
                while(true) {
                    if(!_recording) {
                        break;
                    }
                    short buffer[] = new short[BUFFER_SIZE];
                    _recorder.read(buffer, 0, BUFFER_SIZE);
                    final int converted[] = new int[BUFFER_SIZE];
                    for(int i = 0; i<buffer.length;i++) {
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
