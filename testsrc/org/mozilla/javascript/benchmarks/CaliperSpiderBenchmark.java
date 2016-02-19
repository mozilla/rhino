package org.mozilla.javascript.benchmarks;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.Macrobenchmark;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

@SuppressWarnings("unused")
public class CaliperSpiderBenchmark
{
    public static final File FILE_BASE = new File("testsrc/benchmarks/sunspider-0.9.1");

    static Script compileBenchmark(Context cx, String fileName)
        throws IOException
    {
        File f = new File(FILE_BASE, fileName);
        FileReader rdr = new FileReader(f);
        try {
            return cx.compileReader(rdr, fileName, 1, null);
        } finally {
            rdr.close();
        }
    }

    @SuppressWarnings("unused")
    public static class Spider
    {
        @Param("9") int optLevel;

        private Context cx;
        private Scriptable scope;

        private final HashMap<String, Script> scripts = new HashMap<String, Script>();

        private static final String[] BENCHMARKS = {
            "3d-cube.js", "3d-morph.js", "3d-raytrace.js",
            "access-binary-trees.js", "access-fannkuch.js", "access-nbody.js", "access-nsieve.js",
            "bitops-3bit-bits-in-byte.js", "bitops-bits-in-byte.js", "bitops-bitwise-and.js", "bitops-nsieve-bits.js",
            "controlflow-recursive.js", "crypto-aes.js", "crypto-md5.js", "crypto-sha1.js",
            "date-format-tofte.js", "date-format-xparb.js",
            "math-cordic.js", "math-partial-sums.js", "math-spectral-norm.js",
            "regexp-dna.js",
            "string-base64.js", "string-fasta.js", "string-tagcloud.js",
            "string-unpack-code.js", "string-validate-input.js"
        };

        @BeforeExperiment
        @SuppressWarnings("unused")
        void create()
            throws IOException
        {
            cx = Context.enter();
            cx.setOptimizationLevel(optLevel);
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = new Global(cx);

            for (String bn : BENCHMARKS) {
                compileScript(cx, bn);
            }
        }

        private void compileScript(Context cx, String fileName)
            throws IOException
        {
            Script s = compileBenchmark(cx, fileName);
            scripts.put(fileName, s);
        }

        @AfterExperiment
        @SuppressWarnings("unused")
        void close()
        {
            Context.exit();
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void threeDCube(int iterations)
        {
            runBenchmark("3d-cube.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void threeDMorph(int iterations)
        {
            runBenchmark("3d-morph.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void accessBinaryTrees(int iterations)
        {
            runBenchmark("access-binary-trees.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void accessFannkuch(int iterations)
        {
            runBenchmark("access-fannkuch.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void accessNBody(int iterations)
        {
            runBenchmark("access-nbody.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void accessNSieve(int iterations)
        {
            runBenchmark("access-nsieve.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void bitops3BitBitsInByte(int iterations)
        {
            runBenchmark("bitops-3bit-bits-in-byte.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void bitopsBitsInByte(int iterations)
        {
            runBenchmark("bitops-bits-in-byte.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void BitopsBitwiseAnd(int iterations)
        {
            runBenchmark("bitops-bitwise-and.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void bisopsNsieveBits(int iterations)
        {
            runBenchmark("bitops-nsieve-bits.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void controlFlowRecursive(int iterations)
        {
            runBenchmark("controlflow-recursive.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void cryptoAES(int iterations)
        {
            runBenchmark("crypto-aes.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void cryptoMD5(int iterations)
        {
            runBenchmark("crypto-md5.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void cryptoSHA1(int iterations)
        {
            runBenchmark("crypto-sha1.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void dateFormatToFTE(int iterations)
        {
            runBenchmark("date-format-tofte.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void dateFormatXparb(int iterations)
        {
            runBenchmark("date-format-xparb.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void mathCordic(int iterations)
        {
            runBenchmark("math-cordic.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void mathPartialSums(int iterations)
        {
            runBenchmark("math-partial-sums.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void mathSpectralNorm(int iterations)
        {
            runBenchmark("math-spectral-norm.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void regexpDNA(int iterations)
        {
            runBenchmark("regexp-dna.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void stringBase64(int iterations)
        {
            runBenchmark("string-base64.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void stringFasta(int iterations)
        {
            runBenchmark("string-fasta.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void stringTagCloud(int iterations)
        {
            runBenchmark("string-tagcloud.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void stringUnpackCode(int iterations)
        {
            runBenchmark("string-unpack-code.js", iterations);
        }

        @Macrobenchmark
        @SuppressWarnings("unused")
        public void stringValidateInput(int iterations)
        {
            runBenchmark("string-validate-input.js", iterations);
        }

        private void runBenchmark(String name, int iterations)
        {
            Script s = scripts.get(name);
            for (int i = 0; i < iterations; i++) {
                s.exec(cx, scope);
            }
        }
    }
}
