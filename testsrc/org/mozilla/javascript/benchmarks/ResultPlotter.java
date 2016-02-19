package org.mozilla.javascript.benchmarks;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Trial;
import com.google.caliper.model.Value;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ResultPlotter
    implements ResultProcessor

{
    private PrintWriter out;
    private final HashMap<String, Double> results = new HashMap<String, Double>();

    public ResultPlotter()
        throws IOException
    {
        out = new PrintWriter(
            new FileWriter(new File(System.getProperty("rhino.benchmark.report"), "caliper-spider.csv"))
        );
    }

    @Override
    public void close()
    {
        boolean once = false;
        for (String n : results.keySet()) {
            if (once) {
                out.print(',');
            } else {
                once = true;
            }
            out.print(n);
        }
        out.println();

        once = false;
        for (Double v : results.values()) {
            if (once) {
                out.print(',');
            } else {
                once = true;
            }
            out.printf("%.2f", v);
        }
        out.println();

        out.close();
    }

    @Override
    public void processTrial(Trial trial)
    {
        if (trial.instrumentSpec().className().contains("RuntimeInstrument")) {
            double runningAvg = 0.0;
            for (Measurement m : trial.measurements()) {
                runningAvg += (m.value().magnitude() / m.weight());
            }
            double avg = runningAvg / (double) trial.measurements().size();
            results.put(trial.scenario().benchmarkSpec().methodName(), avg);
        }
    }
}
