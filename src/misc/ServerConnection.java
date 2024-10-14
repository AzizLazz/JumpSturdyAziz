import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import ai.SearchType;
import ai.SturdyJumpersAI;
import misc.Tools;
import model.BitBoard;

public class ServerConnection {

    public static long ZEIT = 2000L;


    public static void main(String[] args) {
        System.out.println("-------Starting Server Connection------\n\n\n");
        String serverAddress = "localhost";
        int port = 5095; // The port on which the Python server is listening

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();  // Correct method call
                System.out.println("New client connected.");

                try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                    System.out.println("Waiting to receive FEN...");
                    String fen = input.readLine(); // Correctly reads from the client
                    System.out.println("Received FEN: " + fen);

                    if (fen != null && !fen.isEmpty()) {
                        // Process the FEN and decide on the move
                        String move = processFEN(fen);

                        // Send the move back to the client
                        output.println(move);
                        System.out.println("Sent move: " + move);
                    }
                } catch (Exception e) {
                    System.out.println("Error during communication: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    socket.close();  // Ensure socket is closed after handling
                }
            }
        } catch (Exception e) {
            System.out.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String processFEN(String fen) {
        // Assuming you have a method in SturdyJumpersAI to handle just FEN
        // Simulating that we're still playing as 'red' for simplicity
        boolean isRed = false;  // You would set this according to your game logic

        BitBoard board = new BitBoard(fen); // Assuming constructor from FEN
        String bestMove = Tools.byteListToMoveSequence( new ai.MerthanAlphaBetaExperiment().findBestMove(board,isRed, (int) ZEIT));
        return bestMove;
    }
}
