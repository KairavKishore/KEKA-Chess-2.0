package chess.cheat;

/** The two kinds of cheating chess, which differ in how much each side may get away with. */
public enum CheatLevel {
    /** Twenty cheat points to spend on transforming non-pawn pieces, three switch tokens. */
    STANDARD("Cheating", 20, 3, false),
    /**
     * Pawns can be transformed too, there are five switch tokens and extra
     * rewards and penalties, and the board itself turns on the players.
     */
    ANGRY("Angry chaotic", 20, 5, true);

    /** How many challenges each side may make in an angry chaotic game. */
    private static final int ANGRY_CHALLENGE_LIMIT = 3;

    private final String label;
    private final int cheatPoints;
    private final int switchTokens;
    private final boolean chaotic;

    CheatLevel(String label, int cheatPoints, int switchTokens, boolean chaotic) {
        this.label = label;
        this.cheatPoints = cheatPoints;
        this.switchTokens = switchTokens;
        this.chaotic = chaotic;
    }

    /**
     * How many cheat points each side has to spend on secret transformations
     * before the game. Making a piece stronger costs points and making it
     * weaker gives them back.
     */
    public int getCheatPoints() {
        return cheatPoints;
    }

    /** How many times each side may re-transform a piece as a reward during the game. */
    public int getSwitchTokens() {
        return switchTokens;
    }

    /** True if pawns may be transformed as well as pieces. */
    public boolean allowsPawns() {
        return chaotic;
    }

    /** True for the mode with debuffs, Disrupt rewards, and rockets, storms and lava. */
    public boolean isChaotic() {
        return chaotic;
    }

    /** How many challenges each side may make in a game, or -1 for no limit. */
    public int getChallengeLimit() {
        return chaotic ? ANGRY_CHALLENGE_LIMIT : -1;
    }

    /** The name to show in a list of modes. */
    @Override
    public String toString() {
        return label;
    }
}
