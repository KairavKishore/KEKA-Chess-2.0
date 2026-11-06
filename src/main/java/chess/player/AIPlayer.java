package chess.player;

import chess.ai.MoveStrategy;
import chess.engine.Board;
import chess.engine.Game;
import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A computer player. How it chooses its moves is up to the
 * {@link MoveStrategy} it is given; this class only looks after the
 * threading.
 *
 * <p>Thinking can take seconds, and a window freezes if its thread is kept
 * busy for that long. So the strategy runs on a background thread and the
 * answer is handed back on the thread the game lives on:
 * <ol>
 *   <li>{@link #requestMove} copies the board, on the caller's thread;</li>
 *   <li>the {@code worker} runs the strategy on that copy, so the search
 *       never reads a board that is being drawn or changed;</li>
 *   <li>the {@code replyThread} delivers the move.</li>
 * </ol>
 *
 * <p>Both threads are passed in as {@link Executor}s. The application gives
 * a background thread and JavaFX's {@code Platform::runLater}; tests give
 * {@code Runnable::run} to do everything at once on one thread.
 */
public class AIPlayer implements Player {

    private final MoveStrategy strategy;
    private final Executor worker;
    private final Executor replyThread;
    private final long minimumThinkingMillis;

    /**
     * @param strategy              how to choose a move
     * @param worker                runs the strategy
     * @param replyThread           delivers the chosen move
     * @param minimumThinkingMillis the least time to take over a move, so
     *                              that a quick strategy's reply does not land
     *                              in the same instant as the opponent's move
     */
    public AIPlayer(MoveStrategy strategy, Executor worker, Executor replyThread, long minimumThinkingMillis) {
        this.strategy = strategy;
        this.worker = worker;
        this.replyThread = replyThread;
        this.minimumThinkingMillis = minimumThinkingMillis;
    }

    @Override
    public String getName() {
        return "Computer (" + strategy.getName() + ")";
    }

    @Override
    public boolean isHuman() {
        return false;
    }

    @Override
    public void requestMove(Game game, Consumer<Move> reply) {
        Board board = new Board(game.getBoard());
        PieceColor sideToMove = game.getTurn();

        worker.execute(() -> {
            long started = System.currentTimeMillis();
            Move move = strategy.chooseMove(board, sideToMove);
            pauseUntil(started + minimumThinkingMillis);
            replyThread.execute(() -> reply.accept(move));
        });
    }

    private static void pauseUntil(long time) {
        long remaining = time - System.currentTimeMillis();
        if (remaining <= 0) {
            return;
        }
        try {
            Thread.sleep(remaining);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // shutting down: answer straight away
        }
    }
}
