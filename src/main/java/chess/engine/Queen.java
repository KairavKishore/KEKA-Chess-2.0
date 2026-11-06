package chess.engine;

/** Slides along ranks, files and diagonals. */
public final class Queen extends SlidingPiece {

    public Queen(PieceColor color) {
        super(color, PieceType.QUEEN, ALL_DIRECTIONS);
    }
}
