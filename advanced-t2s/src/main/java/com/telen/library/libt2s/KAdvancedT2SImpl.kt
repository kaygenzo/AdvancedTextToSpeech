package com.telen.library.libt2s

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by karim on 18/02/2018.
 */



class KAdvancedT2SImpl(var context: Context, var engine: String?, var locale: String, var speechRate: Float, var pitch: Float): AdvancedTextToSpeech {

    private val TAG = "AdvancedT2SImpl"
    private val readingQueue = ArrayList<String>()
    private var index = 0

    class Builder(var context: Context, var speechRate: Float, var pitch: Float, var locale: String, var engine: String?) {

        constructor(context: Context): this(context, 1f, 1f, "en_US", null)

        fun speechRate(speechRate: Float) : Builder {
            this.speechRate=speechRate
            return this
        }
        fun pitch(pitch: Float): Builder {
            this.pitch=pitch
            return this
        }
        fun locale(locale:String): Builder {
            this.locale=locale
            return this
        }
        fun engine(engine: String): Builder {
            this.engine=engine
            return this
        }
        fun build(): AdvancedTextToSpeech {
            return KAdvancedT2SImpl(context, engine, locale, speechRate, pitch)
        }
    }

    override fun add(textToRead: String): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    readingQueue.add(textToRead)
                    emitter.onComplete()
                })
    }

    override fun add(position: Int, textToRead: String): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    if (position >= 0 && position <= readingQueue.size) {
                        readingQueue.add(position, textToRead)
                        emitter.onComplete()
                    } else
                        emitter.onError(IndexOutOfBoundsException("size=" + readingQueue.size + " position=" + position))
                })
    }

    override fun add(textsToRead: MutableList<String>): Completable {
        return stop()
                .andThen(Completable.create{ emitter ->
                    readingQueue.addAll(textsToRead)
                    emitter.onComplete()
                })
    }

    override fun replace(position: Int, textToRead: String): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    if(position >= 0 && position < readingQueue.size) {
                        readingQueue[position] = textToRead
                        emitter.onComplete()
                    }
                    else
                        emitter.onError(IndexOutOfBoundsException("size="+readingQueue.size+" position="+position))
                })
    }

    override fun replaceAll(newtextsToRead: MutableList<String>): Completable {
        return stop()
                .andThen(clear())
                .andThen(Completable.create { emitter ->
                    readingQueue.addAll(newtextsToRead)
                    emitter.onComplete()
                })
    }

    override fun remove(position: Int): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    if(position>=0 && position<readingQueue.size) {
                        readingQueue.removeAt(position)
                        emitter.onComplete()
                    }
                    else
                        emitter.onError(IndexOutOfBoundsException("size="+readingQueue.size+" position="+position))
                })
    }

    override fun clear(): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    readingQueue.clear()
                    index=0
                    emitter.onComplete()
                })
    }

    override fun init(): Completable {
        return release().andThen(checkEngineNotNull())
                .andThen(Completable.create { emitter ->
                    index=0
                    val tts = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
                        if(status == TextToSpeech.SUCCESS) {
                            TextToSpeechInstance.getInstance().tts.setSpeechRate(speechRate)
                            TextToSpeechInstance.getInstance().tts.setPitch(pitch)
                            val audioAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                        .build()
                            } else {
                                TODO("VERSION.SDK_INT < LOLLIPOP")
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                TextToSpeechInstance.getInstance().tts.setAudioAttributes(audioAttributes)
                            }

                            emitter.onComplete()
                        }
                        else
                            emitter.onError(Exception("Error when initializing text to speech"))
                    }, engine)
                    TextToSpeechInstance.getInstance().tts = tts
                })
                .andThen(changeLanguage(locale))
                .doOnError{ _ -> TextToSpeechInstance.getInstance().tts = null }
    }

    override fun release(): Completable {
        return Completable.create { emitter ->
            val tts: TextToSpeech? = TextToSpeechInstance.getInstance().tts
            if(tts!=null) {
                tts.stop()
                tts.shutdown()
                TextToSpeechInstance.getInstance().tts = null
                engine=null
            }
            emitter.onComplete()
        }
    }

    override fun next(): Completable {
        return stop().andThen(Single.create ( SingleOnSubscribe<String> { emitter ->
            if(!readingQueue.isEmpty()) {
                nextIndex()
                if(index>=readingQueue.size)
                    index=readingQueue.size-1
                var text: String = readingQueue[index]
                emitter.onSuccess(text)
            }
            else
                emitter.onError(Exception("Queue empty"))
        }).flatMapCompletable { text -> speak(text) })
    }

    override fun previous(): Completable {
        return stop().andThen(Single.create (SingleOnSubscribe<String> { emitter ->
            if(!readingQueue.isEmpty()) {
                previousIndex()
                if (index < 0) {
                    index = 0
                }
                var text = readingQueue[index]
                emitter.onSuccess(text)
            }
            else
                emitter.onError(Exception("Queue empty"))
        }).flatMapCompletable { text -> speak(text) })
    }

    override fun changeLanguage(locale: String): Completable {

        return stop().andThen(Completable.create { emitter ->
            this.locale=locale
            val tts: TextToSpeech? = TextToSpeechInstance.getInstance().tts
            if(tts!=null) {
                val splittedLocale = locale.split("_")
                val result = tts.setLanguage(getLocale())
                when(result) {
                    TextToSpeech.LANG_AVAILABLE -> {
                        if(splittedLocale.size==1)
                            emitter.onComplete()
                        else
                            emitter.onError(Exception("Country and/or variant not available"))
                    }
                    TextToSpeech.LANG_COUNTRY_AVAILABLE -> {
                        if(splittedLocale.size==2)
                            emitter.onComplete()
                        else
                            emitter.onError(Exception("Variant not available"))
                    }
                    TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> emitter.onComplete()
                    TextToSpeech.LANG_MISSING_DATA -> emitter.onError(Exception("Missing language data"))
                    TextToSpeech.LANG_NOT_SUPPORTED -> emitter.onError(Exception("Language not supported"))
                    else -> emitter.onError(Exception("Unknown error during change language"))
                }
            }
            else
                emitter.onError(Exception("TTS not initialized"))
        })
    }

    override fun speedUp(): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    val tts: TextToSpeech? = TextToSpeechInstance.getInstance().tts
                    if(tts!=null) {
                        speechRate+=0.1f
                        val result = tts.setSpeechRate(speechRate)
                        if(result==TextToSpeech.SUCCESS)
                            emitter.onComplete()
                        else
                            emitter.onError(Exception("Cannot change speech rate"))
                    }
                    else
                        emitter.onError(Exception("TTS not initialized"))
                })
    }

    override fun speedDown(): Completable {
        return stop()
                .andThen(Completable.create { emitter ->
                    val tts: TextToSpeech? = TextToSpeechInstance.getInstance().tts
                    if(tts!=null) {
                        speechRate-=0.1f
                        val result = tts.setSpeechRate(speechRate)
                        if(result==TextToSpeech.SUCCESS)
                            emitter.onComplete()
                        else
                            emitter.onError(Exception("Cannot change speech rate"))
                    }
                    else
                        emitter.onError(Exception("TTS not initialized"))
                })
    }

    override fun getStack(): Single<MutableList<String>> {
        return Single.just(readingQueue)
    }

    override fun start(): Completable {
        return stop().andThen(playAll())
    }

    override fun stop(): Completable {
        return Completable.create { emitter ->
            val tts: TextToSpeech? = TextToSpeechInstance.getInstance().tts
            var result: Int = TextToSpeech.SUCCESS
            if(tts!=null) {
                result = tts.stop()
            }
            if(result==TextToSpeech.SUCCESS)
                emitter.onComplete()
            else
                emitter.onError(Exception("Problem occurred when trying to stop the tts"))
        }
    }

    override fun getEngines(): Single<TextToSpeech.EngineInfo> {

        return Single.create({ emitter ->
            val tts = TextToSpeech(context, null)
            try {
                val engines = tts.engines
                val engineNames = Array(engines.size, {""})
                for (i in 0..engines.size) {
                    val engine = engines[i]
                    engineNames[i] = engine.name
                }
                val builder = AlertDialog.Builder(context)
                        .setItems(engineNames, object: DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                emitter.onSuccess(engines[which])
                            }
                        })
                        .setOnCancelListener { dialog ->
                            emitter.onError(Exception("operation canceled"))
                        }
                val dialog: Dialog = builder.create()
                dialog.show()
            } finally {
                tts.shutdown()
            }
        })
    }

    private fun nextIndex() {
        index = Math.max(0,Math.min(index+1, readingQueue.size-1))
        Log.d(TAG,"index="+index)
    }

    private fun previousIndex() {
        index = Math.max(0,Math.min(index-1, readingQueue.size-1))
    }


    private fun getLocale(): Locale {
        val splittedLocale = locale.split("_")
        val language = when(splittedLocale.size) {
            2 -> Locale(splittedLocale[0], splittedLocale[1])
            3 -> Locale(splittedLocale[0], splittedLocale[1], splittedLocale[2])
            else -> Locale(locale)
        }

        return language
    }

    private fun speak( text: String): Completable {
        return Completable.create { emitter ->
            val tts: TextToSpeech? = TextToSpeechInstance.getInstance().tts
            val currentTimeStamp: Long =  System.currentTimeMillis()
            tts?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                override fun onError(utteranceId: String?) {
                }

                override fun onDone(utteranceId: String?) {
                    if(currentTimeStamp.toString().equals(utteranceId))
                        emitter.onComplete()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if(currentTimeStamp.toString().equals(utteranceId))
                        emitter.onError(Exception("Problem occurred when trying to read text"))
                }

                override fun onStart(utteranceId: String?) {
                }

            })
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentTimeStamp.toString())
            else {
                val map: HashMap<String, String> = HashMap()
                map[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = currentTimeStamp.toString()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, map)
            }
        }
    }

    private fun playAll(): Completable {
        return Single.create(SingleOnSubscribe<List<Completable>> { emitter ->
            if(!readingQueue.isEmpty()) {
                val stack: MutableList<Completable> = ArrayList()
                for (i in index..readingQueue.size-1) {
                    val text: String = readingQueue[i]
                    stack.add(speak(text).doOnComplete({ nextIndex() }))
                }
                emitter.onSuccess(stack)
            }
            else
                emitter.onError(Exception("Queue empty"))
        }).flatMapCompletable { completables -> Completable.concat(completables) }
    }

    private fun checkEngineNotNull(): Completable {

        return Single.create( SingleOnSubscribe <Boolean> { emitter ->
            if(TextUtils.isEmpty(engine))
                emitter.onSuccess(true)
            else
                emitter.onSuccess(false)
        }).flatMap<String> ({ isNull ->
            if(isNull)
                engines.map { engineInfo -> engineInfo.name }
            else
                Single.just(engine)
        }).flatMapCompletable ({ engine ->
            this.engine=engine
            Completable.complete()
        }).subscribeOn(AndroidSchedulers.mainThread())
    }
}