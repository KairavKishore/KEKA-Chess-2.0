package chess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

/** Drives the console front end with typed commands and reads what it prints. */
class MainTest {

    /** Runs the console with one command per argument and returns everything it printed. */
    private static String run(String... commands) {
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        Main.run(new Scanner(String.join("\n", commands)), new PrintStream(printed, true));
        return printed.toString().replace("\r\n", "\n");
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }

    @Test
    void showsTheHelpAndTheStartingBoardThenStopsAtTheEndOfInput() {
        String output = run();
        assertTrue(output.startsWith("Chess engine\n"));
        assertTrue(output.contains("1 | R  N  B  Q  K  B  N  R |"));
        assertTrue(output.endsWith("White to move > "));
    }

    @Test
    void playsAMoveAndRedrawsTheBoard() {
        String output = run("e2e4", "quit");
        assertTrue(output.contains("4 | .  .  .  .  P  .  .  . |"));
        assertTrue(output.contains("Black to move > "));
    }

    @Test
    void commandsMayBeTypedInAnyCaseWithSpacesAround() {
        assertTrue(run("  E2E4  ").contains("Black to move > "));
    }

    @Test
    void refusesIllegalMovesAndNonsenseWithoutRedrawing() {
        String output = run("e2e5", "z9z9", "resign", "", "e7e8x", "quit");
        assertTrue(output.contains("Not a legal move: e2e5"));
        assertTrue(output.contains("Not a square: z9"));
        assertTrue(output.contains("Unknown command: resign"));
        assertTrue(output.contains("Not a promotion piece: x"));
        assertEquals(1, count(output, "+------------------------+\n8 |"));  // the board was drawn once
        assertFalse(output.contains("Black to move"));
    }

    @Test
    void listsMoves() {
        String output = run("moves e2", "moves a1", "moves", "moves z9");
        assertTrue(output.contains("2 moves: e2e3 e2e4"));
        assertTrue(output.contains("No moves."));
        assertTrue(output.contains("20 moves: "));
        assertTrue(output.contains("Not a square: z9"));
    }

    @Test
    void singleMoveIsNotCalledMoves() {
        assertTrue(run("fen 7k/8/8/8/8/8/1r6/K7 w - - 0 1", "moves").contains("1 move: a1b2"));
    }

    @Test
    void announcesCheck() {
        assertTrue(run("e2e4", "d7d5", "f1b5").contains("Check.\nBlack to move > "));
    }

    @Test
    void announcesCheckmateAndTheWinner() {
        String output = run("f2f3", "e7e5", "g2g4", "d8h4", "e1e2");
        assertTrue(output.contains("Checkmate. Black wins.\nType undo to take the last move back, or quit."));
        assertTrue(output.contains("Not a legal move: e1e2"));
    }

    @Test
    void announcesEachKindOfDraw() {
        assertTrue(run("fen 7k/5K2/6Q1/8/8/8/8/8 b - - 0 1")
                .contains("The game is a draw: stalemate."));
        assertTrue(run("fen 7k/8/8/8/8/8/1r6/K7 w - - 0 1", "a1b2")
                .contains("The game is a draw: not enough pieces left to checkmate."));
        assertTrue(run("fen 8/8/8/4k3/8/8/4K3/R7 w - - 99 80", "a1a2")
                .contains("The game is a draw: fifty moves without a capture or pawn move."));
        assertTrue(run("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8")
                .contains("The game is a draw: the same position three times."));
    }

    @Test
    void undoesAndRedoes() {
        String output = run("undo", "redo", "e2e4", "undo", "fen", "redo", "fen");
        assertTrue(output.contains("There is no move to undo."));
        assertTrue(output.contains("There is no move to redo."));
        assertTrue(output.contains("Took back e2e4"));
        assertTrue(output.contains("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        assertTrue(output.contains("Replayed e2e4"));
        assertTrue(output.contains("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"));
    }

    @Test
    void loadsAPositionFromFenKeepingItsCase() {
        String output = run("FEN 4k3/8/8/8/8/8/8/4K2R w K - 0 1", "e1g1", "fen");
        assertTrue(output.contains("1 | .  .  .  .  K  .  .  R |"));
        assertTrue(output.contains("4k3/8/8/8/8/8/8/5RK1 b - - 1 1"));  // castled
    }

    @Test
    void reportsBadFenAndKeepsTheCurrentGame() {
        String output = run("e2e4", "fen not a position", "fen");
        assertTrue(output.contains("FEN needs 4 or 6 fields but has 3"));
        assertTrue(output.contains("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"));
    }

    @Test
    void promotesToAQueenUnlessAnotherPieceIsNamed() {
        assertTrue(run("fen 4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8", "fen")
                .contains("Q3k3/8/8/8/8/8/8/4K3 b - - 0 1"));
        assertTrue(run("fen 4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8n", "fen")
                .contains("N3k3/8/8/8/8/8/8/4K3 b - - 0 1"));
        assertTrue(run("fen 4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8r", "fen")
                .contains("R3k3/8/8/8/8/8/8/4K3 b - - 0 1"));
        assertTrue(run("fen 4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8b", "fen")
                .contains("B3k3/8/8/8/8/8/8/4K3 b - - 0 1"));
        assertTrue(run("fen 4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8q", "fen")
                .contains("Q3k3/8/8/8/8/8/8/4K3 b - - 0 1"));
    }

    @Test
    void quitAndExitStopReadingCommands() {
        assertFalse(run("quit", "e2e4").contains("Black to move"));
        assertFalse(run("exit", "e2e4").contains("Black to move"));
    }
}
