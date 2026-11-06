package chess.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves straight ahead but captures diagonally, and is the only piece whose
 * direction depends on its colour: White's pawns go up the board, Black's go
 * down.
 */
public final class Pawn extends Piece {

    /** What a pawn can become on the last rank. The queen is first, as the usual choice. */
    private static final PieceType[] PROMOTIONS = {
        PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    public Pawn(PieceColor color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        boolean white = getColor() == PieceColor.WHITE;
        int forward = white ? 1 : -1;
        int startRank = white ? 1 : Board.SIZE - 2;

        int file = from.getFile();
        int rankAhead = from.getRank() + forward;
        if (!Position.isValid(file, rankAhead)) {
            return moves;  // already on the last rank
        }

        // Straight ahead: one square, or two from the starting rank.
        // Both need empty squares, since a pawn cannot capture forwards.
        Position oneAhead = new Position(file, rankAhead);
        if (board.isEmpty(oneAhead)) {
            addAdvance(from, oneAhead, null, moves);
            if (from.getRank() == startRank) {
                Position twoAhead = new Position(file, rankAhead + forward);
                if (board.isEmpty(twoAhead)) {
                    moves.add(new Move(from, twoAhead, this, null));
                }
            }
        }

        // Diagonally ahead, only onto an enemy piece.
        for (int side = -1; side <= 1; side += 2) {
            int captureFile = file + side;
            if (Position.isValid(captureFile, rankAhead)) {
                Position target = new Position(captureFile, rankAhead);
                Piece occupant = board.getPiece(target);
                if (isEnemy(occupant)) {
                    addAdvance(from, target, occupant, moves);
                } else if (target.equals(board.getEnPassantTarget())) {
                    // En passant: the target square is the empty one an enemy
                    // pawn has just skipped over. That pawn stands beside us,
                    // on our own rank, and is the piece that gets captured.
                    Piece beside = board.getPiece(new Position(captureFile, from.getRank()));
                    if (isEnemy(beside) && beside.getType() == PieceType.PAWN) {
                        moves.add(Move.enPassant(from, target, this, beside));
                    }
                }
            }
        }
        return moves;
    }

    @Override
    public boolean attacks(Board board, Position from, Position target) {
        int forward = getColor() == PieceColor.WHITE ? 1 : -1;
        return target.getRank() == from.getRank() + forward
                && Math.abs(target.getFile() - from.getFile()) == 1;
    }

    /**
     * Adds a move one rank forward (straight or capturing). On the last rank
     * the pawn must promote, so that becomes four moves, one per choice.
     */
    private void addAdvance(Position from, Position to, Piece captured, List<Move> moves) {
        boolean lastRank = to.getRank() == Board.homeRank(getColor().opposite());
        if (!lastRank) {
            moves.add(new Move(from, to, this, captured));
            return;
        }
        for (PieceType promotion : PROMOTIONS) {
            moves.add(new Move(from, to, this, captured, promotion));
        }
    }
}
