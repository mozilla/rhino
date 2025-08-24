package org.mozilla.javascript.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class MainActivity extends Activity {

    private int k() {
        return Math.min(3, 1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StringBuilder sb = new StringBuilder();
        try {
            List<TestCase> tests = TestCase.getTestCases(this);
            for (TestCase test : tests) {
                if (sb.length() != 0) {
                    sb.append('\n');
                }
                try {
                    sb.append(test.toString() + ":" + test.run());

                } catch (Exception e) {
                    e.printStackTrace();
                    sb.append(test.toString() + ": FAIL" + e.getMessage());
                }
            }
        } catch (IOException e) {
            sb.append(e);
        }

        TextView label = new TextView(this);
        label.setText(sb.toString());
        setContentView(label);
    }
}
