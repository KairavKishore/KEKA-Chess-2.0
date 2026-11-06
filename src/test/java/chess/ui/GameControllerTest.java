package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.ai.RandomStrategy;
import chess.engine.Fen;
import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.Position;
import chess.player.AIPlayer;
import chess.player.Player;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class GameControllerTest {

    /** A player that waits to be told its move by the test, like a person at the board. */
    private static final class ManualPlayer implements Player {
        private final boolean human;
        Consumer<Move> reply;
        int requests;

        ManualPlayer() {
            this(true);
        }

        /** With {@code human} false, this stands in for a computer whose moves the test dictates. */
        ManualPlayer(boolean human) {
            this.human = human;
        }

        @Override
        public String getName() {
            return "Manual";
        }

        @Override
        public boolean isHuman() {
            return human;
        }

        @Override
        public void requestMove(Game game, Consumer<Move> reply) {
            this.reply = reply;
            requests++;
        }

        void play(Game game, String from, String to) {
            for (Move move : game.legalMovesFrom(Position.of(from))) {
                if (move.getTo().equals(Position.of(to))) {
                    reply.accept(move);
                    return;
                }
            }
            throw new AssertionError("No legal move from " + from + " to " + to);
        }
    }

    private static Player computer(long seed) {
        return new AIPlayer(new RandomStrategy(new Random(seed)), Runnable::run, Runnable::run, 0);
    }

    @Test
    void asksNobodyUntilStarted() {
        ManualPlayer white = new ManualPlayer();
        new GameController(new Game(), white, new ManualPlayer());
        assertEquals(0, white.requests);
    }

    @Test
    void asksEachPlayerInTurnAndPlaysTheirMoves() {
        Game game = new Game();
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();
        GameController controller = new GameController(game, white, black);

        controller.start();
        assertSame(white, controller.getPlayerToMove());
        assertEquals(1, white.requests);
        assertEquals(0, black.requests);

        white.play(game, "e2", "e4");
        assertEquals(PieceColor.BLACK, game.getTurn());
        assertSame(black, controller.getPlayerToMove());
        assertEquals(1, black.requests);

        black.play(game, "e7", "e5");
        assertEquals(2, white.requests);
        assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2", game.toFen());
    }

    @Test
    void startsWithBlackWhenItIsBlacksTurn() {
        Game game = Game.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();

        new GameController(game, white, black).start();

        assertEquals(0, white.requests);
        assertEquals(1, black.requests);
    }

    @Test
    void stopsAskingWhenTheGameIsOver() {
        Game game = new Game();
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();
        new GameController(game, white, black).start();

        white.play(game, "f2", "f3");
        black.play(game, "e7", "e5");
        white.play(game, "g2", "g4");
        black.play(game, "d8", "h4");  // checkmate

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertEquals(2, white.requests);  // not asked a third time
    }

    @Test
    void asksNobodyIfTheGameIsAlreadyOver() {
        ManualPlayer black = new ManualPlayer();
        new GameController(Game.fromFen("R5k1/5ppp/8/8/8/8/8/4K3 b - - 0 1"), new ManualPlayer(), black).start();
        assertEquals(0, black.requests);
        assertNull(black.reply);
    }

    @Test
    void replyThatArrivesAfterStoppingIsIgnored() {
        Game game = new Game();
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();
        GameController controller = new GameController(game, white, black);
        controller.start();

        controller.stop();
        white.play(game, "e2", "e4");  // like a computer finishing its thinking too late

        assertEquals(Fen.STARTING_POSITION, game.toFen());
        assertEquals(0, black.requests);
    }

    @Test
    void sameReplyDeliveredTwiceIsOnlyPlayedOnce() {
        Game game = new Game();
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();
        new GameController(game, white, black).start();
        Consumer<Move> firstReply = white.reply;
        Move e4 = game.legalMovesFrom(Position.of("e2")).get(1);

        firstReply.accept(e4);
        firstReply.accept(e4);  // would be an illegal move by now, if it were not ignored

        assertEquals(1, game.getMoveHistory().size());
    }

    @Test
    void personAgainstComputerGetsAnAnswerToEachMove() {
        Game game = new Game();
        ManualPlayer person = new ManualPlayer();
        new GameController(game, person, computer(3)).start();

        person.play(game, "e2", "e4");

        assertEquals(2, game.getMoveHistory().size());  // the computer has replied
        assertEquals(PieceColor.WHITE, game.getTurn());
        assertEquals(2, person.requests);
    }

    // ------------------------------------------------------- undo and redo

    @Test
    void nothingToUndoOrRedoAtTheStart() {
        GameController controller = new GameController(new Game(), new ManualPlayer(), new ManualPlayer());
        controller.start();
        assertFalse(controller.canUndo());
        assertFalse(controller.canRedo());
        assertThrows(IllegalStateException.class, controller::undo);
        assertThrows(IllegalStateException.class, controller::redo);
    }

    @Test
    void betweenTwoPeopleUndoTakesBackOneMoveAndAsksThatPlayerAgain() {
        Game game = new Game();
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();
        GameController controller = new GameController(game, white, black);
        controller.start();
        white.play(game, "e2", "e4");
        black.play(game, "e7", "e5");

        controller.undo();

        assertEquals(1, game.getMoveHistory().size());
        assertSame(black, controller.getPlayerToMove());
        black.play(game, "c7", "c5");  // the fresh request works
        assertEquals("rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2", game.toFen());
        assertFalse(controller.canRedo());  // a new move was played
    }

    @Test
    void againstTheComputerUndoTakesBackItsReplyAndYourMove() {
        Game game = new Game();
        ManualPlayer person = new ManualPlayer();
        ManualPlayer machine = new ManualPlayer(false);
        GameController controller = new GameController(game, person, machine);
        controller.start();
        person.play(game, "e2", "e4");
        machine.play(game, "e7", "e5");
        assertTrue(controller.canUndo());

        controller.undo();

        assertEquals(Fen.STARTING_POSITION, game.toFen());
        assertSame(person, controller.getPlayerToMove());
        assertFalse(controller.canUndo());
        assertTrue(controller.canRedo());
    }

    @Test
    void redoAgainstTheComputerReplaysYourMoveAndItsReply() {
        Game game = new Game();
        ManualPlayer person = new ManualPlayer();
        ManualPlayer machine = new ManualPlayer(false);
        GameController controller = new GameController(game, person, machine);
        controller.start();
        person.play(game, "e2", "e4");
        machine.play(game, "e7", "e5");
        String afterBoth = game.toFen();
        controller.undo();
        int machineRequests = machine.requests;

        controller.redo();

        assertEquals(afterBoth, game.toFen());
        assertSame(person, controller.getPlayerToMove());
        assertEquals(machineRequests, machine.requests);  // the computer was not asked to think again
        assertFalse(controller.canRedo());
    }

    @Test
    void undoWhileTheComputerIsThinkingDropsItsAnswer() {
        Game game = new Game();
        ManualPlayer person = new ManualPlayer();
        ManualPlayer machine = new ManualPlayer(false);
        GameController controller = new GameController(game, person, machine);
        controller.start();
        person.play(game, "e2", "e4");
        Consumer<Move> lateAnswer = machine.reply;  // the computer is now "thinking"

        controller.undo();
        assertEquals(Fen.STARTING_POSITION, game.toFen());
        person.play(game, "d2", "d4");

        // The answer to the abandoned position arrives. e7e5 happens to be
        // legal here too, which is exactly why it must be ignored.
        Game scratch = Game.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        lateAnswer.accept(scratch.legalMovesFrom(Position.of("e7")).get(1));

        assertEquals(1, game.getMoveHistory().size());
        assertEquals("d2d4", game.getLastMove().toString());
    }

    @Test
    void redoThatEndsOnTheComputersTurnAsksItToMove() {
        Game game = new Game();
        ManualPlayer person = new ManualPlayer();
        ManualPlayer machine = new ManualPlayer(false);
        GameController controller = new GameController(game, person, machine);
        controller.start();
        person.play(game, "e2", "e4");   // the computer has not answered yet
        controller.undo();
        int machineRequests = machine.requests;

        controller.redo();

        assertEquals(1, game.getMoveHistory().size());
        assertSame(machine, controller.getPlayerToMove());
        assertEquals(machineRequests + 1, machine.requests);
    }

    @Test
    void computersOpeningMoveAloneCannotBeUndone() {
        Game game = new Game();
        ManualPlayer machine = new ManualPlayer(false);
        ManualPlayer person = new ManualPlayer();
        GameController controller = new GameController(game, machine, person);
        controller.start();
        machine.play(game, "e2", "e4");

        assertFalse(controller.canUndo());  // it would only move again

        person.play(game, "e7", "e5");
        machine.play(game, "g1", "f3");
        assertTrue(controller.canUndo());
        controller.undo();
        assertEquals(1, game.getMoveHistory().size());  // back to after the computer's first move
        assertSame(person, controller.getPlayerToMove());
    }

    @Test
    void finishedGameCanBeUndoneAndPlayedOn() {
        Game game = new Game();
        ManualPlayer white = new ManualPlayer();
        ManualPlayer black = new ManualPlayer();
        GameController controller = new GameController(game, white, black);
        controller.start();
        white.play(game, "f2", "f3");
        black.play(game, "e7", "e5");
        white.play(game, "g2", "g4");
        black.play(game, "d8", "h4");
        assertEquals(GameStatus.CHECKMATE, game.getStatus());

        controller.undo();
        black.play(game, "d7", "d6");

        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertSame(white, controller.getPlayerToMove());
    }

    @Test
    void gameBetweenTwoComputersCannotBeUndone() {
        Game game = new Game();
        ManualPlayer first = new ManualPlayer(false);
        ManualPlayer second = new ManualPlayer(false);
        GameController controller = new GameController(game, first, second);
        controller.start();
        first.play(game, "e2", "e4");
        second.play(game, "e7", "e5");

        assertFalse(controller.canUndo());
        assertFalse(controller.canRedo());
    }

    @Test
    void twoComputersPlayAWholeGameToItsEnd() {
        for (long seed = 1; seed <= 5; seed++) {
            Game game = new Game();
            new GameController(game, computer(seed), computer(seed + 100)).start();

            assertTrue(game.getStatus().isGameOver(), "seed " + seed + " stopped at " + game.toFen());
            assertTrue(game.legalMoves().isEmpty());
        }
    }
}
