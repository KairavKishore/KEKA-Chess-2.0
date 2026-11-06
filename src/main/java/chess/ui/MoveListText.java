package chess.ui;

import chess.ai.MaterialEvaluator;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.PieceColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The wording of the move list and the captured pieces. Kept apart from JavaFX so it can be tested without a window. */
final class MoveListText {

    private MoveListText() {
    }

    /**
     * The game so far, one line for each move by both sides, such as
     * "1. e2e4 e7e5". If Black made the first move recorded, White's half of
     * the first line is shown as "...".
     */
    static List<String> lines(List<Move> moves) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = null;
        for (Move move : moves) {
            boolean white = move.getPiece().getColor() == PieceColor.WHITE;
            if (white || line == null) {
                if (line != null) {
                    lines.add(line.toString());
                }
                line = new StringBuilder((lines.size() + 1) + ". ");
                line.append(white ? move.toString() : "... " + move);
            } else {
                line.append(' ').append(move);
            }
        }
        if (line != null) {
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * The pieces one side has captured, as chess symbols, most valuable
     * first; empty if it has captured nothing.
     */
    static String capturedBy(PieceColor side, List<Move> moves) {
        List<Piece> captured = new ArrayList<>();
        for (Move move : moves) {
            if (move.isCapture() && move.getPiece().getColor() == side) {
                captured.add(move.getCaptured());
            }
        }
        captured.sort(Comparator.comparingInt((Piece piece) -> MaterialEvaluator.valueOf(piece.getType())).reversed());
        StringBuilder text = new StringBuilder();
        for (Piece piece : captured) {
            text.append(PieceGlyphs.of(piece.getType()));
        }
        return text.toString();
    }
}
