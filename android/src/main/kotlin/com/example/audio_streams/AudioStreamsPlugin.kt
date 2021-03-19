package com.example.audio_streams

import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import com.example.audio_streams.Recorder;
import io.flutter.plugin.common.*

class AudioStreamsPlugin: MethodCallHandler {
  private val _recorder = Recorder();
  private  val   _channel:EventChannel;
  private var _denoise = false;
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "audio_streams")
      channel.setMethodCallHandler(AudioStreamsPlugin(registrar))
    }
  }

  constructor(registrar: Registrar) {
    _channel = EventChannel(registrar.messenger(),"audio");
    _channel.setStreamHandler(object:EventChannel.StreamHandler{
      @RequiresApi(Build.VERSION_CODES.M)
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
          _recorder.start(_denoise,events);
      }
      override fun onCancel(arguments: Any?) {
        _recorder.stop();
      }
    })
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if(call.method == "initialize") {
         val opt = call.argument<Boolean>("denoise_enabled");
          if(opt !== null) {
              this._denoise = opt;
          }
        result.success(null);
    } else if(call.method == "stop") {
            _recorder.stop();
    } else {
      result.notImplemented()
    }
  }
}
