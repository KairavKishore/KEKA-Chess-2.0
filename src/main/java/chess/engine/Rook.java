package chess.engine;

/** Slides along ranks and files. */
public final class Rook extends SlidingPiece {

    public Rook(PieceColor color) {
        super(color, PieceType.ROOK, ORTHOGONAL);
    }
}
