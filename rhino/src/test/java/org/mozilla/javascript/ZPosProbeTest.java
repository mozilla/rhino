package org.mozilla.javascript;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ZPosProbeTest {

    private static void dump(String src) throws IOException {
        String out =
                InterpreterIcodeCapture.capture(
                        () -> {
                            try (Context cx = Context.enter()) {
                                cx.setInterpretedMode(true);
                                cx.compileString(src, "test", 1, null);
                            }
                        });
        System.out.println("PROBE src: " + src);
        for (String line : out.split("\n")) {
            if (line.contains("LINE :") || line.contains("POS :")) {
                System.out.println("PROBE   " + line.trim());
            }
        }
    }

    @Test
    public void probe() throws IOException {
        dump("o.a.b;");
        dump("  o.a.b;");
        dump("foo.bar.baz();");
        dump("var x = o.a.b;");
        dump("o.a.b = 1;");
        dump("f(o.a, p.b);");
        dump("o[k].m();");
    }
}
