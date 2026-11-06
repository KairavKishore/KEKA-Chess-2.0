package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoveTest {

    private static final Position E5 = Position.of("e5");
    private static final Position D6 = Position.of("d6");
    private static final Position E7 = Position.of("e7");
    private static final Position E8 = Position.of("e8");

    @Test
    void movesAreComparedByValue() {
        Move first = new Move(E5, D6, new Pawn(WHITE), new Knight(BLACK));
        Move second = new Move(E5, D6, new Pawn(WHITE), new Knight(BLACK));
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "e5d6");
    }

    @Test
    void movesDifferIfAnyPartDiffers() {
        Move move = new Move(E5, D6, new Pawn(WHITE), new Knight(BLACK));
        assertNotEquals(move, new Move(E7, D6, new Pawn(WHITE), new Knight(BLACK)));   // from
        assertNotEquals(move, new Move(E5, E8, new Pawn(WHITE), new Knight(BLACK)));   // to
        assertNotEquals(move, new Move(E5, D6, new Bishop(WHITE), new Knight(BLACK))); // piece
        assertNotEquals(move, new Move(E5, D6, new Pawn(WHITE), new Rook(BLACK)));     // captured
        assertNotEquals(move, new Move(E5, D6, new Pawn(WHITE), null));
    }

    @Test
    void eachPromotionChoiceIsADifferentMove() {
        Move queen = new Move(E7, E8, new Pawn(WHITE), null, PieceType.QUEEN);
        Move knight = new Move(E7, E8, new Pawn(WHITE), null, PieceType.KNIGHT);
        assertNotEquals(queen, knight);
        assertNotEquals(queen, new Move(E7, E8, new Pawn(WHITE), null));
        assertEquals(queen, new Move(E7, E8, new Pawn(WHITE), null, PieceType.QUEEN));
    }

    @Test
    void enPassantIsADifferentMoveFromAnOrdinaryCaptureOnTheSameSquares() {
        Move enPassant = Move.enPassant(E5, D6, new Pawn(WHITE), new Pawn(BLACK));
        Move ordinary = new Move(E5, D6, new Pawn(WHITE), new Pawn(BLACK));
        assertNotEquals(enPassant, ordinary);
        assertTrue(enPassant.isEnPassant());
        assertTrue(enPassant.isCapture());
        assertFalse(ordinary.isEnPassant());
    }

    @Test
    void onlyAKingMovingTwoFilesIsCastling() {
        Position e1 = Position.of("e1");
        assertTrue(new Move(e1, Position.of("g1"), new King(WHITE), null).isCastling());
        assertTrue(new Move(e1, Position.of("c1"), new King(WHITE), null).isCastling());
        assertFalse(new Move(e1, Position.of("f1"), new King(WHITE), null).isCastling());
        assertFalse(new Move(e1, Position.of("g1"), new Rook(WHITE), null).isCastling());
    }

    @Test
    void printsInCoordinateNotation() {
        assertEquals("e5d6", new Move(E5, D6, new Pawn(WHITE), null).toString());
        assertEquals("e7e8r", new Move(E7, E8, new Pawn(WHITE), null, PieceType.ROOK).toString());
    }

    @Test
    void needsBothSquaresAndAPiece() {
        assertThrows(NullPointerException.class, () -> new Move(null, D6, new Pawn(WHITE), null));
        assertThrows(NullPointerException.class, () -> new Move(E5, null, new Pawn(WHITE), null));
        assertThrows(NullPointerException.class, () -> new Move(E5, D6, null, null));
        assertThrows(NullPointerException.class, () -> Move.enPassant(E5, D6, new Pawn(WHITE), null));
    }
}
