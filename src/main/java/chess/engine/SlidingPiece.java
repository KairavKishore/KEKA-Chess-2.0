package chess.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * A piece that slides any number of squares in a straight line until
 * something is in the way: the rook, the bishop and the queen.
 *
 * <p>The three differ only in which directions they slide, so each subclass
 * just passes its directions to this class and inherits the rest.
 */
public abstract class SlidingPiece extends Piece {

    private final int[][] directions;

    /**
     * Called by the three subclasses to say which lines they slide along.
     *
     * @param directions the lines this piece slides along, as {file step, rank step}
     */
    protected SlidingPiece(PieceColor color, PieceType type, int[][] directions) {
        super(color, type);
        this.directions = directions;
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        for (int[] direction : directions) {
            int file = from.getFile() + direction[0];
            int rank = from.getRank() + direction[1];

            while (Position.isValid(file, rank)) {
                Position to = new Position(file, rank);
                Piece occupant = board.getPiece(to);

                if (occupant == null) {
                    moves.add(new Move(from, to, this, null));
                } else {
                    if (isEnemy(occupant)) {
                        moves.add(new Move(from, to, this, occupant));
                    }
                    break;  // any piece, friend or enemy, blocks the line
                }

                file += direction[0];
                rank += direction[1];
            }
        }
        return moves;
    }

    @Override
    public boolean attacks(Board board, Position from, Position target) {
        int fileStep = Integer.signum(target.getFile() - from.getFile());
        int rankStep = Integer.signum(target.getRank() - from.getRank());
        if (!slidesAlong(fileStep, rankStep)) {
            return false;
        }
        // Walk towards the target. Reaching it means nothing was in the way;
        // running off the board means it was never on this line.
        int file = from.getFile() + fileStep;
        int rank = from.getRank() + rankStep;
        while (Position.isValid(file, rank)) {
            Position square = new Position(file, rank);
            if (square.equals(target)) {
                return true;
            }
            if (!board.isEmpty(square)) {
                return false;
            }
            file += fileStep;
            rank += rankStep;
        }
        return false;
    }

    private boolean slidesAlong(int fileStep, int rankStep) {
        for (int[] direction : directions) {
            if (direction[0] == fileStep && direction[1] == rankStep) {
                return true;
            }
        }
        return false;
    }
}
