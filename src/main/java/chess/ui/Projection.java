package chess.ui;

import chess.engine.Board;
import chess.engine.Position;

/**
 * Where the board appears on screen. It turns a point on the board into a
 * point in the window, either looking straight down (flat) or from behind
 * one player's side (pseudo-3D).
 *
 * <p>Board points are measured in squares: {@code x} runs from 0 at the left
 * edge to 8 at the right, and {@code depth} from 0 at the edge nearest the
 * viewer to 8 at the far edge.
 *
 * <p>The 3D look is ordinary perspective done by hand. Things further away
 * are drawn smaller: every length at depth {@code d} is multiplied by
 * {@code DISTANCE / (DISTANCE + d)}, as if the viewer's eye were
 * {@code DISTANCE} squares in front of the near edge. Because the far edge
 * shrinks towards the centre, the square board becomes a trapezium. This
 * class is plain arithmetic with no JavaFX in it, so it can be tested
 * without a window.
 */
final class Projection {

    /** How far the viewer's eye is from the near edge, in squares. Smaller is more dramatic. */
    private static final double DISTANCE = 20;

    /** How much the board is squashed top to bottom, as if seen from above and behind. */
    private static final double TILT = 0.92;

    /** The size of a square at the near edge, in pixels. */
    private static final double FLAT_UNIT = 60;
    private static final double PERSPECTIVE_UNIT = 62;

    private final boolean perspective;
    private final boolean flipped;
    private final double unit;
    private final double centerX;
    private final double baseY;

    /**
     * @param width       the width of the area to draw in, in pixels
     * @param height      the height of the area to draw in
     * @param perspective true for the pseudo-3D view, false for flat
     * @param flipped     true to look from Black's side
     */
    Projection(double width, double height, boolean perspective, boolean flipped) {
        this.perspective = perspective;
        this.flipped = flipped;
        this.unit = perspective ? PERSPECTIVE_UNIT : FLAT_UNIT;
        this.centerX = width / 2;
        // Centre the board vertically. In 3D it is nudged down, to leave
        // room above the far edge for the pieces standing on it.
        double drawnHeight = Board.SIZE * unit * (perspective ? TILT * scale(Board.SIZE) : 1);
        this.baseY = (height + drawnHeight) / 2 + (perspective ? unit * 0.3 : 0);
    }

    boolean isPerspective() {
        return perspective;
    }

    boolean isFlipped() {
        return flipped;
    }

    /** The size of a square at the near edge, in pixels. */
    double getUnit() {
        return unit;
    }

    /** How much smaller things are drawn at the given depth: 1 at the near edge, less further away. */
    double scale(double depth) {
        return perspective ? DISTANCE / (DISTANCE + depth) : 1;
    }

    /** How much a circle lying on the board is squashed top to bottom. */
    double flatness() {
        return perspective ? TILT * 0.8 : 1;
    }

    /** The screen x of a point on the board. */
    double screenX(double x, double depth) {
        return centerX + (x - Board.SIZE / 2.0) * unit * scale(depth);
    }

    /** The screen y of a point on the board. Screen y grows downwards, so far points have smaller y. */
    double screenY(double depth) {
        return baseY - depth * unit * (perspective ? TILT : 1) * scale(depth);
    }

    /** The column a square is drawn in, counting from the left of the screen. */
    int column(Position position) {
        return flipped ? Board.SIZE - 1 - position.getFile() : position.getFile();
    }

    /** The row a square is drawn in, counting from the edge nearest the viewer. */
    int row(Position position) {
        return flipped ? Board.SIZE - 1 - position.getRank() : position.getRank();
    }

    /** The square drawn at a column and row: the reverse of {@link #column} and {@link #row}. */
    Position positionAt(int column, int row) {
        return flipped
                ? new Position(Board.SIZE - 1 - column, Board.SIZE - 1 - row)
                : new Position(column, row);
    }

    /**
     * The outline of a rectangle on the board as it appears on screen, as
     * x, y pairs: near left, near right, far right, far left.
     */
    double[] outline(double left, double near, double right, double far) {
        return new double[] {
            screenX(left, near), screenY(near),
            screenX(right, near), screenY(near),
            screenX(right, far), screenY(far),
            screenX(left, far), screenY(far),
        };
    }
}
