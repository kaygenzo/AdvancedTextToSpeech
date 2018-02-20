package com.telen.library.libt2s;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by karim on 10/02/2018.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextToSpeechInstance.class, Log.class})
public class KAdvancedT2SImplTests {

    private AdvancedTextToSpeech advancedTts;
    @Mock Context mContext;
    @Mock TextToSpeechInstance mTTSInstance;
    @Mock TextToSpeech tts;

    private String text1 = "This is an example chain 1";
    private String text2 = "This is an example chain 2";
    private String text3 = "This is an example chain 3";
    private TestObserver testObserver;
    private List<String> list = new ArrayList<>();
    private TestTextToSpeech testTts;

    @BeforeClass
    public static void setupClass() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> Schedulers.trampoline());
    }

    @AfterClass
    public static void tearDownClass() {
        RxAndroidPlugins.reset();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        KAdvancedT2SImpl.Builder builder = new KAdvancedT2SImpl.Builder(mContext);
        advancedTts = builder.build();
        testObserver = new TestObserver();
        list.add(text1);
        list.add(text2);
        list.add(text3);
        when(mTTSInstance.getTts()).thenReturn(tts);
        mockStatic(TextToSpeechInstance.class);
        mockStatic(Log.class);
        when(TextToSpeechInstance.getInstance()).thenReturn(mTTSInstance);
        testTts = new TestTextToSpeech(mContext, null, tts);
    }

    @After
    public void release() {
    }

    @Test
    public void shouldAddOneString() {
        // add text1 to stack: {text1}
        advancedTts.getStack().flatMapCompletable(strings -> {
            assertEquals(strings.size(),0);
            return advancedTts.add(text1);
        })
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),1);
                    //add text2 to beginning of stack: {text2, text1}
                    return advancedTts.add(0,text2);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),2);
                    assertArrayEquals(strings.toArray(),new String[] {text2, text1});
                    //add text3 in the middle: {text2, text3, text1}
                    return advancedTts.add(1,text3);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),3);
                    assertArrayEquals(strings.toArray(),new String[] {text2, text3, text1});
                    return advancedTts.add(3,text1);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),4);
                    assertArrayEquals(strings.toArray(),new String[] {text2, text3, text1, text1});
                    return Completable.complete();
                })).subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldNotAddStringAtBadIndex() {
        advancedTts.add(text1)
                .andThen(advancedTts.add(2,text2))
                .subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertError(IndexOutOfBoundsException.class);
    }

    @Test
    public void shouldAddManyStrings() {
        advancedTts.getStack().flatMapCompletable(strings -> {
            assertEquals(strings.size(),0);
            return advancedTts.add(list);
        })
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),3);
                    assertArrayEquals(strings.toArray(),new String[] {text1, text2, text3});
                    return advancedTts.add(text1);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),4);
                    assertArrayEquals(strings.toArray(),new String[] {text1, text2, text3, text1});
                    return advancedTts.add(list);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),7);
                    assertArrayEquals(strings.toArray(),new String[] {text1, text2, text3, text1, text1, text2, text3});
                    return Completable.complete();
                })).subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldRemoveTextString() {
        advancedTts.add(list)
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),3);
                    assertArrayEquals(strings.toArray(),new String[] {text1, text2, text3});
                    return advancedTts.remove(1);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),2);
                    assertArrayEquals(strings.toArray(),new String[] {text1, text3});
                    return advancedTts.remove(0);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),1);
                    assertArrayEquals(strings.toArray(),new String[] {text3});
                    return advancedTts.remove(0);
                }))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),0);
                    return Completable.complete();
                })).subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldNotRemoveTextStringAtWrongIndex() {
        advancedTts.add(list)
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),3);
                    assertArrayEquals(strings.toArray(),new String[] {text1, text2, text3});
                    return advancedTts.remove(3);
                })).subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertError(IndexOutOfBoundsException.class);
    }

    @Test
    public void shouldNotRemoveTextStringWhenQueueEmpty() {
        advancedTts.remove(3).subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(IndexOutOfBoundsException.class);
    }

    @Test
    public void shouldReplaceTextString() {
        advancedTts.add(list)
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertArrayEquals(strings.toArray(),new String[] {text1, text2, text3});
                    return Completable.complete();
                }))
                .andThen(advancedTts.replace(0, text3))
                .andThen(advancedTts.replace(1, text1))
                .andThen(advancedTts.replace(2, text2))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertArrayEquals(strings.toArray(),new String[] {text3, text1, text2});
                    return Completable.complete();
                }))
                .subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldReplaceAllTextStrings() {

        List<String> newList = new ArrayList<>();
        newList.add(text2);
        newList.add(text3);
        newList.add(text1);

        advancedTts.add(list)
                .andThen(advancedTts.replaceAll(newList))
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertArrayEquals(strings.toArray(),new String[] {text2, text3, text1});
                    return Completable.complete();
                }))
                .subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldClearStack() {
        advancedTts.add(list)
                .andThen(advancedTts.clear())
                .andThen(advancedTts.getStack().flatMapCompletable(strings -> {
                    assertEquals(strings.size(),0);
                    return Completable.complete();
                }))
                .subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldReleaseIfTtsNull() {
        when(mTTSInstance.getTts()).thenReturn(null);
        advancedTts.release().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verifyZeroInteractions(tts);
    }

    @Test
    public void shouldReleaseIfTtsNotNull() {
        advancedTts.release().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts, times(1)).stop();
        verify(tts, times(1)).shutdown();
        verify(mTTSInstance, times(1)).setTts(null);
    }

    @Test
    public void shouldNotChangeLanguageWhenTtsNull() {
        when(mTTSInstance.getTts()).thenReturn(null);
        advancedTts.changeLanguage("anything").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldNotChangeLanguageWhenLanguageNotSupported() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_NOT_SUPPORTED);
        advancedTts.changeLanguage("en").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldNotChangeLanguageWhenLanguageMissingData() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_MISSING_DATA);
        advancedTts.changeLanguage("en").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldNotChangeLanguageWhenCountryNotSupported() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_AVAILABLE);
        advancedTts.changeLanguage("en_US").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldNotChangeLanguageWhenVariantNotSupported() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_COUNTRY_AVAILABLE);
        advancedTts.changeLanguage("en_US_blabla").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldChangeLanguageWhenCountrySupported() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_COUNTRY_AVAILABLE);
        advancedTts.changeLanguage("en_US").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldChangeLanguageWhenVariantSupported() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE);
        advancedTts.changeLanguage("en_US_blabla").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldChangeLanguage() {
        when(tts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_AVAILABLE);
        advancedTts.changeLanguage("en").subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
    }

    @Test
    public void shouldTriggerErrorWhenStopFail() {
        when(tts.stop()).thenReturn(TextToSpeech.ERROR);
        advancedTts.stop().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
        verify(tts, times(1)).stop();
    }

    @Test
    public void shouldStopTextToSpeech() {
        when(tts.stop()).thenReturn(TextToSpeech.SUCCESS);
        advancedTts.stop().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts, times(1)).stop();
    }

    @Test
    public void shouldSpeedUpSoundWhenTtsInitialized() {
        when(tts.setSpeechRate(any(Float.class))).thenReturn(TextToSpeech.SUCCESS);
        advancedTts.speedUp().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts, times(1)).setSpeechRate(any(Float.class));
    }

    @Test
    public void shouldNotSpeedUpSoundWhenTtsCannotSetSpeechRate() {
        when(tts.setSpeechRate(any(Float.class))).thenReturn(TextToSpeech.ERROR);
        advancedTts.speedUp().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
        verify(tts, times(1)).setSpeechRate(any(Float.class));
    }

    @Test
    public void shouldNotSpeedUpSoundWhenTtsNotInitialized() {
        when(mTTSInstance.getTts()).thenReturn(null);
        advancedTts.speedUp().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
        verifyZeroInteractions(tts);
    }

    @Test
    public void shouldSpeedDownSoundWhenTtsInitialized() {
        when(tts.setSpeechRate(any(Float.class))).thenReturn(TextToSpeech.SUCCESS);
        advancedTts.speedDown().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts, times(1)).setSpeechRate(any(Float.class));
    }

    @Test
    public void shouldNotSpeedDownSoundWhenTtsCannotSetSpeechRate() {
        when(tts.setSpeechRate(any(Float.class))).thenReturn(TextToSpeech.ERROR);
        advancedTts.speedDown().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
        verify(tts, times(1)).setSpeechRate(any(Float.class));
    }

    @Test
    public void shouldNotSpeedDownSoundWhenTtsNotInitialized() {
        when(mTTSInstance.getTts()).thenReturn(null);
        advancedTts.speedDown().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
        verifyZeroInteractions(tts);
    }

    @Test
    public void shouldNotPlayNextIfStopFail() {
        when(tts.stop()).thenReturn(TextToSpeech.ERROR);
        advancedTts.next().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldNotPlayNextIfQueueEmpty() {
        advancedTts.clear().andThen(advancedTts.next()).subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldPlayNext() {
        when(mTTSInstance.getTts()).thenReturn(testTts);
        advancedTts.add(list)
                .andThen(advancedTts.next())
                .subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts,times(1)).speak(eq(text2), any(Integer.class), any(HashMap.class));
    }

    @Test
    public void shouldNotPlayPreviousIfStopFail() {
        when(tts.stop()).thenReturn(TextToSpeech.ERROR);
        advancedTts.previous().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldNotPlayPreviousIfQueueEmpty() {
        advancedTts.clear().andThen(advancedTts.previous()).subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldPlayPrevious() {
        when(mTTSInstance.getTts()).thenReturn(testTts);
        advancedTts.add(list)
                .andThen(advancedTts.previous())
                .subscribe(testObserver);

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts,times(1)).speak(eq(text1), any(Integer.class), any(HashMap.class));
    }

    @Test
    public void shouldReturnQueue() {
        advancedTts.add(list).andThen(advancedTts.getStack()).subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        testObserver.assertResult(list);
    }

    @Test
    public void shouldNotStartPlayingIfQueueEmpty() {
        when(mTTSInstance.getTts()).thenReturn(testTts);
        advancedTts.start().subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertError(Exception.class);
    }

    @Test
    public void shouldStartPlayingAllQueue() {
        when(mTTSInstance.getTts()).thenReturn(testTts);
        advancedTts.add(list).andThen(advancedTts.start()).subscribe(testObserver);
        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        verify(tts, times(3)).speak(any(String.class), any(Integer.class), any(HashMap.class));
    }
}
