package chess.ai;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.List;

/**
 * What the strategies that look ahead have in common: a search depth, an
 * {@link Evaluator} for the positions at the end of each line, the scoring
 * of checkmate and draws, and counters for reporting.
 *
 * <p>Scores are always from the point of view of the side to move in the
 * position being scored: positive is good for that side. Going one move
 * deeper swaps the point of view, so a child's score is negated. That form
 * of minimax is called negamax, and it lets one piece of code serve both
 * the maximising and the minimising player.
 *
 * <p>Depth is counted in half-moves ("plies"): depth 2 is my move and your
 * reply.
 *
 * <p>A strategy object is used by one thread at a time.
 */
public abstract class SearchStrategy implements MoveStrategy {

    /**
     * The score for giving checkmate. Far more than any material, so a mate
     * always beats winning pieces.
     */
    public static final int CHECKMATE = 1_000_000;

    /** More than any real score: the starting point when looking for a maximum. */
    protected static final int INFINITY = CHECKMATE + 1;

    /** After this many half-moves without a capture or pawn move the game is drawn. */
    private static final int HALFMOVES_FOR_DRAW = 100;

    private final int depth;
    private final Evaluator evaluator;

    private Move bestMove;
    private int lastScore;
    private long nodesSearched;

    /**
     * @param depth     how many half-moves to look ahead, at least 1
     * @param evaluator scores the positions where the search stops
     */
    protected SearchStrategy(int depth, Evaluator evaluator) {
        if (depth < 1) {
            throw new IllegalArgumentException("Search depth must be at least 1: " + depth);
        }
        this.depth = depth;
        this.evaluator = evaluator;
    }

    /** How many half-moves this strategy looks ahead. */
    public int getDepth() {
        return depth;
    }

    @Override
    public final Move chooseMove(Board board, PieceColor sideToMove) {
        bestMove = null;
        nodesSearched = 0;
        lastScore = searchFromRoot(board, sideToMove);
        if (bestMove == null) {
            throw new IllegalStateException(sideToMove + " has no legal moves");
        }
        return bestMove;
    }

    /**
     * Searches to the full depth, calling {@link #foundBetterRootMove} for
     * each first move that beats the ones before it.
     *
     * @return the score of the best move, for the side to move
     */
    protected abstract int searchFromRoot(Board board, PieceColor sideToMove);

    /** Records the best first move found so far. */
    protected void foundBetterRootMove(Move move) {
        bestMove = move;
    }

    /**
     * The score of the move last chosen, for the side that chose it, in
     * centipawns. {@code CHECKMATE - n} means it forces mate with the
     * {@code n}th half-move from now.
     */
    public int getLastScore() {
        return lastScore;
    }

    /** How many positions the last search visited. */
    @Override
    public long getNodesSearched() {
        return nodesSearched;
    }

    /** Counts one position visited. Called once at the top of every search call. */
    protected void countNode() {
        nodesSearched++;
    }

    /**
     * True if the position is drawn whatever happens next. Draws by
     * repetition need the history of the game, which a search working from
     * a bare board does not have.
     */
    protected boolean isDrawn(Board board) {
        return board.getHalfmoveClock() >= HALFMOVES_FOR_DRAW || board.hasInsufficientMaterial();
    }

    /**
     * The score of a position where the search stops looking ahead.
     *
     * @param ply how many half-moves this position is from the root
     */
    protected int scoreLeaf(Board board, PieceColor sideToMove, int ply) {
        // The evaluator cannot see checkmate. Finding out costs a move
        // generation, so it is only done when the side to move is in check.
        if (board.isInCheck(sideToMove) && !hasLegalMove(board, sideToMove)) {
            return checkmatedScore(ply);
        }
        int whiteScore = evaluator.evaluate(board);
        return sideToMove == PieceColor.WHITE ? whiteScore : -whiteScore;
    }

    /** The score of a position where the side to move has no legal move: checkmate or stalemate. */
    protected int scoreNoMoves(Board board, PieceColor sideToMove, int ply) {
        return board.isInCheck(sideToMove) ? checkmatedScore(ply) : 0;
    }

    /**
     * Being mated is the worst score there is, but being mated later is a
     * little less bad. Seen from the other side, this makes the search
     * choose the quickest mate instead of putting it off for ever.
     */
    private static int checkmatedScore(int ply) {
        return -(CHECKMATE - ply);
    }

    private boolean hasLegalMove(Board board, PieceColor sideToMove) {
        for (Move move : board.pseudoLegalMoves(sideToMove)) {
            if (playIfLegal(board, move, sideToMove)) {
                board.undoMove(move);
                return true;
            }
        }
        return false;
    }

    /**
     * The moves to try, some of which may leave the mover's own king in
     * check. The search weeds those out with {@link #playIfLegal} as it
     * goes, which is much cheaper than asking the board for fully legal
     * moves at every position.
     */
    protected List<Move> candidateMoves(Board board, PieceColor sideToMove) {
        return board.pseudoLegalMoves(sideToMove);
    }

    /**
     * Plays a candidate move on the board, unless it would leave the mover's
     * king in check.
     *
     * @return true if the move was played, in which case the caller must
     *         {@link Board#undoMove undo} it; false if it was illegal and the
     *         board is unchanged
     */
    protected boolean playIfLegal(Board board, Move move, PieceColor mover) {
        board.makeMove(move);
        if (board.isInCheck(mover)) {
            board.undoMove(move);
            return false;
        }
        return true;
    }
}
