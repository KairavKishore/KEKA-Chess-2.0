package chess.ai;

import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.List;
import java.util.Random;

/**
 * Picks any legal move, each with the same chance. It plays terribly, but it
 * is the simplest possible strategy and a baseline to measure the others
 * against.
 */
public class RandomStrategy implements MoveStrategy {

    private final Random random;

    public RandomStrategy() {
        this(new Random());
    }

    /**
     * @param random the source of randomness. A {@code new Random(seed)} makes
     *               the choices repeatable, which tests need.
     */
    public RandomStrategy(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(Board board, PieceColor sideToMove) {
        List<Move> moves = board.legalMoves(sideToMove);
        if (moves.isEmpty()) {
            throw new IllegalStateException(sideToMove + " has no legal moves");
        }
        return moves.get(random.nextInt(moves.size()));
    }

    @Override
    public String getName() {
        return "random";
    }
}
