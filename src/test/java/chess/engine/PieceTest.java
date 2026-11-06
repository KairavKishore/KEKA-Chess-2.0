package chess.engine;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PieceTest {

    @Test
    void whitePiecesPrintInUpperCaseAndBlackInLowerCase() {
        assertEquals('K', new King(WHITE).getSymbol());
        assertEquals('Q', new Queen(WHITE).getSymbol());
        assertEquals('R', new Rook(WHITE).getSymbol());
        assertEquals('B', new Bishop(WHITE).getSymbol());
        assertEquals('N', new Knight(WHITE).getSymbol());
        assertEquals('P', new Pawn(WHITE).getSymbol());

        assertEquals('k', new King(BLACK).getSymbol());
        assertEquals('n', new Knight(BLACK).getSymbol());
        assertEquals('p', new Pawn(BLACK).getSymbol());
    }

    @Test
    void piecesKnowTheirTypeAndColour() {
        Piece knight = new Knight(BLACK);
        assertEquals(PieceType.KNIGHT, knight.getType());
        assertEquals(BLACK, knight.getColor());
        assertEquals("BLACK KNIGHT", knight.toString());
    }

    @Test
    void piecesOfTheSameKindAndColourAreEqual() {
        assertEquals(new Rook(WHITE), new Rook(WHITE));
        assertEquals(new Rook(WHITE).hashCode(), new Rook(WHITE).hashCode());
        assertNotEquals(new Rook(WHITE), new Rook(BLACK));
        assertNotEquals(new Rook(WHITE), new Bishop(WHITE));
    }

    @Test
    void oppositeColourFlipsTheSide() {
        assertEquals(BLACK, WHITE.opposite());
        assertEquals(WHITE, BLACK.opposite());
    }
}
