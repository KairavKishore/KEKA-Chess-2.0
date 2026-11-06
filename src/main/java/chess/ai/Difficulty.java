package chess.ai;

/**
 * How strong the computer opponent is. Apart from the weakest level, the
 * difficulty is simply how many half-moves ahead it searches.
 */
public enum Difficulty {
    /** Plays any legal move. */
    RANDOM("Random moves", 0),
    /** Sees its own move and the reply: avoids leaving pieces to be taken. */
    EASY("Easy", 2),
    /** Sees two moves by each side: wins material with simple tactics. */
    MEDIUM("Medium", 4),
    /** One half-move deeper again, which takes several times as long. */
    HARD("Hard", 5);

    private final String label;
    private final int depth;

    Difficulty(String label, int depth) {
        this.label = label;
        this.depth = depth;
    }

    /** The search depth in half-moves, or 0 for the level that does not search. */
    public int getDepth() {
        return depth;
    }

    /** Builds the strategy that plays at this level. Each call gives a new one. */
    public MoveStrategy createStrategy() {
        if (this == RANDOM) {
            return new RandomStrategy();
        }
        return new AlphaBetaStrategy(depth, new PieceSquareEvaluator());
    }

    /** The name to show in a list of levels. */
    @Override
    public String toString() {
        return label;
    }
}
