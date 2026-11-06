package chess.ui;

import chess.cheat.CheatGame;
import chess.cheat.CheatGame.Debuff;
import chess.cheat.CheatGame.SquareState;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a game of cheating chess as one side sees it. Every piece is drawn
 * as it appears; that side's own transformed pieces also get a badge
 * showing what they really are, and move accordingly.
 */
public class CheatBoardSource implements BoardSource {

    private final CheatGame game;
    private final PieceColor viewer;

    /**
     * @param viewer the side whose view of the board this is
     */
    public CheatBoardSource(CheatGame game, PieceColor viewer) {
        this.game = game;
        this.viewer = viewer;
    }

    @Override
    public Piece pieceAt(Position position) {
        return game.looksPiece(position);
    }

    @Override
    public boolean canSelect(Position position) {
        Piece piece = game.truePiece(position);
        return game.getPhase() == CheatGame.Phase.PLAY && game.getTurn() == viewer
                && piece != null && piece.getColor() == viewer;
    }

    @Override
    public List<Move> movesFrom(Position from) {
        return canSelect(from) ? game.movesFrom(from) : new ArrayList<>();
    }

    @Override
    public Move getLastMove() {
        return game.getLastMove();
    }

    /** The viewer's king, when it looks to be in check. Whether it really is, the viewer cannot know. */
    @Override
    public Position getAlarmSquare() {
        if (game.getPhase() != CheatGame.Phase.PLAY || !game.looksInCheck(viewer)) {
            return null;
        }
        for (int file = 0; file < chess.engine.Board.SIZE; file++) {
            for (int rank = 0; rank < chess.engine.Board.SIZE; rank++) {
                Position position = new Position(file, rank);
                Piece piece = game.truePiece(position);
                if (piece != null && piece.getColor() == viewer && piece.getType() == PieceType.KING) {
                    return position;
                }
            }
        }
        return null;
    }

    @Override
    public String getBanner() {
        if (!game.isOver()) {
            return null;
        }
        if (game.getWinner() == null) {
            return "DRAW";
        }
        return game.getWinner() == viewer ? "YOU WIN" : "YOU LOSE";
    }

    /** Only the viewer's own secrets are shown; the other side's are what the game is about. */
    @Override
    public PieceType secretAt(Position position) {
        Piece piece = game.truePiece(position);
        return piece != null && piece.getColor() == viewer ? game.secretAt(position) : null;
    }

    @Override
    public SquareState stateAt(Position position) {
        return game.getSquareState(position);
    }

    @Override
    public int getStormSeverity() {
        return game.getStormSeverity();
    }

    @Override
    public boolean isCheatingMove(Move move) {
        return !game.wouldLookLegal(move);
    }

    @Override
    public boolean marksCheatingMoves() {
        return true;
    }

    @Override
    public boolean isFoggy() {
        return game.isFoggy();
    }

    @Override
    public boolean isBlob() {
        return game.getDebuff(viewer) == Debuff.BLOB;
    }

    @Override
    public boolean isHalfDark() {
        return game.getDebuff(viewer) == Debuff.HALF_DARK;
    }

    @Override
    public boolean isBackwards() {
        return game.getDebuff(viewer) == Debuff.BACKWARDS;
    }
}
