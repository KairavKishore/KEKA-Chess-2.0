package chess.ui;

import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.PieceColor;
import chess.player.Player;

/** The wording of the status bar. Kept apart from JavaFX so it can be tested without a window. */
final class StatusText {

    private StatusText() {
    }

    /**
     * One line saying whose turn it is, or how the game ended.
     *
     * @param toMove the player whose turn it is
     */
    static String describe(Game game, Player toMove) {
        String side = sideName(game.getTurn());
        GameStatus status = game.getStatus();
        switch (status) {
            case CHECKMATE:
                return "Checkmate. " + sideName(game.getTurn().opposite()) + " wins.";
            case STALEMATE:
                return "Stalemate: " + side + " has no legal move. The game is a draw.";
            case DRAW_BY_FIFTY_MOVES:
                return "Draw: fifty moves without a capture or pawn move.";
            case DRAW_BY_REPETITION:
                return "Draw: the same position three times.";
            case DRAW_BY_INSUFFICIENT_MATERIAL:
                return "Draw: not enough pieces left to checkmate.";
            default:
                String check = status == GameStatus.CHECK ? " " + side + " is in check." : "";
                String thinking = toMove.isHuman() ? "" : " " + toMove.getName() + " is thinking…";
                return side + " to move." + check + thinking;
        }
    }

    private static String sideName(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }
}
