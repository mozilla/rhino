package org.mozilla.javascript.interpreterv2;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InstructionStats {
    private static final boolean ENABLED = true;
    private static final Map<String, AtomicLong> instructionCounts = new ConcurrentHashMap<>();
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(InstructionStats::writeStatsToFile));
        }
    }

    public static void recordInstruction(String instructionName) {
        if (!ENABLED) return;

        instructionCounts.computeIfAbsent(instructionName, k -> new AtomicLong()).incrementAndGet();
    }

    private static void writeStatsToFile() {
        if (!ENABLED || instructionCounts.isEmpty()) return;

        String timestamp =
                LocalDateTime.now(SYSTEM_ZONE)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "rhino-instruction-stats-" + timestamp + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, UTF_8))) {
            writer.write("=== Rhino InterpreterV2 Instruction Statistics ===\n");
            writer.write("Generated at: " + LocalDateTime.now(SYSTEM_ZONE) + "\n");
            writer.write("Total unique instructions: " + instructionCounts.size() + "\n");
            writer.write("\n");

            long totalCount = instructionCounts.values().stream().mapToLong(AtomicLong::get).sum();
            writer.write("Total instructions executed: " + totalCount + "\n");
            writer.write("\n");

            writer.write("Instruction counts (sorted by frequency):\n");
            writer.write("==========================================\n");

            instructionCounts.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
                    .forEach(
                            entry -> {
                                try {
                                    writer.write(
                                            String.format(
                                                    "%-40s: %,d\n",
                                                    entry.getKey(), entry.getValue().get()));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

            System.out.println("Instruction statistics written to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to write instruction statistics: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean isEnabled() {
        return ENABLED;
    }
}
