package com.telen.library.libt2s;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Created by karim on 06/02/2018.
 */

public interface AdvancedTextToSpeech {
    Completable add(String textToRead);
    Completable add(int position, String textToRead);
    Completable add(List<String> textsToRead);
    Completable replace(int position, String textToRead);
    Completable replaceAll(List<String> newtextsToRead);
    Completable remove(int position);
    Completable clear();
    Completable init();
    Completable release();
    Completable next();
    Completable previous();
    Completable changeLanguage(String locale);
    Completable speedUp();
    Completable speedDown();
    Single<List<String>> getStack();
    Completable start();
    Completable stop();
    Single<TextToSpeech.EngineInfo> getEngines();
}
