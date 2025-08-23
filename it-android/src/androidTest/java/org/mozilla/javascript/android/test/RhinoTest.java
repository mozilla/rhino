package org.mozilla.javascript.android.test;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RhinoTest {

    @Test
    public void test() {
        Log.i("HelloWorldTest", "hello world from JUnit test with log");
        System.out.println("hello world from JUnit test with sysout");
    }
}
