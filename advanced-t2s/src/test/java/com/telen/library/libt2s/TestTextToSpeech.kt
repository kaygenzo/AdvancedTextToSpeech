package com.telen.library.libt2s

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.HashMap

/**
 * Created by karim on 19/02/2018.
 */
class TestTextToSpeech(context: Context, listener: OnInitListener?, val textToSpeech: TextToSpeech) : TextToSpeech(context, listener) {
    private var utteranceProgressListener: UtteranceProgressListener? = null
    private var utteranceId: String? = null

    override fun speak(text: CharSequence?, queueMode: Int, params: Bundle?, utteranceId: String?): Int {
        this.utteranceId=utteranceId
        this.utteranceProgressListener?.onDone(this.utteranceId)
        textToSpeech.speak(text, queueMode, params, utteranceId)
        return SUCCESS
    }

    override fun speak(text: String?, queueMode: Int, params: HashMap<String, String>): Int {
        utteranceId = params[Engine.KEY_PARAM_UTTERANCE_ID]
        this.utteranceProgressListener?.onDone(this.utteranceId)
        textToSpeech.speak(text, queueMode, params)
        return SUCCESS
    }

    override fun stop(): Int {
        textToSpeech.stop()
        return SUCCESS
    }

    override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener?): Int {
        this.utteranceProgressListener=listener
        textToSpeech.setOnUtteranceProgressListener(listener)
        return SUCCESS
    }
}