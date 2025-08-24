package org.mozilla.javascript.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class MainActivity extends Activity {

    private int k() {
        return Math.min(3,1);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Map<String, String> tests = TestFinder.listFiles(this);
        StringBuilder sb = new StringBuilder();
        System.out.println("Warmup");
        long start = System.currentTimeMillis();
        int j=0;
        for (int i = 0; i < 1_000_000; i++) {
            j+= k();
        }

        start = System.currentTimeMillis()-start;
        sb.append(j + ": Took  " + start + " ms");
        System.out.println(sb.toString());
        tests.forEach(
                (name, js) -> {
                    System.out.println("Executing " + name);
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
