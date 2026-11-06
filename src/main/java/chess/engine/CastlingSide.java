package chess.engine;

/** The two directions a king can castle in. */
public enum CastlingSide {
    /** Towards the h-file: the king ends on the g-file. Written O-O. */
    KING_SIDE,
    /** Towards the a-file: the king ends on the c-file. Written O-O-O. */
    QUEEN_SIDE
}
