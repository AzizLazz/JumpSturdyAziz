package benchmark;

import model.BitBoard;
import ai.Evaluate;

public class Benchmark {

    public static void benchmark(BitBoard b) {
        long startTime, endTime;

        // Benchmark loop
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            // Generate moves
            //b.getAllPossibleMoves(true);
            Evaluate.evaluateSimple(false, b.redSingles, b.blueSingles, b.redDoubles, b.blueDoubles, b.red_on_blue, b.blue_on_red);
        }
        endTime = System.nanoTime();

        // Calculate benchmark results
        long elapsedTime = endTime - startTime;
        double averageTimePerCall = (double) elapsedTime / 1000;

        //Print benchmark results
        System.out.println("Gesamtzeit: " + (elapsedTime / 1e6) + " ms");
        System.out.println("Zeit pro Aufruf: " + averageTimePerCall + "ns");
    }
}
