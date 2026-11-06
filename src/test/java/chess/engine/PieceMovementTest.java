package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Checks how each piece moves by placing pieces on an otherwise empty board.
 *
 * <p>JUnit creates a new instance of this class for every test, so each test
 * starts with its own empty board.
 */
class PieceMovementTest {

    private final Board board = new Board();

    private void place(String square, Piece piece) {
        board.setPiece(Position.of(square), piece);
    }

    /** Asserts that the piece on {@code from} can move to exactly the listed squares. */
    private void assertMoves(String from, String... expectedTargets) {
        List<Move> moves = board.pseudoLegalMovesFrom(Position.of(from));
        Set<String> actualTargets = new TreeSet<>();
        for (Move move : moves) {
            actualTargets.add(move.getTo().toString());
        }
        assertEquals(new TreeSet<>(Arrays.asList(expectedTargets)), actualTargets);
        assertEquals(expectedTargets.length, moves.size());  // no move listed twice
    }

    private Move moveBetween(String from, String to) {
        for (Move move : board.pseudoLegalMovesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                return move;
            }
        }
        throw new AssertionError("No move from " + from + " to " + to);
    }

    // ---------------------------------------------------------------- rook

    @Test
    void rookSlidesAlongItsRankAndFile() {
        place("d4", new Rook(WHITE));
        assertMoves("d4",
                "d1", "d2", "d3", "d5", "d6", "d7", "d8",
                "a4", "b4", "c4", "e4", "f4", "g4", "h4");
    }

    @Test
    void rookStopsBeforeItsOwnPieceAndOnAnEnemyPiece() {
        place("d4", new Rook(WHITE));
        place("d6", new Pawn(WHITE));
        place("f4", new Pawn(BLACK));
        assertMoves("d4",
                "d5",                // d6 is our own pawn
                "e4", "f4",          // f4 is captured, g4 and h4 are behind it
                "d1", "d2", "d3",
                "a4", "b4", "c4");
    }

    @Test
    void rookInTheCornerStillReachesFourteenSquares() {
        place("a1", new Rook(BLACK));
        assertMoves("a1",
                "a2", "a3", "a4", "a5", "a6", "a7", "a8",
                "b1", "c1", "d1", "e1", "f1", "g1", "h1");
    }

    // -------------------------------------------------------------- bishop

    @Test
    void bishopSlidesAlongBothDiagonals() {
        place("d4", new Bishop(WHITE));
        assertMoves("d4",
                "a1", "b2", "c3", "e5", "f6", "g7", "h8",
                "a7", "b6", "c5", "e3", "f2", "g1");
    }

    @Test
    void bishopStopsBeforeItsOwnPieceAndOnAnEnemyPiece() {
        place("d4", new Bishop(BLACK));
        place("f6", new Pawn(WHITE));
        place("b2", new Pawn(BLACK));
        assertMoves("d4",
                "e5", "f6",          // f6 is captured, g7 and h8 are behind it
                "c3",                // b2 is our own pawn
                "a7", "b6", "c5", "e3", "f2", "g1");
    }

    @Test
    void hemmedInBishopHasNoMoves() {
        place("c1", new Bishop(WHITE));
        place("b2", new Pawn(WHITE));
        place("d2", new Pawn(WHITE));
        assertMoves("c1");
    }

    // --------------------------------------------------------------- queen

    @Test
    void queenCombinesRookAndBishop() {
        place("d4", new Queen(WHITE));
        assertMoves("d4",
                "d1", "d2", "d3", "d5", "d6", "d7", "d8",
                "a4", "b4", "c4", "e4", "f4", "g4", "h4",
                "a1", "b2", "c3", "e5", "f6", "g7", "h8",
                "a7", "b6", "c5", "e3", "f2", "g1");
    }

    @Test
    void queenIsBlockedLikeAnyOtherSlider() {
        place("a1", new Queen(WHITE));
        place("a3", new Pawn(WHITE));
        place("c1", new Pawn(BLACK));
        place("c3", new Pawn(BLACK));
        assertMoves("a1", "a2", "b1", "c1", "b2", "c3");
    }

    // -------------------------------------------------------------- knight

    @Test
    void knightHasEightJumpsFromTheCentre() {
        place("d4", new Knight(WHITE));
        assertMoves("d4", "b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5");
    }

    @Test
    void knightHasTwoJumpsFromTheCorner() {
        place("a1", new Knight(WHITE));
        assertMoves("a1", "b3", "c2");
    }

    @Test
    void knightJumpsOverPiecesNextToIt() {
        place("d4", new Knight(WHITE));
        for (String square : new String[] {"c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5"}) {
            place(square, new Pawn(BLACK));
        }
        assertMoves("d4", "b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5");
    }

    @Test
    void knightCapturesEnemiesButNotItsOwnPieces() {
        place("d4", new Knight(WHITE));
        place("f5", new Pawn(WHITE));
        place("e6", new Pawn(BLACK));
        assertMoves("d4", "b3", "b5", "c2", "c6", "e2", "e6", "f3");
    }

    // ---------------------------------------------------------------- king

    @Test
    void kingStepsOneSquareInEveryDirection() {
        place("d4", new King(WHITE));
        assertMoves("d4", "c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5");
    }

    @Test
    void kingHasThreeMovesFromACorner() {
        place("a1", new King(WHITE));
        assertMoves("a1", "a2", "b1", "b2");

        place("h8", new King(BLACK));
        assertMoves("h8", "g7", "g8", "h7");
    }

    @Test
    void kingCapturesEnemiesButNotItsOwnPieces() {
        place("d4", new King(WHITE));
        place("d5", new Pawn(WHITE));
        place("e5", new Pawn(BLACK));
        assertMoves("d4", "c3", "c4", "c5", "d3", "e3", "e4", "e5");
    }

    // ---------------------------------------------------------------- pawn

    @Test
    void pawnOnItsStartingRankMovesOneOrTwoSquares() {
        place("e2", new Pawn(WHITE));
        assertMoves("e2", "e3", "e4");

        place("c7", new Pawn(BLACK));
        assertMoves("c7", "c6", "c5");
    }

    @Test
    void pawnThatHasMovedAdvancesOnlyOneSquare() {
        place("e3", new Pawn(WHITE));
        assertMoves("e3", "e4");

        place("c6", new Pawn(BLACK));
        assertMoves("c6", "c5");
    }

    @Test
    void pawnCannotMoveOrCaptureStraightAhead() {
        place("e2", new Pawn(WHITE));
        place("e3", new Knight(BLACK));
        assertMoves("e2");
    }

    @Test
    void pawnCannotJumpOverAPieceOnItsDoubleStep() {
        place("e7", new Pawn(BLACK));
        place("e6", new Knight(BLACK));
        assertMoves("e7");
    }

    @Test
    void pawnDoubleStepNeedsTheSecondSquareEmptyToo() {
        place("e2", new Pawn(WHITE));
        place("e4", new Pawn(BLACK));
        assertMoves("e2", "e3");
    }

    @Test
    void whitePawnCapturesDiagonallyUpTheBoard() {
        place("e4", new Pawn(WHITE));
        place("d5", new Pawn(BLACK));
        place("f5", new Pawn(BLACK));
        place("d3", new Pawn(BLACK));  // behind the pawn: cannot be captured
        assertMoves("e4", "e5", "d5", "f5");
    }

    @Test
    void blackPawnCapturesDiagonallyDownTheBoard() {
        place("e5", new Pawn(BLACK));
        place("d4", new Pawn(WHITE));
        place("f4", new Pawn(WHITE));
        place("d6", new Pawn(WHITE));  // behind the pawn: cannot be captured
        assertMoves("e5", "e4", "d4", "f4");
    }

    @Test
    void pawnDoesNotCaptureItsOwnPieces() {
        place("e4", new Pawn(WHITE));
        place("d5", new Knight(WHITE));
        assertMoves("e4", "e5");
    }

    @Test
    void pawnOnTheEdgeOfTheBoardCapturesInwardOnly() {
        place("a2", new Pawn(WHITE));
        place("b3", new Pawn(BLACK));
        assertMoves("a2", "a3", "a4", "b3");

        place("h7", new Pawn(BLACK));
        place("g6", new Pawn(WHITE));
        assertMoves("h7", "h6", "h5", "g6");
    }

    @Test
    void pawnOnTheLastRankHasNowhereToGo() {
        place("e8", new Pawn(WHITE));
        assertMoves("e8");

        place("b1", new Pawn(BLACK));
        assertMoves("b1");
    }

    // ------------------------------------------------------------ captures

    @Test
    void captureMovesRememberThePieceTheyTake() {
        place("d4", new Rook(WHITE));
        place("d6", new Bishop(BLACK));

        Move capture = moveBetween("d4", "d6");
        assertTrue(capture.isCapture());
        assertEquals(new Bishop(BLACK), capture.getCaptured());
        assertEquals(new Rook(WHITE), capture.getPiece());

        Move quiet = moveBetween("d4", "d5");
        assertFalse(quiet.isCapture());
        assertNull(quiet.getCaptured());
    }

    @Test
    void emptySquareHasNoMoves() {
        assertMoves("d4");
    }
}
