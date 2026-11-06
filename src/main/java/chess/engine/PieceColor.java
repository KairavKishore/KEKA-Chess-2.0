package chess.engine;

/**
 * The two sides in a game of chess.
 *
 * <p>Called PieceColor rather than Color so that it does not clash with
 * {@code javafx.scene.paint.Color} in the GUI code.
 */
public enum PieceColor {
    WHITE,
    BLACK;

    /** The other side: WHITE gives BLACK, BLACK gives WHITE. */
    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
