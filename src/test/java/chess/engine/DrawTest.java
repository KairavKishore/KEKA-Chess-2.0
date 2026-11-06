package chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The fifty-move rule, threefold repetition and insufficient material. */
class DrawTest {

    private static Move play(Game game, String from, String to) {
        for (Move move : game.legalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                game.makeMove(move);
                return move;
            }
        }
        throw new AssertionError("No legal move from " + from + " to " + to);
    }

    private static boolean insufficient(String fen) {
        return Game.fromFen(fen).getBoard().hasInsufficientMaterial();
    }

    /** Both sides move a knight out and back, returning to the position they started from. */
    private static void shuffleKnights(Game game) {
        play(game, "g1", "f3");
        play(game, "g8", "f6");
        play(game, "f3", "g1");
        play(game, "f6", "g8");
    }

    // ---------------------------------------------------------- statuses

    @Test
    void drawsAreGameOverButCheckIsNot() {
        assertFalse(GameStatus.IN_PROGRESS.isGameOver());
        assertFalse(GameStatus.CHECK.isGameOver());
        assertTrue(GameStatus.CHECKMATE.isGameOver());
        assertFalse(GameStatus.CHECKMATE.isDraw());
        for (GameStatus draw : new GameStatus[] {
                GameStatus.STALEMATE, GameStatus.DRAW_BY_FIFTY_MOVES,
                GameStatus.DRAW_BY_REPETITION, GameStatus.DRAW_BY_INSUFFICIENT_MATERIAL}) {
            assertTrue(draw.isGameOver());
            assertTrue(draw.isDraw());
        }
    }

    // ------------------------------------------------------- fifty moves

    @Test
    void halfmoveClockCountsQuietMovesAndResetsOnPawnMovesAndCaptures() {
        Game game = new Game();
        play(game, "g1", "f3");
        play(game, "b8", "c6");
        assertEquals(2, game.getBoard().getHalfmoveClock());

        play(game, "e2", "e4");  // pawn move
        assertEquals(0, game.getBoard().getHalfmoveClock());

        play(game, "c6", "d4");
        play(game, "f3", "d4");  // capture
        assertEquals(0, game.getBoard().getHalfmoveClock());
    }

    @Test
    void fiftyMovesEachWithoutProgressIsADraw() {
        Game game = Game.fromFen("8/8/8/4k3/8/8/4K3/R7 w - - 99 80");
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());

        Move hundredth = play(game, "a1", "a2");

        assertEquals(GameStatus.DRAW_BY_FIFTY_MOVES, game.getStatus());
        assertTrue(game.legalMoves().isEmpty());
        assertTrue(game.legalMovesFrom(Position.of("e5")).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> game.makeMove(hundredth));

        game.undo();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertFalse(game.legalMoves().isEmpty());
    }

    @Test
    void checkmateOnTheHundredthHalfMoveStillWins() {
        Game game = Game.fromFen("7k/8/6K1/8/8/8/8/R7 w - - 99 80");
        play(game, "a1", "a8");
        assertEquals(GameStatus.CHECKMATE, game.getStatus());
    }

    // -------------------------------------------------------- repetition

    @Test
    void samePositionThreeTimesIsADraw() {
        Game game = new Game();      // first time
        shuffleKnights(game);        // second time
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());

        shuffleKnights(game);        // third time

        assertEquals(GameStatus.DRAW_BY_REPETITION, game.getStatus());
        assertTrue(game.legalMoves().isEmpty());

        game.undo();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        game.redo();
        assertEquals(GameStatus.DRAW_BY_REPETITION, game.getStatus());
    }

    @Test
    void samePiecesWithDifferentCastlingRightsIsNotARepetition() {
        Game game = Game.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        // The rooks go out and back twice, so the pieces stand like this for
        // the third time. But after the first trip the rights to castle with
        // those rooks had gone, which made it a different position: this one
        // has only come up twice.
        for (int trip = 0; trip < 2; trip++) {
            play(game, "h1", "h2");
            play(game, "h8", "h7");
            play(game, "h2", "h1");
            play(game, "h7", "h8");
        }

        assertEquals("r3k2r/8/8/8/8/8/8/R3K2R w Qq - 8 5", game.toFen());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    // ----------------------------------------------- insufficient material

    @Test
    void loneKingsAndSingleMinorPiecesCannotMate() {
        assertTrue(insufficient("8/8/8/4k3/8/8/4K3/8 w - - 0 1"));     // king v king
        assertTrue(insufficient("8/8/8/4k3/8/8/4K3/6N1 w - - 0 1"));   // knight
        assertTrue(insufficient("8/8/8/4k3/8/8/4K3/5B2 w - - 0 1"));   // bishop
        assertTrue(insufficient("5b2/8/8/4k3/8/8/4K3/8 w - - 0 1"));   // Black's bishop
    }

    @Test
    void bishopsAllOnOneSquareColourCannotMate() {
        assertTrue(insufficient("2b5/8/8/4k3/8/8/4K3/5B2 w - - 0 1"));   // c8 and f1 are both light
        assertFalse(insufficient("5b2/8/8/4k3/8/8/4K3/5B2 w - - 0 1"));  // f8 is dark, f1 is light
    }

    @Test
    void anyPawnRookOrQueenIsEnoughToPlayOn() {
        assertFalse(insufficient(Fen.STARTING_POSITION));
        assertFalse(insufficient("8/8/8/4k3/8/8/4KP2/8 w - - 0 1"));
        assertFalse(insufficient("8/8/8/4k3/8/8/4K3/R7 w - - 0 1"));
        assertFalse(insufficient("8/8/8/4k3/8/8/4K3/3q4 w - - 0 1"));
        assertFalse(insufficient("8/8/8/4k3/8/8/4K3/5BN1 w - - 0 1"));  // bishop and knight can mate
        assertFalse(insufficient("8/8/8/4k3/8/8/4K3/5NN1 w - - 0 1"));  // two knights: mate is possible, if not forced
    }

    @Test
    void capturingTheLastPieceEndsTheGameAsADraw() {
        Game game = Game.fromFen("7k/8/8/8/8/8/1r6/K7 w - - 0 1");
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());

        play(game, "a1", "b2");

        assertEquals(GameStatus.DRAW_BY_INSUFFICIENT_MATERIAL, game.getStatus());
        assertTrue(game.getStatus().isDraw());
        assertTrue(game.legalMoves().isEmpty());
    }
}
