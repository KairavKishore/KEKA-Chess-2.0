package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Taking moves back, on the board and in a game. */
class UndoTest {

    private static Move find(Game game, String from, String to) {
        for (Move move : game.legalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                return move;
            }
        }
        throw new AssertionError("No legal move from " + from + " to " + to);
    }

    private static Move play(Game game, String from, String to) {
        Move move = find(game, from, to);
        game.makeMove(move);
        return move;
    }

    /**
     * Plays the move from the position and takes it back, checking that
     * everything FEN records is restored: the pieces, the side to move, the
     * castling rights, the en passant square and both counters.
     */
    private static void assertUndoRestores(String fen, String from, String to) {
        Game game = Game.fromFen(fen);
        Move move = play(game, from, to);
        assertFalse(fen.equals(game.toFen()), "the move should change the position");

        assertEquals(move, game.undo());
        assertEquals(fen, game.toFen());
    }

    // --------------------------------------------- every kind of move

    @Test
    void undoesAQuietMove() {
        assertUndoRestores(Fen.STARTING_POSITION, "g1", "f3");
    }

    @Test
    void undoesACaptureAndPutsThePieceBack() {
        assertUndoRestores("4k3/8/8/3q4/4P3/8/8/4K3 w - - 7 20", "e4", "d5");
    }

    @Test
    void undoesADoubleStepAndForgetsItsEnPassantSquare() {
        assertUndoRestores(Fen.STARTING_POSITION, "e2", "e4");
    }

    @Test
    void undoRestoresAnEnPassantSquareTheMoveHadCleared() {
        assertUndoRestores("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2", "g1", "f3");
    }

    @Test
    void undoesEnPassantAndPutsThePawnBackBesideTheCapturer() {
        assertUndoRestores("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 10", "e5", "d6");
    }

    @Test
    void undoesCastlingOnEitherSideAndRestoresTheRights() {
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 15", "e1", "g1");
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 15", "e1", "c1");
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 3 15", "e8", "g8");
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 3 15", "e8", "c8");
    }

    @Test
    void undoRestoresCastlingRightsLostByMovingAKingOrRook() {
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1", "e2");
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "h1", "h4");
    }

    @Test
    void undoRestoresCastlingRightsLostWhenARookWasCaptured() {
        assertUndoRestores("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "a1", "a8");
    }

    @Test
    void undoesAPromotionBackToAPawn() {
        assertUndoRestores("4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7", "a8");
    }

    @Test
    void undoesAPromotionByCapture() {
        assertUndoRestores("1n2k3/P7/8/8/8/8/8/4K3 w - - 5 1", "a7", "b8");
    }

    // ------------------------------------------------------- the board

    @Test
    void boardUndoesSeveralMovesInReverseOrder() {
        Board board = Board.startingPosition();
        Move first = board.legalMovesFrom(Position.of("e2")).get(1);   // e2e4
        board.makeMove(first);
        Move second = board.legalMovesFrom(Position.of("d7")).get(1);  // d7d5
        board.makeMove(second);
        Move third = board.legalMovesFrom(Position.of("e4")).get(1);   // e4xd5
        assertTrue(third.isCapture());
        board.makeMove(third);

        board.undoMove(third);
        assertEquals(new Pawn(BLACK), board.getPiece(Position.of("d5")));
        assertEquals(new Pawn(WHITE), board.getPiece(Position.of("e4")));
        assertEquals(Position.of("d6"), board.getEnPassantTarget());

        board.undoMove(second);
        board.undoMove(first);
        assertEquals(Board.startingPosition().toString(), board.toString());
        assertNull(board.getEnPassantTarget());
    }

    @Test
    void boardRefusesToUndoAnythingButTheLastMove() {
        Board board = Board.startingPosition();
        Move first = board.legalMovesFrom(Position.of("e2")).get(0);
        Move second = board.legalMovesFrom(Position.of("e7")).get(0);
        board.makeMove(first);
        board.makeMove(second);

        assertThrows(IllegalArgumentException.class, () -> board.undoMove(first));
        board.undoMove(second);
        board.undoMove(first);
        assertThrows(IllegalStateException.class, () -> board.undoMove(first));
    }

    @Test
    void copiedBoardStartsWithNoMovesToUndo() {
        Board board = Board.startingPosition();
        Move move = board.legalMovesFrom(Position.of("e2")).get(0);
        board.makeMove(move);

        Board copy = new Board(board);

        assertThrows(IllegalStateException.class, () -> copy.undoMove(move));
        board.undoMove(move);  // the original still can
    }

    @Test
    void askingForLegalMovesLeavesNothingToUndo() {
        Board board = Board.startingPosition();
        Move move = board.legalMoves(WHITE).get(0);
        assertThrows(IllegalStateException.class, () -> board.undoMove(move));
    }

    // -------------------------------------------------------- the game

    @Test
    void newGameHasNothingToUndoOrRedo() {
        Game game = new Game();
        assertFalse(game.canUndo());
        assertFalse(game.canRedo());
        assertNull(game.getLastMove());
        assertTrue(game.getMoveHistory().isEmpty());
        assertThrows(IllegalStateException.class, game::undo);
        assertThrows(IllegalStateException.class, game::redo);
    }

    @Test
    void undoGivesTheTurnBack() {
        Game game = new Game();
        play(game, "e2", "e4");
        assertEquals(BLACK, game.getTurn());

        game.undo();

        assertEquals(WHITE, game.getTurn());
        assertEquals(Fen.STARTING_POSITION, game.toFen());
        assertFalse(game.canUndo());
        assertTrue(game.canRedo());
    }

    @Test
    void redoReplaysUndoneMovesInTheOrderTheyWerePlayed() {
        Game game = new Game();
        Move e4 = play(game, "e2", "e4");
        Move e5 = play(game, "e7", "e5");
        Move nf3 = play(game, "g1", "f3");
        String afterThreeMoves = game.toFen();

        game.undo();
        game.undo();
        game.undo();
        assertEquals(Fen.STARTING_POSITION, game.toFen());

        assertEquals(e4, game.redo());
        assertEquals(e5, game.redo());
        assertEquals(nf3, game.redo());
        assertEquals(afterThreeMoves, game.toFen());
        assertFalse(game.canRedo());
        assertEquals(List.of(e4, e5, nf3), game.getMoveHistory());
    }

    @Test
    void playingANewMoveClearsTheRedoStack() {
        Game game = new Game();
        play(game, "e2", "e4");
        play(game, "e7", "e5");
        game.undo();
        assertTrue(game.canRedo());

        Move sicilian = play(game, "c7", "c5");

        assertFalse(game.canRedo());
        assertThrows(IllegalStateException.class, game::redo);
        assertEquals(sicilian, game.getLastMove());
        assertEquals(2, game.getMoveHistory().size());
    }

    @Test
    void moveHistoryAndLastMoveFollowUndo() {
        Game game = new Game();
        Move e4 = play(game, "e2", "e4");
        Move e5 = play(game, "e7", "e5");
        assertEquals(List.of(e4, e5), game.getMoveHistory());
        assertEquals(e5, game.getLastMove());

        game.undo();

        assertEquals(List.of(e4), game.getMoveHistory());
        assertEquals(e4, game.getLastMove());
    }

    @Test
    void checkmateCanBeTakenBack() {
        Game game = new Game();
        play(game, "f2", "f3");
        play(game, "e7", "e5");
        play(game, "g2", "g4");
        play(game, "d8", "h4");
        assertEquals(GameStatus.CHECKMATE, game.getStatus());

        game.undo();

        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(BLACK, game.getTurn());
        assertEquals(new Queen(BLACK), game.getBoard().getPiece(Position.of("d8")));
    }

    @Test
    void wholeGameCanBeUndoneBackToTheStart() {
        Game game = new Game();
        String[][] moves = {
            {"e2", "e4"}, {"e7", "e5"}, {"g1", "f3"}, {"b8", "c6"}, {"f1", "c4"}, {"f8", "c5"},
            {"e1", "g1"}, {"g8", "f6"}, {"d2", "d4"}, {"e5", "d4"}, {"f3", "d4"}, {"e8", "g8"},
        };
        for (String[] move : moves) {
            play(game, move[0], move[1]);
        }
        assertEquals(7, game.getFullmoveNumber());

        while (game.canUndo()) {
            game.undo();
        }
        assertEquals(Fen.STARTING_POSITION, game.toFen());

        while (game.canRedo()) {
            game.redo();
        }
        assertEquals("r1bq1rk1/pppp1ppp/2n2n2/2b5/2BNP3/8/PPP2PPP/RNBQ1RK1 w - - 1 7", game.toFen());
    }
}
