package com.telen.library.advancedtexttospeech;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.telen.library.libt2s.AdvancedT2SImpl;
import com.telen.library.libt2s.AdvancedTextToSpeech;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
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



        final AdvancedTextToSpeech advancedTts = AdvancedT2SImpl.getInstance(SampleActivity.this);

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

                advancedTts.init("voxygen.tts.voixelectra")
                        .andThen(advancedTts.changeLanguage("fr"))
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

    }
}
