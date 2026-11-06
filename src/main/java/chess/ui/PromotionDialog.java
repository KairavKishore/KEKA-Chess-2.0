package chess.ui;

import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Window;

/**
 * Asks which piece a pawn should become. A pawn reaching the last rank has
 * four moves with the same start and end squares, one per piece, and only
 * the player can say which is meant.
 */
final class PromotionDialog {

    private static final double GLYPH_SIZE = 44;

    private PromotionDialog() {
    }

    /**
     * Shows the dialog and waits for an answer.
     *
     * @param owner      the window to appear over
     * @param candidates the promotion moves to choose between
     * @param theme      the colours to draw the pieces in
     * @return the chosen move, or null if the dialog was cancelled
     */
    static Move choose(Window owner, List<Move> candidates, BoardTheme theme) {
        Dialog<Move> dialog = create(candidates, theme);
        dialog.initOwner(owner);
        return dialog.showAndWait().orElse(null);
    }

    /** Builds the dialog without showing it: a button for each piece, and Cancel. */
    static Dialog<Move> create(List<Move> candidates, BoardTheme theme) {
        Dialog<Move> dialog = new Dialog<>();
        dialog.setTitle("Promotion");
        dialog.setHeaderText("Promote the pawn to:");

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER);
        for (Move candidate : candidates) {
            PieceColor color = candidate.getPiece().getColor();
            Text glyph = new Text();
            PieceArt.paint(glyph, candidate.getPromotion(), color, GLYPH_SIZE, theme);

            Button button = new Button();
            button.setGraphic(glyph);
            button.setAccessibleText(candidate.getPromotion().name().toLowerCase());
            button.setOnAction(event -> {
                dialog.setResult(candidate);
                dialog.close();
            });
            buttons.getChildren().add(button);
        }
        dialog.getDialogPane().setContent(buttons);
        dialog.getDialogPane().getStylesheets().add(ChessApp.STYLESHEET);

        // A dialog can only be closed if it has a button of a type like
        // Cancel, which leaves the result empty.
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        return dialog;
    }
}
