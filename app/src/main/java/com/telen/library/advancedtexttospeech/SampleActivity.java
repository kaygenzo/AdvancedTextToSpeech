package com.telen.library.advancedtexttospeech;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.telen.library.libt2s.AdvancedT2SImpl;
import com.telen.library.libt2s.AdvancedTextToSpeech;
import com.telen.library.libt2s.KAdvancedT2SImpl;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SampleActivity extends AppCompatActivity {

    private static final String TAG = "SampleActivity";

    private ImageButton start;
    private ImageButton previous;
    private ImageButton next;
    private ImageButton pause;
    private ImageButton plus;
    private ImageButton minus;
    private Button mInitTts;
    private Button chooseEngine;

    private Button french;
    private Button british;
    private Button american;
    private Button spanish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start=(ImageButton)findViewById(R.id.start);
        pause=(ImageButton)findViewById(R.id.pause);
        mInitTts=(Button)findViewById(R.id.init_tts);
        previous=(ImageButton)findViewById(R.id.previous);
        next=(ImageButton)findViewById(R.id.next);
        plus=(ImageButton)findViewById(R.id.plus);
        minus=(ImageButton)findViewById(R.id.minus);
        chooseEngine=(Button)findViewById(R.id.request_engines);

        french=(Button)findViewById(R.id.french);
        british=(Button)findViewById(R.id.british);
        american=(Button)findViewById(R.id.american);
        spanish=(Button)findViewById(R.id.spanish);

        KAdvancedT2SImpl.Builder kBuilder = new KAdvancedT2SImpl.Builder(this)
                .speechRate(1f)
                .pitch(1f)
                .locale("es_ES")
                .engine("com.google.android.tts")
                ;

        final AdvancedTextToSpeech kAdvancedTts = kBuilder.build();

        AdvancedT2SImpl.Builder builder = new AdvancedT2SImpl.Builder(SampleActivity.this)
                .speechRate(1f)
                .pitch(1f)
                .locale("es_ES")
                .engine("com.google.android.tts")
                ;
        final AdvancedTextToSpeech advancedTts = builder.build();

        mInitTts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<String> texts = new ArrayList<>();
                texts.add("Phrase numero une");
                texts.add("Phrase numero deux");
                texts.add("Phrase numero trois");
                texts.add("Phrase numero quatre");
                texts.add("Phrase numero cinq");
                texts.add("Phrase numero Six");

                advancedTts.init()
                        .andThen(advancedTts.replaceAll(texts))
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
//                                Snackbar.make(mainContent, "initialized", Snackbar.LENGTH_SHORT).show();
                                Log.d(TAG,"init::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
//                                Snackbar.make(mainContent, "onError", Snackbar.LENGTH_SHORT).show();
                                Log.d(TAG,"init::onError error="+e.getMessage());
                            }
                        });
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.start()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"start::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"start::onError error="+e.getMessage());
                            }
                        });

            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.stop()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"stop::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"stop::onError error="+e.getMessage());
                            }
                        });
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.previous()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"previous::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"previous::onError error="+e.getMessage());
                            }
                        });
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.next()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"next::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"next::onError error="+e.getMessage());
                            }
                        });
            }
        });

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.speedUp()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"next::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"next::onError error="+e.getMessage());
                            }
                        });
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.speedDown()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"next::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"next::onError error="+e.getMessage());
                            }
                        });
            }
        });

        chooseEngine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.getEngines()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<TextToSpeech.EngineInfo>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onSuccess(@NonNull TextToSpeech.EngineInfo engineInfo) {
                                Toast.makeText(SampleActivity.this, "Engine chosen: "+engineInfo.name, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.e(TAG,"",e);
                                Toast.makeText(SampleActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        french.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.changeLanguage("fr_FR")
                        .andThen(advancedTts.start())
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"changeLanguage::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"changeLanguage::onError error="+e.getMessage());
                            }
                        });
            }
        });

        british.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.changeLanguage("en_GB")
                        .andThen(advancedTts.start())
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"changeLanguage::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"changeLanguage::onError error="+e.getMessage());
                            }
                        });
            }
        });


        american.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.changeLanguage("en_US")
                        .andThen(advancedTts.start())
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"changeLanguage::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"changeLanguage::onError error="+e.getMessage());
                            }
                        });
            }
        });

        spanish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advancedTts.changeLanguage("es_ES")
                        .andThen(advancedTts.start())
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG,"changeLanguage::onComplete");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG,"changeLanguage::onError error="+e.getMessage());
                            }
                        });
            }
        });

    }
}
