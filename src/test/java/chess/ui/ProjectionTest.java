package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.engine.Board;
import chess.engine.Position;
import org.junit.jupiter.api.Test;

class ProjectionTest {

    private static final double SIZE = 560;
    private static final double TOLERANCE = 1e-9;

    private final Projection flat = new Projection(SIZE, SIZE, false, false);
    private final Projection perspective = new Projection(SIZE, SIZE, true, false);

    // ---------------------------------------------------------------- flat

    @Test
    void flatBoardIsMadeOfEqualSquares() {
        double unit = flat.getUnit();
        for (int i = 0; i < Board.SIZE; i++) {
            assertEquals(unit, flat.screenX(i + 1, 0) - flat.screenX(i, 0), TOLERANCE);
            assertEquals(unit, flat.screenY(i) - flat.screenY(i + 1), TOLERANCE);
            assertEquals(unit, flat.screenX(i + 1, 8) - flat.screenX(i, 8), TOLERANCE);  // same width far away
        }
        assertEquals(1.0, flat.scale(8), TOLERANCE);
        assertEquals(1.0, flat.flatness(), TOLERANCE);
        assertFalse(flat.isPerspective());
    }

    @Test
    void flatBoardIsCentredInItsArea() {
        assertEquals(SIZE / 2, flat.screenX(4, 0), TOLERANCE);
        assertEquals(SIZE / 2, (flat.screenY(0) + flat.screenY(8)) / 2, TOLERANCE);
    }

    // ---------------------------------------------------------- perspective

    @Test
    void farEdgeIsNarrowerThanTheNearEdge() {
        double nearWidth = perspective.screenX(8, 0) - perspective.screenX(0, 0);
        double farWidth = perspective.screenX(8, 8) - perspective.screenX(0, 8);
        assertTrue(farWidth < nearWidth);
        assertTrue(farWidth > nearWidth / 2, "still a board, not a triangle");
        assertEquals(nearWidth * perspective.scale(8), farWidth, TOLERANCE);
    }

    @Test
    void thingsGetSmallerWithDistance() {
        assertEquals(1.0, perspective.scale(0), TOLERANCE);
        for (int depth = 0; depth < Board.SIZE; depth++) {
            assertTrue(perspective.scale(depth + 1) < perspective.scale(depth));
        }
    }

    @Test
    void furtherRowsAreHigherOnTheScreenAndShallower() {
        double previousHeight = Double.MAX_VALUE;
        for (int row = 0; row < Board.SIZE; row++) {
            double height = perspective.screenY(row) - perspective.screenY(row + 1);
            assertTrue(height > 0, "row " + row + " goes up the screen");
            assertTrue(height < previousHeight, "row " + row + " is shallower than the one in front");
            previousHeight = height;
        }
    }

    @Test
    void boardStaysCentredAndSymmetricalInPerspective() {
        for (int depth = 0; depth <= Board.SIZE; depth++) {
            assertEquals(SIZE / 2, perspective.screenX(4, depth), TOLERANCE);
            double left = SIZE / 2 - perspective.screenX(0, depth);
            double right = perspective.screenX(8, depth) - SIZE / 2;
            assertEquals(left, right, TOLERANCE);
        }
    }

    @Test
    void wholeBoardFitsInItsAreaInBothViews() {
        for (Projection projection : new Projection[] {flat, perspective}) {
            for (double x : new double[] {-0.36, 8.36}) {
                for (double depth : new double[] {-0.36, 8.36}) {
                    assertTrue(projection.screenX(x, depth) >= 0 && projection.screenX(x, depth) <= SIZE);
                    assertTrue(projection.screenY(depth) >= 0 && projection.screenY(depth) <= SIZE);
                }
            }
        }
        // In 3D there must also be room above the far row for a piece standing on it.
        assertTrue(perspective.screenY(7.3) - perspective.getUnit() * perspective.scale(7.3) > 0);
    }

    @Test
    void circlesOnTheBoardAreSquashedInPerspectiveOnly() {
        assertTrue(perspective.flatness() < 1);
        assertTrue(perspective.flatness() > 0.5);
    }

    @Test
    void outlineListsCornersNearLeftNearRightFarRightFarLeft() {
        double[] corners = perspective.outline(2, 3, 3, 4);
        assertEquals(8, corners.length);
        assertEquals(perspective.screenX(2, 3), corners[0], TOLERANCE);
        assertEquals(perspective.screenY(3), corners[1], TOLERANCE);
        assertEquals(perspective.screenX(3, 3), corners[2], TOLERANCE);
        assertEquals(perspective.screenX(3, 4), corners[4], TOLERANCE);
        assertEquals(perspective.screenY(4), corners[5], TOLERANCE);
        assertEquals(perspective.screenX(2, 4), corners[6], TOLERANCE);
        assertTrue(corners[2] - corners[0] > corners[4] - corners[6], "the near side is the wider one");
    }

    // ------------------------------------------------------------ flipping

    @Test
    void fromWhitesSideA1IsNearLeft() {
        assertEquals(0, flat.column(Position.of("a1")));
        assertEquals(0, flat.row(Position.of("a1")));
        assertEquals(7, flat.column(Position.of("h8")));
        assertEquals(7, flat.row(Position.of("h8")));
        assertEquals(4, flat.column(Position.of("e2")));
        assertEquals(1, flat.row(Position.of("e2")));
    }

    @Test
    void fromBlacksSideH8IsNearLeft() {
        Projection flipped = new Projection(SIZE, SIZE, true, true);
        assertTrue(flipped.isFlipped());
        assertEquals(0, flipped.column(Position.of("h8")));
        assertEquals(0, flipped.row(Position.of("h8")));
        assertEquals(7, flipped.column(Position.of("a1")));
        assertEquals(7, flipped.row(Position.of("a1")));
        assertEquals(3, flipped.column(Position.of("e7")));
        assertEquals(1, flipped.row(Position.of("e7")));
    }

    @Test
    void everySquareMapsToItsOwnPlaceAndBack() {
        for (boolean flipped : new boolean[] {false, true}) {
            Projection projection = new Projection(SIZE, SIZE, true, flipped);
            for (int file = 0; file < Board.SIZE; file++) {
                for (int rank = 0; rank < Board.SIZE; rank++) {
                    Position position = new Position(file, rank);
                    assertEquals(position,
                            projection.positionAt(projection.column(position), projection.row(position)));
                }
            }
        }
    }

    @Test
    void flippingDoesNotChangeTheShapeOfTheBoard() {
        Projection flipped = new Projection(SIZE, SIZE, true, true);
        assertEquals(perspective.screenX(3, 5), flipped.screenX(3, 5), TOLERANCE);
        assertEquals(perspective.screenY(5), flipped.screenY(5), TOLERANCE);
    }
}
