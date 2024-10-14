package ai;

import misc.Tools;
import model.BitBoard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static ai.Evaluate.MAXIMUM_WITH_BUFFER_POSITIVE;
import static model.BitBoard.WINNER_BLUE;
import static model.BitBoard.WINNER_RED;

public class MerthanAlphaBetaExperiment {

    public long endTime;
    public int bestDepthReached;
    private final ai.ZobristHashing zobristHashing;
    private final ai.TranspositionTable transpositionTable;

    public MerthanAlphaBetaExperiment() {
        zobristHashing = new ai.ZobristHashing();
        transpositionTable = new ai.TranspositionTable();
    }


/*        //TODO REMOVE
        if((board.redSingles &(1L << Tools.positionToIndex("C1")))!=0){
            System.out.println("won here");
        }
        if(board.previousMove[0] == 49 && board.previousMove[1] == 58){//B2-C1
            System.out.println("won here 2");
            miscCounter++;
        }*/

/*        if(board.previousMove[0] == 49 && board.previousMove[1] == 58){//B2-C1 is 49 58, C1-B2 is 58,49
            System.out.println("won here 2");
            miscCounter++;
        }*/

/*        if(board.previousMove[board.previousMove.length-2] == 57 && board.previousMove[board.previousMove.length-1] == 48){//B2-C1 is 49 58, C1-B2 is 57,48
            //System.out.println("prevented here 2");
            miscCounter++;
        }*/

    //Required so that Evaluation isnt called lots of times while sorting, so needs to be saved somewhere in relation to the moves
    public static byte[][] sortMovesPerformant(BitBoard board,boolean maximizingPlayer){
        byte[][] moves = board.getAllPossibleMovesByte(maximizingPlayer);
        final int multiplier = (maximizingPlayer?-1:1);

        int[][] movesAndEval = new int[moves.length][3];
        for (int i = 0; i < moves.length; i++) {
            byte[] move = moves[i];
            int eval = Evaluate.evaluateMoveComplex(true,move[0],move[1],board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red);
            movesAndEval[i]= new int[]{move[0],move[1],multiplier*eval};
        }
        Arrays.sort(movesAndEval, Comparator.comparingInt(a -> a[2]));//Compare by eval

        byte[][] byteArray = new byte[moves.length][2];

        for (int i = 0; i < moves.length; i++) {
            byteArray[i][0] = (byte) movesAndEval[i][0];
            byteArray[i][1] = (byte) movesAndEval[i][1];
        }
        return byteArray;
    }

    public static int counter =0;
    public static int endReachedCounter = 0;
    public static int miscCounter = 0;
    public static int cutoffCounter = 0;


    public static long ZEIT = 2000L;

    static final boolean sortMovesBeforeEach = true;
    static final boolean useTranspositionTable = false; //switch Transposition Table
    public final static boolean saveSequence = false;
    public final static boolean dynamicTimeManagement = false;

    public final static boolean log = false; //CHANGE WHEN NEEDED
    public final static boolean detailedLog = false;

    public static long ruhesucheTime=0;

    public int alphaBeta(BitBoard board, int depth, int alpha, int beta, boolean maximizingPlayer, List<byte[]> bestMoves) {
        if (System.currentTimeMillis() > endTime) {//Deactivated for debugging with  && false
            //return 0; // Return a neutral value if time limit is reached
            return maximizingPlayer ? -MAXIMUM_WITH_BUFFER_POSITIVE : MAXIMUM_WITH_BUFFER_POSITIVE; // Return worst value when time limit reached to not pick these
        }

        long zobristKey = 0;
        if (useTranspositionTable) {

            ai.TranspositionTable.TranspositionEntry entry = null;
            zobristKey = zobristHashing.generateZobristKey(board);
            entry = transpositionTable.get(zobristKey);

            if (entry != null && entry.depth >= depth) {
                if (entry.alpha == entry.beta) {
                    if (bestMoves != null && entry.bestMove != null) {
                        bestMoves.add(entry.bestMove);
                    }
                    return entry.value; // Exakte Übereinstimmung
                }
                if (entry.alpha > alpha) {
                    alpha = entry.alpha; // Obergrenze anpassen
                }
                if (entry.beta < beta) {
                    beta = entry.beta; // Untergrenze anpassen
                }
                if (alpha >= beta) {
                    if (bestMoves != null && entry.bestMove != null) {
                        bestMoves.clear();
                        bestMoves.add(entry.bestMove);
                    }
                    return entry.value; // Beta-Cutoff
                }
            }
        }

        counter++;

/*        if(board.previousMoves().contains("C1-B1")){
            Math.abs(0);
        }*/

        boolean canWin = null != ai.BitBoardManipulation.canWinWithMovesFusioned(maximizingPlayer, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red);
        boolean depthZero = depth == 0;
        boolean gameEnded = board.currentWinningState() != BitBoard.WINNER_ONGOING;

        if (depthZero || gameEnded || canWin) {
            //TODO: isRed worked, now replaced by true
            int eval = ai.Evaluate.evaluateComplex(board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red);
            endReachedCounter++;
            //Depth 0, to account for attacked pieces afterwards
            if (depthZero) {//If ruhesuche gets a value, return ruhesuche (doesnt take long)

                //Only do Ruhesuche if canwin not set to "winning soon" aka fusion or on winning spot already to not return a false (after FORCED ruhesuche/attacking) value
                long start = System.currentTimeMillis();
                int[] ruhesucheArray = (Math.abs(eval) < 2000000000) ? ai.BitBoardManipulation.ruhesucheWithPositions(board, maximizingPlayer) : null; // If not winning/canwin next move, do ruhesuche. else it doesnt matter
                ruhesucheTime += System.currentTimeMillis() - start;
                return (ruhesucheArray == null) ? eval : ruhesucheArray[ruhesucheArray.length - 1];
            } else {//Aka canWin || gameEnded
                //MODIFIED: previously only for canWin, Now its either canWin or gameEnded that shows a preference for higher depth/less moves
                return maximizingPlayer ? (eval + depth) : (eval - depth);//Prefer moves with higher depth, needs buffer so changed Integer.MAX_VALUE
            }
        }
        byte[][] moves = sortMovesBeforeEach ? board.getAllPossibleMovesByteSorted(maximizingPlayer) : board.getAllPossibleMovesByte(maximizingPlayer);
        //Arrays.stream(moves).toList().stream().map(e->Tools.parseMoveToString(e)).collect(Collectors.joining(","))
        List<byte[]> currentBestMoves;
        if (saveSequence) {
            currentBestMoves = new ArrayList<>();
        }
        int evalBound = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;//These dont need the buffer variable
        for (byte[] move : moves) {
            BitBoard executedMoveBoard = board.doMoveAndReturnBitboard(move, maximizingPlayer);
            List<byte[]> childBestMoves;
            int eval;
            if (saveSequence) {
                childBestMoves = new ArrayList<>();
                eval = alphaBeta(executedMoveBoard, depth - 1, alpha, beta, !maximizingPlayer, childBestMoves);
            } else {
                eval = alphaBeta(executedMoveBoard, depth - 1, alpha, beta, !maximizingPlayer, null);
            }
            boolean condition = maximizingPlayer ? (eval > evalBound) : (eval < evalBound);


            if (condition) {
                evalBound = eval;
                if (saveSequence) {
                    currentBestMoves.clear();
                    currentBestMoves.add(move);
                    currentBestMoves.addAll(childBestMoves);
                }
                //System.out.println("Debug: New best move: " + Tools.parseMoveToString(move) + " with eval " + evalBound+ "for max:"+maximizingPlayer);
            }


            if (maximizingPlayer) {
                alpha = Math.max(alpha, eval);
            } else {
                beta = Math.min(beta, eval);
            }
            if (beta <= alpha) {
                cutoffCounter++;
                break; // Alpha-Beta cutoff
            }
            //AlphaBeta method was called: 9090503 and end point reached/Evaluated: 7787512 cutoffs: 1130481 misc0
            //AlphaBeta method was called: 5868583 and end point reached/Evaluated: 4222722 cutoffs: 1505685 misc0
        }
        if (saveSequence) {
            bestMoves.clear();
            bestMoves.addAll(currentBestMoves);
        }


        //System.out.println((maximizingPlayer ? "Maximizing" : "Minimizing") + " Player Best Moves: " + bestMoves);
        if (useTranspositionTable) {
            transpositionTable.put(zobristKey, depth, evalBound, alpha, beta, bestMoves.get(0));
        }
        return evalBound;
    }

    /*            if(eval>2100000000){
                System.out.println(depth+"won remove"+(eval-2100000000));

                System.out.println(executedMoveBoard.previousMoves()+ executedMoveBoard.previousMove);
                if(executedMoveBoard.previousMoves().contains("[C7-C6] [C3-C4] [C6-B6] [C4-B4]")){
                    executedMoveBoard.printCommented("Mod");

                }
                //executedMoveBoard.print();
                //System.out.println("evaal:"+executedMoveBoard.eval());
                //System.out.println("done");
            }*/


    public int alphaBetaLessObjects(int depth, int alpha, int beta, boolean maximizingPlayer, long r, long b, long rr, long bb, long br, long rb) {
        if (System.currentTimeMillis() > endTime) {//Deactivated for debugging with  && false
            //return 0; // Return a neutral value if time limit is reached
            return maximizingPlayer? -MAXIMUM_WITH_BUFFER_POSITIVE : MAXIMUM_WITH_BUFFER_POSITIVE; // Return worst value when time limit reached to not pick these
        }
        counter++;


        boolean canWin = null != BitBoardManipulation.canWinWithMovesFusioned(maximizingPlayer, r,b,rr,bb,br,rb);
        boolean depthZero = depth == 0;
        boolean gameEnded = BitBoard.currentWinningStateStaticOptimized(r,b,rr,bb,br,rb) != BitBoard.WINNER_ONGOING;

        if (depthZero|| gameEnded ||canWin) {
            //TODO: isRed worked, now replaced by true
            int eval = Evaluate.evaluateComplex(r,b,rr,bb,br,rb);
            endReachedCounter++;
            //Depth 0, to account for attacked pieces afterwards

            if(depthZero){//If ruhesuche gets a value, return ruhesuche (doesnt take long)

                //Only do Ruhesuche if canwin not set to "winning soon" aka fusion or on winning spot already to not return a false (after FORCED ruhesuche/attacking) value
                //long start = System.currentTimeMillis();
                int ruhesucheEval = (Math.abs(eval)<2000000000)? BitBoardManipulation.ruhesucheWithPositionsOptimized(maximizingPlayer,r,b,rr,bb,br,rb):BitBoardManipulation.RUHESUCHE_NOT_PERFORMED; // If not winning/canwin next move, do ruhesuche. else it doesnt matter
                //ruhesucheTime += System.currentTimeMillis()-start;
                return (ruhesucheEval == BitBoardManipulation.RUHESUCHE_NOT_PERFORMED)? eval : ruhesucheEval;
/*                int ruhesucheArray = (Math.abs(eval)<2000000000)? BitBoardManipulation.ruhesuche(board,maximizingPlayer):BitBoardManipulation.RUHESUCHE_NOT_PERFORMED; // If not winning/canwin next move, do ruhesuche. else it doesnt matter
                ruhesucheTime += System.nanoTime()-start;
                return (ruhesucheArray == BitBoardManipulation.RUHESUCHE_NOT_PERFORMED)? eval : ruhesucheArray;*/

            }else{//Aka canWin || gameEnded
                //MODIFIED: previously only for canWin, Now its either canWin or gameEnded that shows a preference for higher depth/less moves
                return maximizingPlayer?(eval+depth):(eval-depth);//Prefer moves with higher depth, needs buffer so changed Integer.MAX_VALUE
            }
            //return eval;
        }

        byte[][] moves = BitBoard.generateSortedByteMovesForPiecesWithListStatic(maximizingPlayer,r,b,rr,bb,br,rb); //sortMovesBeforeEach ? board.getAllPossibleMovesByteSorted(maximizingPlayer):board.getAllPossibleMovesByte(maximizingPlayer);

        //Arrays.stream(moves).toList().stream().map(e->Tools.parseMoveToString(e)).collect(Collectors.joining(","))

        int evalBound = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;//These dont need the buffer variable
        for (byte[] move : moves) {
            long[] executedMoveBoard = BitBoardManipulation.doMoveAndReturnModifiedBitBoards(move[0],move[1],maximizingPlayer,r,b,rr,bb,br,rb);//board.doMoveAndReturnBitboard(move, maximizingPlayer);
            int eval = alphaBetaLessObjects(depth - 1, alpha, beta, !maximizingPlayer, executedMoveBoard[0], executedMoveBoard[1], executedMoveBoard[2], executedMoveBoard[3], executedMoveBoard[4], executedMoveBoard[5]);
            boolean condition = maximizingPlayer ? (eval > evalBound) : (eval < evalBound);

/*            if(eval>2100000000){
                System.out.println(depth+"won remove"+(eval-2100000000));

                System.out.println(executedMoveBoard.previousMoves()+ executedMoveBoard.previousMove);
                if(executedMoveBoard.previousMoves().contains("[C7-C6] [C3-C4] [C6-B6] [C4-B4]")){
                    executedMoveBoard.printCommented("Mod");

                }
                //executedMoveBoard.print();
                //System.out.println("evaal:"+executedMoveBoard.eval());
                //System.out.println("done");
            }*/

            if (condition) {
                evalBound = eval;
            }

            if (maximizingPlayer) {
                alpha = Math.max(alpha, eval);
            } else {
                beta = Math.min(beta, eval);
            }
            if (beta <= alpha) {
                cutoffCounter++;
                break; // Alpha-Beta cutoff
            }

        }
        //System.out.println((maximizingPlayer ? "Maximizing" : "Minimizing") + " Player Best Moves: " + bestMoves);
        return evalBound;
    }

    public List<byte[]> findBestMove(BitBoard board, boolean isRed, int timeLimitMillis) {

        endTime = dynamicTimeManagement ? (System.currentTimeMillis() + TIME_LIMIT) : (System.currentTimeMillis() + timeLimitMillis);
        bestDepthReached = 0;
        ArrayList<byte[]> bestMoveSequence = new ArrayList<>();
        int currentBestValue = isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Initialize based on the starting player, dont need the buffer variable

        int confirmedBestValue = isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Only after all options have gone through
        byte[] winningMoves = ai.BitBoardManipulation.canWinWithMovesFusioned(isRed, board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red);
        if(winningMoves!=null){ // If we can win, dont do alphabeta search, just do the moves to win
            if(winningMoves.length==2){
                return List.of(winningMoves);//new ArrayList<>(new byte[]{(byte)2});
            }
            if(winningMoves.length==3){
                return List.of(new byte[]{winningMoves[0], winningMoves[1]});
            }
        }

        byte[][] moves = sortMovesBeforeEach? board.getAllPossibleMovesByteSorted(isRed): board.getAllPossibleMovesByte(isRed);
        //without streams: 2061100n with streams: 2317900
        long start = System.nanoTime();
        //if(log) Tools.printRed("Before sort:"+Arrays.stream(moves).map(Tools::parseMoveToString).toList());
        //TODO: can be removed, no benefit I guess
        Arrays.sort(moves, Comparator.comparingInt(e -> (isRed?-1:1)* Evaluate.evaluateMoveComplex(isRed,e[0],e[1],board.redSingles, board.blueSingles, board.redDoubles, board.blueDoubles, board.red_on_blue, board.blue_on_red)));
        //if(log) Tools.printBlue("After sort:"+Arrays.stream(moves).map(Tools::parseMoveToString).toList());
        if(detailedLog)System.out.println("Sorting took: "+(System.nanoTime()-start));
        //if(true)System.exit(0);
        int lastIndexReached=0;
        for (int depth = 1; ; depth++) {


            if(log)Tools.printInColor("New Depth reached: "+depth+" currentbest:"+currentBestValue,true);

            currentBestValue = isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Initialize based on the starting player, dont need the buffer variable
            //if(depth==6)break; //TODO REMOVE
            ArrayList<byte[]> currentBestMoveSequence = new ArrayList<>();
            //byte[][] moves = board.getAllPossibleMovesByte(isRed);
            //System.out.println("Moves here:" + legalMoves);
            //System.out.println("isRedTurn:" + isRed);

            boolean didCompleteAndResultsAreValid = true;

            for (int i=0;i<moves.length;i++) {
                byte[] move = moves[i];

                BitBoard executedMoveBoard = board.doMoveAndReturnBitboard(move,isRed);
                byte winState =executedMoveBoard.currentWinningState();
                if(winState == (isRed?WINNER_RED:WINNER_BLUE)){ // If found winning move first move, just do it
                    if(log) System.out.println("WON, NO ALPHABETA");
                    return List.of(move);
                }

                //List<String> childBestMoves = new ArrayList<>();
                List<byte[]> childBestMoves = null;
                if(saveSequence)childBestMoves = new ArrayList<>();

                int moveValue = alphaBeta(executedMoveBoard, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isRed, childBestMoves);
                if(detailedLog) System.out.println("AlphaBetaStart: move: "+Tools.parseMoveToString(move)+" has value:"+moveValue+" \tsequence:"+ (childBestMoves != null ? childBestMoves.stream().map(Tools::parseMoveToString).toList() : "empty"));
                if (isRed ? moveValue > currentBestValue : moveValue < currentBestValue) { // Compare based on the starting player
                    currentBestValue = moveValue;
                    currentBestMoveSequence.clear();
                    currentBestMoveSequence.add(move);
                    //childBestMoves.stream().map(Tools::parseMove).toList()
                    if(saveSequence)currentBestMoveSequence.addAll(childBestMoves);//Else we just add the move
                }

                if (System.currentTimeMillis() > endTime) {
                    didCompleteAndResultsAreValid = false;
                    if(log){
                        Tools.printRed("Time limit reached in findBest Moves, discarded dumb best move sequence: "+Tools.byteListToMoveSequence(currentBestMoveSequence)+" depth "+depth+", only completed "+i+"/"+moves.length+" last: "+Tools.parseMoveToString(move));
                        lastIndexReached=i;
                    }
                    break; // Time limit reached
                }
            }

            if(didCompleteAndResultsAreValid){
                bestMoveSequence = currentBestMoveSequence;
                confirmedBestValue = currentBestValue;
            }


            bestDepthReached = depth;

            if(detailedLog)System.out.println("AlphaBetaStart for DEPTH: "+depth +" bestmoveValue " +confirmedBestValue+ " sequence:"+Tools.byteListToMoveSequence(bestMoveSequence));


            if (System.currentTimeMillis() > endTime) {
                if(log)System.out.println("time limit reached in going through depths");
                break; // Time limit reached
            }


        }
        miscCounter = ((lastIndexReached+1) * 100) / moves.length;

        if(log)System.out.println("Best Move Sequence: " + Tools.byteListToMoveSequence(bestMoveSequence));
        if(log)System.out.println("Current best valuee: " + currentBestValue);
        if(log)System.out.println("Depth Reached: " + bestDepthReached+" and last index was "+lastIndexReached+"/"+moves.length);
        if(log)System.out.println("AlphaBeta called: " + counter+" End Evaluated: "+endReachedCounter+ " Cuts: "+cutoffCounter+" Depth Reached: " + bestDepthReached+" and last index was "+lastIndexReached+"/"+moves.length+" misc: depth"+bestDepthReached+": "+miscCounter+"%");
        if(log)System.out.println("Time Elapsed: " + (System.currentTimeMillis() - (endTime - timeLimitMillis)) + " ms Ruhesuche took:"+ ruhesucheTime);

        if(dynamicTimeManagement) {
            adjustTimeLimit(moves.length, board);
            System.out.println("New Limit Time Limit: " + TIME_LIMIT);
        }
        ruhesucheTime=0;
        counter=0;
        endReachedCounter =0;
        transpositionTable.clear();


        return bestMoveSequence;
    }


    public List<byte[]> findBestMoveNoObjects(boolean isRed, int timeLimitMillis,long r, long b, long rr, long bb, long br, long rb) {
        endTime = System.currentTimeMillis() + timeLimitMillis;

        bestDepthReached = 0;
        ArrayList<byte[]> bestMoveSequence = new ArrayList<>();
        int currentBestValue = isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Initialize based on the starting player, dont need the buffer variable
//TODO: Changed from not initialized to 0, might cause bugs

        int confirmedBestValue = isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Only after all options have gone through
        byte[] winningMoves = BitBoardManipulation.canWinWithMovesFusioned(isRed, r,b,rr,bb,br,rb);
        if(winningMoves!=null){ // If we can win, dont do alphabeta search, just do the moves to win
            if(winningMoves.length==2){
                return List.of(winningMoves);//new ArrayList<>(new byte[]{(byte)2});
            }
            if(winningMoves.length==3){
                return List.of(new byte[]{winningMoves[0], winningMoves[1]});
            }
        }

        byte[][] moves = BitBoard.generateSortedByteMovesForPiecesWithListStatic(isRed,r,b,rr,bb,br,rb);
        //without streams: 2061100n with streams: 2317900
        long start = System.nanoTime();
        //if(log) Tools.printRed("Before sort:"+Arrays.stream(moves).map(Tools::parseMoveToString).toList());
        //TODO: can be removed, no benefit I guess
        Arrays.sort(moves, Comparator.comparingInt(e -> (isRed?-1:1)* Evaluate.evaluateMoveComplex(isRed,e[0],e[1],r,b,rr,bb,br,rb)));
        //if(log) Tools.printBlue("After sort:"+Arrays.stream(moves).map(Tools::parseMoveToString).toList());
        if(detailedLog)System.out.println("Sorting took: "+(System.nanoTime()-start));
        //if(true)System.exit(0);
        int lastIndexReached=0;
        for (int depth = 1; ; depth++) {


            if(log)Tools.printInColor("|New Depth reached: "+depth+" currentbest:"+currentBestValue+" move length "+moves.length+" "+(BitBoard.currentWinningStateStaticOptimized(r,b,rr,bb,br,rb)),true);

            currentBestValue = isRed ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Initialize based on the starting player, dont need the buffer variable
            //if(depth==6)break; //TODO REMOVE
            ArrayList<byte[]> currentBestMoveSequence = new ArrayList<>();
            //byte[][] moves = board.getAllPossibleMovesByte(isRed);
            //System.out.println("Moves here:" + legalMoves);
            //System.out.println("isRedTurn:" + isRed);

            boolean didCompleteAndResultsAreValid = true;

            for (int i=0;i<moves.length;i++) {
                byte[] move = moves[i];

                long[] executedMoveBoard = BitBoardManipulation.doMoveAndReturnModifiedBitBoards(move[0],move[1],isRed,r,b,rr,bb,br,rb);//.doMoveAndReturnBitboard(move,isRed);
                byte winState =BitBoard.currentWinningStateStaticOptimized(executedMoveBoard[0],executedMoveBoard[1],executedMoveBoard[2],executedMoveBoard[3],executedMoveBoard[4],executedMoveBoard[5]);
                if(winState == (isRed?WINNER_RED:WINNER_BLUE)){ // If found winning move first move, just do it
                    if(log) System.out.println("WON, NO ALPHABETA");
                    return List.of(move);
                }

                //List<String> childBestMoves = new ArrayList<>();
                if(depth>=1000){
                    //Math.abs(0);
                    BitBoard.fromLongArray(executedMoveBoard).printCommented("depth too high"+Tools.movesToString(moves)+" >>"+ (winningMoves != null ? winningMoves.length : -1));
                    //System.exit(0);
                }

                int moveValue = alphaBetaLessObjects(depth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isRed, executedMoveBoard[0],executedMoveBoard[1],executedMoveBoard[2],executedMoveBoard[3],executedMoveBoard[4],executedMoveBoard[5]);
                if(detailedLog)System.out.println("|AlphaBetaStart: move: "+Tools.parseMoveToString(move)+" has value:"+moveValue+" \tsequence:empty");
                if (isRed ? moveValue > currentBestValue : moveValue < currentBestValue) { // Compare based on the starting player
                    currentBestValue = moveValue;
                    currentBestMoveSequence.clear();
                    currentBestMoveSequence.add(move);
                    //childBestMoves.stream().map(Tools::parseMove).toList()
                    //if(saveSequence)currentBestMoveSequence.addAll(childBestMoves);//Else we just add the move
                }



                if (System.currentTimeMillis() > endTime) {//TODO: BUG; WHEN NOT SET TO && FALSE; TOO MUCH DEPTH
                    didCompleteAndResultsAreValid = false;
                    if(log){
                        Tools.printRed("Time limit reached in findBest Moves, discarded dumb best move sequence: "+Tools.byteListToMoveSequence(currentBestMoveSequence)+" depth "+depth+", only completed "+i+"/"+moves.length+" last: "+Tools.parseMoveToString(move));
                        lastIndexReached=i;
                    }
                    break; // Time limit reached
                }
            }


            if(didCompleteAndResultsAreValid){
                bestMoveSequence = currentBestMoveSequence;
                confirmedBestValue = currentBestValue;
            }

            bestDepthReached = depth;

            if(detailedLog)System.out.println("AlphaBetaStart for DEPTH: "+depth +" bestmoveValue " +confirmedBestValue+ " sequence:"+Tools.byteListToMoveSequence(bestMoveSequence));


            if (System.currentTimeMillis() > endTime) {//TODO REMOVE false
                if(log)System.out.println("time limit reached in going through depths");
                break; // Time limit reached
            }


        }
        miscCounter = ((lastIndexReached+1) * 100) / moves.length;


        //if(log)System.out.println("Best Move Sequence: " + Tools.byteListToMoveSequence(bestMoveSequence));
        //if(log)System.out.println("Current best valuee: " + currentBestValue);
        //if(log)System.out.println("Depth Reached: " + bestDepthReached+" and last index was "+lastIndexReached+"/"+moves.length);
        if(log)System.out.println("AlphaBeta called: " + counter+" End Evaluated: "+endReachedCounter+ " Cuts: "+cutoffCounter+" Depth Reached: " + bestDepthReached+" and last index was "+lastIndexReached+"/"+moves.length+" misc: depth"+bestDepthReached+": "+miscCounter+"%");
        //if(log)System.out.println("Time Elapsed: " + (System.currentTimeMillis() - (endTime - timeLimitMillis)) + " ms Ruhesuche took:"+ ruhesucheTime);
        ruhesucheTime=0;
        counter=0;
        endReachedCounter =0;

        return bestMoveSequence;
    }

    private static int evaluateGamePhase(BitBoard board) {
        // Heuristik zur Bestimmung der Spielphase (1: Eröffnung, 2: Mittelspiel, 3: Endspiel)
        int totalPieces = board.countTotalPieces();
        if (totalPieces > 20) {
            return 1;
        } else if (totalPieces > 10) {
            return 2;
        } else {
            return 3;
        }
    }

    private static int estimateRemainingMoves(BitBoard board) {
        //Heuristik zur Schätzung der verbleibenden Züge bis zum Spielende
        int totalPieces = board.countTotalPieces();
        if (totalPieces > 20) {
            return 40; // Schätzung für Eröffnung
        } else if (totalPieces > 10) {
            return 20; // Schätzung für Mittelspiel
        } else {
            return 10; // Schätzung für Endspiel
        }
    }

    private static int TIME_LIMIT = 2000; // 2 sec
    private static final int MAX_TIME_LIMIT = 4000;
    private static final int remainingTime = 30000;

    private static void adjustTimeLimit(int numMoves, BitBoard board) {
        int timeLimit = TIME_LIMIT;

        // Spielphasenanalyse: Anpassung des Zeitlimits basierend auf der Spielphase
        int gamePhase = evaluateGamePhase(board);
        switch (gamePhase) {
            case 1: // Eröffnung
                timeLimit *= 1.5; // Erhöhtes Zeitlimit in der Eröffnung
                break;
            case 2: // Mittelspiel
                timeLimit *= 1.2; // Mittel hohes Zeitlimit im Mittelspiel
                break;
            case 3: // Endspiel
                timeLimit *= 1.3; // Erhöhtes Zeitlimit im Endspiel
                break;
        }

        if (numMoves < 10) timeLimit *= 0.9;
        else if (numMoves > 30) timeLimit *= 1.1;


        // Verbleibende Zeit analysieren und anpassen
        int estimatedMovesLeft = estimateRemainingMoves(board);
        int timePerMove = remainingTime / estimatedMovesLeft;
        timeLimit = Math.min(timeLimit, timePerMove);

        TIME_LIMIT = Math.min(timeLimit, MAX_TIME_LIMIT);
    }

    public static void main(String[] args) {

        MerthanAlphaBetaExperiment hey = new MerthanAlphaBetaExperiment();
        BitBoard board = new BitBoard("b01b0bb1b0/1bbb0b0b0b0r01/6r01/8/8/8/1r0r0r0r03/r0r0r0r0r0r0");

        System.out.println("bestMoveSequence: "+(hey.findBestMove(board,true, (int) ZEIT)));
    }

}

