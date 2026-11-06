package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Attacked squares, check, and the filter that removes moves leaving your own king in check. */
class CheckTest {

    private final Board board = new Board();

    private void place(String square, Piece piece) {
        board.setPiece(Position.of(square), piece);
    }

    private boolean attacked(String square, PieceColor byColor) {
        return board.isSquareAttacked(Position.of(square), byColor);
    }

    /** Asserts that the piece on {@code from} can legally move to exactly the listed squares. */
    private void assertLegalMoves(String from, String... expectedTargets) {
        Set<String> actualTargets = new TreeSet<>();
        for (Move move : board.legalMovesFrom(Position.of(from))) {
            actualTargets.add(move.getTo().toString());
        }
        assertEquals(new TreeSet<>(Arrays.asList(expectedTargets)), actualTargets);
    }

    // ------------------------------------------------------------- attacks

    @Test
    void findsEachKing() {
        place("g1", new King(WHITE));
        assertEquals(Position.of("g1"), board.findKing(WHITE));
        assertNull(board.findKing(BLACK));
    }

    @Test
    void rookAttacksAlongLinesUntilSomethingBlocksIt() {
        place("a1", new Rook(WHITE));
        place("a5", new Pawn(BLACK));
        assertTrue(attacked("a4", WHITE));
        assertTrue(attacked("a5", WHITE));   // the blocker itself
        assertFalse(attacked("a6", WHITE));  // behind the blocker
        assertTrue(attacked("h1", WHITE));
        assertFalse(attacked("b2", WHITE));  // not on a rank or file
        assertFalse(attacked("a4", BLACK));  // wrong colour asked about
    }

    @Test
    void bishopAttacksDiagonallyAndKnightInAnLShape() {
        place("c1", new Bishop(BLACK));
        place("d4", new Knight(BLACK));
        assertTrue(attacked("h6", BLACK));
        assertFalse(attacked("c5", BLACK));
        assertTrue(attacked("e6", BLACK));
        assertFalse(attacked("d5", BLACK));
    }

    @Test
    void pawnAttacksDiagonallyEvenOntoEmptySquares() {
        place("e4", new Pawn(WHITE));
        assertTrue(attacked("d5", WHITE));
        assertTrue(attacked("f5", WHITE));
        assertFalse(attacked("e5", WHITE));  // it can move here but not capture here
        assertFalse(attacked("d3", WHITE));  // behind it

        place("b7", new Pawn(BLACK));
        assertTrue(attacked("a6", BLACK));
        assertTrue(attacked("c6", BLACK));
        assertFalse(attacked("a8", BLACK));
    }

    @Test
    void kingAttacksTheSquaresNextToIt() {
        place("e1", new King(WHITE));
        assertTrue(attacked("d2", WHITE));
        assertFalse(attacked("e3", WHITE));
        assertFalse(attacked("e1", WHITE));  // not its own square
    }

    // --------------------------------------------------------------- check

    @Test
    void kingIsInCheckWhenAnEnemyPieceAttacksIt() {
        place("e1", new King(WHITE));
        place("e8", new Rook(BLACK));
        assertTrue(board.isInCheck(WHITE));

        place("e4", new Pawn(WHITE));  // blocks the file
        assertFalse(board.isInCheck(WHITE));
    }

    @Test
    void sideWithoutAKingIsNeverInCheck() {
        place("e8", new Rook(BLACK));
        assertFalse(board.isInCheck(WHITE));
    }

    // -------------------------------------------------------- legal moves

    @Test
    void pinnedPieceCanOnlyMoveAlongThePin() {
        place("e1", new King(WHITE));
        place("e2", new Rook(WHITE));
        place("e8", new Rook(BLACK));
        // Sideways moves would uncover the king. Capturing the attacker is fine.
        assertLegalMoves("e2", "e3", "e4", "e5", "e6", "e7", "e8");
    }

    @Test
    void pinnedKnightCannotMoveAtAll() {
        place("e1", new King(WHITE));
        place("e2", new Knight(WHITE));
        place("e8", new Rook(BLACK));
        assertLegalMoves("e2");
    }

    @Test
    void kingCannotStepOntoAnAttackedSquare() {
        place("e1", new King(WHITE));
        place("d8", new Rook(BLACK));  // covers the whole d-file
        assertLegalMoves("e1", "e2", "f1", "f2");
    }

    @Test
    void kingCannotCaptureADefendedPiece() {
        place("e1", new King(WHITE));
        place("e2", new Queen(BLACK));
        place("e8", new Rook(BLACK));  // defends the queen
        assertLegalMoves("e1");        // every other square is covered by the queen
    }

    @Test
    void checkMustBeAnsweredByMovingBlockingOrCapturing() {
        place("e1", new King(WHITE));
        place("a4", new Rook(WHITE));
        place("h3", new Bishop(WHITE));
        place("e8", new Rook(BLACK));

        assertLegalMoves("e1", "d1", "d2", "f1", "f2");  // step off the file
        assertLegalMoves("a4", "e4");                    // block
        assertLegalMoves("h3", "e6");                    // block; nothing else helps
        assertEquals(6, board.legalMoves(WHITE).size());
    }

    @Test
    void checkCanBeAnsweredByCapturingTheAttacker() {
        place("e1", new King(WHITE));
        place("a8", new Rook(WHITE));
        place("e8", new Rook(BLACK));
        assertLegalMoves("a8", "e8");
    }
}
