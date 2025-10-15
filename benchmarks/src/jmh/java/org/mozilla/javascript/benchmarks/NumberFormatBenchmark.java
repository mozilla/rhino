package org.mozilla.javascript.benchmarks;

import java.util.concurrent.TimeUnit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.dtoa.DecimalFormatter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NumberFormatBenchmark {

    private static final double DENORMAL = 4.47118444E-314;

    @Benchmark
    public Object scriptRuntimePi() {
        return ScriptRuntime.toString(Math.PI);
    }

    @Benchmark
    public Object scriptRuntimeOne() {
        return ScriptRuntime.toString(1.0);
    }

    @Benchmark
    public Object scriptRuntimeDenormal() {
        return ScriptRuntime.toString(DENORMAL);
    }

    @Benchmark
    public Object scriptRuntimeSmall() {
        return ScriptRuntime.toString(0.00001);
    }

    @Benchmark
    public Object scriptRuntimeBig() {
        return ScriptRuntime.toString(314000000000000000000.0);
    }

    @Benchmark
    public Object toFixedPi() {
        return DecimalFormatter.toFixed(Math.PI, 20);
    }

    @Benchmark
    public Object toExponentialPi() {
        return DecimalFormatter.toExponential(Math.PI, 20);
    }

    @Benchmark
    public Object toExponentialPiDefault() {
        return DecimalFormatter.toExponential(Math.PI, -1);
    }

    @Benchmark
    public Object toFixedDenormal() {
        return DecimalFormatter.toFixed(DENORMAL, 20);
    }

    @Benchmark
    public Object toExponentialDenormal() {
        return DecimalFormatter.toExponential(DENORMAL, 20);
    }
}
