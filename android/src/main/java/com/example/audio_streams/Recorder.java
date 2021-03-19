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

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import io.flutter.plugin.common.EventChannel;

interface Callback {
    void callback(short buffer[]);
}

public class Recorder {
    AudioRecord _recorder;
    boolean _recording = false;
     AudioDispatcher dispatcher;
    static int BUFFER_SIZE = 2048;
    Handler _mainThread = new Handler(Looper.getMainLooper());
    public Recorder() {
        _recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100,
                1,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void start(Boolean denoiseEnabled,final EventChannel.EventSink _sink) {
        for(int i =0;i<3;i++) {
            if(_recorder.getState() != AudioRecord.STATE_INITIALIZED ) {
                try {
                    Thread.sleep(500, 0);
                }catch(Exception e) {

                }
            }
        }
        _recorder.startRecording();
        AudioProcessor highPass = new HighPass(1100,44100);
        AudioProcessor lowPass = new HighPass(9870,44100);
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(44100, 16,1, true, false);
        TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(_recorder, format);

        dispatcher = new AudioDispatcher(audioStream,BUFFER_SIZE,BUFFER_SIZE);
        dispatcher.addAudioProcessor(highPass);
        dispatcher.addAudioProcessor(lowPass);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float buffer[] =  audioEvent.getFloatBuffer();
                final int converted[] = new int[BUFFER_SIZE];
                for(int i = 0; i<buffer.length;i++) {
                    converted[i] = (int) (buffer[i] * 32768);
                }
                _mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        _sink.success(converted);
                    }});
                return true;
            }
            @Override
            public void processingFinished() {
              //  _recording = false;
              //  _recorder.stop();
            }
        });

        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Log.d("Audio Streaming", "Starting Recording ...");
                while(true) {
                    dispatcher.run();
                }
                //Log.d("Audio Streaming", "Stopped Recording ...");
              //  dispatcher.stop();
            }
        });
        _recording = true;
        streamThread.start();
    }

    void stop() {
        if(!_recording) {
            return;
        }
        _recording = false;
        _recorder.stop();
    }
}
