package chess;

import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Text front end for trying the engine out: prints the board and plays the
 * moves you type.
 */
public class Main {

    /** Plays a game on the console. */
    public static void main(String[] args) {
        run(new Scanner(System.in), System.out);
    }

    /**
     * Reads commands until "quit" or the end of the input. Taking the input
     * and output as parameters lets the tests play a scripted game.
     */
    static void run(Scanner input, PrintStream out) {
        Game game = new Game();

        out.println("Chess engine");
        out.println("  e2e4       play a move (castle by moving the king two squares: e1g1)");
        out.println("  e7e8n      promote to a knight; q, r, b or n, and a queen if left out");
        out.println("  moves e2   list the moves of the piece on a square");
        out.println("  moves      list every move for the side to move");
        out.println("  undo       take back the last move");
        out.println("  redo       replay a move that was taken back");
        out.println("  fen        print the position as FEN");
        out.println("  fen <FEN>  start from a position given as FEN");
        out.println("  quit       exit");

        boolean showBoard = true;
        while (true) {
            if (showBoard) {
                out.println();
                out.print(game.getBoard());
                showBoard = false;

                GameStatus status = game.getStatus();
                if (status == GameStatus.CHECKMATE) {
                    out.println("Checkmate. " + sideName(game.getTurn().opposite()) + " wins.");
                } else if (status.isDraw()) {
                    out.println("The game is a draw: " + drawReason(status) + ".");
                } else if (status == GameStatus.CHECK) {
                    out.println("Check.");
                }
                if (status.isGameOver()) {
                    out.println("Type undo to take the last move back, or quit.");
                }
            }
            out.print(sideName(game.getTurn()) + " to move > ");
            if (!input.hasNextLine()) {
                break;  // end of input
            }
            String line = input.nextLine().trim();
            String command = line.toLowerCase(Locale.ROOT);

            if (command.equals("quit") || command.equals("exit")) {
                break;
            }
            if (command.isEmpty()) {
                continue;
            }
            try {
                if (command.equals("undo")) {
                    if (game.canUndo()) {
                        out.println("Took back " + game.undo());
                        showBoard = true;
                    } else {
                        out.println("There is no move to undo.");
                    }
                } else if (command.equals("redo")) {
                    if (game.canRedo()) {
                        out.println("Replayed " + game.redo());
                        showBoard = true;
                    } else {
                        out.println("There is no move to redo.");
                    }
                } else if (command.equals("fen")) {
                    out.println(game.toFen());
                } else if (command.startsWith("fen ")) {
                    // FEN is case-sensitive, so read it from the line as typed.
                    game = Game.fromFen(line.substring("fen ".length()));
                    showBoard = true;
                } else if (command.equals("moves")) {
                    printMoves(out, game.legalMoves());
                } else if (command.startsWith("moves ")) {
                    Position from = Position.of(command.substring("moves ".length()).trim());
                    printMoves(out, game.legalMovesFrom(from));
                } else if (command.length() == 4 || command.length() == 5) {
                    Position from = Position.of(command.substring(0, 2));
                    Position to = Position.of(command.substring(2, 4));
                    PieceType promotion = command.length() == 5 ? promotionFor(command.charAt(4)) : null;
                    Move move = findMove(game.legalMovesFrom(from), to, promotion);
                    if (move == null) {
                        out.println("Not a legal move: " + command);
                    } else {
                        game.makeMove(move);
                        showBoard = true;
                    }
                } else {
                    out.println("Unknown command: " + command);
                }
            } catch (IllegalArgumentException e) {
                out.println(e.getMessage());
            }
        }
    }

    /**
     * The move in the list that ends on the given square, or null if there is
     * none. A promoting pawn has four moves to the same square: this picks
     * the one for the requested piece, or the first (the queen) if
     * {@code promotion} is null.
     */
    private static Move findMove(List<Move> moves, Position to, PieceType promotion) {
        for (Move move : moves) {
            if (move.getTo().equals(to) && (promotion == null || promotion == move.getPromotion())) {
                return move;
            }
        }
        return null;
    }

    private static PieceType promotionFor(char letter) {
        switch (letter) {
            case 'q':
                return PieceType.QUEEN;
            case 'r':
                return PieceType.ROOK;
            case 'b':
                return PieceType.BISHOP;
            case 'n':
                return PieceType.KNIGHT;
            default:
                throw new IllegalArgumentException("Not a promotion piece: " + letter);
        }
    }

    private static void printMoves(PrintStream out, List<Move> moves) {
        if (moves.isEmpty()) {
            out.println("No moves.");
            return;
        }
        String label = moves.size() == 1 ? " move:" : " moves:";
        StringBuilder line = new StringBuilder(moves.size() + label);
        for (Move move : moves) {
            line.append(' ').append(move);
        }
        out.println(line);
    }

    private static String drawReason(GameStatus status) {
        switch (status) {
            case STALEMATE:
                return "stalemate";
            case DRAW_BY_FIFTY_MOVES:
                return "fifty moves without a capture or pawn move";
            case DRAW_BY_REPETITION:
                return "the same position three times";
            case DRAW_BY_INSUFFICIENT_MATERIAL:
                return "not enough pieces left to checkmate";
            default:
                throw new IllegalArgumentException("Not a draw: " + status);
        }
    }

    private static String sideName(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }
}
