package ai;

import java.util.HashMap;
import java.util.Map;

public class TranspositionTable {
    private final Map<Long, TranspositionEntry> table;

    public TranspositionTable() {
        this.table = new HashMap<>();
    }

    public void put(long zobristKey, int depth, int value, int alpha, int beta, byte[] bestMove) {
        table.put(zobristKey, new TranspositionEntry(depth, value, alpha, beta, bestMove));
    }


    public TranspositionEntry get(long zobristKey) {
        return table.get(zobristKey);
    }

    public void clear() {
        table.clear();
    }

    public static class TranspositionEntry {
        int depth;
        int value;
        int alpha;
        int beta;
        byte[] bestMove;

        public TranspositionEntry(int depth, int value, int alpha, int beta, byte[] bestMove) {
            this.depth = depth;
            this.value = value;
            this.alpha = alpha;
            this.beta = beta;
            this.bestMove = bestMove;
        }
    }
}
