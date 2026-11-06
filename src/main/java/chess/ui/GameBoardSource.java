package chess.ui;

import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.Position;

import java.util.List;

/** Draws an ordinary game of chess: everything is as it appears and only legal moves are offered. */
public class GameBoardSource implements BoardSource {

    private final Game game;

    public GameBoardSource(Game game) {
        this.game = game;
    }

    @Override
    public Piece pieceAt(Position position) {
        return game.getBoard().getPiece(position);
    }

    @Override
    public boolean canSelect(Position position) {
        Piece piece = pieceAt(position);
        boolean gameOver = game.legalMoves().isEmpty();
        return piece != null && piece.getColor() == game.getTurn() && !gameOver;
    }

    @Override
    public List<Move> movesFrom(Position from) {
        return game.legalMovesFrom(from);
    }

    @Override
    public Move getLastMove() {
        return game.getLastMove();
    }

    @Override
    public Position getAlarmSquare() {
        return game.isInCheck() ? game.getBoard().findKing(game.getTurn()) : null;
    }

    @Override
    public String getBanner() {
        GameStatus status = game.getStatus();
        if (status == GameStatus.CHECKMATE) {
            return "CHECKMATE";
        }
        if (status == GameStatus.STALEMATE) {
            return "STALEMATE";
        }
        return status.isDraw() ? "DRAW" : null;
    }
}
