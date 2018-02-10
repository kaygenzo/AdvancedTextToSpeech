package com.telen.library.libt2s;

import android.speech.tts.TextToSpeech;

/**
 * Created by karim on 06/02/2018.
 * Singleton
 */
public class TextToSpeechInstance {
    private static TextToSpeechInstance mInstance;
    private TextToSpeech tts;

    private TextToSpeechInstance() {
    }

    public static TextToSpeechInstance getInstance() {
        if(mInstance==null)
            mInstance=new TextToSpeechInstance();
        return mInstance;
    }

    public void setTts(TextToSpeech tts) {
        this.tts = tts;
    }

    public TextToSpeech getTts() {
        return tts;
    }
}
