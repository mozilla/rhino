package org.mozilla.javascript.benchmarks;

import java.io.FileReader;
import java.io.IOException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import org.openjdk.jmh.annotations.*;

public class SunSpiderBenchmark {
  private static final String TEST_BASE = "testsrc/benchmarks/sunspider-0.9.1/";

  static abstract class AbstractState {
    Context cx;
    Scriptable scope;
    Script script;
    String fileName;

    AbstractState(String fileName) {
      this.fileName = TEST_BASE + fileName;
    }

    @Setup(Level.Trial)
    public void setUp() {
      cx = Context.enter();
      cx.setLanguageVersion(Context.VERSION_ES6);
      cx.setOptimizationLevel(9);
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
    public ThreeDCubeState() {
      super("3d-cube.js");
    }
  }

  @Benchmark
  public Object threeDCube(ThreeDCubeState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class ThreeDMorphState extends AbstractState {
    public ThreeDMorphState() {
      super("3d-morph.js");
    }
  }

  @Benchmark
  public Object threeDMorph(ThreeDMorphState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class ThreeDRayState extends AbstractState {
    public ThreeDRayState() {
      super("3d-raytrace.js");
    }
  }

  @Benchmark
  public Object threeDRayTrace(ThreeDRayState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class AccessBinaryTreesState extends AbstractState {
    public AccessBinaryTreesState() {
      super("access-binary-trees.js");
    }
  }

  @Benchmark
  public Object accessBinaryTrees(AccessBinaryTreesState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class AccessFannkuchState extends AbstractState {
    public AccessFannkuchState() {
      super("access-fannkuch.js");
    }
  }

  @Benchmark
  public Object accessFannkuch(AccessFannkuchState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class AccessNBodyState extends AbstractState {
    public AccessNBodyState() {
      super("access-nbody.js");
    }
  }

  @Benchmark
  public Object accessNBody(AccessNBodyState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class AccessFannAccessNsieveState extends AbstractState {
    public AccessFannAccessNsieveState() {
      super("access-nsieve.js");
    }
  }

  @Benchmark
  public Object accessNsieve(AccessFannAccessNsieveState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class Bitops3BitState extends AbstractState {
    public Bitops3BitState() {
      super("bitops-3bit-bits-in-byte.js");
    }
  }

  @Benchmark
  public Object bitops3BitBitsInByte(Bitops3BitState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class BitopsBitsState extends AbstractState {
    public BitopsBitsState() {
      super("bitops-bits-in-byte.js");
    }
  }

  @Benchmark
  public Object bitopsBitsInByte(BitopsBitsState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class BitopsAndState extends AbstractState {
    public BitopsAndState() {
      super("bitops-bitwise-and.js");
    }
  }

  @Benchmark
  public Object bitopsBitwiseAnd(BitopsAndState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class BitopsNsieveState extends AbstractState {
    public BitopsNsieveState() {
      super("bitops-nsieve-bits.js");
    }
  }

  @Benchmark
  public Object bitopsNsieveBits(BitopsNsieveState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class RecursiveState extends AbstractState {
    public RecursiveState() {
      super("controlflow-recursive.js");
    }
  }

  @Benchmark
  public Object controlflowRecursive(RecursiveState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class CryptoAesState extends AbstractState {
    public CryptoAesState() {
      super("crypto-aes.js");
    }
  }

  @Benchmark
  public Object cryptoAes(CryptoAesState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class CryptoMd5State extends AbstractState {
    public CryptoMd5State() {
      super("crypto-md5.js");
    }
  }

  @Benchmark
  public Object cryptoMd5(CryptoMd5State state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class CryptoShaState extends AbstractState {
    public CryptoShaState() {
      super("crypto-sha1.js");
    }
  }

  @Benchmark
  public Object cryptoSha1(CryptoShaState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class DateFormatToFteState extends AbstractState {
    public DateFormatToFteState() {
      super("date-format-tofte.js");
    }
  }

  @Benchmark
  public Object dateFormatToFte(DateFormatToFteState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class DateFormatXparbState extends AbstractState {
    public DateFormatXparbState() {
      super("date-format-xparb.js");
    }
  }

  @Benchmark
  public Object dateFormatXparb(DateFormatXparbState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class MathCordicState extends AbstractState {
    public MathCordicState() {
      super("math-cordic.js");
    }
  }

  @Benchmark
  public Object mathCordic(MathCordicState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class MathPartialState extends AbstractState {
    public MathPartialState() {
      super("math-partial-sums.js");
    }
  }

  @Benchmark
  public Object mathPartialSums(MathPartialState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class MathSpectralNormState extends AbstractState {
    public MathSpectralNormState() {
      super("math-spectral-norm.js");
    }
  }

  @Benchmark
  public Object mathSpectralNorm(MathSpectralNormState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class RegexpState extends AbstractState {
    public RegexpState() {
      super("regexp-dna.js");
    }
  }

  @Benchmark
  public Object regexpDna(RegexpState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class StringBase64State extends AbstractState {
    public StringBase64State() {
      super("string-base64.js");
    }
  }

  @Benchmark
  public Object stringBase64(StringBase64State state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class StringFastaState extends AbstractState {
    public StringFastaState() {
      super("string-fasta.js");
    }
  }

  @Benchmark
  public Object stringFasta(StringFastaState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class StringTagcloudState extends AbstractState {
    public StringTagcloudState() {
      super("string-tagcloud.js");
    }
  }

  @Benchmark
  public Object stringTagcloud(StringTagcloudState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class StringUnpackState extends AbstractState {
    public StringUnpackState() {
      super("string-unpack-code.js");
    }
  }

  @Benchmark
  public Object stringUnpackCode(StringUnpackState state) {
    return state.run();
  }

  @State(Scope.Thread)
  public static class StringValidateState extends AbstractState {
    public StringValidateState() {
      super("string-validate-input.js");
    }
  }

  @Benchmark
  public Object stringValidateInput(StringValidateState state) {
    return state.run();
  }
}
