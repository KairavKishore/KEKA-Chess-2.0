package chess.ui;

import chess.engine.PieceColor;
import chess.engine.PieceType;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/** How a piece is painted: a chess symbol shaded from top to bottom, outlined, with a shadow behind it. */
final class PieceArt {

    private static final LinearGradient WHITE_FILL = topToBottom("#ffffff", "#e6d7a8");
    private static final LinearGradient BLACK_FILL = topToBottom("#4a4a4a", "#050505");
    private static final Color WHITE_PIECE_OUTLINE = Color.web("#111111");

    private PieceArt() {
    }

    private static LinearGradient topToBottom(String top, String bottom) {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(top)), new Stop(1, Color.web(bottom)));
    }

    /** Makes {@code text} show the given piece at the given size, in the theme's colours. */
    static void paint(Text text, PieceType type, PieceColor color, double size, BoardTheme theme) {
        boolean white = color == PieceColor.WHITE;
        text.setText(PieceGlyphs.of(type));
        text.setFont(Font.font("DejaVu Sans", size));
        text.setFill(white ? WHITE_FILL : BLACK_FILL);
        // The outline is what makes a white piece show on a light square and
        // a black piece on a dark one.
        text.setStroke(white ? WHITE_PIECE_OUTLINE : theme.getBlackPieceOutline());
        // Thin on black pieces: a thick light outline fills in the fine
        // lines of the queen and king and makes them look hollow.
        text.setStrokeWidth(white ? 1.2 : 0.45);
        text.setEffect(new DropShadow(size * 0.12, size * 0.04, size * 0.07, Color.rgb(0, 0, 0, 0.55)));
    }
}
