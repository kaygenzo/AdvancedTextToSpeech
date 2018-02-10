package com.telen.library.libt2s;

import android.content.Context;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by karim on 10/02/2018.
 */

public class T2SManagerTests {

    private AdvancedTextToSpeech advancedTts;
    @Mock Context mContext;

    private String text1 = "This is an example chain 1";
    private String text2 = "This is an example chain 2";
    private String text3 = "This is an example chain 3";
    private TestObserver testObserver;
    private List<String> list = new ArrayList<>();

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
        advancedTts = AdvancedT2SImpl.getInstance(mContext);
        testObserver = new TestObserver();
        list.add(text1);
        list.add(text2);
        list.add(text3);
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

}
