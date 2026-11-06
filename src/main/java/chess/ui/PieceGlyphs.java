package chess.ui;

import chess.engine.PieceType;

/**
 * The solid chess symbols in Unicode. Both sides are drawn with the same
 * shapes and differ in colour, which looks more even than mixing them with
 * the outlined set.
 */
final class PieceGlyphs {

    /** Indexed by {@code PieceType.ordinal()}: king, queen, rook, bishop, knight, pawn. */
    private static final String[] GLYPHS = {"♚", "♛", "♜", "♝", "♞", "♟"};

    private PieceGlyphs() {
    }

    /** The symbol for a kind of piece. */
    static String of(PieceType type) {
        return GLYPHS[type.ordinal()];
    }
}
