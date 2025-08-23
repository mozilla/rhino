package org.mozilla.javascript.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Map;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Map<String, String> tests = TestFinder.listFiles(this);
        StringBuilder sb = new StringBuilder();
        tests.forEach((name, js) -> {

            try (Context cx = Context.enter()) {
                Scriptable scope = cx.initStandardObjects();
                String jsCode = "3+5";
                Object result = cx.evaluateString(scope, js, name, 1, null);
                sb.append(name).append(":").append(result).append("\n");
            } catch (Exception e) {
                sb.append(name).append(":").append(e.getMessage()).append("\n");
            }
        });

        TextView label = new TextView(this);
        label.setText(sb.toString());
        setContentView(label);
    }
}