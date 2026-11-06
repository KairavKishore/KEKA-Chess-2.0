package chess.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A game in progress: a board plus whose turn it is, the moves played so
 * far, and the rules that need all of those (which moves are legal, check,
 * checkmate and the draws).
 *
 * <p>This is the class the user interface and the AI talk to. They ask for
 * the legal moves, choose one, and hand it back to {@link #makeMove(Move)}.
 *
 * <p>Undo and redo follow the Command pattern. A {@link Move} is the command:
 * an object holding everything needed to carry an action out
 * ({@link Board#makeMove}) and to reverse it ({@link Board#undoMove}). The
 * game keeps the moves it has played on one stack and the moves it has taken
 * back on another.
 *
 * <p>A game also tells its {@link GameListener}s whenever it changes (the
 * Observer pattern), which is how a board on screen knows to redraw.
 */
public class Game {

    /** The same position this many times is a draw. */
    private static final int REPETITIONS_FOR_DRAW = 3;

    /** This many half-moves without a capture or pawn move is a draw: fifty moves by each side. */
    private static final int HALFMOVES_FOR_DRAW = 100;

    private final Board board;
    private PieceColor turn;
    private int fullmoveNumber;

    /** Moves played, most recent on top. */
    private final Deque<Move> undoStack = new ArrayDeque<>();

    /** Moves taken back, the next one to replay on top. Emptied when a new move is played. */
    private final Deque<Move> redoStack = new ArrayDeque<>();

    /**
     * A key for every position the game has been through, ending with the
     * current one, for spotting repetitions.
     */
    private final List<String> positionKeys = new ArrayList<>();

    /** Told about every change to the game. */
    private final List<GameListener> listeners = new ArrayList<>();

    /** Starts a new game from the standard position with White to move. */
    public Game() {
        this(Board.startingPosition(), PieceColor.WHITE);
    }

    /** Starts from any position, with the given side to move. */
    public Game(Board board, PieceColor turn) {
        this(board, turn, 1);
    }

    /**
     * @param fullmoveNumber the number of the move about to be played: 1 at
     *                       the start, going up after each of Black's moves
     */
    Game(Board board, PieceColor turn, int fullmoveNumber) {
        this.board = Objects.requireNonNull(board, "board");
        this.turn = Objects.requireNonNull(turn, "turn");
        this.fullmoveNumber = fullmoveNumber;
        positionKeys.add(Fen.positionKey(board, turn));
    }

    /**
     * Starts from a position written in FEN, for example
     * {@code Game.fromFen("8/8/8/4k3/8/8/4K3/R7 w - - 0 1")}. See {@link Fen}.
     *
     * @throws IllegalArgumentException if the text is not valid FEN
     */
    public static Game fromFen(String fen) {
        return Fen.parse(fen);
    }

    /** The current position in FEN. See {@link Fen}. */
    public String toFen() {
        return Fen.format(this);
    }

    /**
     * The current position. Read from it freely; change it only through this
     * class's {@link #makeMove(Move)}, {@link #undo()} and {@link #redo()}.
     */
    public Board getBoard() {
        return board;
    }

    /** The side whose move it is. */
    public PieceColor getTurn() {
        return turn;
    }

    /** The number of the move about to be played: 1 at the start, going up after each of Black's moves. */
    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    /**
     * Every move available to the side whose turn it is. Empty when the game
     * is over, including when it has been drawn.
     */
    public List<Move> legalMoves() {
        if (drawByRule() != null) {
            return new ArrayList<>();
        }
        return board.legalMoves(turn);
    }

    /**
     * The moves available to the piece on one square. Empty if the square is
     * empty or holds a piece of the side that is not on move, or if the game
     * is over.
     */
    public List<Move> legalMovesFrom(Position from) {
        Piece piece = board.getPiece(from);
        if (piece == null || piece.getColor() != turn || drawByRule() != null) {
            return new ArrayList<>();
        }
        return board.legalMovesFrom(from);
    }

    /** True if the side to move is in check (which includes checkmate). */
    public boolean isInCheck() {
        return board.isInCheck(turn);
    }

    /** Whether the game is still going, and if not, how it ended. */
    public GameStatus getStatus() {
        // Checkmate and stalemate come first: a mating move ends the game
        // even if it is also the hundredth half-move or a third repetition.
        if (board.legalMoves(turn).isEmpty()) {
            return isInCheck() ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        GameStatus draw = drawByRule();
        if (draw != null) {
            return draw;
        }
        return isInCheck() ? GameStatus.CHECK : GameStatus.IN_PROGRESS;
    }

    /** The draw rule that has ended the game, or null if none has. Does not cover stalemate. */
    private GameStatus drawByRule() {
        if (board.hasInsufficientMaterial()) {
            return GameStatus.DRAW_BY_INSUFFICIENT_MATERIAL;
        }
        if (board.getHalfmoveClock() >= HALFMOVES_FOR_DRAW) {
            return GameStatus.DRAW_BY_FIFTY_MOVES;
        }
        String current = positionKeys.get(positionKeys.size() - 1);
        if (Collections.frequency(positionKeys, current) >= REPETITIONS_FOR_DRAW) {
            return GameStatus.DRAW_BY_REPETITION;
        }
        return null;
    }

    /**
     * Plays a move and passes the turn to the other side. Any moves that had
     * been undone can no longer be redone.
     *
     * @throws IllegalArgumentException if the move is not one of {@link #legalMoves()}
     */
    public void makeMove(Move move) {
        if (!legalMoves().contains(move)) {
            throw new IllegalArgumentException("Illegal move: " + move);
        }
        play(move);
        redoStack.clear();
        notifyListeners();
    }

    private void play(Move move) {
        board.makeMove(move);
        undoStack.push(move);
        if (turn == PieceColor.BLACK) {
            fullmoveNumber++;
        }
        turn = turn.opposite();
        positionKeys.add(Fen.positionKey(board, turn));
    }

    /** True if a move has been played that {@link #undo()} could take back. */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** True if a move has been taken back that {@link #redo()} could replay. */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Takes back the last move, so that it is the same side's turn again.
     * The move can be replayed with {@link #redo()}.
     *
     * @return the move that was taken back
     * @throws IllegalStateException if there is no move to undo
     */
    public Move undo() {
        if (!canUndo()) {
            throw new IllegalStateException("There is no move to undo");
        }
        Move move = undoStack.pop();
        board.undoMove(move);
        positionKeys.remove(positionKeys.size() - 1);
        turn = turn.opposite();
        if (turn == PieceColor.BLACK) {
            fullmoveNumber--;
        }
        redoStack.push(move);
        notifyListeners();
        return move;
    }

    /**
     * Replays the move most recently taken back by {@link #undo()}.
     *
     * @return the move that was replayed
     * @throws IllegalStateException if there is no move to redo
     */
    public Move redo() {
        if (!canRedo()) {
            throw new IllegalStateException("There is no move to redo");
        }
        Move move = redoStack.pop();
        play(move);
        notifyListeners();
        return move;
    }

    /** Registers a listener to be told after every move, undo and redo. */
    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Stops telling a listener about changes. Does nothing if it was not registered. */
    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        // Iterate over a copy, so that a listener may remove itself while being told.
        for (GameListener listener : new ArrayList<>(listeners)) {
            listener.gameChanged(this);
        }
    }

    /** The last move played, or null at the start of the game. Useful for highlighting it. */
    public Move getLastMove() {
        return undoStack.peek();
    }

    /** Every move played so far, in the order they were played. */
    public List<Move> getMoveHistory() {
        List<Move> moves = new ArrayList<>(undoStack.size());
        for (Iterator<Move> oldestFirst = undoStack.descendingIterator(); oldestFirst.hasNext(); ) {
            moves.add(oldestFirst.next());
        }
        return moves;
    }
}
