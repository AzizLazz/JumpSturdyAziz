package ai;

public class TestWrapper {
    // wie viele Zustaende wurden untersucht
    public int untersuchteZustaende;
    // best possible value
    public int bestValue;
    // best possible move
    public String bestMove;
    public boolean isRed;

    public TestWrapper(int untersuchteZustaende, int value, String bestMove, boolean isRed) {
        this.untersuchteZustaende = untersuchteZustaende;
        this.bestValue = value;
        this.bestMove = bestMove;
        this.isRed = isRed;
    }

    public TestWrapper(int untersuchteZustaende, int bestValue, boolean isRed) {
        this.untersuchteZustaende = untersuchteZustaende;
        this.bestValue = bestValue;
        this.isRed = isRed;
    }
}