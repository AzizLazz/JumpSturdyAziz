package misc.deprecated;

import java.util.Arrays;

public class JumpSturdyBoard {
    public static final int EMPTY = 0;
    public static final int CORNER = 9; // -1
    public static final int RED_ON_RED = 1;  // 'A'
    public static final int RED_ON_BLUE = 2; // 'B'
    public static final int BLUE_ON_BLUE = 3; // 'C'
    public static final int BLUE_ON_RED = 4;  // 'D'
    public static final int RED = 5;          // 'X'
    public static final int BLUE = 6;         // 'Y'

    private static final String[][] TEMP_MAPPINGS = {
            {"r0", "X"},
            {"b0", "Y"},
            {"rr", "A"},
            {"rb", "D"}, // Blue on red
            {"bb", "C"},
            {"br", "B"} // Red on blue
    };
    public int[][] board = new int[8][8];


    public JumpSturdyBoard() {
        //Fill completely with empty
        Arrays.fill(board, new int[]{0, 0, 0, 0, 0, 0, 0, 0});
        board[0][0] = CORNER;
        board[7][7] = CORNER;
        board[0][7] = CORNER;
        board[7][0] = CORNER;
    }

    public JumpSturdyBoard(String fen) {
        board = fenToBoard(fen);
    }

    public String boardToFen() {
        StringBuilder fenBuilder = new StringBuilder();
        for (int row = 0; row < board.length; row++) {
            if (row > 0) {
                fenBuilder.append('/');
            }
            int emptyCount = 0;
            for (int col = 0; col < board[row].length; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) { // For the number of empty
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount);
                        emptyCount = 0;
                    }
                    fenBuilder.append(getFenCharacter(piece)); // Just add the character
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount);
            }
        }
        String fen = fenBuilder.toString();
        for (String[] mapping : TEMP_MAPPINGS) { // Put in the normal characters instead of mappings again e.g. X to r0
            fen = fen.replace(mapping[1], mapping[0]);
        }
        return fen.replace(".", "");//Finally, remove all .dots (empty space)
    }

    private char getFenCharacter(int piece) {
        return switch (piece) {
            case RED_ON_RED -> 'A';
            case RED_ON_BLUE -> 'B';
            case BLUE_ON_BLUE -> 'C';
            case BLUE_ON_RED -> 'D';
            case RED -> 'X';
            case BLUE -> 'Y';
            default -> '.'; // Handle empty spaces as . because char cant be empty
        };
    }

    public static int[] coordinatesFromMove(String move) {
        String[] moveFromTo = move.split("-");//Divide
        int[] first = coordinatesForSinglePosition(moveFromTo[0]);
        int[] second = coordinatesForSinglePosition(moveFromTo[1]);
        return new int[]{first[0], first[1], second[0], second[1]}; // First two are the original, next two are the future
    }

    public static int[] coordinatesForSinglePosition(String position) { //Corners not handled yet, perhaps throw
        char letter = position.charAt(0);
        char number = position.charAt(1);
        return new int[]{8 - Character.getNumericValue(number), (letter - 'A')};//8- because started from top, -'A' so B has value 1, G is 6 etc
    }


    private String cleanFen(String fen) {// Makes FEN easier to read using mappings that occupy 1 character
        for (String[] mapping : TEMP_MAPPINGS) {
            fen = fen.replace(mapping[0], mapping[1]);
        }
        return fen;
    }

    public int[][] fenToBoard(String fen) {
        int[][] board = new int[8][8];
        board[0] = new int[6];
        board[7] = new int[6];

        int row = 7, col = 0;
        fen = cleanFen(fen); // Clean/Map Fen to make it easier

        for (int i = 0; i < fen.length() ; i++) {
            char c = fen.charAt(i);
            if (c == '/') {
                row--;
                col = 0;
            } else if (Character.isDigit(c)) {
                int count = c - '0'; // Convert char to number
                for (int j = 0; j < count; j++) {
                    board[row][col++] = EMPTY;
                }
            } else {
                board[row][col++] = getPieceValue(c);
            }
        }
        board[0] = surroundArray(board[0]); //Add the corners to first and last row
        board[7] = surroundArray(board[7]);
        return board;
    }

    public static int[] surroundArray(int[] original) {
        int[] newArray = new int[original.length + 2];
        // Set the first and last elements to -1
        newArray[0] = CORNER;
        newArray[newArray.length - 1] = CORNER;
        // Copy the original array into the new array starting at index 1
        System.arraycopy(original, 0, newArray, 1, original.length);
        return newArray;
    }

    private int getPieceValue(char c) {
        return switch (c) {
            case 'A' -> RED_ON_RED;
            case 'B' -> RED_ON_BLUE;
            case 'C' -> BLUE_ON_BLUE;
            case 'D' -> BLUE_ON_RED;
            case 'X' -> RED;
            case 'Y' -> BLUE;
            default -> EMPTY; // Default case handles unexpected characters
        };
    }


    public boolean movePiece(int startX, int startY, int endX, int endY) {
        if (!isValidPosition(endX, endY) || board[startX][startY] == EMPTY) return false;

        int piece = board[startX][startY];
        int targetPiece = board[endX][endY];

        boolean isValidMove = (isSinglePiece(piece) && handleSinglePieceMove(piece, startX, startY, endX, endY, targetPiece)) ||
                (isDoublePiece(piece) && handleKnightMove(startX, startY, endX, endY));

        if (isValidMove) {
            executeMove(startX, startY, endX, endY, piece, targetPiece);
            return true;
        }
        return false;
    }

    private boolean handleSinglePieceMove(int piece, int startX, int startY, int endX, int endY, int targetPiece) {
        // Allow movement into an empty space or capture if the target is an enemy's single piece
        int direction = piece == RED ? 1 : -1;//Down or up
        //Same X, either up or down 1, empty or own (single) to create a double
        return ((startX == endX && (endY - startY == direction)) && targetPiece == EMPTY || targetPiece == piece) ||
                //diagonal, forward and one to side when piece is enemy
                (Math.abs(startX - endX) == 1 && (endY - startY == direction) && isEnemy(piece, targetPiece));
    }

    private boolean handleKnightMove(int startX, int startY, int endX, int endY) {
        // Knight-like move validation
        return (Math.abs(startX - endX) == 2 && Math.abs(startY - endY) == 1) ||
                (Math.abs(startX - endX) == 1 && Math.abs(startY - endY) == 2);
    }

    private void executeMove(int startX, int startY, int endX, int endY, int piece, int targetPiece) {
        int newPiece = determineNewPiece(piece, targetPiece);
        board[endX][endY] = newPiece;
        board[startX][startY] = isDoublePiece(piece) ? getBottomPiece(piece) : EMPTY;
    }

    private int determineNewPiece(int movingPiece, int targetPiece) {
        if (targetPiece == EMPTY) return movingPiece;
        if (isSinglePiece(targetPiece)) return createDoublePiece(movingPiece, targetPiece);
        return movingPiece; // Capture only the top piece if the target is a double
    }

    private int createDoublePiece(int topPiece, int bottomPiece) {
        // Logic to create a new double piece based on the types of the top and bottom pieces
        return switch (topPiece) {
            case RED -> (bottomPiece == RED) ? RED_ON_RED : RED_ON_BLUE;
            case BLUE -> (bottomPiece == BLUE) ? BLUE_ON_BLUE : BLUE_ON_RED;
            default -> topPiece;
        };
    }

    private boolean isEnemy(int piece, int targetPiece) {
        // Simplified checking for enemy pieces
        return (piece == RED && (targetPiece == BLUE || targetPiece == BLUE_ON_BLUE || targetPiece == BLUE_ON_RED)) ||
                (piece == BLUE && (targetPiece == RED || targetPiece == RED_ON_RED || targetPiece == RED_ON_BLUE));
    }

    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < board.length && y >= 0 && y < board[0].length && board[x][y] != CORNER;
    }

    private boolean isSinglePiece(int piece) {
        return piece == RED || piece == BLUE;
    }

    private boolean isDoublePiece(int piece) {
        return piece == RED_ON_RED || piece == RED_ON_BLUE || piece == BLUE_ON_BLUE || piece == BLUE_ON_RED;
    }

    private int getBottomPiece(int doublePiece) {
        // Return the bottom piece of a double
        return switch (doublePiece) {
            case RED_ON_RED, RED_ON_BLUE -> RED;
            case BLUE_ON_BLUE, BLUE_ON_RED -> BLUE;
            default -> EMPTY;
        };
    }


    @Override
    public String toString() {
        return Arrays.deepToString(board) + "\nFEN: " + boardToFen() + "\n";
    }

    public void printBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(this.board[i][j] + " ");
            }
            System.out.println();
        }
    }
}