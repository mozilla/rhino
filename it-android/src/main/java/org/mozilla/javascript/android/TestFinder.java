package org.mozilla.javascript.android;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class TestFinder {

    public static Map<String, String> listFiles(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] files = assetManager.list("tests");
            Map<String, String> tests = new TreeMap<>();
            if (files != null) {
                for (String file : files) {
                    try (InputStream in = assetManager.open("tests/" + file)) {
                        Scanner scanner = new Scanner(in).useDelimiter("\\A");
                        if (scanner.hasNext()) {
                            tests.put(file, scanner.next());
                        }
                    }
                }
            }
            return tests;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }
}
