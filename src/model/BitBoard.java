package model;

package model;

import ai.BitBoardManipulation;
import ai.Evaluate;
import misc.Tools;

import java.util.*;
import java.util.stream.Collectors;

import static misc.deprecated.JumpSturdyBoard.*;
import static misc.deprecated.JumpSturdyBoard.BLUE;
import static misc.deprecated.JumpSturdyBoard.RED;
import static misc.Tools.*;

public class BitBoard {

    public boolean redsTurn = true;
    private static final int BOARD_WIDTH = 8;
    private static final int BOARD_HEIGHT = 8;

    public static final byte WINNER_ONGOING = 0;
    public static final byte WINNER_RED = 1;
    public static final byte WINNER_BLUE = 2;
    public static final byte WINNER_DRAW = 3;
    public static final long CORNER_MASK = ~(1L | (1L << 7) | (1L << 56) | (1L << 63));
    // Bitboards for the game pieces
    public long redSingles;   // Bitboard for red single pieces
    public long blueSingles;  // Bitboard for blue single pieces
    public long redDoubles;   // Bitboard for red double pieces (top knights)
    public long blueDoubles;  // Bitboard for blue double pieces (top knights)

    public long red_on_blue;   // Bitboard for red double pieces (top knights)
    public long blue_on_red;  // Bitboard for blue double pieces (top knights)

    public byte[] previousMove = new byte[]{};


    // Masks for edges to handle movements correctly
    public static final long NOT_A_FILE = 0xfefefefefefefefeL; // 11111110...
    public static final long NOT_H_FILE = 0x7f7f7f7f7f7f7f7fL; // 01111111...
    public static final long NOT_AB_FILE = 0xFCFCFCFCFCFCFCFCL; // Not columns A and B
    public static final long NOT_GH_FILE = 0x3F3F3F3F3F3F3F3FL; // Not columns G and H

    public static final long topRowMask = 0xFFL;  // Mask for the top row (bits 0-7)
    public static final long bottomRowMask = 0xFFL << 56;  // Mask for the bottom row (bits 56-63)

    // Directions for single figures based on team
    private static final int RED_DIRECTION = 8;   // Red moves down
    private static final int BLUE_DIRECTION = -8;   // Blue moves up

    // Constructor to initialize the game board
    public BitBoard() {
        //removed, probably not necessary unless set in a weird way
        //setupInitialBoard();
    }

    // Setup the initial board with pieces in their starting positions
    private void setupInitialBoard() {
        // Initialize red and blue singles on their respective rows
        //redSingles = (0xFFL << (8 * 2)) | (0xFFL << (8 * 3));
        //blueSingles = (0xFFL << (8 * 5)) | (0xFFL << (8 * 6));

        redSingles = 0L;
        blueSingles = 0L;

        // Doubles are initially empty
        redDoubles = 0L;
        blueDoubles = 0L;
        red_on_blue = 0L;
        blue_on_red = 0L;
    }

    public BitBoard(String fen) {
        readFEN(fen);
    }

    public void readFEN(String fen) {

        // Define the FEN mappings
        final String[][] TEMP_MAPPINGS = {
                {"r0", "X"},
                {"b0", "Y"},
                {"rr", "A"},
                {"rb", "D"}, // Blue on red
                {"bb", "C"},
                {"br", "B"} // Red on blue
        };

        for (String[] mapping : TEMP_MAPPINGS) { // For easier indexes
            fen = fen.replace(mapping[0], mapping[1]);
        }

        String[] rows = fen.split("/");

        for (int row = 0; row < rows.length; row++) {
            int col = (row == 0 || row == 7) ? 1 : 0;
            for (int i = 0; i < rows[row].length(); i++) {
                char c = rows[row].charAt(i);
                if (Character.isDigit(c)) {
                    // Empty squares
                    col += c - '0';
                } else {
                    byte index = (byte) ((7 - row) * 8 + col);
                    switch (c) {
                        case 'X' -> {
                            redSingles |= 1L << index;
                        }
                        case 'Y' -> {
                            blueSingles |= 1L << index;
                        }
                        case 'A' -> {
                            redDoubles |= 1L << index;
                        }
                        case 'C' -> {
                            blueDoubles |= 1L << index;
                        }
                        case 'D' -> {
                            blue_on_red |= 1L << index;
                        }
                        case 'B' -> {
                            red_on_blue |= 1L << index;
                        }
                    }
                    col++;
                }
            }
        }
    }

    public String toFEN() {
        StringBuilder fen = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;

            for (int col = (row == 0 || row == 7) ? 1 : 0; col < ((row == 0 || row == 7) ? 7 : 8); col++) {//Start end have corners,6 length
                int index = (7 - row) * 8 + col;
                if (index == 0 || index == 7 || index == 56) {//63 never happens
                    continue; // Skip corners
                }
                String piece; // Set depending on bitboards
                if ((redSingles & (1L << index)) != 0) {
                    piece = "r0";
                } else if ((blueSingles & (1L << index)) != 0) {
                    piece = "b0";
                } else if ((redDoubles & (1L << index)) != 0) {
                    piece = "rr";
                } else if ((blueDoubles & (1L << index)) != 0) {
                    piece = "bb";
                } else if ((blue_on_red & (1L << index)) != 0) {
                    piece = "rb";
                } else if ((red_on_blue & (1L << index)) != 0) {
                    piece = "br";
                } else {
                    emptyCount++;
                    piece = "";
                }

                if (!piece.isEmpty()) { // Count if empty for number
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece);
                } else if (col == 7 || col == 6 && (row == 0 || row == 7)) {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                }
            }
            if (row < 7) {
                fen.append('/');
            }
        }
        return fen.toString();
    }

    // Method to read a 2D array and convert it into a bitboard
    public void readBoard(int[][] board) {
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                int piece = board[row][col];
                int index = row * BOARD_WIDTH + col;
                switch (piece) {
                    case EMPTY, CORNER:
                        // Do nothing
                        break;
                    case RED_ON_RED:
                        redDoubles |= 1L << index;
                        break;
                    case RED_ON_BLUE:
                        red_on_blue |= 1L << index;
                        break;
                    case BLUE_ON_BLUE:
                        blueDoubles |= 1L << index;
                        break;
                    case BLUE_ON_RED:
                        blue_on_red |= 1L << index;
                        break;
                    case RED:
                        redSingles |= 1L << index;
                        break;
                    case BLUE:
                        blueSingles |= 1L << index;
                        break;
                }
            }
        }
    }

    //Returns if the piece at position belongs to red
    public boolean isItRedsTurnByPositionOfPieces(byte position) {
        detectOverlap(redSingles, blueSingles, redDoubles, blueDoubles, red_on_blue, blue_on_red);
        boolean redSingleBitSet = (redSingles & (1L << position)) != 0;
        boolean blueSingleBitSet = (blueSingles & (1L << position)) != 0;
        boolean redDoubleBitSet = (redDoubles & (1L << position)) != 0;
        boolean blueDoubleBitSet = (blueDoubles & (1L << position)) != 0;
        boolean redOnBlueBitSet = (red_on_blue & (1L << position)) != 0;
        boolean blueOnRedBitSet = (blue_on_red & (1L << position)) != 0;
        if (!redSingleBitSet && !blueSingleBitSet && !redDoubleBitSet && !blueDoubleBitSet && !redOnBlueBitSet && !blueOnRedBitSet) {
            printCommented("ERROR");
            throw new RuntimeException("Invalid position, either empty or corner" + position + " " + Tools.indexToStringPosition(position));
        }
        return redSingleBitSet || redDoubleBitSet || redOnBlueBitSet;//No check for all necessary as otherwise would throw, blues turn otherwise
    }

    //Static as it might be used in other classes too
    //TODO: REMOVE LATER FOR PERFORMANCE REASONS WHEN NOT THROWING
/*    public static void detectOverlap(long redSingles, long blueSingles, long redDoubles, long blueDoubles, long red_on_blue, long blue_on_red) {  //Should not be called after bug has been detected/only in tests for performance reasons
        boolean noOverlap = true;

        for (byte position = 0; position < 64; position++) {
            int bitCount = 0;

            if ((redSingles & (1L << position)) != 0) bitCount++;
            if ((blueSingles & (1L << position)) != 0) bitCount++;
            if ((redDoubles & (1L << position)) != 0) bitCount++;
            if ((blueDoubles & (1L << position)) != 0) bitCount++;
            if ((red_on_blue & (1L << position)) != 0) bitCount++;
            if ((blue_on_red & (1L << position)) != 0) bitCount++;

            if (bitCount > 1) {
                noOverlap = false;
                Tools.printInColor("Overlap detected at position: " + position + " " + Tools.indexToStringPosition(position), true);
            }
        }

        if (!noOverlap) {
            // Tools.printInColor("No overlap in bit positions.", false);
            throw new IllegalStateException("Overlaps in bitboards detected");
        }
    }*/

    public static void detectOverlap(long redSingles, long blueSingles, long redDoubles, long blueDoubles, long red_on_blue, long blue_on_red) {
        boolean noOverlap = true;

        for (byte position = 0; position < 64; position++) {
            int bitCount = 0;
            StringBuilder overlapInfo = new StringBuilder("Overlap detected at position: " + position + " " + Tools.indexToStringPosition(position) + " in bitboards: ");

            if ((redSingles & (1L << position)) != 0) {
                bitCount++;
                overlapInfo.append("redSingles ");
            }
            if ((blueSingles & (1L << position)) != 0) {
                bitCount++;
                overlapInfo.append("blueSingles ");
            }
            if ((redDoubles & (1L << position)) != 0) {
                bitCount++;
                overlapInfo.append("redDoubles ");
            }
            if ((blueDoubles & (1L << position)) != 0) {
                bitCount++;
                overlapInfo.append("blueDoubles ");
            }
            if ((red_on_blue & (1L << position)) != 0) {
                bitCount++;
                overlapInfo.append("red_on_blue ");
            }
            if ((blue_on_red & (1L << position)) != 0) {
                bitCount++;
                overlapInfo.append("blue_on_red ");
            }

            if (bitCount > 1) {
                noOverlap = false;
                Tools.printInColor(overlapInfo.toString(), true);
            }
        }

        if (!noOverlap) {
            throw new IllegalStateException("Overlaps in bitboards detected");
        }
    }

    public void detectOverlap(){//Member for easier call
        detectOverlap(redSingles,blueSingles,redDoubles,blueDoubles,red_on_blue,blue_on_red);
    }

    @Deprecated //DONT USE IN PERFORMANCE INTENSIVE LOCATIONS
    public int eval(){
        return Evaluate.evaluateComplex(true,redSingles,blueSingles,redDoubles,blueDoubles,red_on_blue,blue_on_red);
    }

    public byte currentWinningState() {
        // Define masks for the top and bottom rows

        long bluePieces = blueSingles | blueDoubles | blue_on_red;
        long redPieces = redSingles | redDoubles | red_on_blue;

        // Check if any blue piece is in the top row or no red pieces
        if ((bluePieces & topRowMask) != 0 || redPieces == 0) {
            return WINNER_BLUE;
        }
        // Check if any red piece is in the bottom row or no blue pieces
        else if ((redPieces & bottomRowMask) != 0 || bluePieces == 0) {
            return WINNER_RED;
        }
        // Check if there are any possible moves for blue
        else if (getPossibleMovesForTeam(false) == 0) {
            // Check if there are any possible moves for red, if no -> draw
            if (getPossibleMovesForTeam(true) == 0) return WINNER_DRAW;
            else return WINNER_RED;
        }
        // Check if there are any possible moves for red
        else if (getPossibleMovesForTeam(true) == 0) {
            // Check if there are any possible moves for blue, if no -> draw
            if (getPossibleMovesForTeam(false) == 0) return WINNER_DRAW;
            else return WINNER_BLUE;
        }
        return WINNER_ONGOING;
    }

    public static byte currentWinningStateStaticOptimized(long r, long b, long rr, long bb, long br, long rb) {
        // Define masks for the top and bottom rows

        long bluePieces = b| bb | rb;
        long redPieces = r | rr | br;

        // Check if any blue piece is in the top row or no red pieces
        if ((bluePieces & topRowMask) != 0 || redPieces == 0) {
            return WINNER_BLUE;
        }
        // Check if any red piece is in the bottom row or no blue pieces
        else if ((redPieces & bottomRowMask) != 0 || bluePieces == 0) {
            return WINNER_RED;
        }
        //TODO: changed the next part, lets just check if there are figures left, contest they would lose anyways and here it probably doesnt matter? Weird errors but performance gain


/*        // Check if there are any possible moves for blue
        else if (getPossibleMovesForTeam(false) == 0) {
            // Check if there are any possible moves for red, if no -> draw
            if (getPossibleMovesForTeam(true) == 0) return WINNER_DRAW;
            else return WINNER_RED;
        }
        // Check if there are any possible moves for red
        else if (getPossibleMovesForTeam(true) == 0) {
            // Check if there are any possible moves for blue, if no -> draw
            if (getPossibleMovesForTeam(false) == 0) return WINNER_DRAW;
            else return WINNER_BLUE;
        }*/
        return WINNER_ONGOING;
    }

    public void doMoveNoParse(byte[] move,boolean isRedTurn, boolean checkIfPossible){
        if(preserveAllMoves){
            byte[] addedMoves = new byte[previousMove.length+2];
            System.arraycopy(previousMove, 0, addedMoves, 0, previousMove.length);
            //Append
            addedMoves[previousMove.length] = move[0];
            addedMoves[previousMove.length + 1] = move[1];
            previousMove = addedMoves;
        }else{
            previousMove = move;
        }


        if (checkIfPossible) {

            if (isItRedsTurnByPositionOfPieces(move[0]) != isRedTurn) {
                throw new IllegalMoveException("Player can't move enemy piece:"+move[0]+"-"+move[1]+" > "+Tools.parseMoveToString(move));
            }
            long possibleMoves = getPossibleMovesForIndividualPiece(move[0], isRedTurn);

            if ((possibleMoves & (1L << move[1])) == 0) { //Move not included, index conversion
                throw new IllegalMoveException("Move is not possible:" + move);
            }
        }
        if (isRedTurn) {
            if ((redDoubles != 0 && (redDoubles & (1L << move[0])) != 0)
                    || (red_on_blue != 0 && (red_on_blue & (1L << move[0])) != 0)) {
                moveDoublePiece(move[0], move[1], true);
            } else {
                moveSinglePiece(move[0], move[1], true);
            }
        } else {
            if ((blueDoubles != 0 && (blueDoubles & (1L << move[0])) != 0)
                    || (blue_on_red != 0 && (blue_on_red & (1L << move[0])) != 0)) {
                moveDoublePiece(move[0], move[1], false);
            } else {
                moveSinglePiece(move[0], move[1], false);
            }
        }
    }

    @Deprecated
    public boolean doMove(String move, boolean isRedTurn, boolean checkIfPossible) {
        byte[] indices = parseMove(move);
        if(preserveAllMoves){
            byte[] addedMoves = new byte[previousMove.length+2];
            System.arraycopy(previousMove, 0, addedMoves, 0, previousMove.length);
            //Append
            addedMoves[previousMove.length] = indices[0];
            addedMoves[previousMove.length + 1] = indices[1];
            previousMove = addedMoves;
        }else{
            previousMove = indices;

        }

        if (isItRedsTurnByPositionOfPieces(indices[0]) != isRedTurn) {
            throw new BoardException(this,"Player can't move enemy piece:"+indices[0]+"-"+indices[1]+" > "+Tools.parseMoveToString(indices));
        }
        if (checkIfPossible) {
            long possibleMoves = getPossibleMovesForIndividualPiece(indices[0], isRedTurn);

            if ((possibleMoves & (1L << indices[1])) == 0) { //Move not included, index conversion
                throw new BoardException(this,"Move is not possible:" + move+" isred:"+isRedTurn);
            }
        }
        if (isRedTurn) {
            if ((redDoubles != 0 && (redDoubles & (1L << indices[0])) != 0)
                    || (red_on_blue != 0 && (red_on_blue & (1L << indices[0])) != 0)) {
                moveDoublePiece(indices[0], indices[1], true);
            } else {
                moveSinglePiece(indices[0], indices[1], true);
            }
        } else {
            if ((blueDoubles != 0 && (blueDoubles & (1L << indices[0])) != 0)
                    || (blue_on_red != 0 && (blue_on_red & (1L << indices[0])) != 0)) {
                moveDoublePiece(indices[0], indices[1], false);
            } else {
                moveSinglePiece(indices[0], indices[1], false);
            }
        }
        return !isRedTurn; //Now optionally returns whose turn it is in case we test chaining same team calls etc in later steps
    }




    public void moveSinglePiece(byte fromIndex, byte toIndex, boolean isRed) {
        long ownSingles = isRed ? redSingles : blueSingles;
        long enemySingles = isRed ? blueSingles : redSingles;
        long ownDoubles = isRed ? redDoubles : blueDoubles;
        long enemyDoubles = isRed ? blueDoubles : redDoubles;
        long ownOnEnemy = isRed ? red_on_blue : blue_on_red;
        long enemyOnOwn = isRed ? blue_on_red : red_on_blue;

        // Clear the bit at the original position
        ownSingles &= ~(1L << fromIndex);

        // Handling different scenarios on the destination
        if ((enemySingles & (1L << toIndex)) != 0) {
            // Capture enemy single, transform to own single
            ownSingles |= (1L << toIndex);
            enemySingles &= ~(1L << toIndex);
        } else if ((enemyDoubles & (1L << toIndex)) != 0) {
            // Lands on enemy double, transforms to ownOnEnemy
            ownOnEnemy |= (1L << toIndex);
            enemyDoubles &= ~(1L << toIndex);
        } else if ((ownSingles & (1L << toIndex)) != 0) {
            // Lands on enemy double, transforms to ownOnEnemy
            ownDoubles |= (1L << toIndex);
            ownSingles &= ~(1L << toIndex);
        } else if ((enemyOnOwn & (1L << toIndex)) != 0) {
            // Lands on enemyOnOwn, transforms to own double
            ownDoubles |= (1L << toIndex);
            enemyOnOwn &= ~(1L << toIndex);
        } else {
            // Regular move to an empty square, becomes a single
            ownSingles |= (1L << toIndex);
        }

        // Update the bitboards
        redSingles = isRed ? ownSingles : enemySingles;
        blueSingles = isRed ? enemySingles : ownSingles;
        redDoubles = isRed ? ownDoubles : enemyDoubles;
        blueDoubles = isRed ? enemyDoubles : ownDoubles;
        red_on_blue = isRed ? ownOnEnemy : enemyOnOwn;
        blue_on_red = isRed ? enemyOnOwn : ownOnEnemy;
    }

    public void moveDoublePiece(byte fromIndex, byte toIndex, boolean isRed) {
        long ownDoubles = isRed ? redDoubles : blueDoubles;
        long ownSingles = isRed ? redSingles : blueSingles;
        long enemySingles = isRed ? blueSingles : redSingles;
        long enemyDoubles = isRed ? blueDoubles : redDoubles;
        long enemyOnOwn = isRed ? blue_on_red : red_on_blue;
        long ownOnEnemy = isRed ? red_on_blue : blue_on_red;


        boolean bottomIsEnemy = (ownOnEnemy & (1L << fromIndex)) != 0;
        // Remove the double piece from the original position
        ownDoubles &= ~(1L << fromIndex);
        ownOnEnemy &= ~(1L << fromIndex);
        // Determine the bottom type of the double

        //System.out.println("bt" + bottomIsEnemy);
        // Handle the landing cases
        if ((ownSingles & (1L << toIndex)) != 0) {
            // Landing on own single, turn it into own double
            ownDoubles |= (1L << toIndex);
            ownSingles &= ~(1L << toIndex);
        } else if ((enemyOnOwn & (1L << toIndex)) != 0) {
            // Landing on enemy_on_own, turn it into own double
            ownDoubles |= (1L << toIndex);
            enemyOnOwn &= ~(1L << toIndex);
        } else if ((enemyDoubles & (1L << toIndex)) != 0) {
            // Landing on enemy double, turn it into own double
            ownOnEnemy |= (1L << toIndex);
            enemyDoubles &= ~(1L << toIndex);
        } else if ((enemySingles & (1L << toIndex)) != 0) {
            // Landing on enemy single, turn it into own single
            ownSingles |= (1L << toIndex);
            enemySingles &= ~(1L << toIndex);
        } else { //TODO: hope all cases are covered and nothing forgotten
            // Regular move to empty space, place the top of the double as a single
            ownSingles |= (1L << toIndex);

        }

        // Always turn the former bottom of the double into a single at the original position
        if (bottomIsEnemy) {
            enemySingles |= (1L << fromIndex);
        } else {
            ownSingles |= (1L << fromIndex);
        }

        // Update the state using ternary operators
        redDoubles = isRed ? ownDoubles : enemyDoubles;
        blueDoubles = isRed ? enemyDoubles : ownDoubles;
        redSingles = isRed ? ownSingles : enemySingles;
        blueSingles = isRed ? enemySingles : ownSingles;
        blue_on_red = isRed ? enemyOnOwn : ownOnEnemy;
        red_on_blue = isRed ? ownOnEnemy : enemyOnOwn;
    }


    public long getPossibleMovesForTeam(boolean red) {
        if (red) {
            return getPossibleMovesSingles(redSingles, true) | getPossibleMovesDoubles(redDoubles | red_on_blue, true);
        } else {
            return getPossibleMovesSingles(blueSingles, false) | getPossibleMovesDoubles(blueDoubles | blue_on_red, false);
        }
    }

    public long getPossibleMovesSingles(long singles, boolean isRed) {
        int direction = isRed ? RED_DIRECTION : BLUE_DIRECTION;
        long emptySpaces = ~(redSingles | blueSingles | redDoubles | blueDoubles | red_on_blue | blue_on_red) & CORNER_MASK; // All empty spaces
        long enemyPieces = isRed ? (blue_on_red | blueDoubles | blueSingles) : (redSingles | redDoubles | red_on_blue); // Enemy single figures

        //commentedBits("Empty:",emptySpaces);
        //commentedBits("Cornermask",CORNER_MASK);
        //commentedBits("not_a",NOT_A_FILE);
        //commentedBits("Enemy:",enemyPieces);
        //commentedBits("Own:",singles);

        long jumpable = (emptySpaces | (isRed ? redSingles : blueSingles));
        // Forward moves (no capture)
        long forwardMoves = shift(singles, direction) & jumpable;//Removes all occupied spaces, TODO: maybe read doubleGenesis
        //commentedBits("Fwd:",forwardMoves);
        // Side moves (left and right)
        long leftMoves = shift(singles & NOT_A_FILE, -1) & jumpable;
        long rightMoves = shift(singles & NOT_H_FILE, 1) & jumpable;

        // Capture moves (diagonal)
        long leftCapture = shift(singles & NOT_A_FILE, direction - 1) & enemyPieces; //+-7 9 so diagonal
        long rightCapture = shift(singles & NOT_H_FILE, direction + 1) & enemyPieces;
        //System.out.println("Possible moves:");
        return forwardMoves | leftMoves | rightMoves | leftCapture | rightCapture;
    }





    public long getPossibleMovesDoubles(long doubles, boolean isRed) {//FIXED
        // All occupied spaces
        long occupiedSpaces = redSingles | blueSingles | redDoubles | blueDoubles | red_on_blue | blue_on_red;
        long emptySpaces = ~occupiedSpaces & CORNER_MASK; // All empty spaces, excluding corners

        //long emptyOrSingleDoubleable = (emptySpaces | (isRed ? redSingles : blueSingles) | (isRed? redDoubles : blueDoubles));
        long jumpable = (emptySpaces | (redSingles | blueSingles) | (isRed ? blueDoubles : redDoubles) | (isRed ? blue_on_red : red_on_blue));

        long twoForwardOneLeft = shift(doubles & (isRed ? NOT_A_FILE : NOT_H_FILE), isRed ? 15 : -15);
        long oneForwardTwoLeft = shift(doubles & (isRed ? NOT_AB_FILE : NOT_GH_FILE), isRed ? 6 : -6);

        long twoForwardOneRight = shift(doubles & (isRed ? NOT_H_FILE : NOT_A_FILE), isRed ? 17 : -17);
        long oneForwardTwoRight = shift(doubles & (isRed ? NOT_GH_FILE : NOT_AB_FILE), isRed ? 10 : -10);

        return jumpable & (twoForwardOneLeft | oneForwardTwoLeft | twoForwardOneRight | oneForwardTwoRight);
        //All possible moves for doubles. We can capture on all 4 fields, though do we need extra capture?

    }

    /**
     * Gathers all possible moves for the current player.
     * This method decides the piece color based on whose turn it is,
     * gathers all singles and doubles, and calculates possible moves for each.
     *
     * @return List of all possible moves in standard chess notation, like "A2-A3".
     */

    @Deprecated
    public List<String> getAllPossibleMoveStringsDeprecated(boolean isRed) {
        List<String> moves = new ArrayList<>();
        long singles = isRed ? redSingles : blueSingles;
        long doubles = isRed ? redDoubles | red_on_blue : blueDoubles | blue_on_red;

        moves.addAll(generateMovesForPieces(singles, getPossibleMovesSingles(singles, isRed), isRed));
        moves.addAll(generateMovesForPieces(doubles, getPossibleMovesDoubles(doubles, isRed), isRed));

        return moves.stream().sorted().collect(Collectors.toList());
    }

    public List<String> getAllPossibleMoveStrings(boolean isRed){
        byte[][] moves = generateByteMovesForPiecesWithList(isRed);
        List<String> asText = new ArrayList<>(moves.length);
        for (byte[] move : moves) {
            asText.add(Tools.parseMoveToString(move));
        }
        return asText;
    }

    public List<String> getAllPossibleMoveStringsSorted(boolean isRed){
        return getAllPossibleMoveStrings(isRed).stream().sorted().collect(Collectors.toList());
    }

    public byte[][] getAllPossibleMovesByte(boolean isRed) {
        return generateByteMovesForPiecesWithList(isRed);
    }

    public byte[][] getAllPossibleMovesByteSorted(boolean isRed){
        return generateSortedByteMovesForPiecesWithList(isRed);
    }

    /**
     * Generates a list of moves from a given set of pieces and their possible moves.
     * This method iterates through each bit of the pieces' bitboard, checks if a piece is present,
     * and then calculates valid moves for it using another provided bitboard of possible moves.
     *
     * @param pieces        A long representing the bitboard of pieces.
     * @param possibleMoves A long representing the bitboard of all possible moves for these pieces.
     * @param isRed         A boolean indicating if the current moves are for Red pieces.
     * @return A list of moves in the format "from-to" like "E2-E4".
     */
    private List<String> generateMovesForPieces(long pieces, long possibleMoves, boolean isRed) {
        List<String> moveList = new ArrayList<>();
        for (byte fromIndex = 0; fromIndex < 64; fromIndex++) {
            if ((pieces & (1L << fromIndex)) != 0) {  // There is a piece at fromIndex
                long movesFromThisPiece = possibleMoves & getPossibleMovesForIndividualPiece(fromIndex, isRed);
                for (byte toIndex = 0; toIndex < 64; toIndex++) {
                    if ((movesFromThisPiece & (1L << toIndex)) != 0) {  // Valid move to toIndex
                        moveList.add(indexToStringPosition(fromIndex) + "-" + indexToStringPosition(toIndex));
                    }
                }
            }
        }
        return moveList;
    }


    /**
     *
     * More efficient version, no Text parsing. tested Performance difference seems to be 5x as fast
     *
     * **/

    private byte[][] generateByteMovesForPiecesWithList(boolean isRed) {

        List<byte[]> moveList = new ArrayList<>();
        long ourPieces = (isRed?(redSingles|redDoubles|red_on_blue):(blueSingles|blueDoubles|blue_on_red));
        for (byte fromIndex = 1; fromIndex < 63; fromIndex++) {
            if((ourPieces & (1L << fromIndex)) != 0){//IF there is a piece at position
                long moves = getPossibleMovesForIndividualPiece(fromIndex,isRed);
                for (byte toIndex = 1; toIndex < 63; toIndex++) {
                    if ((moves & (1L << toIndex)) != 0) {  // Valid move to toIndex
                        //moveList.add(indexToStringPosition(fromIndex) + "-" + indexToStringPosition(toIndex));
                        //moveArray[counter++] = ;
                        moveList.add(new byte[]{fromIndex,toIndex});
                    }
                }
            }

        }
        return moveList.toArray(new byte[moveList.size()][]);
    }

    private byte[][] generateSortedByteMovesForPiecesWithList(boolean isRed) {

        List<byte[]> normalMoveList = new ArrayList<>();
        List<byte[]> prioritizedMoveList = new ArrayList<>();

        long ourPieces = (isRed?(redSingles|redDoubles|red_on_blue):(blueSingles|blueDoubles|blue_on_red));
        for (byte fromIndex = 62; fromIndex > 0; fromIndex--) {
            if(fromIndex==7||fromIndex==56)continue;//Skip corners
            if((ourPieces & (1L << fromIndex)) != 0){//IF there is a piece at position
                long moves = getPossibleMovesForIndividualPiece(fromIndex,isRed);
                for (byte toIndex = 62; toIndex > 0; toIndex--) {
                    if(toIndex==7||toIndex==56)continue;//Skip corners
                    if ((moves & (1L << toIndex)) != 0) {  // Valid move to toIndex
                        //moveList.add(indexToStringPosition(fromIndex) + "-" + indexToStringPosition(toIndex));
                        //moveArray[counter++] = ;
                        int dif = (fromIndex-toIndex);
                        dif= Math.abs(dif);
/*                        if(dif == 7 || dif == 9){//Capture with single, good move
                            prioritizedMoveList.add(new byte[]{fromIndex,toIndex});
                        }else if(dif == 15||dif==17){//TwoForwardJump
                            prioritizedMoveList.add(new byte[]{fromIndex,toIndex});
                        }else{
                            normalMoveList.add(new byte[]{fromIndex,toIndex});
                        }*/
                        if(dif == 7 || dif == 9||dif == 15||dif==17){//Capture with single, good move
                            prioritizedMoveList.add(new byte[]{fromIndex,toIndex});
                        }else{
                            normalMoveList.add(new byte[]{fromIndex,toIndex});
                        }
                    }
                }
            }

        }
        prioritizedMoveList.addAll(normalMoveList);//Prioritized first
        return prioritizedMoveList.toArray(new byte[normalMoveList.size()][]);
    }

    public static byte[][] generateSortedByteMovesForPiecesWithListStatic(boolean isRed,long r, long b, long rr, long bb, long br, long rb) {

        List<byte[]> normalMoveList = new ArrayList<>();
        List<byte[]> prioritizedMoveList = new ArrayList<>();

        long ourPieces = (isRed?(r|rr|br):(b|bb|rb));
        for (byte fromIndex = 62; fromIndex > 0; fromIndex--) {
            if(fromIndex==7||fromIndex==56)continue;//Skip corners
            if((ourPieces & (1L << fromIndex)) != 0){//IF there is a piece at position
                long moves = getPossibleMovesForIndividualPieceStatic(fromIndex,isRed,r,b,rr,bb,br,rb);
                for (byte toIndex = 62; toIndex > 0; toIndex--) {
                    if(toIndex==7||toIndex==56)continue;//Skip corners
                    if ((moves & (1L << toIndex)) != 0) {  // Valid move to toIndex
                        //moveList.add(indexToStringPosition(fromIndex) + "-" + indexToStringPosition(toIndex));
                        //moveArray[counter++] = ;
                        int dif = (fromIndex-toIndex);
                        dif= Math.abs(dif);
/*                        if(dif == 7 || dif == 9){//Capture with single, good move
                            prioritizedMoveList.add(new byte[]{fromIndex,toIndex});
                        }else if(dif == 15||dif==17){//TwoForwardJump
                            prioritizedMoveList.add(new byte[]{fromIndex,toIndex});
                        }else{
                            normalMoveList.add(new byte[]{fromIndex,toIndex});
                        }*/
                        if(dif == 7 || dif == 9||dif == 15||dif==17){//Capture with single, good move
                            prioritizedMoveList.add(new byte[]{fromIndex,toIndex});
                        }else{
                            normalMoveList.add(new byte[]{fromIndex,toIndex});
                        }
                    }
                }
            }

        }
        prioritizedMoveList.addAll(normalMoveList);//Prioritized first
        return prioritizedMoveList.toArray(new byte[normalMoveList.size()][]);
    }

    public static long getPossibleMovesForIndividualPieceStatic(byte index, boolean isRed,long r, long b, long rr, long bb, long br, long rb) {
        long singlePieceMask = 1L << index;
        //long moves = 0L;

        // Überprüfen, ob es sich um einen Einzelstein handelt
        if (((isRed ? r : b) & singlePieceMask) != 0) {
            // Erzeuge ein Bitboard, das nur diesen Stein enthält
            // Rufe die vorhandene Methode auf, um mögliche Züge für diesen einen Stein zu ermitteln
            return BitBoardManipulation.getPossibleMovesSingles(singlePieceMask, isRed,r,b,rr,bb,br,rb);
        }

        // Überprüfen, ob es sich um einen Doppelstein handelt
        if (((isRed ? (rr | br) : (bb | rb)) & singlePieceMask) != 0) {
            // Erzeuge ein Bitboard, das nur diesen Stein enthält

            // Rufe die vorhandene Methode auf, um mögliche Züge für diesen einen Doppelstein zu ermitteln
            return BitBoardManipulation.getPossibleMovesDoubles(singlePieceMask, isRed,r,b,rr,bb,br,rb);
        }
        throw new IllegalStateException("index"+index +" doesnt fit any figure type");
        //Removed move variable, returning directly as cant be both single and double
    }

    public void removePositionDebug(byte pos){
        redSingles &= ~(1L << pos);
        blueSingles&= ~(1L << pos);
        redDoubles&= ~(1L << pos);
        blueDoubles &= ~(1L << pos);
        red_on_blue&= ~(1L << pos);
        blue_on_red&= ~(1L << pos);
    }

    public void addPositionDebug(byte pos, String pieceType){
        pieceType = pieceType.toUpperCase();
        switch (pieceType){
            case "R" -> redSingles |= (1L << pos);
            case "B" -> blueSingles |= (1L << pos);
            case "RR" -> redDoubles |= (1L << pos);
            case "BB" -> blueDoubles |= (1L << pos);
            case "BR" -> red_on_blue |= (1L << pos);
            case "RB" -> blue_on_red |= (1L << pos);
            default -> throw new RuntimeException("Illegal pieceType");
        }
    }

    public long getPossibleMovesForIndividualPiece(byte index, boolean isRed) {
        long singlePieceMask = 1L << index;
        //long moves = 0L;

        // Überprüfen, ob es sich um einen Einzelstein handelt
        if (((isRed ? redSingles : blueSingles) & singlePieceMask) != 0) {
            // Erzeuge ein Bitboard, das nur diesen Stein enthält
            // Rufe die vorhandene Methode auf, um mögliche Züge für diesen einen Stein zu ermitteln
            return getPossibleMovesSingles(singlePieceMask, isRed);
        }

        // Überprüfen, ob es sich um einen Doppelstein handelt
        if (((isRed ? (redDoubles | red_on_blue) : (blueDoubles | blue_on_red)) & singlePieceMask) != 0) {
            // Erzeuge ein Bitboard, das nur diesen Stein enthält

            // Rufe die vorhandene Methode auf, um mögliche Züge für diesen einen Doppelstein zu ermitteln
            return getPossibleMovesDoubles(singlePieceMask, isRed);
        }
        throw new IllegalStateException("index"+index +" doesnt fit any figure type");
        //Removed move variable, returning directly as cant be both single and double
    }



    public static BitBoard fromLongArray(long[] bitBoards) {
        BitBoard newBoard = new BitBoard();
        newBoard.redSingles = bitBoards[0];
        newBoard.blueSingles = bitBoards[1];
        newBoard.redDoubles = bitBoards[2];
        newBoard.blueDoubles = bitBoards[3];
        newBoard.red_on_blue = bitBoards[4];
        newBoard.blue_on_red = bitBoards[5];
        return newBoard;
    }

    public static BitBoard fromLongs(long r, long b, long rr, long bb, long br, long rb){
        BitBoard newBoard = new BitBoard();
        newBoard.redSingles = r;
        newBoard.blueSingles =b;
        newBoard.redDoubles = rr;
        newBoard.blueDoubles =bb;
        newBoard.red_on_blue =br;
        newBoard.blue_on_red =rb;
        return newBoard;
    }

    public static BitBoard fromLongArrayWithPreviousMove(long[] bitBoards,byte[] move){
        BitBoard newBoard = new BitBoard();
        newBoard.previousMove = move;
        newBoard.redSingles = bitBoards[0];
        newBoard.blueSingles = bitBoards[1];
        newBoard.redDoubles = bitBoards[2];
        newBoard.blueDoubles = bitBoards[3];
        newBoard.red_on_blue = bitBoards[4];
        newBoard.blue_on_red = bitBoards[5];
        return newBoard;
    }

    public void deleteRandomFigure(boolean fromRed){
        byte index = Tools.getRandomIndex( (fromRed?(redSingles|redDoubles|red_on_blue):(blueSingles|blueDoubles|blue_on_red)));
        removePositionDebug(index);
    }

    @Deprecated
    public BitBoard doMoveAndReturnBitboard(byte from, byte to,boolean isRed){
        return BitBoard.fromLongArrayWithPreviousMove(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                from, to, isRed,
                redSingles, blueSingles, redDoubles, blueDoubles,
                red_on_blue, blue_on_red),new byte[]{from,to});
    }

    public String previousMoves(){
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < previousMove.length; i+=2) {
            b.append("["+Tools.indexToStringPosition(previousMove[i])+"-"+Tools.indexToStringPosition(previousMove[i+1])+"] ");
        }
        return b.toString()+" length: "+previousMove.length;
    }

    public static final boolean preserveAllMoves = true; //TODO: Probably quite a performance impact, remove when not debugging; false means only has last move, true means every move



    public BitBoard doMoveAndReturnBitboard(byte[] move,boolean isRed){
        if(preserveAllMoves){
            byte[] addedMoves = new byte[previousMove.length+2];
            System.arraycopy(previousMove, 0, addedMoves, 0, previousMove.length);
            //Append
            addedMoves[previousMove.length] = move[0];
            addedMoves[previousMove.length + 1] = move[1];
            return BitBoard.fromLongArrayWithPreviousMove(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                    move[0], move[1], isRed,
                    redSingles, blueSingles, redDoubles, blueDoubles,
                    red_on_blue, blue_on_red),addedMoves); //Added if else due to reference error
        }else{
            return BitBoard.fromLongArrayWithPreviousMove(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                    move[0], move[1], isRed,
                    redSingles, blueSingles, redDoubles, blueDoubles,
                    red_on_blue, blue_on_red),move);
        }

    }

    public long[] getPlayer(boolean isRed){
        return isRed ? new long[]{redSingles, redDoubles, red_on_blue} : new long[]{blueSingles, blueDoubles, blue_on_red};
    }

    public int countTotalPieces() {
        return Long.bitCount(redSingles) +
                Long.bitCount(blueSingles) +
                Long.bitCount(redDoubles) +
                Long.bitCount(blueDoubles) +
                Long.bitCount(red_on_blue) +
                Long.bitCount(blue_on_red);
    }


    public static void main(String[] args) {
        // Example usage
        String move = "A8-H1";
        byte[] indices = parseMove(move);
        System.out.println("From Index: " + indices[0] + ", To Index: " + indices[1]);

    }

    public void print(){
        System.out.println(this);
    }

    public void printCommented(String comment){//Also shifts on purpose to make it more obvious
        System.out.println("\n"+"_".repeat(70)+"\n"+comment+"\n|\t\t\t"+this.toString().replace("\n","\n|\t\t\t")+"\n| "+toFEN()+"\n|"+"_".repeat(70));
    }

    public void printWithBitboard(String comment, long bitboard){//Also shifts on purpose to make it more obvious
        ///System.out.println(comment+"\n|\t\t\t"+this.toString().replace("\n","\n|\t\t\t")+"\n|"+"_".repeat(50));
        String toString = toString();
        String[] toStringSplit = toString.split("\n");
        String[] bitboardString = Tools.bitboardAsString(bitboard).split("\n");

        StringBuilder b = new StringBuilder(toStringSplit[0]+"\n");
        for (int i = 0; i < bitboardString.length; i++) {
            b.append(toStringSplit[i + 1]).append("  ").append(bitboardString[i]).append("\n");
        }
        b.append(toStringSplit[toStringSplit.length-1]);
        System.out.println("\n"+"_".repeat(70));
        System.out.println(comment+"\n|\t\t\t"+b.toString().replace("\n","\n|\t\t\t")+"\n| "+toFEN()+"\n|"+"_".repeat(70));
    }

    /**
     * Method using emojis 😎 to display the JumpSturdy board effectively
     *
     * @return String representation with Emojis and multiple control characters for advanced visualisation
     * Credits: Merthan Erdem
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int start = -1;
        int end =-1;
        int previousStart =-1;
        int previousEnd = -1;
        String specialCharacter = "";
        if(previousMove.length!=0){
            start = previousMove[previousMove.length-2];
            end=previousMove[previousMove.length-1];
            String moveEmoji = determineMoveEmoji(start,end);
            specialCharacter =moveEmoji.isEmpty()?"":"\u001B[47m"+moveEmoji+ RESET;

            if(previousMove.length>3){
                previousStart = previousMove[previousMove.length-4];
                previousEnd=previousMove[previousMove.length-3];
            }
        }

        int eval = eval();
        // Append column labels (A-H)
        sb.append("   A  B  C  D   E  F  G  H       History:").append(Tools.stringInColor("    Eval:"+eval,eval>=0)).append("   \t\t\t").append(toFEN()).append("\n");

        for (int row = 0; row < 8; row++) {
            // Append row number
            sb.append((8 - row)).append(" ");
            // 😎😎😎😎😎😎😎😎😎
            for (int col = 0; col < 8; col++) {
                int index = row * 8 + col;
                if (index == 0 || index == 7 || index == 56 || index == 63) {// all corners
                    sb.append("\uD83D\uDD33 ");//🔳
                    continue;
                }
                if((index == start || index == end) ){ // Only add grey background when not single move (for doubles)
                    //sb.append("\u001B[7m");
                    //sb.append("\u001B[35m");
                    sb.append("\u001B[47m");
                }
                if(index == previousStart||index==previousEnd){
                    sb.append(Tools.getAnsiBackgroundColor(238));
                }
                if((!specialCharacter.isBlank()) &&index == start){ // ↘️ etc
                    //System.out.println("Special:"+specialCharacter);
                    sb.append(specialCharacter);//If not empty, append
                    sb.append("\u001B[0m ");
                    continue;
                }
                if ((redSingles & (1L << index)) != 0) {
                    sb.append("\uD83D\uDD34");//🔴   😎
                } else if ((blueSingles & (1L << index)) != 0) {
                    sb.append("\uD83D\uDD35");//🔵
                } else if ((redDoubles & (1L << index)) != 0) {
                    sb.append("\uD83D\uDFE5");//🟥
                } else if ((blueDoubles & (1L << index)) != 0) {
                    sb.append("\uD83D\uDFE6");//🟦
                } else if ((blue_on_red & (1L << index)) != 0) {
                    sb.append("\u001B[41m\uD83D\uDD35\u001B[0m");// NO FITTING EMOJI, uses Red Background Control Characters - blue on red
                } else if ((red_on_blue & (1L << index)) != 0) {
                    sb.append("\u001B[44m\uD83D\uDD34\u001B[0m");// NO FITTING EMOJI, uses Blue Background Control Characters - blue on red
                } else {
                    sb.append("⬜");
                }
                if((index == start || index == end||index==previousStart||index==previousEnd) ){
                    sb.append("\u001B[0m");
                }
                sb.append(" ");//now moved here after reset so it doesnt include the s
            }
            boolean redDidLastMove = previousMove.length > 0 && isItRedsTurnByPositionOfPieces(previousMove[previousMove.length - 1]); //All complex code to show the previous moves in rows in colors
            String rowMove = (previousMove.length>= ((8-row)*2))? Tools.parseMoveToString(new byte[]{previousMove[ previousMove.length-1- ((7-row)*2+1)  ],previousMove[ previousMove.length-1- (7-row)*2 ]}) : "";

            //rowMove = (row % (redDidLastMove?2:1) == 0)? "\u001B[31m"+rowMove+ RESET : Tools.BLUE + rowMove+RESET;
            rowMove = (row % 2 == (redDidLastMove?1:0))? "\u001B[31m"+rowMove+ RESET : Tools.BLUE + rowMove+RESET;

            sb.append(" ").append(8 - row).append("   ").append(rowMove).append("\n"); // Append row number
        }
        sb.append("   A  B  C  D   E  F  G  H\n");
        return sb.toString();
    }


    private String determineMoveEmoji(int start, int end) {
        if(end-start==8) return "⬇️";
        if(end-start==-8) return "⬆️";
        if(end-start==1)return "➡️";
        if(end-start==-1)return "⬅️";

        if(end-start==9)return "↘\uFE0F";
        if(end-start==-9)return "↖\uFE0F";
        if(end-start==7)return "↙\uFE0F";
        if(end-start==-7)return "↗\uFE0F";

        return "";
    }





}
