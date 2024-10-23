package ai;

import misc.Tools;
import model.*;

import java.util.List;

import static ai.MerthanAlphaBetaExperiment.log;

public class SturdyJumpersAI {


    private static int untersuchteZustaende = 0;
    private static boolean timeUp;
    public static long TIME_LIMIT = 500000000L;//TODO: changed to 0.5 seconds 2000000000L; original // 2 seconds in nanoseconds


    public static String findBestMove(SearchType searchType, BitBoard board, boolean isRed) {
        return findBestMoveTestWrapper(searchType, board, isRed).bestMove;
    }


    public static TestWrapper findBestMoveTestWrapper(SearchType searchType, BitBoard board, boolean isRed) {
        switch (searchType) {
            case MINIMAX -> {
                return alphaBetaWrapper(board, isRed, false);
            }
            case ALPHABETA -> {
                return alphaBetaWrapper(board, isRed, true);
            }
        }
        return alphaBetaWrapper(board, isRed, true);
    }

    public static TestWrapper alphaBetaWrapper(BitBoard board, boolean isRed, boolean withCutoffs) {
        int bestValue = Integer.MIN_VALUE;
        String bestMove = null;
        List<String> legalMoves = board.getAllPossibleMoveStringsDeprecated(isRed);
        int depth = 1;

        if(log)System.out.println("TODO: Replace nextMoveWin, dont use lastRowMove");
/*TODO: REPLACE THIS
        if(BitBoardManipulation.doesNextMoveWin(isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles,
                board.red_on_blue, board.blue_on_red))
            return new TestWrapper(untersuchteZustaende, bestValue, Tools.lastRowMove(legalMoves, isRed), isRed);
*/


        untersuchteZustaende = 0;
        long startTime = System.nanoTime();
        timeUp = false;

        while (!timeUp) {
            for (String move : legalMoves) {
                //System.out.println("OG MOVE: " + move);
                untersuchteZustaende++;
                BitBoard newBoard = BitBoard.fromLongArray(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                        Tools.parseMove(move)[0], Tools.parseMove(move)[1], isRed,
                        board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles,
                        board.red_on_blue, board.blue_on_red));

                int moveValue = withCutoffs ?
                        alphaBetaSearch(newBoard, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isRed, startTime) :
                        alphaBetaSearchWithoutCutoffs(newBoard, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isRed, startTime);
                //System.out.println("Evaluated Move: " + move + ". Move Value: " + moveValue + ". At Depth: " + depth + ". isRed: " + isRed);
                if (moveValue > bestValue) {
                    // System.out.println("Best move CHANGED from " + bestMove + " with value " + bestValue + " to " + move + " with value " + moveValue);
                    bestValue = moveValue;
                    bestMove = move;
                }
                if (timeUp) break;
            }
            depth++;

        }
        if(log)System.out.println("Laufzeit: " + ((System.nanoTime() - startTime) / 1e6) + " ms\n" +
                "Erreichte Tiefe: " + (depth - 1) + "\n" +
                "Bester Zug: " + bestMove + ". Value: " + bestValue + "\n" +
                "Untersuchte Zustände: " + untersuchteZustaende
        );

        return new TestWrapper(untersuchteZustaende, bestValue, bestMove, isRed);
    }



    // AlphaBeta with time management
    private static int alphaBetaSearch(BitBoard board, int depth, int alpha, int beta, boolean isRed, long startTime) {
        if (System.nanoTime() - startTime > TIME_LIMIT) {
            timeUp = true;
            return isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }

        if (depth <= 0 || board.currentWinningState() != BitBoard.WINNER_ONGOING) {
            int[] ruhesucheWithPosLastIndexIsEval = BitBoardManipulation.ruhesucheWithPositions(board,isRed);//TODO: Use this

            //if (ruhesuche!= BitBoardManipulation.RUHESUCHE_NOT_PERFORMED) return ruhesuche;
            //if (BitBoardManipulation.doesNextMoveWin(isRed,board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red) == true) return 500;

            int eval = Evaluate.evaluateComplex(isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red); /*Evaluate.evaluateSimple(isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red) -
                    Evaluate.evaluateSimple(!isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red);*/
            //return BitBoardManipulation.ruhesuche(board,isRed);
            return eval;
        }

        List<String> legalMoves = board.getAllPossibleMoveStringsDeprecated(isRed);

        if (!isRed) {
            int maxEval = Integer.MIN_VALUE;
            for (String move : legalMoves) {
                untersuchteZustaende++;

                BitBoard newBoard = BitBoard.fromLongArray(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                        Tools.parseMove(move)[0], Tools.parseMove(move)[1], isRed,
                        board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles,
                        board.red_on_blue, board.blue_on_red));
                int eval = alphaBetaSearch(newBoard, depth - 1, alpha, beta, !isRed, startTime);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, maxEval);
                // System.out.println("Maximierer Move: " + move + ". Move Value: " + alpha  + ". At Depth: " + depth + ". isRed: " + isRed);
                if (beta <= alpha) {
                    // System.out.println("Beta Cut-off");
                    break; // Beta cut-off
                }
                if (timeUp) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (String move : legalMoves) {
                untersuchteZustaende++;
                BitBoard newBoard = BitBoard.fromLongArray(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                        Tools.parseMove(move)[0], Tools.parseMove(move)[1], isRed,
                        board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles,
                        board.red_on_blue, board.blue_on_red));
                int eval = alphaBetaSearch(newBoard, depth - 1, alpha, beta, !isRed, startTime);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, minEval);
                //System.out.println("Minimierer Move: " + move + ". Move Value: " + beta  + ". At Depth: " + depth + ". isRed: " + isRed);
                if (alpha >= beta) {
                    //System.out.println("Alpha Cut-off");
                    break; // Alpha cut-off
                }
                if (timeUp) break;
            }
            return minEval;
        }
    }

    // Minimax without cut-offs (not modified, can be used as is)
    private static int alphaBetaSearchWithoutCutoffs(BitBoard board, int depth, int alpha, int beta, boolean isRed, long startTime) {
        if (System.nanoTime() - startTime > TIME_LIMIT) {
            timeUp = true;
            return isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        if (depth <= 0 || board.currentWinningState() != BitBoard.WINNER_ONGOING) {
            return Evaluate.evaluateSimple(isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red) -
                    Evaluate.evaluateSimple(!isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red);
        }

        List<String> legalMoves = board.getAllPossibleMoveStringsDeprecated(isRed);
        if (!isRed) {
            int maxEval = Integer.MIN_VALUE;
            for (String move : legalMoves) {
                //System.out.println("Move: " + move + ". Depth: " + depth + ". Bereits untersucht: " + untersuchteZustaende + ". Move für Red: " + isRed);
                untersuchteZustaende++;
                BitBoard newBoard = BitBoard.fromLongArray(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                        Tools.parseMove(move)[0], Tools.parseMove(move)[1], false,
                        board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles,
                        board.red_on_blue, board.blue_on_red));
                int eval = alphaBetaSearchWithoutCutoffs(newBoard, depth - 1, alpha, beta, true, startTime);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                System.out.println("Move: " + move + ". Value: " + eval + ". Move für Red: " + isRed);
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (String move : legalMoves) {
                //System.out.println("Move: " + move + ". Depth: " + depth + ". Bereits untersucht: " + untersuchteZustaende + ". Move für Red: " + isRed);
                untersuchteZustaende++;
                BitBoard newBoard = BitBoard.fromLongArray(BitBoardManipulation.doMoveAndReturnModifiedBitBoards(
                        Tools.parseMove(move)[0], Tools.parseMove(move)[1], true,
                        board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles,
                        board.red_on_blue, board.blue_on_red));
                int eval = alphaBetaSearchWithoutCutoffs(newBoard, depth - 1, alpha, beta, false, startTime);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                System.out.println("Move: " + move + ". Value: " + eval + ". Move für Red: " + isRed);
            }
            return minEval;
        }
    }
}
