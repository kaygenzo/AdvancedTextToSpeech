package com.telen.library.libt2s;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by karim on 03/02/2018.
 * TODO: add audio attributes such as canal, possibilities to speak over music, other ?
 */

public class AdvancedT2SImpl implements AdvancedTextToSpeech {

    private static final String TAG = "AdvancedT2SImpl";

    private Context mContext;
    private List<String> readingQueue = new ArrayList<>();
    private int index = 0;
    private float speechRate = 1f;
    private float pitch = 1f;
    private String locale;
    private String engine;

    public static class Builder {
        private Context mContext;
        private float speechRate=1f;
        private float pitch=1f;
        private String locale = "en_US";
        private String engine=null;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder speechRate(float speechRate) {
            this.speechRate = speechRate;
            return this;
        }

        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }

        public Builder engine(String engine) {
            this.engine = engine;
            return this;
        }

        public AdvancedT2SImpl build() {
            return new AdvancedT2SImpl(mContext, engine, locale, speechRate, pitch);
        }
    }

    private AdvancedT2SImpl(Context context, String engine, String locale, float speechRate, float pitch) {
        this.mContext=context;
        this.speechRate=speechRate;
        this.pitch=pitch;
        this.locale=locale;
        this.engine=engine;
    }

    @Override
    public Completable add(String textToRead) {
        return stop()
                .andThen(Completable.create(emitter -> {
                    readingQueue.add(textToRead);
                    emitter.onComplete();
                }));
    }

    @Override
    public Completable add(int position, String textToRead) {
        return stop()
                .andThen(Completable.create(emitter -> {
                    if(position>=0 && position <= readingQueue.size()) {
                        readingQueue.add(position, textToRead);
                        emitter.onComplete();
                    }
                    else
                        emitter.onError(new IndexOutOfBoundsException("size="+readingQueue.size()+" position="+position));
                }));
    }

    @Override
    public Completable add(List<String> textsToRead) {
        return stop()
                .andThen(Completable.create(emitter -> {
                    readingQueue.addAll(textsToRead);
                    emitter.onComplete();
                }));
    }

    @Override
    public Completable replace(int position, String textToRead) {
        return stop()
                .andThen(Completable.create(emitter -> {
                    if(position >= 0 && position < readingQueue.size()) {
                        readingQueue.set(position, textToRead);
                        emitter.onComplete();
                    }
                    else
                        emitter.onError(new IndexOutOfBoundsException("size="+readingQueue.size()+" position="+position));
                }));
    }

    @Override
    public Completable replaceAll(final List<String> newTextsToRead) {
        return stop()
                .andThen(clear())
                .andThen(Completable.create(emitter -> {
                    readingQueue.addAll(newTextsToRead);
                    emitter.onComplete();
                }));
    }

    @Override
    public Completable remove(int position) {
        return stop()
                .andThen(Completable.create(emitter -> {
                    if(position>=0 && position < readingQueue.size()) {
                        readingQueue.remove(position);
                        emitter.onComplete();
                    }
                    else
                        emitter.onError(new IndexOutOfBoundsException("size="+readingQueue.size()+" position="+position));
                }));
    }

    @Override
    public Completable clear() {
        return stop()
                .andThen(Completable.create(emitter -> {
                    readingQueue.clear();
                    index=0;
                    emitter.onComplete();
                }));
    }

    @Override
    public Completable init() {
        return release()
                .andThen(checkEngineNotNull())
                .andThen(Completable.create(emitter -> {
                    index=0;
                    TextToSpeech tts = new TextToSpeech(mContext, status -> {
                        if(status==TextToSpeech.SUCCESS) {
                            TextToSpeechInstance.getInstance().getTts().setSpeechRate(speechRate);
                            TextToSpeechInstance.getInstance().getTts().setPitch(pitch);

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                        .build();
                                TextToSpeechInstance.getInstance().getTts().setAudioAttributes(audioAttributes);
                            }

                            emitter.onComplete();
                        }
                        else
                            emitter.onError(new Exception("Error when initializing text to speech"));
                    }, engine);
                    TextToSpeechInstance.getInstance().setTts(tts);
                }))
                .andThen(changeLanguage(locale))
                .doOnError(throwable -> TextToSpeechInstance.getInstance().setTts(null));
    }

    @Override
    public Completable release() {
        return Completable.create(emitter -> {
            TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
            if(tts!=null) {
                tts.stop();
                tts.shutdown();
                TextToSpeechInstance.getInstance().setTts(null);
                engine=null;
            }
            emitter.onComplete();
        });
    }

    @Override
    public Completable next() {
         return stop().andThen(Single.create((SingleOnSubscribe<String>) emitter -> {
             if(!readingQueue.isEmpty()) {
                 nextIndex();
                 if(index>= readingQueue.size()) {
                     index= readingQueue.size()-1;
                 }
                 String text = readingQueue.get(index);
                 emitter.onSuccess(text);
             }
             else
                 emitter.onError(new Exception("Queue empty"));
        })).flatMapCompletable(text -> speak(text));
    }

    @Override
    public Completable previous() {
        return stop().andThen(Single.create((SingleOnSubscribe<String>) emitter -> {
            if(!readingQueue.isEmpty()) {
                previousIndex();
                if(index<0) {
                    index=0;
                }
                String text = readingQueue.get(index);
                emitter.onSuccess(text);
            }
            else
                emitter.onError(new Exception("Queue empty"));
        })).flatMapCompletable(text -> speak(text));
    }

    @Override
    public Completable changeLanguage(String locale) {
        return stop().andThen(Completable.create(emitter -> {
            this.locale=locale;
            final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
            if(tts!=null) {
                final String[] splittedLocale = locale.split("_");
                int result = tts.setLanguage(getLocale());
                switch (result) {
                    case TextToSpeech.LANG_AVAILABLE:
                        if(splittedLocale.length==1)
                            emitter.onComplete();
                        else
                            emitter.onError(new Exception("Country and/or variant not available"));
                        break;
                    case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                        if(splittedLocale.length==2)
                            emitter.onComplete();
                        else
                            emitter.onError(new Exception("Variant not available"));
                        break;
                    case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                        emitter.onComplete();
                        break;
                    case TextToSpeech.LANG_MISSING_DATA:
                        emitter.onError(new Exception("Missing language data"));
                        break;
                    case TextToSpeech.LANG_NOT_SUPPORTED:
                        emitter.onError(new Exception("Language not supported"));
                        break;
                    default:
                        emitter.onError(new Exception("Unknown error during change language"));
                }
            }
            else
                emitter.onError(new Exception("TTS not initialized"));
        }));
    }

    @Override
    public Completable speedUp() {
        return stop()
                .andThen(Completable.create(emitter -> {
                    final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
                    if(tts!=null) {
                        speechRate+=0.1f;
                        int result = tts.setSpeechRate(speechRate);
                        if(result==TextToSpeech.SUCCESS)
                            emitter.onComplete();
                        else
                            emitter.onError(new Exception("Cannot change speech rate"));
                    }
                    else
                        emitter.onError(new Exception("TTS not initialized"));
                }));
    }

    @Override
    public Completable speedDown() {
        return stop()
                .andThen(Completable.create(emitter -> {
                    final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
                    if(tts!=null) {
                        speechRate-=0.1f;
                        int result = tts.setSpeechRate(speechRate);
                        if(result==TextToSpeech.SUCCESS)
                            emitter.onComplete();
                        else
                            emitter.onError(new Exception("Cannot change speech rate"));
                    }
                    else
                        emitter.onError(new Exception("TTS not initialized"));
                }));
    }

    @Override
    public Single<List<String>> getStack() {
        return Single.just(readingQueue);
    }

    @Override
    public Completable start() {
        return stop().andThen(playAll());
    }

    @Override
    public Completable stop() {
        return Completable.create(emitter -> {
            final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
            int result = TextToSpeech.SUCCESS;
            if(tts!=null) {
                result = tts.stop();
            }
            if(result==TextToSpeech.SUCCESS)
                emitter.onComplete();
            else
                emitter.onError(new Exception("Problem occurred when trying to stop the tts"));
        });
    }

    /**
     * Need to be subscribed from the main thread
     * @return
     */
    @Override
    public Single<TextToSpeech.EngineInfo> getEngines() {
        return Single.create(emitter -> {
            TextToSpeech tts = new TextToSpeech(mContext,null);
            try {
                final List<TextToSpeech.EngineInfo> engines = tts.getEngines();
                final String[] engineNames = new String[engines.size()];
                for (int i = 0; i < engines.size(); i++) {
                    TextToSpeech.EngineInfo engine = engines.get(i);
                    engineNames[i] = engine.name;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setItems(engineNames, (dialog, which) -> emitter.onSuccess(engines.get(which)))
                        .setOnCancelListener(dialog -> emitter.onError(new Exception("operation canceled")));
                AlertDialog dialog = builder.create();
                dialog.show();
            } finally {
                tts.shutdown();
            }
        });
    }

    private void nextIndex() {
        index = Math.max(0,Math.min(index+1, readingQueue.size()-1));
        Log.d(TAG,"index="+index);
    }

    private void previousIndex() {
        index = Math.max(0,Math.min(index-1, readingQueue.size()-1));
    }

    private Completable speak(String text) {
        return Completable.create(emitter -> {
            final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
            final long currentTimeStamp = System.currentTimeMillis();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    if(String.valueOf(currentTimeStamp).equals(utteranceId)) {
                        emitter.onComplete();
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    if(String.valueOf(currentTimeStamp).equals(utteranceId))
                        emitter.onError(new Exception("Problem occurred when trying to read text"));
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, String.valueOf(currentTimeStamp));
            }
            else {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(currentTimeStamp));
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
            }
        });
    }

    private Locale getLocale() {
        String[] splittedLocale = locale.split("_");
        Locale language;
        switch (splittedLocale.length) {
            case 2:
                language = new Locale(splittedLocale[0], splittedLocale[1]);
                break;
            case 3:
                language = new Locale(splittedLocale[0], splittedLocale[1], splittedLocale[2]);
                break;
            default:
                language = new Locale(locale);
        }

        return language;
    }

    private Completable checkEngineNotNull() {
        return Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            if(TextUtils.isEmpty(engine))
                emitter.onSuccess(true);
            else
                emitter.onSuccess(false);
        }).flatMap(isNull -> {
            if(isNull)
                return getEngines().map(engineInfo -> engineInfo.name);
            else
                return Single.just(engine);
        }).flatMapCompletable(engine -> {
            this.engine=engine;
            return Completable.complete();
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    private Completable playAll() {
        return Single.create((SingleOnSubscribe<List<Completable>>) emitter -> {
            if(!readingQueue.isEmpty()) {
                List<Completable> stack = new ArrayList<>();
                for (int i = index; i < readingQueue.size(); i++) {
                    String text = readingQueue.get(i);
                    stack.add(speak(text).doOnComplete(() -> nextIndex()));
                }
                emitter.onSuccess(stack);
            }
            else
                emitter.onError(new Exception("Queue empty"));
        }).flatMapCompletable(completables -> Completable.concat(completables));
    }
}
