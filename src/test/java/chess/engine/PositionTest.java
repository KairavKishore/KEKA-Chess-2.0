package chess.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void parsesAlgebraicNotation() {
        Position e4 = Position.of("e4");
        assertEquals(4, e4.getFile());
        assertEquals(3, e4.getRank());
    }

    @Test
    void cornersMapToTheEndsOfTheGrid() {
        assertEquals(new Position(0, 0), Position.of("a1"));
        assertEquals(new Position(7, 0), Position.of("h1"));
        assertEquals(new Position(0, 7), Position.of("a8"));
        assertEquals(new Position(7, 7), Position.of("h8"));
    }

    @Test
    void printsAsAlgebraicNotation() {
        assertEquals("e4", new Position(4, 3).toString());
    }

    @Test
    void everySquareSurvivesARoundTripThroughText() {
        for (int file = 0; file < Board.SIZE; file++) {
            for (int rank = 0; rank < Board.SIZE; rank++) {
                Position position = new Position(file, rank);
                assertEquals(position, Position.of(position.toString()));
            }
        }
    }

    @Test
    void rejectsSquaresOffTheBoard() {
        assertThrows(IllegalArgumentException.class, () -> new Position(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, -1));
    }

    @Test
    void rejectsTextThatIsNotASquare() {
        assertThrows(IllegalArgumentException.class, () -> Position.of("i1"));
        assertThrows(IllegalArgumentException.class, () -> Position.of("a9"));
        assertThrows(IllegalArgumentException.class, () -> Position.of("a0"));
        assertThrows(IllegalArgumentException.class, () -> Position.of("E4"));
        assertThrows(IllegalArgumentException.class, () -> Position.of("e"));
        assertThrows(IllegalArgumentException.class, () -> Position.of("e44"));
        assertThrows(IllegalArgumentException.class, () -> Position.of(""));
        assertThrows(IllegalArgumentException.class, () -> Position.of(null));
    }

    @Test
    void positionsAreComparedByValue() {
        Position first = new Position(2, 5);
        Position second = new Position(2, 5);
        assertEquals(first, first);
        assertEquals(first, second);
        assertNotEquals(first, null);
        assertNotEquals(first, "c6");
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Position(5, 2));
    }
}
