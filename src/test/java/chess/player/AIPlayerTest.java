package chess.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.ai.MoveStrategy;
import chess.ai.RandomStrategy;
import chess.engine.Board;
import chess.engine.Fen;
import chess.engine.Game;
import chess.engine.Move;
import chess.engine.PieceColor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AIPlayerTest {

    /** Runs a task at once, on the calling thread. */
    private static final Executor DIRECT = Runnable::run;

    /** A strategy that records what it was given and plays the first legal move. */
    private static final class RecordingStrategy implements MoveStrategy {
        Board board;
        PieceColor sideToMove;
        Thread thread;

        @Override
        public Move chooseMove(Board board, PieceColor sideToMove) {
            this.board = board;
            this.sideToMove = sideToMove;
            this.thread = Thread.currentThread();
            Move move = board.legalMoves(sideToMove).get(0);
            board.makeMove(move);  // a strategy may do what it likes with its board
            return move;
        }

        @Override
        public String getName() {
            return "recording";
        }
    }

    @Test
    void repliesWithTheStrategysMove() {
        Game game = new Game();
        List<Move> replies = new ArrayList<>();
        Player player = new AIPlayer(new RandomStrategy(), DIRECT, DIRECT, 0);

        player.requestMove(game, replies::add);

        assertEquals(1, replies.size());
        assertTrue(game.legalMoves().contains(replies.get(0)));
    }

    @Test
    void doesNotPlayTheMoveItself() {
        Game game = new Game();
        new AIPlayer(new RandomStrategy(), DIRECT, DIRECT, 0).requestMove(game, move -> { });
        assertEquals(Fen.STARTING_POSITION, game.toFen());
        assertNull(game.getLastMove());
    }

    @Test
    void givesTheStrategyItsOwnCopyOfTheBoardAndTheSideToMove() {
        Game game = Game.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        RecordingStrategy strategy = new RecordingStrategy();

        new AIPlayer(strategy, DIRECT, DIRECT, 0).requestMove(game, move -> { });

        assertNotSame(game.getBoard(), strategy.board);
        assertEquals(PieceColor.BLACK, strategy.sideToMove);
        // The strategy played a move on its copy; the game's board is untouched.
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", game.toFen());
    }

    @Test
    void isNotHumanAndIsNamedAfterItsStrategy() {
        Player player = new AIPlayer(new RandomStrategy(), DIRECT, DIRECT, 0);
        assertFalse(player.isHuman());
        assertEquals("Computer (random)", player.getName());
    }

    @Test
    void thinksOnTheWorkerAndRepliesOnTheReplyThread() throws InterruptedException {
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "test-worker"));
        ExecutorService replier = Executors.newSingleThreadExecutor(r -> new Thread(r, "test-replier"));
        try {
            RecordingStrategy strategy = new RecordingStrategy();
            AtomicReference<Thread> replyThread = new AtomicReference<>();
            AtomicReference<Move> reply = new AtomicReference<>();
            CountDownLatch replied = new CountDownLatch(1);

            new AIPlayer(strategy, worker, replier, 0).requestMove(new Game(), move -> {
                replyThread.set(Thread.currentThread());
                reply.set(move);
                replied.countDown();
            });

            assertTrue(replied.await(5, TimeUnit.SECONDS), "no reply within five seconds");
            assertEquals("test-worker", strategy.thread.getName());
            assertEquals("test-replier", replyThread.get().getName());
            assertNotEquals(Thread.currentThread(), strategy.thread);
            assertTrue(new Game().legalMoves().contains(reply.get()));
        } finally {
            worker.shutdownNow();
            replier.shutdownNow();
        }
    }

    @Test
    void requestMoveReturnsBeforeTheStrategyHasFinished() throws InterruptedException {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch mayFinish = new CountDownLatch(1);
            CountDownLatch replied = new CountDownLatch(1);
            MoveStrategy slow = new MoveStrategy() {
                @Override
                public Move chooseMove(Board board, PieceColor sideToMove) {
                    try {
                        mayFinish.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return board.legalMoves(sideToMove).get(0);
                }

                @Override
                public String getName() {
                    return "slow";
                }
            };

            new AIPlayer(slow, worker, DIRECT, 0).requestMove(new Game(), move -> replied.countDown());

            // We got here while the strategy is still blocked: the caller was not held up.
            assertEquals(1, replied.getCount());
            mayFinish.countDown();
            assertTrue(replied.await(5, TimeUnit.SECONDS));
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    void takesAtLeastTheMinimumThinkingTime() {
        long started = System.currentTimeMillis();
        new AIPlayer(new RandomStrategy(), DIRECT, DIRECT, 150).requestMove(new Game(), move -> { });
        assertTrue(System.currentTimeMillis() - started >= 150);
    }
}
