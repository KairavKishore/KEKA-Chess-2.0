package chess.engine;

/** Where a game stands, from the point of view of the side whose turn it is. */
public enum GameStatus {
    /** The side to move has legal moves and is not in check. */
    IN_PROGRESS,
    /** The side to move is in check but can get out of it. */
    CHECK,
    /** The side to move is in check and has no legal move: it has lost. */
    CHECKMATE,
    /** The side to move is not in check but has no legal move: a draw. */
    STALEMATE,
    /** Fifty moves by each side without a capture or a pawn move: a draw. */
    DRAW_BY_FIFTY_MOVES,
    /** The same position has come up three times: a draw. */
    DRAW_BY_REPETITION,
    /** Neither side has enough pieces left to give checkmate: a draw. */
    DRAW_BY_INSUFFICIENT_MATERIAL;

    /** True once no more moves can be played. */
    public boolean isGameOver() {
        return this != IN_PROGRESS && this != CHECK;
    }

    /** True if the game is over with no winner. */
    public boolean isDraw() {
        return isGameOver() && this != CHECKMATE;
    }
}
