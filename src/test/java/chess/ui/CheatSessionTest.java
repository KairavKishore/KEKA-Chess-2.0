package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.cheat.CheatAI;
import chess.cheat.CheatGame;
import chess.cheat.CheatGame.ChaosNotice;
import chess.cheat.CheatGame.Phase;
import chess.cheat.CheatGame.Reward;
import chess.cheat.CheatGame.SquareState;
import chess.cheat.CheatLevel;
import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;
import chess.ui.JockyLines.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Plays cheating chess through a stand-in board and window, with the computer answering at once on the same thread. */
class CheatSessionTest {

    /** Remembers what the session asked of the board, and lets the test click and move. */
    private static final class FakeBoard implements BoardInput {
        Consumer<Move> moveReply;
        Consumer<Position> picker;
        int refreshes;

        @Override
        public void requestMove(Consumer<Move> reply) {
            moveReply = reply;
        }

        @Override
        public void pickSquares(Consumer<Position> picker) {
            this.picker = picker;
        }

        @Override
        public void highlight(Position position) {
        }

        @Override
        public void refresh() {
            refreshes++;
        }

        void click(String square) {
            picker.accept(Position.of(square));
        }
    }

    /** Answers the session's questions as the test tells it to, and records everything it is told. */
    private static final class FakeWindow implements CheatSession.Host {
        PieceType identity = PieceType.QUEEN;
        Reward reward = Reward.EXPOSE;
        boolean clock = true;
        final List<String> statuses = new ArrayList<>();
        final List<Event> cheers = new ArrayList<>();
        final List<ChaosNotice> announcements = new ArrayList<>();
        final long[] timeAdded = new long[2];
        int clockSwaps;
        int hacks;
        List<Reward> offered;

        @Override
        public PieceType chooseIdentity(Position square) {
            return identity;
        }

        @Override
        public Reward chooseReward(List<Reward> options) {
            offered = options;
            return reward;
        }

        @Override
        public void status(String text) {
            statuses.add(text);
        }

        @Override
        public void jocky(Event event) {
            cheers.add(event);
        }

        @Override
        public void announce(ChaosNotice notice) {
            announcements.add(notice);
        }

        @Override
        public boolean hasClock() {
            return clock;
        }

        @Override
        public void addTime(PieceColor color, long millis) {
            timeAdded[color.ordinal()] += millis;
        }

        @Override
        public void swapClocks() {
            clockSwaps++;
        }

        @Override
        public void hacked() {
            hacks++;
        }

        String status() {
            return statuses.get(statuses.size() - 1);
        }
    }

    /**
     * A computer that transforms nothing and plays from a script, falling
     * back on its first move, so that tests can predict it.
     */
    private static class ScriptedComputer extends CheatAI {
        private final String[] script;
        private int next;

        ScriptedComputer(String... script) {
            // It never misses anything it could have seen.
            super(new Random(1) {
                @Override
                public double nextDouble() {
                    return 0.0;
                }
            });
            this.script = script;
        }

        @Override
        public void setUp(CheatGame game, PieceColor color) {
            game.finishSetup(color);
        }

        @Override
        public Move chooseMove(Board belief, List<Move> ownMoves, PieceColor color) {
            if (next < script.length) {
                String wanted = script[next++];
                for (Move move : ownMoves) {
                    if (move.toString().equals(wanted)) {
                        return move;
                    }
                }
                throw new AssertionError("The computer has no move " + wanted);
            }
            return ownMoves.get(0);
        }
    }

    /**
     * A source of randomness under which the computer never fiddles the
     * clock, no disaster strikes, and wherever one of several things is
     * picked it is the first.
     */
    private static Random calm() {
        return new Random(1) {
            @Override
            public double nextDouble() {
                return 1.0;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private CheatGame game = new CheatGame(CheatLevel.STANDARD, calm());
    private final FakeBoard board = new FakeBoard();
    private final FakeWindow window = new FakeWindow();

    private CheatSession session(CheatAI computer) {
        return new CheatSession(game, computer, board, Runnable::run, Runnable::run, 0, window, calm());
    }

    private CheatSession started(CheatAI computer) {
        CheatSession session = session(computer);
        session.start();
        session.finishSwitching();
        return session;
    }

    private void move(String from, String to) {
        for (Move move : game.movesFrom(Position.of(from))) {
            if (move.getTo().equals(Position.of(to))) {
                board.moveReply.accept(move);
                return;
            }
        }
        throw new AssertionError("No move from " + from + " to " + to);
    }

    // ---------------------------------------------------------------- setup

    @Test
    void startsWithTransformationsAndTheComputerDoesItsOwnOutOfSight() {
        CheatSession session = session(new CheatAI(new Random(3)));
        session.start();

        assertTrue(session.isSettingUp());
        assertTrue(game.getCheatPoints(PieceColor.BLACK) >= 0);
        assertNotNull(board.picker);
        assertNull(board.moveReply);
        assertEquals("Click a piece to secretly transform it. 20 cheat points left. Press Start when ready.",
                window.status());
        assertFalse(session.canAccuse());
        assertFalse(session.canFiddleClock());
    }

    @Test
    void clickingAPieceTransformsItIntoWhatThePlayerChooses() {
        CheatSession session = session(new ScriptedComputer());
        session.start();

        window.identity = PieceType.QUEEN;
        board.click("b1");

        assertEquals(PieceType.QUEEN, game.secretAt(Position.of("b1")));
        assertEquals(PieceType.KNIGHT, game.looksPiece(Position.of("b1")).getType());
        assertEquals(14, game.getCheatPoints(PieceColor.WHITE));
        assertTrue(window.status().contains("14 cheat points left"), window.status());
        assertEquals(List.of(Event.SWITCH), window.cheers);
        assertTrue(session.isSettingUp());
    }

    @Test
    void clicksThatCannotBeTransformedOrAreCancelledChangeNothing() {
        session(new ScriptedComputer()).start();

        board.click("e4");   // empty
        board.click("e1");   // the king
        board.click("a2");   // a pawn, in standard mode
        board.click("d8");   // the computer's
        window.identity = null;
        board.click("b1");   // the dialog is cancelled
        window.identity = PieceType.KNIGHT;
        board.click("b1");   // "transformed" into what it already is

        assertEquals(20, game.getCheatPoints(PieceColor.WHITE));
        assertNull(game.secretAt(Position.of("b1")));
    }

    @Test
    void demotingAPieceGivesPointsBackAndWhatCannotBeAffordedIsRefused() {
        CheatSession session = session(new ScriptedComputer());
        session.start();
        board.click("b1");
        board.click("g1");
        board.click("c1");                     // three queens at 6 each: 2 points left
        assertEquals(2, game.getCheatPoints(PieceColor.WHITE));
        board.click("f1");                     // a fourth cannot be afforded
        assertNull(game.secretAt(Position.of("f1")));

        window.identity = PieceType.PAWN;
        board.click("d1");                     // the real queen becomes a pawn: 8 back
        assertEquals(10, game.getCheatPoints(PieceColor.WHITE));
        assertTrue(window.status().contains("10 cheat points left"), window.status());

        window.identity = PieceType.QUEEN;
        board.click("f1");                     // now it can
        assertEquals(PieceType.QUEEN, game.secretAt(Position.of("f1")));
        assertTrue(session.isSettingUp());     // the game waits for Start, however many points are left
    }

    @Test
    void startButtonBeginsPlayWithTransformationsUnused() {
        CheatSession session = started(new ScriptedComputer());
        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals(20, game.getCheatPoints(PieceColor.WHITE));
        session.finishSwitching();  // pressing it again does nothing
        assertEquals(Phase.PLAY, game.getPhase());
    }

    // ----------------------------------------------------------------- play

    @Test
    void computerRepliesToEachMove() {
        CheatSession session = started(new ScriptedComputer());

        move("e2", "e4");

        assertEquals(PieceColor.WHITE, game.getTurn());
        assertEquals(PieceColor.BLACK, game.getLastMove().getPiece().getColor());
        assertEquals("Your move.", window.status());
        assertTrue(window.statuses.contains("The computer is thinking…"));
        assertTrue(session.canAccuse());
        assertEquals(List.of(Event.MOVE), window.cheers);
    }

    @Test
    void computerChallengesAMoveThatLooksImpossibleAndTheNewsStaysOnScreen() {
        CheatSession session = session(new ScriptedComputer());
        session.start();
        board.click("b1");          // a queen that looks like a knight
        session.finishSwitching();
        move("b2", "b4");

        move("b1", "b3");           // no knight can do that

        assertTrue(window.cheers.contains(Event.CAUGHT_YOU));
        assertNull(game.secretAt(Position.of("b3")));
        assertEquals(PieceType.KNIGHT, game.truePiece(Position.of("b3")).getType());  // it takes the harshest reward
        assertEquals("The computer challenged you and was right. Your knight on b3 was secretly a queen. "
                + "Not any more: it is stripped. Your move.", window.status());
    }

    @Test
    void warnsWhenTheKingLooksToBeInCheck() {
        started(new ScriptedComputer("e7e6", "d8h4"));
        move("f2", "f3");
        move("a2", "a3");
        assertEquals("Your move. Your king looks to be in check. Or does it?", window.status());
    }

    @Test
    void losingTheKingEndsTheGame() {
        CheatSession session = started(new ScriptedComputer("e7e6", "d8h4", "h4e1"));
        move("f2", "f3");
        move("a2", "a3");
        move("a3", "a4");   // ignoring a check that was real

        assertTrue(game.isOver());
        assertEquals("Your king has been captured. You lose.", window.status());
        assertTrue(window.cheers.contains(Event.YOU_LOSE));
        assertNull(board.moveReply);
        assertFalse(session.canAccuse());
    }

    @Test
    void capturingTheKingWins() {
        started(new ScriptedComputer("f7f6", "a7a6"));
        move("e2", "e4");
        move("d1", "h5");
        move("h5", "e8");

        assertEquals(PieceColor.WHITE, game.getWinner());
        assertEquals("You captured the king. You win!", window.status());
        assertTrue(window.cheers.contains(Event.YOU_WIN));
    }

    @Test
    void stoppingIgnoresTheComputersLateMove() {
        List<Runnable> waiting = new ArrayList<>();
        CheatSession session = new CheatSession(game, new ScriptedComputer(), board, waiting::add,
                Runnable::run, 0, window, calm());
        session.start();
        session.finishSwitching();
        move("e2", "e4");
        assertEquals(1, waiting.size());   // the computer is "thinking"

        session.stop();
        waiting.get(0).run();

        assertEquals(PieceColor.BLACK, game.getTurn());
        assertEquals("e2e4", game.getLastMove().toString());
    }

    // ----------------------------------------------------------- challenges

    @Test
    void failedChallengeLosesTheTurnAndTheComputerMovesAgain() {
        CheatSession session = started(new ScriptedComputer());
        move("e2", "e4");
        Move challenged = game.getLastMove();

        session.accuse();

        assertTrue(window.cheers.contains(Event.WRONG_ACCUSATION));
        assertTrue(window.statuses.stream().anyMatch(text ->
                text.startsWith("Wrong! That move was honest. You lose your turn.")), window.statuses.toString());
        assertEquals(PieceColor.WHITE, game.getTurn());
        assertFalse(game.getLastMove() == challenged, "the computer has moved a second time");
        assertNotNull(board.moveReply);
    }

    /** Sets up a game in which the computer has a queen on b8 that looks like a knight, and has just moved it. */
    private CheatSession withTheComputersFakeKnightJustMoved() {
        game.transform(PieceColor.BLACK, Position.of("b8"), PieceType.QUEEN);
        CheatSession session = started(new ScriptedComputer("b7b5", "b8b6"));
        move("a2", "a3");
        move("h2", "h3");
        assertEquals("b8b6", game.getLastMove().toString());
        return session;
    }

    @Test
    void successfulChallengeCanRevealThePiece() {
        CheatSession session = withTheComputersFakeKnightJustMoved();

        window.reward = Reward.EXPOSE;
        session.accuse();

        assertTrue(window.cheers.contains(Event.CAUGHT_THEM));
        assertEquals(List.of(Reward.EXPOSE, Reward.STRIP, Reward.JAIL, Reward.SWITCH, Reward.EXTRA_MOVE,
                Reward.STEAL_TIME), window.offered);
        assertNull(game.secretAt(Position.of("b6")));
        assertEquals("Caught! The piece on b6 is really a queen. Your move.", window.status());
        assertNotNull(board.moveReply);
        assertFalse(session.canAccuse());
    }

    @Test
    void successfulChallengeCanBeSpentOnASwitchToken() {
        CheatSession session = withTheComputersFakeKnightJustMoved();

        window.reward = Reward.SWITCH;
        session.accuse();
        assertEquals(Phase.REWARD, game.getPhase());
        assertEquals("Caught! Spend a switch token: click one of your pieces to transform it.", window.status());
        assertNotNull(board.picker);
        assertNull(board.moveReply);

        window.identity = PieceType.ROOK;
        board.click("g1");

        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals(PieceType.ROOK, game.secretAt(Position.of("g1")));
        assertEquals(2, game.getSwitchTokens(PieceColor.WHITE));
        assertNotNull(game.secretAt(Position.of("b6")));   // the computer's piece stays hidden
        assertNull(board.picker);
        assertEquals("Transformed. Your move.", window.status());
    }

    @Test
    void successfulChallengeCanStripThePiece() {
        CheatSession session = withTheComputersFakeKnightJustMoved();
        window.reward = Reward.STRIP;
        session.accuse();
        assertEquals(PieceType.KNIGHT, game.truePiece(Position.of("b6")).getType());
        assertEquals("Caught! That knight was secretly a queen. Not any more: it is stripped. Your move.",
                window.status());
    }

    @Test
    void successfulChallengeCanJailThePiece() {
        CheatSession session = withTheComputersFakeKnightJustMoved();
        window.reward = Reward.JAIL;
        session.accuse();
        assertEquals(Position.of("b6"), game.getJailedSquare());
        assertEquals("Caught! The piece on b6 is locked up for two turns. Your move.", window.status());
    }

    @Test
    void successfulChallengeCanWinAnExtraMove() {
        CheatSession session = withTheComputersFakeKnightJustMoved();
        window.reward = Reward.EXTRA_MOVE;
        session.accuse();
        assertEquals("Caught! You get two moves in a row. Your move.", window.status());

        Move computersLast = game.getLastMove();
        move("b2", "b3");
        assertEquals(PieceColor.WHITE, game.getTurn());       // the computer missed its turn
        assertEquals("b2b3", game.getLastMove().toString());
        assertFalse(computersLast == game.getLastMove());
        move("c2", "c3");
        assertEquals(PieceColor.BLACK, game.getLastMove().getPiece().getColor());  // and now it has replied
    }

    @Test
    void successfulChallengeCanStealTime() {
        CheatSession session = withTheComputersFakeKnightJustMoved();
        window.reward = Reward.STEAL_TIME;
        session.accuse();
        assertEquals(30_000, window.timeAdded[PieceColor.WHITE.ordinal()]);
        assertEquals(-30_000, window.timeAdded[PieceColor.BLACK.ordinal()]);
        assertEquals("Caught! 30 seconds stolen from its clock. Your move.", window.status());
    }

    @Test
    void failedChallengeCanCostTimeInsteadOfTheTurn() {
        // The second of the penalties on offer with a clock running: lose time.
        game = new CheatGame(CheatLevel.STANDARD, new Random(1) {
            @Override
            public double nextDouble() {
                return 1.0;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(1, bound - 1);
            }
        });
        CheatSession session = started(new ScriptedComputer());
        move("e2", "e4");

        session.accuse();

        assertEquals(-30_000, window.timeAdded[PieceColor.WHITE.ordinal()]);
        assertEquals("Wrong! That move was honest. 30 seconds docked from your clock. Your move.", window.status());
        assertEquals(PieceColor.WHITE, game.getTurn());
    }

    @Test
    void angryGamesAllowOnlyTwoClockFiddles() {
        game = new CheatGame(CheatLevel.ANGRY, calm());
        CheatSession session = started(new ScriptedComputer());
        assertEquals(2, session.getFiddlesLeft());
    }

    @Test
    void timeWarpSwapsTheClocks() {
        // Disasters after every move, and always the sixth on the list: the time warp.
        game = new CheatGame(CheatLevel.ANGRY, new Random(1) {
            @Override
            public double nextDouble() {
                return 0.0;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(5, bound - 1);
            }
        });
        started(new ScriptedComputer());
        move("e2", "e4");
        assertTrue(window.clockSwaps >= 1);
        assertTrue(window.cheers.contains(Event.TIME_WARP));
    }

    @Test
    void gettingHackedIsPassedOnToTheWindow() {
        // Disasters after every move, and always the last on the list: the hack.
        game = new CheatGame(CheatLevel.ANGRY, new Random(1) {
            @Override
            public double nextDouble() {
                return 0.0;
            }

            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        });
        started(new ScriptedComputer());
        move("e2", "e4");

        assertTrue(window.hacks >= 1);
        assertTrue(window.cheers.contains(Event.HACKED));
        assertTrue(window.statuses.stream().anyMatch(text -> text.contains("YOU HAVE BEEN HACKED")));
        assertFalse(game.isOver());   // the game itself is untouched
    }

    @Test
    void rewardCanBeTurnedDown() {
        CheatSession session = withTheComputersFakeKnightJustMoved();
        window.reward = null;
        session.accuse();
        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals("Caught! Your move.", window.status());
        assertNotNull(game.secretAt(Position.of("b6")));
    }

    // ------------------------------------------------------------ the clock

    @Test
    void fiddlingTheClockAddsTimeAFewTimesAGame() {
        CheatSession session = started(new ScriptedComputer());
        assertTrue(session.canFiddleClock());
        assertEquals(3, session.getFiddlesLeft());

        session.fiddleClock();

        assertEquals(20_000, window.timeAdded[PieceColor.WHITE.ordinal()]);
        assertEquals(2, session.getFiddlesLeft());
        assertTrue(window.cheers.contains(Event.FIDDLE));
        session.fiddleClock();
        session.fiddleClock();
        assertFalse(session.canFiddleClock());
        session.fiddleClock();   // nothing left: ignored
        assertEquals(60_000, window.timeAdded[PieceColor.WHITE.ordinal()]);
    }

    @Test
    void noFiddlingWithoutAClock() {
        window.clock = false;
        CheatSession session = started(new ScriptedComputer());
        assertFalse(session.canFiddleClock());
        session.fiddleClock();
        assertEquals(0, window.timeAdded[PieceColor.WHITE.ordinal()]);
    }

    @Test
    void computerThatNoticesAFiddledClockDocksTheTime() {
        CheatAI sharpEyed = new ScriptedComputer() {
            @Override
            public boolean shouldAccuse(CheatGame cheatGame) {
                return cheatGame.canAccuse() && cheatGame.lastMoverFiddledClock();
            }
        };
        CheatSession session = started(sharpEyed);
        session.fiddleClock();
        move("e2", "e4");

        assertEquals(20_000 - 40_000, window.timeAdded[PieceColor.WHITE.ordinal()]);
        assertTrue(window.cheers.contains(Event.CAUGHT_YOU));
        // Nothing of the player's is hidden, so there is nothing to strip or expose: it moves twice instead.
        assertEquals("The computer challenged you and was right. You fiddled the clock: 40 seconds docked. "
                + "It takes an extra move. Your move.", window.status());
        assertEquals(PieceColor.WHITE, game.getTurn());
    }

    @Test
    void catchingTheComputerFiddlingDocksItsTime() {
        // A source of randomness under which the computer fiddles every turn it can.
        Random eager = new Random(1) {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
        CheatSession session = new CheatSession(game, new ScriptedComputer(), board, Runnable::run,
                Runnable::run, 0, window, eager);
        session.start();
        session.finishSwitching();
        move("e2", "e4");
        assertEquals(20_000, window.timeAdded[PieceColor.BLACK.ordinal()]);  // there for anyone watching

        window.reward = Reward.SWITCH;
        session.accuse();

        assertEquals(20_000 - 40_000, window.timeAdded[PieceColor.BLACK.ordinal()]);
        assertTrue(window.status().startsWith("Caught! It fiddled the clock: 40 seconds docked. "), window.status());
    }

    @Test
    void runningOutOfTimeLosesTheGame() {
        CheatSession session = started(new ScriptedComputer());
        move("e2", "e4");

        session.timeUp(PieceColor.WHITE);

        assertTrue(game.isOver());
        assertEquals(PieceColor.BLACK, game.getWinner());
        assertEquals("You ran out of time. You lose.", window.status());
        assertNull(board.moveReply);
        assertTrue(window.cheers.contains(Event.OUT_OF_TIME));
    }

    // -------------------------------------------------------- angry chaotic

    @Test
    void failedChallengeInAngryModeBringsADebuffButKeepsTheTurn() {
        game = new CheatGame(CheatLevel.ANGRY, calm());
        CheatSession session = started(new ScriptedComputer());
        move("e2", "e4");

        session.accuse();

        assertEquals(PieceColor.WHITE, game.getTurn());
        assertNotNull(game.getDebuff(PieceColor.WHITE));
        assertTrue(window.cheers.contains(Event.DEBUFF));
        String expected = CheatSession.describe(game.getDebuff(PieceColor.WHITE));
        assertTrue(window.status().startsWith("Wrong! That move was honest. Your punishment: " + expected + ". "),
                window.status());
        assertTrue(window.status().endsWith("(" + expected + ")"), window.status());
        assertEquals(2, game.getChallengesLeft(PieceColor.WHITE));
    }

    @Test
    void everyDebuffHasItsOwnDescription() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CheatGame.Debuff debuff : CheatGame.Debuff.values()) {
            assertTrue(seen.add(CheatSession.describe(debuff)), debuff.name());
        }
    }

    @Test
    void disruptCanMakeASquareSafe() {
        game = new CheatGame(CheatLevel.ANGRY, calm());
        CheatSession session = withTheComputersFakeKnightJustMoved();

        window.reward = Reward.DISRUPT_SAFE;
        session.accuse();
        assertTrue(window.offered.contains(Reward.DISRUPT_SAFE));
        assertEquals("Caught! Disrupt: click the square to make safe.", window.status());

        board.click("e2");

        assertEquals(SquareState.SAFE, game.getSquareState(Position.of("e2")));
        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals("Nothing on e2 can be captured for a while. Your move.", window.status());
        assertNotNull(board.moveReply);
    }

    @Test
    void disastersAreAnnouncedAndJockyReacts() {
        // Disasters strike whenever the game's randomness says so: here, after every move.
        game = new CheatGame(CheatLevel.ANGRY, new Random(5) {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        });
        started(new ScriptedComputer());

        board.moveReply.accept(game.moves().get(0));

        assertFalse(window.announcements.isEmpty());
        assertTrue(window.cheers.contains(Event.ROCKET) || window.cheers.contains(Event.STORM)
                || window.cheers.contains(Event.LAVA), window.cheers.toString());
        String news = window.announcements.get(window.announcements.size() - 1).getText();
        assertTrue(window.statuses.stream().anyMatch(text -> text.contains(news)), window.statuses.toString());
    }

    @Test
    void wholeAngryGameAgainstTheRealComputerRunsToAnEnd() {
        game = new CheatGame(CheatLevel.ANGRY, new Random(11));
        CheatSession session = new CheatSession(game, new CheatAI(new Random(8)), board, Runnable::run,
                Runnable::run, 0, window, new Random(12));
        session.start();
        session.finishSwitching();
        Random random = new Random(9);
        for (int turn = 0; turn < 400 && !game.isOver(); turn++) {
            List<Move> moves = game.moves();
            board.moveReply.accept(moves.get(random.nextInt(moves.size())));
        }
        // A player moving at random against a computer that takes kings does not last long.
        assertTrue(game.isOver(), "still going after 400 turns");
        assertEquals(PieceColor.BLACK, game.getWinner());
        assertFalse(window.announcements.isEmpty(), "no disaster in a whole angry game");
    }
}
