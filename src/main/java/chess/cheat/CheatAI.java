package chess.cheat;

import chess.ai.AlphaBetaStrategy;
import chess.ai.PieceSquareEvaluator;
import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The computer's side of cheating chess. It plays fair in one respect: it
 * only ever looks at what its side is allowed to see, the position as it
 * appears and the clock, never at the other side's secrets.
 */
public class CheatAI {

    /** How many half-moves ahead it searches the position as it appears. */
    private static final int DEPTH = 3;

    /** How often it notices the other side fiddling the clock. */
    private static final double NOTICES_CLOCK = 0.5;

    /**
     * How often it spots a move that the piece's looks do not allow. Well
     * under half: if it caught every one, a transformed piece could never
     * use its power, and there would be no point in cheating.
     */
    private static final double SPOTS_IMPOSSIBLE_MOVE = 0.3;

    private final Random random;

    public CheatAI() {
        this(new Random());
    }

    /**
     * @param random the source of randomness; a seeded one makes its play repeatable
     */
    public CheatAI(Random random) {
        this.random = random;
    }

    /**
     * Spends a side's cheat points on a few random transformations, then
     * declares it ready. It changes between two and four pieces, so that
     * its army is neither honest nor all queens.
     */
    public void setUp(CheatGame game, PieceColor color) {
        List<Position> pieces = game.transformablePieces(color);
        PieceType[] identities = CheatGame.identities();
        int wanted = 2 + random.nextInt(3);
        int made = 0;
        // Bounded, in case random picks keep choosing what it cannot afford or what a piece already is.
        for (int attempt = 0; attempt < 200 && made < wanted; attempt++) {
            Position piece = pieces.get(random.nextInt(pieces.size()));
            PieceType identity = identities[random.nextInt(identities.length)];
            if (game.secretAt(piece) == null && game.canTransform(color, piece, identity)) {
                game.transform(color, piece, identity);
                made++;
            }
        }
        game.finishSetup(color);
    }

    /**
     * Whether to challenge the move just played. The computer never
     * guesses: it only challenges something it could have seen, which is a
     * move the piece's looks do not allow, or the clock being fiddled. But
     * it is not always paying attention, and misses most of the first and
     * about half of the second.
     */
    public boolean shouldAccuse(CheatGame game) {
        if (!game.canAccuse()) {
            return false;
        }
        return !game.lastMoveLookedLegal() && random.nextDouble() < SPOTS_IMPOSSIBLE_MOVE
                || game.lastMoverFiddledClock() && random.nextDouble() < NOTICES_CLOCK;
    }

    /**
     * Chooses a move.
     *
     * @param belief   the position as this side sees it, from {@link CheatGame#beliefBoard}
     * @param ownMoves the moves this side really has, from {@link CheatGame#moves()}
     * @return one of {@code ownMoves}
     */
    public Move chooseMove(Board belief, List<Move> ownMoves, PieceColor color) {
        // If the king can be taken, take it: the game is won on the spot.
        Position king = belief.findKing(color.opposite());
        for (Move move : ownMoves) {
            if (move.getTo().equals(king)) {
                return move;
            }
        }
        // Otherwise play ordinary good chess against the position as it appears.
        try {
            Move planned = new AlphaBetaStrategy(DEPTH, new PieceSquareEvaluator())
                    .chooseMove(new Board(belief), color);
            for (Move move : ownMoves) {
                if (move.getFrom().equals(planned.getFrom()) && move.getTo().equals(planned.getTo())
                        && Objects.equals(move.getPromotion(), planned.getPromotion())) {
                    return move;
                }
            }
        } catch (IllegalStateException e) {
            // By the ordinary rules this side has no legal move (it looks
            // checkmated or stalemated). Here it can still move, so pick anything.
        }
        // The plan was ruled out by a crater, lava, a storm or a debuff, which
        // the ordinary search knows nothing about.
        return ownMoves.get(random.nextInt(ownMoves.size()));
    }
}
