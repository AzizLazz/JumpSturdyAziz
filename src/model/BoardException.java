package model;

public class BoardException extends RuntimeException{

    public BoardException(String message, Throwable cause) {
        super(message, cause);
    }

    public BoardException(String message) {
        super(message);
    }

    public BoardException(BitBoard board, String message){
        super(message);
        board.printCommented(message);
    }

    public BoardException(BitBoard board, long bitboard, String message){
        super(message);
        board.printWithBitboard(message,bitboard);
    }

}
