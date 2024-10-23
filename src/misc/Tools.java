package misc;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static model.BitBoard.CORNER_MASK;

public class Tools {

    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\u001B[34m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String WHITE = "\u001B[37m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String BOLD = "\u001B[1m";


    public static String getAnsiColor(int colorCode) {
        return "\u001B[38;5;" + colorCode + "m";
    }
    public static String getAnsiBackgroundColor(int colorCode) {
        return "\u001B[48;5;" + colorCode + "m";
    }

    public static void main(String[] args) {

        for (int i = 232; i <= 255; i++) {
            System.out.print(getAnsiColor(i) + "Shade " + (i - 232) + " " + RESET);
            if ((i - 232) % 6 == 5) {
                System.out.println();
            } else {
                System.out.print("  ");
            }
        }
        System.out.println(getAnsiColor(232) + "Very dark grey text" + RESET);
        System.out.println(getAnsiColor(240) + "Medium grey text" + RESET);
        System.out.println(getAnsiColor(246) + "Medium grey text 2" + RESET);
        System.out.println(getAnsiColor(250) + "Light grey text" + RESET);

        System.out.println( Tools.fenToString("3b01b0/1b02b01b01/1r06/1r01br2b01/6r01/8/2r01r0r02/3r0r0r0")) ;
    }

    public static void printInColor(String text, boolean isRed) {
        System.out.println((isRed ? RED : BLUE) + text + RESET);
    }

    public static String stringInColor(String text, boolean isRed) {
        return ((isRed ? RED : BLUE) + text + RESET);
    }

    public static long previousTime = 0;
    public static void timed(String comment){
        Tools.printRed(comment+"| NANOS since last call: "+(System.nanoTime()-previousTime));
        previousTime = System.nanoTime();
    }

    public static void printRed(String text){//easier to call often
        printInColor(text,true);
    }

    public static void printBlue(String text){
        printInColor(text,false);
    }

    public static void printInColor(String text, String color) {
        System.out.println((color) + text + RESET);
    }

    public static void printDivider(){
        System.out.println("\u001B[45m"+"⎯".repeat(100)+"\u001B[0m");
        System.out.println("\u001B[42m"+"⎯".repeat(100)+"\u001B[0m");
        System.out.println("\u001B[44m"+"⎯".repeat(100)+"\u001B[0m");
    }

    public static void printDivider(int length){
        System.out.println("\u001B[45m"+"⎯".repeat(length)+"\u001B[0m");
        System.out.println("\u001B[42m"+"⎯".repeat(length)+"\u001B[0m");
        System.out.println("\u001B[44m"+"⎯".repeat(length)+"\u001B[0m");
    }

    public static String moveMagician(String uncleanMove, List<String> moves) {
        if(uncleanMove.toUpperCase().startsWith("REMOVE")||uncleanMove.toUpperCase().startsWith("ADD")||uncleanMove.toUpperCase().startsWith("START")) return uncleanMove.toUpperCase();//Dont handle in this case
        uncleanMove = uncleanMove.toUpperCase();
        if (uncleanMove.length() == 2 && moves !=null) { // Eg. just C5, then pick any/first move that matches
            for (String str : moves) {
                if (str.endsWith(uncleanMove)) {
                    return str;
                }
            }
        }
        return cleanMove(uncleanMove);
    }

    public static String cleanMove(String move) {
        // Remove any whitespace and convert to uppercase
        move = move.trim().toUpperCase();
        if (move.length() == 5) {
            move = move.substring(0, 2) + "-" + move.substring(3, 5);
        }
        if (move.length() == 3) {
            // Insert a dash between the first and second characters
            move = move.substring(0, 2) + "-" + move.charAt(0) + move.charAt(2);
        } else if (move.length() == 4) {
            // Insert a dash between the second and third characters
            move = move.substring(0, 2) + "-" + move.substring(2);
        }
        // No action needed for length == 5

        return move;
    }

    public static String bitboardToString(long bitboard) {
        StringBuilder sb = new StringBuilder();
        for (int i = 63; i >= 0; i--) {
            long mask = 1L << i;
            if ((bitboard & mask) != 0) {
                sb.append('1');
            } else {
                sb.append('0');
            }
        }
        return sb.toString();
    }

    public static byte positionToIndex(String position) { //index, not bitboard, needs to be shifted
        // Extract the column (file) and row from the position
        char letter = position.charAt(0); // 'F'
        char number = position.charAt(1); // '3'// 'F' - 'A' = 5// '3' - '1' = 2

        // Calculate the index for a 0-based array (bottom-left is 0,0)
        return (byte) ((7 - (number - '1')) * 8 + (letter - 'A')); // Convert to 0-based index for an 8x8 board
    }

    public static String indexToStringPosition(byte index) {
        byte fileIndex = (byte) (index % 8); // Calculate file index (column)
        byte rankIndex = (byte) (index / 8); // Calculate rank index (row)
        char file = (char) ('A' + fileIndex); // Convert file index to letter (A-H)
        char rank = (char) ('1' + (7 - rankIndex)); // Convert rank index to number (1-8), adjusting for 0-based index
        return "" + file + rank; // Concatenate to form the position string
    }

    // Method to parse a move like "F3-F4" and return fromIndex and toIndex
    public static byte[] parseMove(String move) {
        // Split the move into the from and to parts
        String[] parts = move.split("-");
        // Convert each part to an index
        byte fromIndex = positionToIndex(parts[0]);
        byte toIndex = positionToIndex(parts[1]);
        // Return the indices as an array
        return new byte[]{fromIndex, toIndex};
    }

    public static String movesToString(byte[][] moves){
        return Arrays.stream(moves)
                .map(Tools::parseMoveToString)
                .collect(Collectors.joining(","));
    }

    public static String byteListToMoveSequence(List<byte[]> moves){
        return moves.stream().map(Tools::parseMoveToString).collect(Collectors.joining(","));
    }

    public static String parseMoveToString(byte[] move){
        return Tools.indexToStringPosition(move[0])+"-"+Tools.indexToStringPosition(move[1]);
    }

    public static String lastRowMove(List<String> possibleMoves, boolean isRed){
        for(String move : possibleMoves){
            if( (move.charAt(4) == '8' & isRed == false)
                    || (move.charAt(4) == '1' & isRed == true)){
                return move;
            }
        }
        Random random = new Random();
        return possibleMoves.get(random.nextInt(possibleMoves.size()));
    }

    public static byte getRandomIndex(long bitboard) {
        List<Byte> indices = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if ((bitboard & (1L << i)) != 0) {
                indices.add((byte)i);
            }
        }

        if (indices.isEmpty()) {
            return -1; // No bits set to 1
        }

        Random random = new Random();
        return indices.get(random.nextInt(indices.size()));
    }

    // Utility method to shift bitboards for movement
    // TODO: Performance, checked and seems to be VERY similar to inline, so no further opt necessary after 0 check
    public static long shift(long bitboard, int offset) {
        if (bitboard == 0) return 0;
        return offset > 0 ? (bitboard << offset & CORNER_MASK) : (bitboard >>> -offset & CORNER_MASK);
    }

    public static void displayBitboard(long bitboard) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int position = row * 8 + col;  // Start from top left, no inversion
                // IF Corner
                if ((row == 0 || row == 7) && (col == 0 || col == 7)) {
                    System.out.print("X ");
                } else if ((bitboard & (1L << position)) != 0) {
                    System.out.print("1 ");
                } else {
                    System.out.print("0 ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public static String bitboardAsString(long bitboard) {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < 8; row++) {

            for (int col = 0; col < 8; col++) {
                int position = row * 8 + col;  // Start from top left, no inversion
                // IF Corner
                if ((row == 0 || row == 7) && (col == 0 || col == 7)) {
                    s.append("X ");
                } else if ((bitboard & (1L << position)) != 0) {
                    s.append("1 ");
                } else {
                    s.append("0 ");
                }
            }
            s.append("\n");
        }
        return s.toString();
    }

    public static void commentedBits(String comment, long bits) {
        System.out.println(comment);
        displayBitboard(bits);
    }

    public static void sortList(List<?> objectsToOrder, List<?> orderedObjects) {

        HashMap<Object, Integer> indexMap = new HashMap<>();
        int index = 0;
        for (Object object : orderedObjects) {
            indexMap.put(object, index);
            index++;
        }

        Collections.sort(objectsToOrder, new Comparator<Object>() {

            public int compare(Object left, Object right) {

                Integer leftIndex = indexMap.get(left);
                Integer rightIndex = indexMap.get(right);
                if (leftIndex == null) {
                    return -1;
                }
                if (rightIndex == null) {
                    return 1;
                }

                return Integer.compare(leftIndex, rightIndex);
            }
        });
    }


    public static String fenToString(String fen) {
        // Replace numbers with corresponding number of empty squares
        for (int i = 1; i <= 8; i++) {
            fen = fen.replace(i + "", "⬜ ".repeat(i));
        }
        String[][] mappings = {
                {"rb", "\u001B[41m\uD83D\uDD35\u001B[0m "}, // Blue on red
                {"br", "\u001B[44m\uD83D\uDD34\u001B[0m "},  // Red on blue
                {"r0", "\uD83D\uDD34 "}, // 🔴
                {"b0", "\uD83D\uDD35 "}, // 🔵
                {"rr", "\uD83D\uDFE5 "}, // 🟥
                {"bb", "\uD83D\uDFE6 "} // 🟦

        };
        // Replace custom notations with corresponding emojis
        for (String[] mapping : mappings) {
            fen = fen.replace(mapping[0], mapping[1]);
        }
        StringBuilder sb = new StringBuilder();
        String[] rows = fen.split("/");
        // Append column labels (A-H)
        sb.append("   A  B  C  D   E  F  G  H\n");
        for (int row = 7; row >= 0; row--) { // Process rows in correct order
            sb.append((row + 1)).append(" ");
            // Add left corner for the first and last row
            if (row == 0 || row == 7) {
                sb.append("\uD83D\uDD33 ");
            }
            sb.append(rows[row]);
            // Add right corner for the first and last row
            if (row == 0 || row == 7) {
                sb.append("\uD83D\uDD33 ");
            }
            sb.append(" ").append(8 - row).append("\n");
        }
        // Append column labels (A-H)
        sb.append("   A  B  C  D   E  F  G  H\n");

        return sb.toString();
    }

}

class ProgressBarExample { // Might be needed in the future for very pretty console stuff
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i <= 100; i++) {
            // Print the progress bar with percentage
            System.out.println("\r[" + getProgressBar(i) + "] " + i + "%");
            Thread.sleep(100);
        }
    }

    public static void clrscr() {
        //Clears Screen in java
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {
        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static String getProgressBar(int percent) {
        int length = 20;
        int progress = (int) ((percent / 100.0) * length);
        StringBuilder progressBar = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            if (i < progress) {
                progressBar.append('=');
            } else {
                progressBar.append(' ');
            }
        }
        return progressBar.toString();
    }
}

class DynamicConsoleOutput {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        System.out.println("Hello, " + name + "!");
        System.out.println("Please wait...");

        // Simulate some processing
        try {
            for (int i = 1; i <= 10; i++) {
                Thread.sleep(500);
                // Clear the current line
                System.out.print("\033[2K"); // ANSI escape code to clear the current line
                // Move the cursor to the beginning of the line
                System.out.print("\r");
                System.out.print("Processing: " + i * 10 + "%");
            }
            System.out.println("\nProcessing complete!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Continue with user input
        System.out.print("Enter another input: ");
        String userInput = scanner.nextLine();
        System.out.println("You entered: " + userInput);

        scanner.close();
    }


}


class BitPackingComparison {

    static long total=0;
    public static void main(String[] args) {
        Random random = new Random();
        int iterations = 10000;

        long totalTimeShort = 0;
        long totalTimeByteArray = 0;

        for (int i = 0; i < iterations; i++) {


            // Measure time for packing into short
            long startTimeShort = System.nanoTime();
            short packedShort = packIntoShort((byte) random.nextInt(64),(byte) random.nextInt(64));
            total+=packedShort;
            long endTimeShort = System.nanoTime();
            totalTimeShort += (endTimeShort - startTimeShort);

            long startTimeByteArray = System.nanoTime();
            byte[] packedByteArray = packIntoByteArray((byte) random.nextInt(64), (byte) random.nextInt(64));
            total+=packedByteArray[0]+packedByteArray[1];
            long endTimeByteArray = System.nanoTime();
            totalTimeByteArray += (endTimeByteArray - startTimeByteArray);

        }
        //Results were pretty much similar, so not optimizing for now
        System.out.println("Time taken short : " + totalTimeShort + " ns");
        System.out.println("Time taken byte array : " + totalTimeByteArray + " ns"+ total);
    }

    public static short packIntoShort(byte byte1, byte byte2) {
        return (short) ((byte1 << 6) | (byte2 & 0x3F)); // Ensure byte2 is within 6 bits
    }

    public static byte[] packIntoByteArray(byte byte1, byte byte2) {
        byte[] byteArray = new byte[2];
        byteArray[0] = byte1;
        byteArray[1] = byte2;
        return byteArray;
    }
}

