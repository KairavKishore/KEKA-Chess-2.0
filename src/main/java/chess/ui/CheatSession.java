package chess.ui;

import chess.cheat.CheatAI;
import chess.cheat.CheatGame;
import chess.cheat.CheatGame.ChaosNotice;
import chess.cheat.CheatGame.Chaos;
import chess.cheat.CheatGame.Debuff;
import chess.cheat.CheatGame.Penalty;
import chess.cheat.CheatGame.Phase;
import chess.cheat.CheatGame.Reward;
import chess.engine.Board;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;
import chess.ui.JockyLines.Event;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executor;

/**
 * Runs one game of cheating chess between a person, playing White, and the
 * computer: the secret transformations beforehand, the turns, the
 * challenges with their rewards and penalties, the clock cheats, and
 * passing on news of rockets, storms and lava.
 *
 * <p>Like {@code GameController} and {@code AIPlayer}, it has no JavaFX in
 * it. The board, the two threads and a {@link Host} for everything else it
 * needs from the window are handed in, so tests can run a whole game on one
 * thread without a window. All methods, and all answers from the board,
 * must be on the reply thread.
 */
public class CheatSession {

    /** What the session needs from the window around it. */
    public interface Host {

        /** Asks what the piece on a square should secretly become; null if the person thinks better of it. */
        PieceType chooseIdentity(Position square);

        /** Asks which reward to take for a successful challenge; null to take none. */
        Reward chooseReward(List<Reward> options);

        /** Shows a line of text about what is going on. */
        void status(String text);

        /** Tells the mascot what just happened. */
        void jocky(Event event);

        /** Announces a rocket, storm or lava. */
        void announce(ChaosNotice notice);

        /** True if the game is being played with a clock. */
        boolean hasClock();

        /** Adds time to a side's clock, or takes it away if negative. */
        void addTime(PieceColor color, long millis);

        /** Swaps the two sides' clocks. */
        void swapClocks();

        /** Scrambles the board on screen until the person beats the mini-game. */
        void hacked();
    }

    static final PieceColor HUMAN = PieceColor.WHITE;
    static final PieceColor COMPUTER = PieceColor.BLACK;

    /** How much time fiddling the clock adds. */
    static final long FIDDLE_MILLIS = 20_000;

    /** How much time is taken from a side caught fiddling: what it stole and as much again. */
    static final long FIDDLE_PENALTY_MILLIS = 2 * FIDDLE_MILLIS;

    /** How much time a reward steals, or a penalty takes. */
    static final long TIME_PRIZE_MILLIS = 30_000;

    /** How many times each side may fiddle the clock in a game of each kind. */
    static final int FIDDLES_PER_GAME = 3;
    static final int FIDDLES_PER_ANGRY_GAME = 2;

    /** How often the computer fiddles the clock before a move, while it still can. */
    private static final double COMPUTER_FIDDLES = 0.12;

    /** What clicking a square means while the board is in picking mode. */
    private enum Picking {
        TRANSFORM, SAFE_SQUARE
    }

    private final CheatGame game;
    private final CheatAI computer;
    private final BoardInput board;
    private final Executor worker;
    private final Executor replyThread;
    private final long pauseMillis;
    private final Host host;
    private final Random random;

    private Picking picking = Picking.TRANSFORM;
    private int humanFiddlesLeft;
    private int computerFiddlesLeft;

    /** Goes up whenever the computer is asked to think, and when the session stops, so late answers are ignored. */
    private int requestNumber;

    /**
     * @param board       where the person clicks
     * @param worker      runs the computer's thinking
     * @param replyThread delivers the computer's move
     * @param pauseMillis how long the computer waits before moving, so its moves can be followed
     * @param random      decides when the computer fiddles the clock
     */
    public CheatSession(CheatGame game, CheatAI computer, BoardInput board, Executor worker,
            Executor replyThread, long pauseMillis, Host host, Random random) {
        this.game = game;
        this.computer = computer;
        this.board = board;
        this.worker = worker;
        this.replyThread = replyThread;
        this.pauseMillis = pauseMillis;
        this.host = host;
        this.random = random;
        int fiddles = game.getLevel().isChaotic() ? FIDDLES_PER_ANGRY_GAME : FIDDLES_PER_GAME;
        this.humanFiddlesLeft = fiddles;
        this.computerFiddlesLeft = fiddles;
    }

    /** Begins with the transformations: the computer does its own out of sight, then the person takes their time. */
    public void start() {
        game.setClockInUse(host.hasClock());
        computer.setUp(game, COMPUTER);
        startPicking(Picking.TRANSFORM);
        showSetupStatus();
    }

    /** Abandons the game: a move the computer is still choosing will be ignored. */
    public void stop() {
        requestNumber++;
    }

    /** A side's clock has run out: the other side wins. */
    public void timeUp(PieceColor loser) {
        stop();
        game.declareWinner(loser.opposite());
        board.pickSquares(null);
        board.requestMove(null);
        board.refresh();
        host.jocky(Event.OUT_OF_TIME);
        host.status(loser == HUMAN ? "You ran out of time. You lose." : "The computer ran out of time. You win!");
    }

    /** True while the person is still transforming pieces before the game. */
    public boolean isSettingUp() {
        return game.getPhase() == Phase.SETUP;
    }

    /** True if the person may challenge the computer's last move now. */
    public boolean canAccuse() {
        return game.getTurn() == HUMAN && game.canAccuse();
    }

    /** True if the person may fiddle the clock now: on their own turn, with a clock, a limited number of times. */
    public boolean canFiddleClock() {
        return host.hasClock() && humanFiddlesLeft > 0
                && game.getPhase() == Phase.PLAY && game.getTurn() == HUMAN;
    }

    /** How many more times the person may fiddle the clock. */
    public int getFiddlesLeft() {
        return humanFiddlesLeft;
    }

    // ------------------------------------------------------ transformations

    private void startPicking(Picking what) {
        picking = what;
        board.requestMove(null);
        board.pickSquares(this::pick);
    }

    /** A click on a square while the board is in picking mode. */
    private void pick(Position position) {
        if (picking == Picking.SAFE_SQUARE) {
            game.disruptSafeSquare(position);
            board.pickSquares(null);
            nextTurn("Nothing on " + position + " can be captured for a while. ");
            return;
        }
        if (!game.mayTransformNow(HUMAN) || !game.isTransformable(HUMAN, position)) {
            return;
        }
        board.highlight(position);
        PieceType identity = host.chooseIdentity(position);
        board.highlight(null);
        if (identity == null || !game.canTransform(HUMAN, position, identity)) {
            return;
        }
        boolean reward = game.getPhase() == Phase.REWARD;
        game.transform(HUMAN, position, identity);
        host.jocky(Event.SWITCH);
        if (reward) {
            board.pickSquares(null);
            nextTurn("Transformed. ");
        } else {
            showSetupStatus();
        }
    }

    private void showSetupStatus() {
        board.refresh();
        host.status("Click a piece to secretly transform it. " + game.getCheatPoints(HUMAN)
                + " cheat points left. Press Start when ready.");
    }

    /** The person has finished transforming before the game: play begins. */
    public void finishSwitching() {
        if (!isSettingUp()) {
            return;
        }
        board.highlight(null);
        board.pickSquares(null);
        game.finishSetup(HUMAN);
        nextTurn("");
    }

    // ----------------------------------------------------------------- play

    /**
     * Moves the game on to whoever's turn it is.
     *
     * @param note something that has just happened, to show before the usual status
     */
    private void nextTurn(String note) {
        board.refresh();
        if (game.isOver()) {
            board.requestMove(null);
            announceResult(note);
        } else if (game.getTurn() == HUMAN) {
            Debuff debuff = game.getDebuff(HUMAN);
            String check = game.looksInCheck(HUMAN) ? " Your king looks to be in check. Or does it?" : "";
            String curse = debuff == null ? "" : " (" + describe(debuff) + ")";
            if (game.getRollCallKind() != null) {
                curse += " (only " + name(game.getRollCallKind()) + "s may move)";
            }
            if (game.isCeasefire()) {
                curse += " (ceasefire: no captures)";
            }
            host.status(note + "Your move." + check + curse);
            board.requestMove(this::humanMoved);
        } else {
            board.requestMove(null);
            computerTurn(note);
        }
    }

    private void humanMoved(Move move) {
        game.play(move);
        host.jocky(move.isCapture() ? Event.CAPTURE : Event.MOVE);
        nextTurn(newsOfChaos());
    }

    /** Announces any disaster that has just struck, and returns a sentence about it for the status line. */
    private String newsOfChaos() {
        ChaosNotice notice = game.takeChaosNotice();
        if (notice == null) {
            return "";
        }
        if (notice.getKind() == Chaos.TIME_WARP) {
            host.swapClocks();
        } else if (notice.getKind() == Chaos.HACKED) {
            host.hacked();
        }
        host.announce(notice);
        host.jocky(Event.valueOf(notice.getKind().name()));
        return notice.getText() + " ";
    }

    private void computerTurn(String note) {
        String news = note;
        if (computer.shouldAccuse(game)) {
            news += computerChallenges();
            if (game.isOver() || game.getTurn() == HUMAN) {
                nextTurn(news);  // its challenge failed and cost it the turn
                return;
            }
        }
        if (host.hasClock() && computerFiddlesLeft > 0 && random.nextDouble() < COMPUTER_FIDDLES) {
            computerFiddlesLeft--;
            game.noteClockFiddled(COMPUTER);
            host.addTime(COMPUTER, FIDDLE_MILLIS);  // in plain sight, for anyone watching the clock
        }
        host.status(news + "The computer is thinking…");

        // Hand the computer only what its side is allowed to see.
        Board belief = game.beliefBoard(COMPUTER);
        List<Move> moves = game.moves();
        int thisRequest = ++requestNumber;
        String carried = news;
        worker.execute(() -> {
            pause();
            Move move = computer.chooseMove(belief, moves, COMPUTER);
            replyThread.execute(() -> computerMoved(thisRequest, move, carried));
        });
    }

    /** The computer challenges the person's last move. Returns a sentence saying how that went. */
    private String computerChallenges() {
        Position square = game.getLastMove().getTo();
        boolean fiddled = game.lastMoverFiddledClock();
        if (!game.accuse()) {
            return "The computer challenged your move and was wrong. " + payPenalty(COMPUTER);
        }
        host.jocky(Event.CAUGHT_YOU);
        String news = "The computer challenged you and was right. ";
        if (fiddled) {
            host.addTime(HUMAN, -FIDDLE_PENALTY_MILLIS);
            news += "You fiddled the clock: 40 seconds docked. ";
        }
        // It takes the harshest reward going: strip the piece, else expose one, else move twice.
        List<Reward> rewards = game.availableRewards();
        PieceType look = game.looksPiece(square) == null ? null : game.looksPiece(square).getType();
        if (rewards.contains(Reward.STRIP)) {
            PieceType secret = game.secretAt(square);
            game.stripAccused();
            news += "Your " + name(look) + " on " + square + " was secretly a " + name(secret)
                    + ". Not any more: it is stripped. ";
        } else if (rewards.contains(Reward.EXPOSE)) {
            Position exposed = game.exposeAccused();
            news += "Your piece on " + exposed + " is exposed as a "
                    + name(game.truePiece(exposed).getType()) + ". ";
        } else {
            game.takeExtraMove();
            news += "It takes an extra move. ";
        }
        board.refresh();
        return news;
    }

    /**
     * Carries out whatever part of the last failed challenge's penalty
     * falls to the clock, and returns a sentence saying what the penalty was.
     */
    private String payPenalty(PieceColor challenger) {
        boolean you = challenger == HUMAN;
        Penalty penalty = game.getLastPenalty();
        switch (penalty) {
            case LOSE_TURN:
                return you ? "You lose your turn. " : "It loses its turn. ";
            case LOSE_TIME:
                host.addTime(challenger, -TIME_PRIZE_MILLIS);
                return you ? "30 seconds docked from your clock. " : "30 seconds docked from its clock. ";
            case LOSE_TOKEN:
                return you ? "You lose a switch token. " : "It loses a switch token. ";
            case EXPOSED:
                Position exposed = game.getLastExposedOwn();
                return (you ? "Your own piece on " : "Its piece on ") + exposed + " is exposed as a "
                        + name(game.truePiece(exposed).getType()) + ". ";
            default:
                return (you ? "Your punishment: " : "Its punishment: ") + describe(game.getLastDebuff()) + ". ";
        }
    }

    private void computerMoved(int request, Move move, String news) {
        if (request != requestNumber) {
            return;
        }
        game.play(move);
        nextTurn(news + newsOfChaos());
    }

    private void pause() {
        if (pauseMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------ the clock

    /** The person secretly adds time to their clock. Does nothing if that is not allowed now. */
    public void fiddleClock() {
        if (!canFiddleClock()) {
            return;
        }
        humanFiddlesLeft--;
        game.noteClockFiddled(HUMAN);
        host.addTime(HUMAN, FIDDLE_MILLIS);
        host.jocky(Event.FIDDLE);
    }

    // ----------------------------------------------------------- challenges

    /** The person challenges the move the computer just played. Does nothing if that is not allowed now. */
    public void accuse() {
        if (!canAccuse()) {
            return;
        }
        Position square = game.getLastMove().getTo();
        PieceType look = game.looksPiece(square) == null ? null : game.looksPiece(square).getType();
        boolean fiddled = game.lastMoverFiddledClock();
        board.requestMove(null);

        if (!game.accuse()) {
            host.jocky(game.getLastPenalty() == Penalty.DEBUFF ? Event.DEBUFF : Event.WRONG_ACCUSATION);
            nextTurn("Wrong! That move was honest. " + payPenalty(HUMAN));
            return;
        }
        host.jocky(Event.CAUGHT_THEM);
        String news = "Caught! ";
        if (fiddled) {
            host.addTime(COMPUTER, -FIDDLE_PENALTY_MILLIS);
            news += "It fiddled the clock: 40 seconds docked. ";
        }
        takeReward(news, look);
    }

    private void takeReward(String news, PieceType look) {
        List<Reward> options = game.availableRewards();
        Reward reward = options.isEmpty() ? null : host.chooseReward(options);
        if (reward == null || !options.contains(reward)) {
            game.declineReward();
            nextTurn(news);
        } else if (reward == Reward.EXPOSE) {
            Position exposed = game.exposeAccused();
            nextTurn(news + "The piece on " + exposed + " is really a "
                    + name(game.truePiece(exposed).getType()) + ". ");
        } else if (reward == Reward.STRIP) {
            PieceType secret = game.secretAt(game.getLastMove().getTo());
            game.stripAccused();
            nextTurn(news + "That " + name(look) + " was secretly a " + name(secret)
                    + ". Not any more: it is stripped. ");
        } else if (reward == Reward.JAIL) {
            game.jailAccused();
            nextTurn(news + "The piece on " + game.getJailedSquare() + " is locked up for two turns. ");
        } else if (reward == Reward.EXTRA_MOVE) {
            game.takeExtraMove();
            nextTurn(news + "You get two moves in a row. ");
        } else if (reward == Reward.STEAL_TIME) {
            game.stealTime();
            host.addTime(COMPUTER, -TIME_PRIZE_MILLIS);
            host.addTime(HUMAN, TIME_PRIZE_MILLIS);
            nextTurn(news + "30 seconds stolen from its clock. ");
        } else if (reward == Reward.DISRUPT_EXPOSE) {
            Position exposed = game.disruptExposeAnother();
            nextTurn(news + "Disrupt: the piece on " + exposed + " is really a "
                    + name(game.truePiece(exposed).getType()) + ". ");
        } else if (reward == Reward.DISRUPT_UNCAPTURE) {
            Position back = game.disruptUncapture();
            nextTurn(news + "Disrupt: your " + name(game.looksPiece(back).getType()) + " is back, on " + back + ". ");
        } else if (reward == Reward.DISRUPT_SAFE) {
            startPicking(Picking.SAFE_SQUARE);
            board.refresh();
            host.status(news + "Disrupt: click the square to make safe.");
        } else {
            startPicking(Picking.TRANSFORM);
            board.refresh();
            host.status(news + "Spend a switch token: click one of your pieces to transform it.");
        }
    }

    private void announceResult(String note) {
        if (game.getWinner() == HUMAN) {
            host.jocky(Event.YOU_WIN);
            host.status(note + "You captured the king. You win!");
        } else if (game.getWinner() == COMPUTER) {
            host.jocky(Event.YOU_LOSE);
            host.status(note + "Your king has been captured. You lose.");
        } else {
            host.jocky(Event.DRAW);
            host.status(note + "Nobody can make progress. The game is a draw.");
        }
    }

    /** What a debuff does, in a few words. */
    static String describe(Debuff debuff) {
        switch (debuff) {
            case BLOB:
                return "every piece looks the same colour";
            case HALF_DARK:
                return "half the board is blacked out";
            case BACKWARDS:
                return "your clicks land on the opposite square";
            case SAME_COLOUR_SQUARES:
                return "pieces must stay on their own colour of square";
            case NO_QUEEN:
                return "your queen cannot move";
            case NO_PAWNS:
                return "your pawns cannot move";
            case NO_CAPTURES:
                return "you cannot capture anything";
            default:
                return "your queen, rooks and bishops slide only two squares";
        }
    }

    private static String name(PieceType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
