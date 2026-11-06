package chess.engine;

/** Slides along diagonals. */
public final class Bishop extends SlidingPiece {

    public Bishop(PieceColor color) {
        super(color, PieceType.BISHOP, DIAGONAL);
    }
}
