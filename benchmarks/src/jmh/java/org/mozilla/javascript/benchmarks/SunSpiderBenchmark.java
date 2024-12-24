package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.openjdk.jmh.annotations.*;

public class SunSpiderBenchmark {
    private static final String TEST_BASE = "testsrc/benchmarks/sunspider-1.0/";

    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    abstract static class AbstractState {
        Context cx;
        Scriptable scope;
        Script script;
        String fileName;

        AbstractState(String fileName) {
            this.fileName = TEST_BASE + fileName;
        }

        protected abstract boolean isInterpreted();

        @Setup(Level.Trial)
        public void setUp() {
            cx = Context.enter();
            cx.setInterpretedMode(isInterpreted());
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();

            try (FileReader rdr = new FileReader(fileName)) {
                script = cx.compileReader(rdr, fileName, 1, null);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            Context.exit();
        }

        Object run() {
            return script.exec(cx, scope);
        }
    }

    @State(Scope.Thread)
    public static class ThreeDCubeState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public ThreeDCubeState() {
            super("3d-cube.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object threeDCube(ThreeDCubeState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class ThreeDMorphState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public ThreeDMorphState() {
            super("3d-morph.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object threeDMorph(ThreeDMorphState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class ThreeDRayState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public ThreeDRayState() {
            super("3d-raytrace.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object threeDRayTrace(ThreeDRayState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class AccessBinaryTreesState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public AccessBinaryTreesState() {
            super("access-binary-trees.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object accessBinaryTrees(AccessBinaryTreesState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class AccessFannkuchState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public AccessFannkuchState() {
            super("access-fannkuch.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object accessFannkuch(AccessFannkuchState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class AccessNBodyState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public AccessNBodyState() {
            super("access-nbody.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object accessNBody(AccessNBodyState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class AccessFannAccessNsieveState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public AccessFannAccessNsieveState() {
            super("access-nsieve.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object accessNsieve(AccessFannAccessNsieveState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class Bitops3BitState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public Bitops3BitState() {
            super("bitops-3bit-bits-in-byte.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object bitops3BitBitsInByte(Bitops3BitState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class BitopsBitsState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public BitopsBitsState() {
            super("bitops-bits-in-byte.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object bitopsBitsInByte(BitopsBitsState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class BitopsAndState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public BitopsAndState() {
            super("bitops-bitwise-and.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object bitopsBitwiseAnd(BitopsAndState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class BitopsNsieveState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public BitopsNsieveState() {
            super("bitops-nsieve-bits.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object bitopsNsieveBits(BitopsNsieveState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class RecursiveState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public RecursiveState() {
            super("controlflow-recursive.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object controlflowRecursive(RecursiveState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class CryptoAesState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public CryptoAesState() {
            super("crypto-aes.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object cryptoAes(CryptoAesState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class CryptoMd5State extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public CryptoMd5State() {
            super("crypto-md5.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object cryptoMd5(CryptoMd5State state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class CryptoShaState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public CryptoShaState() {
            super("crypto-sha1.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object cryptoSha1(CryptoShaState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class DateFormatToFteState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public DateFormatToFteState() {
            super("date-format-tofte.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object dateFormatToFte(DateFormatToFteState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class DateFormatXparbState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public DateFormatXparbState() {
            super("date-format-xparb.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object dateFormatXparb(DateFormatXparbState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class MathCordicState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public MathCordicState() {
            super("math-cordic.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object mathCordic(MathCordicState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class MathPartialState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public MathPartialState() {
            super("math-partial-sums.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object mathPartialSums(MathPartialState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class MathSpectralNormState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public MathSpectralNormState() {
            super("math-spectral-norm.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object mathSpectralNorm(MathSpectralNormState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class RegexpState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public RegexpState() {
            super("regexp-dna.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object regexpDna(RegexpState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class StringBase64State extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public StringBase64State() {
            super("string-base64.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object stringBase64(StringBase64State state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class StringFastaState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public StringFastaState() {
            super("string-fasta.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object stringFasta(StringFastaState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class StringTagcloudState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public StringTagcloudState() {
            super("string-tagcloud.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object stringTagcloud(StringTagcloudState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class StringUnpackState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public StringUnpackState() {
            super("string-unpack-code.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object stringUnpackCode(StringUnpackState state) {
            return state.run();
        }
    }

    @State(Scope.Thread)
    public static class StringValidateState extends AbstractState {
        @Param({"false", "true"})
        public boolean interpreted;

        public StringValidateState() {
            super("string-validate-input.js");
        }

        @Override
        protected boolean isInterpreted() {
            return interpreted;
        }

        @Benchmark
        public Object stringValidateInput(StringValidateState state) {
            return state.run();
        }
    }
}
