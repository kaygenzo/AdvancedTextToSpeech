package com.telen.library.libt2s;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Created by karim on 03/02/2018.
 */

public class AdvancedT2SImpl implements AdvancedTextToSpeech {

    private static final String TAG = "AdvancedT2SImpl";

    private Context mContext;
    private List<String> readingQueue = new ArrayList<>();
    private int index = 0;
    private float speechRate = 1f;

    public static AdvancedTextToSpeech getInstance(Context context) {
        return new AdvancedT2SImpl(context);
    }

    private AdvancedT2SImpl(Context context) {
        this.mContext=context;
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
    public Completable init(String engine) {
        return release()
                .andThen(Completable.create(emitter -> {
                    index=0;
                    TextToSpeech tts = new TextToSpeech(mContext, status -> {
                        if(status==TextToSpeech.SUCCESS)
                            emitter.onComplete();
                        else
                            emitter.onError(new Exception("Error when initializing text to speech"));
                    }, engine);
                    TextToSpeechInstance.getInstance().setTts(tts);
                }).doOnError(throwable -> TextToSpeechInstance.getInstance().setTts(null)));
    }

    @Override
    public Completable release() {
        return Completable.create(emitter -> {
            TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
            if(tts!=null) {
                tts.stop();
                tts.shutdown();
                TextToSpeechInstance.getInstance().setTts(null);
            }
            emitter.onComplete();
        });
    }

    @Override
    public Completable next() {
        if(!readingQueue.isEmpty()) {
            nextIndex();
            if(index>= readingQueue.size()) {
                index= readingQueue.size()-1;
            }
            String text = readingQueue.get(index);
            return stop().andThen(speak(text));
        }
        else
            return Completable.error(new Exception("Queue empty"));
    }

    @Override
    public Completable previous() {
        if(!readingQueue.isEmpty()) {
            previousIndex();
            if(index<0) {
                index=0;
            }
            String text = readingQueue.get(index);
            return stop().andThen(speak(text));
        }
        else
            return Completable.error(new Exception("Queue empty"));
    }

    @Override
    public Completable changeLanguage(String locale) {
        return Completable.create(emitter -> {
            final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
            if(tts!=null) {
                String[] splittedLocale = locale.split("_");
                Locale language = null;
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

                int result = tts.setLanguage(language);
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
        });
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
                }))
                .andThen(start());
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
                }))
                .andThen(start());
    }

    @Override
    public Single<List<String>> getStack() {
        return Single.just(readingQueue);
    }

    @Override
    public Completable start() {
        if(!readingQueue.isEmpty()) {
            List<Completable> stack = new ArrayList<>();
            for (int i = index; i < readingQueue.size(); i++) {
                String text = readingQueue.get(i);
                stack.add(speak(text).andThen(Completable.create(emitter -> {
                    nextIndex();
                    emitter.onComplete();
                })));
            }

            Completable flow = Completable.concat(stack);

            return stop().andThen(flow);
        }
        else
            return Completable.error(new Exception("Queue empty"));
    }

    @Override
    public Completable stop() {
        final TextToSpeech tts = TextToSpeechInstance.getInstance().getTts();
        if(tts!=null) {
            tts.stop();
        }
        return Completable.complete();
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
}
