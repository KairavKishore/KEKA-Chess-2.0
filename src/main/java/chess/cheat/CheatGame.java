package chess.cheat;

import chess.ai.MaterialEvaluator;
import chess.engine.Board;
import chess.engine.Game;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * A game of cheating chess: chess in which some pieces are not what they
 * appear to be.
 *
 * <p><b>Transformations.</b> Before play, each side secretly transforms
 * some of its pieces. A transformed piece keeps its square and its looks
 * but moves as something else: a knight that is secretly a queen. Each side
 * has a budget of cheat points for this. A piece is worth what players
 * usually reckon (pawn 1, knight and bishop 3, rook 5, queen 9), making one
 * stronger costs the difference, and making one weaker, even down to a
 * pawn, gives the difference back to spend elsewhere. Kings cannot be
 * transformed.
 *
 * <p><b>No check.</b> Nobody has to answer a check, because nobody can be
 * sure what is really attacking. Kings can be captured, and capturing the
 * king wins the game.
 *
 * <p><b>Challenges.</b> Instead of moving, a player may challenge the move
 * just played. The challenge succeeds if the piece that moved is
 * transformed, or if the mover fiddled the clock that turn. A successful
 * challenger takes a reward and still has their move; a failed one pays a
 * penalty.
 *
 * <p><b>Angry chaotic mode</b> adds a limit on challenges, a wider choice of
 * rewards, debuffs in place of the lost turn, and random disasters: rockets
 * that crater part of the board, storms that shorten every piece's reach,
 * and lava that floods squares.
 *
 * <p>The true position is kept on an engine {@link Board}, which supplies
 * the moves. This class adds what each piece looks like, and the rules
 * above. Castling is left out.
 */
public class CheatGame {

    /** The stages a game goes through. */
    public enum Phase {
        /** Both sides are transforming pieces. */
        SETUP,
        /** Moves are being played. */
        PLAY,
        /** A challenge succeeded and the challenger is choosing a reward. */
        REWARD,
        /** A king has been captured, or the game is drawn. */
        OVER
    }

    /** What a successful challenger can choose between. The Disrupt rewards share one use per game. */
    public enum Reward {
        /** Show a transformed enemy piece for what it is, for good. */
        EXPOSE,
        /** Take away the caught piece's secret for good: it becomes what it looks like. */
        STRIP,
        /** Lock the caught piece up: it cannot move for its owner's next two turns. */
        JAIL,
        /** Spend a switch token to transform one of your own pieces again. */
        SWITCH,
        /** Move twice in a row: the cheat misses their next turn. */
        EXTRA_MOVE,
        /** Take half a minute off the cheat's clock and add it to your own. */
        STEAL_TIME,
        /** Disrupt: expose a second hidden enemy piece. */
        DISRUPT_EXPOSE,
        /** Disrupt: make one square safe, so that nothing on it can be captured for a while. */
        DISRUPT_SAFE,
        /** Disrupt: bring back your most valuable captured piece. */
        DISRUPT_UNCAPTURE
    }

    /** What a failed challenge costs. Which one is drawn at random from those that apply. */
    public enum Penalty {
        /** The challenger misses their turn. Cheating mode only. */
        LOSE_TURN,
        /** The challenger is put under a {@link Debuff} for a turn or two. Angry chaotic mode only. */
        DEBUFF,
        /** Half a minute comes off the challenger's clock. */
        LOSE_TIME,
        /** One of the challenger's own hidden pieces is shown for what it is. */
        EXPOSED,
        /** The challenger loses a switch token. */
        LOSE_TOKEN
    }

    /** What a failed challenger may suffer in angry chaotic mode, for a turn or two. */
    public enum Debuff {
        /** Every piece is drawn the same colour. */
        BLOB,
        /** Half of the board is blacked out. */
        HALF_DARK,
        /** Clicks land on the opposite square. */
        BACKWARDS,
        /** Pieces may only move to squares of the colour they stand on. */
        SAME_COLOUR_SQUARES,
        /** The queen may not move. */
        NO_QUEEN,
        /** Pawns may not move. */
        NO_PAWNS,
        /** Nothing may be captured. */
        NO_CAPTURES,
        /** Queens, rooks and bishops may slide only two squares. */
        SHORT_REACH;

        /** True if this only affects what the player sees and clicks, not which moves exist. */
        public boolean isVisual() {
            return this == BLOB || this == HALF_DARK || this == BACKWARDS;
        }
    }

    /** What has happened to a square. */
    public enum SquareState {
        NORMAL,
        /** Blown up by a rocket: nothing can stand on it or pass over it. */
        CRATER,
        /** Flooded with lava: nothing can stand on it or pass over it. */
        LAVA,
        /** Protected by a Disrupt reward: a piece on it cannot be captured. */
        SAFE
    }

    /** The random disasters of angry chaotic mode. */
    public enum Chaos {
        /** A two-by-two crater. */
        ROCKET,
        /** Sliding pieces reach less far. */
        STORM,
        /** One colour of one file is flooded. */
        LAVA,
        /** Only pieces that look like one kind may move. */
        ROLL_CALL,
        /** Nothing may be captured. */
        CEASEFIRE,
        /** The two clocks are swapped. */
        TIME_WARP,
        /** The middle of the board is hidden. */
        FOG,
        /** The board on screen is scrambled until the player beats a mini-game. Nothing really moves. */
        HACKED
    }

    /** A disaster that has just struck, for the interface to announce. */
    public static final class ChaosNotice {
        private final Chaos kind;
        private final String text;

        ChaosNotice(Chaos kind, String text) {
            this.kind = kind;
            this.text = text;
        }

        public Chaos getKind() {
            return kind;
        }

        /** A sentence saying what happened and for how long. */
        public String getText() {
            return text;
        }
    }

    /** A piece that has left the board, with what it looked like. */
    private static final class Lost {
        final Piece piece;
        final PieceType looks;

        Lost(Piece piece, PieceType looks) {
            this.piece = piece;
            this.looks = looks;
        }
    }

    private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

    /** What a piece may be transformed into. */
    private static final PieceType[] IDENTITIES = {
        PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN
    };

    /** After this many half-moves without a capture or pawn move the game is drawn. */
    private static final int HALFMOVES_FOR_DRAW = 100;

    /** The chance of a disaster after each move in angry chaotic mode. */
    private static final double CHAOS_CHANCE = 0.22;

    /** In the worst storm, sliding pieces reach this many squares less than the mildest. */
    private static final int REACH_IN_CALM = 6;

    /** How far sliding pieces reach under the short-reach debuff. */
    private static final int SHORT_REACH = 2;

    /** How long a jailed piece is held, in half-moves: two turns of its owner. */
    private static final int JAIL_PLIES = 4;

    /** The kinds a roll call can single out. */
    private static final PieceType[] ROLL_CALL_KINDS = {
        PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK
    };

    private final CheatLevel level;
    private final Random random;

    /** Where the pieces really are, and what they really are. */
    private Board board = boardOf(START, null, 0);

    /** What the piece on each square looks like, as looks[rank][file]; null on an empty square. */
    private final PieceType[][] looks = new PieceType[Board.SIZE][Board.SIZE];

    private final SquareState[][] squareStates = new SquareState[Board.SIZE][Board.SIZE];

    /** How many more half-moves each square's state lasts. */
    private final int[][] squarePlies = new int[Board.SIZE][Board.SIZE];

    private int stormSeverity;
    private int stormPlies;

    /** During a roll call, the only kind of piece (by its looks) that may move; otherwise null. */
    private PieceType rollCallKind;
    private int rollCallPlies;
    private int ceasefirePlies;
    private int fogPlies;

    /** A piece locked up as a reward for catching it, and for how many more half-moves. */
    private Position jailed;
    private int jailPlies;

    private Phase phase = Phase.SETUP;
    private PieceColor turn = PieceColor.WHITE;

    // One entry per colour, indexed by PieceColor.ordinal().
    private final boolean[] ready = new boolean[2];
    private final int[] cheatPoints;
    private final int[] switchTokens;
    private final int[] challengesLeft;
    private final boolean[] disruptUsed = new boolean[2];
    private final Debuff[] debuffs = new Debuff[2];
    private final int[] debuffTurns = new int[2];
    private final boolean[] fiddledClock = new boolean[2];
    private final boolean[] extraMove = new boolean[2];
    private final List<List<Lost>> lostPieces = List.of(new ArrayList<>(), new ArrayList<>());

    private Move lastMove;
    private boolean lastMoveLookedLegal = true;
    private boolean lastMoverFiddledClock;
    private boolean accusedThisTurn;
    private boolean clockInUse;
    private Penalty lastPenalty;
    private Debuff lastDebuff;
    private Position lastExposedOwn;
    private ChaosNotice chaosNotice;
    private PieceColor winner;

    private final List<Runnable> listeners = new ArrayList<>();

    public CheatGame(CheatLevel level) {
        this(level, new Random());
    }

    /**
     * @param random decides the disasters and debuffs; a seeded one makes a game repeatable
     */
    public CheatGame(CheatLevel level, Random random) {
        this.level = level;
        this.random = random;
        cheatPoints = new int[] {level.getCheatPoints(), level.getCheatPoints()};
        switchTokens = new int[] {level.getSwitchTokens(), level.getSwitchTokens()};
        challengesLeft = new int[] {level.getChallengeLimit(), level.getChallengeLimit()};
        for (int rank = 0; rank < Board.SIZE; rank++) {
            for (int file = 0; file < Board.SIZE; file++) {
                Piece piece = board.getPiece(new Position(file, rank));
                looks[rank][file] = piece == null ? null : piece.getType();
                squareStates[rank][file] = SquareState.NORMAL;
            }
        }
    }

    // ---------------------------------------------------------------- state

    public CheatLevel getLevel() {
        return level;
    }

    public Phase getPhase() {
        return phase;
    }

    /** The side to move. Only meaningful once play has started. */
    public PieceColor getTurn() {
        return turn;
    }

    public boolean isOver() {
        return phase == Phase.OVER;
    }

    /** The side that captured the other's king, or null if the game is unfinished or drawn. */
    public PieceColor getWinner() {
        return winner;
    }

    /** Ends the game in favour of one side, for a reason outside the board such as running out of time. */
    public void declareWinner(PieceColor winningSide) {
        winner = winningSide;
        phase = Phase.OVER;
        changed();
    }

    /** The last move played, or null before the first. */
    public Move getLastMove() {
        return lastMove;
    }

    /** Registers something to run after every change to the game. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    // ----------------------------------------------------- who sees what

    /** The piece really standing on a square, or null. Only the piece's owner should be shown this. */
    public Piece truePiece(Position position) {
        return board.getPiece(position);
    }

    /** The piece on a square as it appears to everyone, or null if the square is empty. */
    public Piece looksPiece(Position position) {
        Piece piece = board.getPiece(position);
        return piece == null ? null : Piece.create(looks[position.getRank()][position.getFile()], piece.getColor());
    }

    /**
     * What a transformed piece really is, or null if the square is empty or
     * its piece is what it appears to be. Only the piece's owner should be
     * shown this.
     */
    public PieceType secretAt(Position position) {
        Piece piece = board.getPiece(position);
        return piece == null || piece.getType() == looks[position.getRank()][position.getFile()]
                ? null : piece.getType();
    }

    /**
     * The position as one side believes it to be: its own pieces as they
     * are and the other side's as they appear. This is all a player, human
     * or computer, has to go on.
     */
    public Board beliefBoard(PieceColor viewer) {
        return boardOf(position -> {
            Piece piece = board.getPiece(position);
            return piece == null || piece.getColor() == viewer ? piece : looksPiece(position);
        }, board.getEnPassantTarget(), board.getHalfmoveClock());
    }

    /**
     * True if, as far as this side can tell, its king is attacked. The
     * attacker may not be what it seems, in which case there may be no
     * danger at all.
     */
    public boolean looksInCheck(PieceColor viewer) {
        Position king = board.findKing(viewer);
        return king != null && beliefBoard(viewer).isSquareAttacked(king, viewer.opposite());
    }

    /** The pieces a side has lost, as they looked, in the order they went. */
    public List<Piece> lostBy(PieceColor color) {
        List<Piece> pieces = new ArrayList<>();
        for (Lost lost : lostPieces.get(color.ordinal())) {
            pieces.add(Piece.create(lost.looks, color));
        }
        return pieces;
    }

    // ---------------------------------------------------------------- setup

    /** How many cheat points a side has left to spend on transformations before the game. */
    public int getCheatPoints(PieceColor color) {
        return cheatPoints[color.ordinal()];
    }

    /** What a kind of piece is worth in cheat points: pawn 1, knight and bishop 3, rook 5, queen 9. */
    public static int pointsOf(PieceType type) {
        switch (type) {
            case QUEEN:
                return 9;
            case ROOK:
                return 5;
            case BISHOP:
            case KNIGHT:
                return 3;
            case PAWN:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * What it would cost in cheat points to give the piece on a square a
     * new identity: the new one's worth less what the piece is now.
     * Negative for a demotion, which gives points back.
     */
    public int transformCost(Position position, PieceType identity) {
        return pointsOf(identity) - pointsOf(board.getPiece(position).getType());
    }

    /** How many switch tokens a side has left to spend on rewards. */
    public int getSwitchTokens(PieceColor color) {
        return switchTokens[color.ordinal()];
    }

    /** The identities a piece can be given. */
    public static PieceType[] identities() {
        return IDENTITIES.clone();
    }

    /**
     * True if the square holds one of this side's pieces that may be
     * transformed: never the king, and pawns only in angry chaotic mode.
     */
    public boolean isTransformable(PieceColor color, Position position) {
        Piece piece = board.getPiece(position);
        if (piece == null || piece.getColor() != color || piece.getType() == PieceType.KING) {
            return false;
        }
        PieceType look = looks[position.getRank()][position.getFile()];
        return look != PieceType.KING && (look != PieceType.PAWN || level.allowsPawns());
    }

    /** True if this side may transform pieces now: during setup, or when spending a switch token. */
    public boolean mayTransformNow(PieceColor color) {
        return phase == Phase.SETUP && !ready[color.ordinal()]
                || phase == Phase.REWARD && color == turn && getSwitchTokens(color) > 0;
    }

    /**
     * True if this side may give this piece this identity now. It must be a
     * change and, before the game, the side must have the cheat points for
     * it. A transformation bought with a switch token costs no points.
     */
    public boolean canTransform(PieceColor color, Position position, PieceType identity) {
        boolean offered = false;
        for (PieceType type : IDENTITIES) {
            offered |= type == identity;
        }
        if (!offered || !mayTransformNow(color) || !isTransformable(color, position)
                || board.getPiece(position).getType() == identity) {
            return false;
        }
        return phase != Phase.SETUP || transformCost(position, identity) <= getCheatPoints(color);
    }

    /**
     * Secretly gives a piece a new identity. It moves as that from now on
     * and looks as it always did. Before the game this is paid for in cheat
     * points (or pays them back, for a demotion); as a reward it uses a
     * switch token.
     *
     * @throws IllegalArgumentException if {@link #canTransform} is false
     */
    public void transform(PieceColor color, Position position, PieceType identity) {
        if (!canTransform(color, position, identity)) {
            throw new IllegalArgumentException("Cannot make " + position + " a " + identity);
        }
        int cost = transformCost(position, identity);
        Board before = board;
        Piece transformed = Piece.create(identity, color);
        board = boardOf(square -> square.equals(position) ? transformed : before.getPiece(square),
                before.getEnPassantTarget(), before.getHalfmoveClock());
        if (phase == Phase.SETUP) {
            cheatPoints[color.ordinal()] -= cost;
        } else {
            switchTokens[color.ordinal()]--;
            phase = Phase.PLAY;  // the reward has been taken
        }
        changed();
    }

    /** Declares that a side has finished transforming. Play starts when both have. */
    public void finishSetup(PieceColor color) {
        if (phase != Phase.SETUP) {
            throw new IllegalStateException("The game has already started");
        }
        ready[color.ordinal()] = true;
        if (ready[0] && ready[1]) {
            phase = Phase.PLAY;
        }
        changed();
    }

    // ----------------------------------------------------------------- play

    /**
     * Every move available to the side to move. Pieces move by what they
     * really are, within whatever the board and any debuff allow. Moves
     * that leave the king open to capture are allowed.
     */
    public List<Move> moves() {
        return phase == Phase.PLAY ? allowedMoves(turn) : new ArrayList<>();
    }

    /** The moves of the piece on one square; empty unless it belongs to the side to move. */
    public List<Move> movesFrom(Position from) {
        Piece piece = board.getPiece(from);
        if (phase != Phase.PLAY || piece == null || piece.getColor() != turn) {
            return new ArrayList<>();
        }
        return onlyAllowed(board.pseudoLegalMovesFrom(from));
    }

    private List<Move> allowedMoves(PieceColor color) {
        return onlyAllowed(board.pseudoLegalMoves(color));
    }

    private List<Move> onlyAllowed(List<Move> candidates) {
        List<Move> allowed = new ArrayList<>();
        for (Move move : candidates) {
            if (isAllowed(move)) {
                allowed.add(move);
            }
        }
        return allowed;
    }

    /** Whether craters, lava, safe squares, the storm and the mover's debuff all permit a move. */
    private boolean isAllowed(Move move) {
        Position from = move.getFrom();
        Position to = move.getTo();
        PieceType type = move.getPiece().getType();
        Debuff debuff = debuffs[move.getPiece().getColor().ordinal()];
        if (isBlocked(to) || from.equals(jailed)) {
            return false;
        }
        boolean capturesBanned = ceasefirePlies > 0 || debuff == Debuff.NO_CAPTURES
                || getSquareState(to) == SquareState.SAFE;
        if (move.isCapture() && capturesBanned) {
            return false;
        }
        // A roll call goes by looks: everyone can see which pieces are moving.
        if (rollCallKind != null && looks[from.getRank()][from.getFile()] != rollCallKind) {
            return false;
        }
        int fileStep = Integer.signum(to.getFile() - from.getFile());
        int rankStep = Integer.signum(to.getRank() - from.getRank());
        int distance = Math.max(Math.abs(to.getFile() - from.getFile()), Math.abs(to.getRank() - from.getRank()));
        if (type != PieceType.KNIGHT) {
            // Everything but a knight travels over the squares in between.
            for (int step = 1; step < distance; step++) {
                if (isBlocked(new Position(from.getFile() + step * fileStep, from.getRank() + step * rankStep))) {
                    return false;
                }
            }
        }
        boolean slides = type == PieceType.QUEEN || type == PieceType.ROOK || type == PieceType.BISHOP;
        if (slides && stormSeverity > 0 && distance > getStormReach()
                || slides && debuff == Debuff.SHORT_REACH && distance > SHORT_REACH) {
            return false;
        }
        if (debuff == Debuff.NO_QUEEN && type == PieceType.QUEEN
                || debuff == Debuff.NO_PAWNS && type == PieceType.PAWN) {
            return false;
        }
        boolean sameColour = (from.getFile() + from.getRank()) % 2 == (to.getFile() + to.getRank()) % 2;
        return debuff != Debuff.SAME_COLOUR_SQUARES || sameColour;
    }

    private boolean isBlocked(Position position) {
        SquareState state = getSquareState(position);
        return state == SquareState.CRATER || state == SquareState.LAVA;
    }

    /**
     * Plays a move. Capturing the king wins.
     *
     * @throws IllegalArgumentException if the move is not one of {@link #moves()}
     */
    public void play(Move move) {
        if (!moves().contains(move)) {
            throw new IllegalArgumentException("Not a move in this position: " + move);
        }
        PieceColor mover = turn;
        Position from = move.getFrom();
        Position to = move.getTo();
        PieceType look = looks[from.getRank()][from.getFile()];

        lastMoveLookedLegal = looksLegal(move, look);
        lastMoverFiddledClock = fiddledClock[mover.ordinal()];
        fiddledClock[mover.ordinal()] = false;

        if (move.isCapture()) {
            // An en passant victim stands beside the capturer, not on the destination.
            Position victim = move.isEnPassant() ? new Position(to.getFile(), from.getRank()) : to;
            lostPieces.get(mover.opposite().ordinal())
                    .add(new Lost(move.getCaptured(), looks[victim.getRank()][victim.getFile()]));
            looks[victim.getRank()][victim.getFile()] = null;
        }
        board.makeMove(move);
        looks[from.getRank()][from.getFile()] = null;
        // A promotion happens in plain sight, so the new piece looks like what it is.
        looks[to.getRank()][to.getFile()] = move.isPromotion() ? move.getPromotion() : look;
        lastMove = move;
        accusedThisTurn = false;
        lastPenalty = null;
        lastDebuff = null;
        lastExposedOwn = null;
        if (to.equals(jailed) || from.equals(jailed)) {
            jailed = null;  // the prisoner has been captured
        }

        if (move.isCapture() && move.getCaptured().getType() == PieceType.KING) {
            winner = mover;
            phase = Phase.OVER;
        } else {
            endTurn(mover);
        }
        changed();
    }

    /**
     * Passes play on after a side's turn: time moves on for debuffs and
     * for the state of the board, a disaster may strike, and the other side
     * is to move. A side with no move available simply misses its turn.
     */
    private void endTurn(PieceColor finished) {
        passTime(finished);
        if (level.isChaotic() && random.nextDouble() < CHAOS_CHANCE) {
            strike(Chaos.values()[random.nextInt(Chaos.values().length)]);
        }
        if (extraMove[finished.ordinal()]) {
            // The reward for catching a cheat: the other side misses this turn.
            extraMove[finished.ordinal()] = false;
            passTime(finished.opposite());
            if (!allowedMoves(finished).isEmpty()) {
                return;
            }
        }
        turn = finished.opposite();
        if (board.getHalfmoveClock() >= HALFMOVES_FOR_DRAW) {
            phase = Phase.OVER;  // a draw
        } else if (allowedMoves(turn).isEmpty() && allowedMoves(finished).isEmpty() && rollCallKind != null) {
            // A roll call that nobody can answer is called off, rather than ending the game.
            rollCallKind = null;
            settleWhoMoves(finished);
        } else {
            settleWhoMoves(finished);
        }
    }

    /** With the turn passed on: a side with no move misses its turn, and if neither can move it is a draw. */
    private void settleWhoMoves(PieceColor finished) {
        if (allowedMoves(turn).isEmpty()) {
            passTime(turn);
            turn = finished;
            if (allowedMoves(turn).isEmpty()) {
                phase = Phase.OVER;  // nobody can move: a draw
            }
        }
    }

    private void passTime(PieceColor finished) {
        int side = finished.ordinal();
        if (debuffs[side] != null && --debuffTurns[side] <= 0) {
            debuffs[side] = null;
        }
        for (int rank = 0; rank < Board.SIZE; rank++) {
            for (int file = 0; file < Board.SIZE; file++) {
                if (squareStates[rank][file] != SquareState.NORMAL && --squarePlies[rank][file] <= 0) {
                    squareStates[rank][file] = SquareState.NORMAL;
                }
            }
        }
        if (stormSeverity > 0 && --stormPlies <= 0) {
            stormSeverity = 0;
        }
        if (rollCallKind != null && --rollCallPlies <= 0) {
            rollCallKind = null;
        }
        ceasefirePlies = Math.max(0, ceasefirePlies - 1);
        fogPlies = Math.max(0, fogPlies - 1);
        if (jailed != null && --jailPlies <= 0) {
            jailed = null;
        }
    }

    /**
     * Whether the move is one the piece could make if it were what it looks
     * like. Only the mover's own shape matters for that, so it is worked out
     * by standing a piece of that shape on the starting square.
     */
    private boolean looksLegal(Move move, PieceType look) {
        Position from = move.getFrom();
        Piece standIn = Piece.create(look, move.getPiece().getColor());
        Board asItLooks = boardOf(position -> position.equals(from) ? standIn : board.getPiece(position),
                board.getEnPassantTarget(), board.getHalfmoveClock());
        for (Move possible : asItLooks.pseudoLegalMovesFrom(from)) {
            if (possible.getTo().equals(move.getTo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if a move that could be played now is one its piece appears able
     * to make. A transformed piece's other moves give it away to anyone
     * watching, though it still takes a challenge to do anything about it.
     */
    public boolean wouldLookLegal(Move move) {
        Position from = move.getFrom();
        return looksLegal(move, looks[from.getRank()][from.getFile()]);
    }

    /**
     * True if the last move was one its piece appeared able to make. A move
     * that looks impossible proves the piece is transformed.
     */
    public boolean lastMoveLookedLegal() {
        return lastMoveLookedLegal;
    }

    // ------------------------------------------------------------ the clock

    /**
     * Records that a side has secretly added time to its clock. It counts
     * as cheating on that side's next move, so a challenge to that move
     * succeeds.
     */
    public void noteClockFiddled(PieceColor color) {
        fiddledClock[color.ordinal()] = true;
    }

    /**
     * Says whether the game is being played with a clock. Rewards and
     * penalties that add or take time are only on offer if it is.
     */
    public void setClockInUse(boolean inUse) {
        clockInUse = inUse;
    }

    /** True if the side that made the last move fiddled its clock beforehand. The clock is in plain view. */
    public boolean lastMoverFiddledClock() {
        return lastMoverFiddledClock;
    }

    // ----------------------------------------------------------- challenges

    /** How many challenges a side has left, or -1 if there is no limit. */
    public int getChallengesLeft(PieceColor color) {
        return challengesLeft[color.ordinal()];
    }

    /** True if the side to move may challenge the move just played: once per turn, before moving. */
    public boolean canAccuse() {
        return phase == Phase.PLAY && lastMove != null && !accusedThisTurn
                && lastMove.getPiece().getColor() != turn && getChallengesLeft(turn) != 0;
    }

    /**
     * The side to move challenges the move just played. It succeeds if the
     * piece that moved is transformed or its owner fiddled the clock. Then
     * the game waits for the challenger to take one of
     * {@link #availableRewards()}, or to {@link #declineReward()}.
     *
     * <p>If it fails, the challenger pays a {@link Penalty} drawn at random
     * from those that apply; see {@link #getLastPenalty()}.
     *
     * @return true if the challenge succeeded
     * @throws IllegalStateException if {@link #canAccuse()} is false
     */
    public boolean accuse() {
        if (!canAccuse()) {
            throw new IllegalStateException("There is nothing to challenge");
        }
        accusedThisTurn = true;
        if (challengesLeft[turn.ordinal()] > 0) {
            challengesLeft[turn.ordinal()]--;
        }
        boolean caught = secretAt(lastMove.getTo()) != null || lastMoverFiddledClock;
        if (caught) {
            phase = Phase.REWARD;
        } else {
            punish(turn);
        }
        changed();
        return caught;
    }

    /** Draws a penalty for a failed challenge and applies it. */
    private void punish(PieceColor challenger) {
        List<Penalty> options = new ArrayList<>();
        if (level.isChaotic()) {
            // Twice, so that a debuff is the likeliest outcome in angry chaotic mode.
            options.add(Penalty.DEBUFF);
            options.add(Penalty.DEBUFF);
        } else {
            options.add(Penalty.LOSE_TURN);
        }
        List<Position> ownSecrets = hiddenPieces(challenger);
        if (clockInUse) {
            options.add(Penalty.LOSE_TIME);
        }
        if (!ownSecrets.isEmpty()) {
            options.add(Penalty.EXPOSED);
        }
        if (getSwitchTokens(challenger) > 0) {
            options.add(Penalty.LOSE_TOKEN);
        }
        lastPenalty = options.get(random.nextInt(options.size()));

        if (lastPenalty == Penalty.DEBUFF) {
            lastDebuff = Debuff.values()[random.nextInt(Debuff.values().length)];
            afflict(challenger, lastDebuff, 1 + random.nextInt(2));
        } else if (lastPenalty == Penalty.EXPOSED) {
            lastExposedOwn = pickOne(ownSecrets);
            expose(lastExposedOwn);
        } else if (lastPenalty == Penalty.LOSE_TOKEN) {
            switchTokens[challenger.ordinal()]--;
        } else if (lastPenalty == Penalty.LOSE_TURN) {
            endTurn(challenger);
        }
        // LOSE_TIME is carried out by whoever owns the clock.
    }

    /** Puts a side under a debuff for a number of its own turns. */
    void afflict(PieceColor color, Debuff debuff, int turns) {
        debuffs[color.ordinal()] = debuff;
        debuffTurns[color.ordinal()] = turns;
    }

    /** The debuff a side is under, or null. */
    public Debuff getDebuff(PieceColor color) {
        return debuffs[color.ordinal()];
    }

    /** What the last failed challenge cost, or null if there has not been one since the last move. */
    public Penalty getLastPenalty() {
        return lastPenalty;
    }

    /** The debuff the last failed challenge brought, if its penalty was one; otherwise null. */
    public Debuff getLastDebuff() {
        return lastDebuff;
    }

    /** The challenger's own piece that the last failed challenge exposed, if that was its penalty; otherwise null. */
    public Position getLastExposedOwn() {
        return lastExposedOwn;
    }

    /** The rewards the successful challenger can actually take; empty if there is nothing to be had. */
    public List<Reward> availableRewards() {
        List<Reward> rewards = new ArrayList<>();
        if (phase != Phase.REWARD) {
            return rewards;
        }
        List<Position> hidden = hiddenPieces(turn.opposite());
        boolean caughtPieceIsHidden = secretAt(lastMove.getTo()) != null;
        if (!hidden.isEmpty()) {
            rewards.add(Reward.EXPOSE);
        }
        if (caughtPieceIsHidden) {
            rewards.add(Reward.STRIP);
        }
        if (board.getPiece(lastMove.getTo()) != null) {
            rewards.add(Reward.JAIL);
        }
        if (getSwitchTokens(turn) > 0 && !transformablePieces(turn).isEmpty()) {
            rewards.add(Reward.SWITCH);
        }
        rewards.add(Reward.EXTRA_MOVE);
        if (clockInUse) {
            rewards.add(Reward.STEAL_TIME);
        }
        if (level.isChaotic() && !disruptUsed[turn.ordinal()]) {
            if (hidden.size() > 1 || hidden.size() == 1 && !hidden.get(0).equals(lastMove.getTo())) {
                rewards.add(Reward.DISRUPT_EXPOSE);
            }
            rewards.add(Reward.DISRUPT_SAFE);
            if (!lostPieces.get(turn.ordinal()).isEmpty() && emptyHomeSquare(turn) != null) {
                rewards.add(Reward.DISRUPT_UNCAPTURE);
            }
        }
        return rewards;
    }

    private void requireReward(Reward reward) {
        if (!availableRewards().contains(reward)) {
            throw new IllegalStateException("That reward is not available: " + reward);
        }
    }

    /**
     * Reward: the challenged piece is shown for what it is from now on. If
     * it was honest and only the clock was fiddled, another hidden piece is
     * exposed in its place.
     *
     * @return the square of the piece exposed
     */
    public Position exposeAccused() {
        requireReward(Reward.EXPOSE);
        Position square = secretAt(lastMove.getTo()) != null
                ? lastMove.getTo() : pickOne(hiddenPieces(turn.opposite()));
        expose(square);
        phase = Phase.PLAY;
        changed();
        return square;
    }

    /**
     * Reward: the challenged piece loses its secret for good and becomes
     * the piece it looks like.
     */
    public void stripAccused() {
        requireReward(Reward.STRIP);
        Position square = lastMove.getTo();
        Piece honest = looksPiece(square);
        Board before = board;
        board = boardOf(position -> position.equals(square) ? honest : before.getPiece(position),
                before.getEnPassantTarget(), before.getHalfmoveClock());
        phase = Phase.PLAY;
        changed();
    }

    /** Reward: the challenged piece cannot move for its owner's next two turns. */
    public void jailAccused() {
        requireReward(Reward.JAIL);
        jailed = lastMove.getTo();
        jailPlies = JAIL_PLIES;
        phase = Phase.PLAY;
        changed();
    }

    /** Reward: after the challenger's coming move, the other side misses its turn. */
    public void takeExtraMove() {
        requireReward(Reward.EXTRA_MOVE);
        extraMove[turn.ordinal()] = true;
        phase = Phase.PLAY;
        changed();
    }

    /**
     * Reward: time passes from the cheat's clock to the challenger's. The
     * game only records that the reward was taken; whoever owns the clock
     * moves the time.
     */
    public void stealTime() {
        requireReward(Reward.STEAL_TIME);
        phase = Phase.PLAY;
        changed();
    }

    /** The square of a piece locked up by a reward, or null if none is. */
    public Position getJailedSquare() {
        return jailed;
    }

    /**
     * Disrupt reward: a hidden enemy piece other than the challenged one is exposed.
     *
     * @return the square of the piece exposed
     */
    public Position disruptExposeAnother() {
        requireReward(Reward.DISRUPT_EXPOSE);
        List<Position> others = hiddenPieces(turn.opposite());
        others.remove(lastMove.getTo());
        Position square = pickOne(others);
        expose(square);
        finishDisrupt();
        return square;
    }

    /** Disrupt reward: for the next two or three turns, a piece on this square cannot be captured. */
    public void disruptSafeSquare(Position position) {
        requireReward(Reward.DISRUPT_SAFE);
        setSquareState(position, SquareState.SAFE, randomDuration());
        finishDisrupt();
    }

    /**
     * Disrupt reward: the challenger's most valuable lost piece comes back
     * on an empty square of its home ranks.
     *
     * @return the square it returns to
     */
    public Position disruptUncapture() {
        requireReward(Reward.DISRUPT_UNCAPTURE);
        List<Lost> lost = lostPieces.get(turn.ordinal());
        Lost best = lost.get(0);
        for (Lost candidate : lost) {
            if (MaterialEvaluator.valueOf(candidate.piece.getType())
                    > MaterialEvaluator.valueOf(best.piece.getType())) {
                best = candidate;
            }
        }
        lost.remove(best);
        Position square = emptyHomeSquare(turn);
        Piece returning = best.piece;
        Board before = board;
        board = boardOf(position -> position.equals(square) ? returning : before.getPiece(position),
                before.getEnPassantTarget(), before.getHalfmoveClock());
        looks[square.getRank()][square.getFile()] = best.looks;
        finishDisrupt();
        return square;
    }

    /** Takes no reward and gets on with the game. */
    public void declineReward() {
        if (phase != Phase.REWARD) {
            throw new IllegalStateException("There is no reward to decline");
        }
        phase = Phase.PLAY;
        changed();
    }

    private void finishDisrupt() {
        disruptUsed[turn.ordinal()] = true;
        phase = Phase.PLAY;
        changed();
    }

    private void expose(Position square) {
        looks[square.getRank()][square.getFile()] = board.getPiece(square).getType();
    }

    private List<Position> hiddenPieces(PieceColor color) {
        List<Position> hidden = new ArrayList<>();
        for (Position position : allSquares()) {
            Piece piece = board.getPiece(position);
            if (piece != null && piece.getColor() == color && secretAt(position) != null) {
                hidden.add(position);
            }
        }
        return hidden;
    }

    /** The squares of the pieces a side could transform. */
    public List<Position> transformablePieces(PieceColor color) {
        List<Position> pieces = new ArrayList<>();
        for (Position position : allSquares()) {
            if (isTransformable(color, position)) {
                pieces.add(position);
            }
        }
        return pieces;
    }

    /** An empty, usable square on a side's first two ranks, back rank first; null if there is none. */
    private Position emptyHomeSquare(PieceColor color) {
        int back = color == PieceColor.WHITE ? 0 : Board.SIZE - 1;
        int forward = color == PieceColor.WHITE ? 1 : -1;
        for (int rank : new int[] {back, back + forward}) {
            for (int file = 0; file < Board.SIZE; file++) {
                Position position = new Position(file, rank);
                if (board.isEmpty(position) && !isBlocked(position)) {
                    return position;
                }
            }
        }
        return null;
    }

    private Position pickOne(List<Position> positions) {
        return positions.get(random.nextInt(positions.size()));
    }

    // ------------------------------------------------------------ disasters

    /** What has happened to a square: nothing, a crater, lava, or made safe. */
    public SquareState getSquareState(Position position) {
        return squareStates[position.getRank()][position.getFile()];
    }

    private void setSquareState(Position position, SquareState state, int plies) {
        squareStates[position.getRank()][position.getFile()] = state;
        squarePlies[position.getRank()][position.getFile()] = plies;
    }

    /** How hard it is storming, from 1 (light) to 5 (severe); 0 if it is not. */
    public int getStormSeverity() {
        return stormSeverity;
    }

    /** The furthest a queen, rook or bishop can slide in the current storm. */
    public int getStormReach() {
        return REACH_IN_CALM - stormSeverity;
    }

    /** During a roll call, the only kind of piece (by its looks) that may move; otherwise null. */
    public PieceType getRollCallKind() {
        return rollCallKind;
    }

    /** True while a ceasefire forbids every capture. */
    public boolean isCeasefire() {
        return ceasefirePlies > 0;
    }

    /** True while fog hides the middle of the board. */
    public boolean isFoggy() {
        return fogPlies > 0;
    }

    /** The disaster that has just struck, if any; asking clears it, so each is announced once. */
    public ChaosNotice takeChaosNotice() {
        ChaosNotice notice = chaosNotice;
        chaosNotice = null;
        return notice;
    }

    /** Two or three turns, counted in half-moves. */
    private int randomDuration() {
        return 2 * (2 + random.nextInt(2));
    }

    /** Makes a disaster happen. The game calls this at random in angry chaotic mode. */
    void strike(Chaos kind) {
        int plies = randomDuration();
        String forTurns = " for " + plies / 2 + " turns.";
        if (kind == Chaos.STORM) {
            stormSeverity = 1 + random.nextInt(5);
            stormPlies = plies;
            chaosNotice = new ChaosNotice(kind, "STORM, force " + stormSeverity + ": nothing slides more than "
                    + getStormReach() + (getStormReach() == 1 ? " square" : " squares") + forTurns);
        } else if (kind == Chaos.ROCKET) {
            // A two-by-two blast. Its corner is chosen so that the whole of it is on the board.
            int file = random.nextInt(Board.SIZE - 1);
            int rank = random.nextInt(Board.SIZE - 1);
            List<Position> blast = List.of(new Position(file, rank), new Position(file + 1, rank),
                    new Position(file, rank + 1), new Position(file + 1, rank + 1));
            ruin(blast, SquareState.CRATER, plies);
            chaosNotice = new ChaosNotice(kind, "ROCKET! " + blast.get(0) + " to " + blast.get(3)
                    + " is a crater" + forTurns);
        } else if (kind == Chaos.ROLL_CALL) {
            rollCallKind = ROLL_CALL_KINDS[random.nextInt(ROLL_CALL_KINDS.length)];
            rollCallPlies = plies;
            String name = rollCallKind.name().toLowerCase();
            chaosNotice = new ChaosNotice(kind, "ROLL CALL: only " + name + "s may move" + forTurns
                    + " No " + name + "s to move? Miss your turn.");
        } else if (kind == Chaos.CEASEFIRE) {
            ceasefirePlies = plies;
            chaosNotice = new ChaosNotice(kind, "CEASEFIRE: nothing may be captured" + forTurns);
        } else if (kind == Chaos.TIME_WARP) {
            chaosNotice = new ChaosNotice(kind, "TIME WARP: the two clocks have swapped.");
        } else if (kind == Chaos.HACKED) {
            // Nothing changes in the game itself: the damage is all on the player's screen.
            chaosNotice = new ChaosNotice(kind, "YOU HAVE BEEN HACKED. The pieces will not hold still "
                    + "until you restore the firewall.");
        } else if (kind == Chaos.FOG) {
            fogPlies = plies;
            chaosNotice = new ChaosNotice(kind, "FOG: the middle of the board is hidden" + forTurns);
        } else {
            // The wheel: light or dark, and a file.
            boolean dark = random.nextBoolean();
            int file = random.nextInt(Board.SIZE);
            List<Position> flooded = new ArrayList<>();
            for (int rank = 0; rank < Board.SIZE; rank++) {
                if (((file + rank) % 2 == 0) == dark) {  // a1 is dark
                    flooded.add(new Position(file, rank));
                }
            }
            ruin(flooded, SquareState.LAVA, plies);
            chaosNotice = new ChaosNotice(kind, "THE FLOOR IS LAVA: the " + (dark ? "dark" : "light")
                    + " squares of file " + (char) ('a' + file) + forTurns);
        }
    }

    /**
     * Turns squares into craters or lava. Any piece standing there is lost.
     * A square with a king on it, or one made safe, is spared: the game
     * should be decided by the players, not by the weather.
     */
    private void ruin(List<Position> squares, SquareState state, int plies) {
        List<Position> ruined = new ArrayList<>();
        for (Position position : squares) {
            Piece piece = board.getPiece(position);
            boolean king = piece != null && piece.getType() == PieceType.KING;
            if (king || getSquareState(position) == SquareState.SAFE) {
                continue;
            }
            if (piece != null) {
                lostPieces.get(piece.getColor().ordinal())
                        .add(new Lost(piece, looks[position.getRank()][position.getFile()]));
                looks[position.getRank()][position.getFile()] = null;
            }
            setSquareState(position, state, plies);
            ruined.add(position);
        }
        Board before = board;
        board = boardOf(position -> ruined.contains(position) ? null : before.getPiece(position),
                before.getEnPassantTarget(), before.getHalfmoveClock());
    }

    // -------------------------------------------------------------- helpers

    private static List<Position> allSquares() {
        List<Position> squares = new ArrayList<>();
        for (int rank = 0; rank < Board.SIZE; rank++) {
            for (int file = 0; file < Board.SIZE; file++) {
                squares.add(new Position(file, rank));
            }
        }
        return squares;
    }

    private static Board boardOf(String placement, Position enPassant, int halfmoveClock) {
        // No castling rights ("-"): castling is left out of cheating chess.
        String target = enPassant == null ? "-" : enPassant.toString();
        return Game.fromFen(placement + " w - " + target + " " + halfmoveClock + " 1").getBoard();
    }

    /** Builds a board holding whatever piece the function gives for each square. */
    private static Board boardOf(Function<Position, Piece> pieces, Position enPassant, int halfmoveClock) {
        StringBuilder placement = new StringBuilder();
        for (int rank = Board.SIZE - 1; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < Board.SIZE; file++) {
                Piece piece = pieces.apply(new Position(file, rank));
                if (piece == null) {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    placement.append(empty);
                    empty = 0;
                }
                placement.append(piece.getSymbol());
            }
            if (empty > 0) {
                placement.append(empty);
            }
            if (rank > 0) {
                placement.append('/');
            }
        }
        return boardOf(placement.toString(), enPassant, halfmoveClock);
    }
}
