package ai;

import model.BitBoard;

import java.util.Random;

public class ZobristHashing {
    public static long[][] pieceKeys = new long[6][64]; // blueSingles, redSingles, blueDouble, redDoubles, blue_on_red, red_on_blue
    private static final long hashSeed = 1325379206L;
    public static boolean initialized = false;



    public static void initializeHashKeys() {
        Random rand = new Random(hashSeed);

        for (int piece = 0; piece < pieceKeys.length; piece++) {
            for (int square = 0; square < pieceKeys[piece].length; square++) {
                if (square == 0 || square == 7 || square == 56 || square == 63) {
                    pieceKeys[piece][square] = 0L;
                } else {
                    pieceKeys[piece][square] = rand.nextLong();
                }
            }
        }
        initialized = true;
    }

    public long generateZobristKey(BitBoard board) {
        if (!initialized) {
            initializeHashKeys();
            initialized = true;
        }
        long hashKey = 0L;
        long[] bluePlayer = board.getPlayer(false);
        long[] redPlayer = board.getPlayer(true);
        for (int piece = 0; piece < bluePlayer.length; piece++) {
            long blueBitboard = bluePlayer[piece];
            long redBitboard = redPlayer[piece];

            while (blueBitboard != 0) {
                int i = Long.numberOfTrailingZeros(blueBitboard);
                if (i != 0 && i != 7 && i != 56 && i != 63) { // Überprüfe, ob das Feld nicht 0, 7, 55 oder 63 ist
                    hashKey ^= pieceKeys[piece * 2][i];
                }
                blueBitboard &= blueBitboard - 1;
            }

            while (redBitboard != 0) {
                int i = Long.numberOfTrailingZeros(redBitboard);
                if (i != 0 && i != 7 && i != 56 && i != 63) { // Überprüfe, ob das Feld nicht 0, 7, 55 oder 63 ist
                    hashKey ^= pieceKeys[(piece * 2) + 1][i];
                }
                redBitboard &= redBitboard - 1;
            }
        }

        return hashKey;
    }
}
