package com.twinbridgecity.streetlegends;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;

import java.util.Locale;

/** Lets the game's announcer speak with the phone's built-in voice. */
public class TtsBridge {
    private TextToSpeech tts;
    private volatile boolean ready = false;

    public TtsBridge(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                ready = true;
            }
        });
    }

    @JavascriptInterface
    public void speak(String text, int queue, float pitch, float rate) {
        if (!ready || text == null) return;
        tts.setPitch(pitch);
        tts.setSpeechRate(rate);
        tts.speak(text, queue == 1 ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH, null, "tbc");
    }

    @JavascriptInterface
    public void stop() {
        if (ready) tts.stop();
    }

    public void shutdown() {
        tts.shutdown();
    }
}
