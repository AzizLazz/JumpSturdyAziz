import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import ai.MerthanAlphaBetaExperiment;
import ai.SearchType;
import ai.SturdyJumpersAI;
import misc.Tools;
import model.BitBoard;

public class ServerConnection {

    public static int ZEIT = 2000;


    static MerthanAlphaBetaExperiment merthanAlphaBetaExperiment=null;

    public static void main(String[] args) {
        merthanAlphaBetaExperiment = new MerthanAlphaBetaExperiment();
        System.out.println("-------Starting Server Connection------\n\n\n");
        String serverAddress = "localhost";
        int port = 5065;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected.");

                try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

                    System.out.println("Waiting to receive FEN...");
                    String fen = input.readLine();
                    System.out.println("Received FEN: " + fen);

                    if (fen != null && !fen.isEmpty()) {
                        // Process the FEN and decide on the move
                        BitBoard board = new BitBoard(fen.substring(0, fen.length() - 2));
                        String move = Tools.parseMoveToString(merthanAlphaBetaExperiment.findBestMove(board, fen.charAt(fen.length()-1) == 'r', ZEIT).get(0));

                        // Send the move back to the client
                        output.println(move);
                        System.out.println("Sent move: " + move);
                    }
                } catch (Exception e) {
                    System.out.println("Error during communication: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    socket.close();
                }
            }
        } catch (Exception e) {
            System.out.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
