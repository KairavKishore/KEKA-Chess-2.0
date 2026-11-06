package chess.ui;

import chess.engine.PieceColor;
import chess.engine.PieceType;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Window;

/** Asks the player to pick a kind of piece from a row of buttons: what a transformed piece should become. */
final class PieceChoiceDialog {

    private static final double GLYPH_SIZE = 44;

    private PieceChoiceDialog() {
    }

    /**
     * Shows the dialog and waits for an answer.
     *
     * @param owner      the window to appear over
     * @param header     the question to ask
     * @param color      the colour to draw the pieces in
     * @param choices    the kinds of piece to offer
     * @param captions   a few words to show under each choice, such as what it costs
     * @param allowed    whether each choice can be taken; the others are shown greyed out
     * @param styleClass an extra style class for the dialog, to match the window's colours; may be null
     * @return the kind chosen, or null if the dialog was cancelled
     */
    static PieceType choose(Window owner, String header, PieceColor color, PieceType[] choices,
            String[] captions, boolean[] allowed, BoardTheme theme, String styleClass) {
        Dialog<PieceType> dialog = new Dialog<>();
        dialog.setTitle("Transform");
        dialog.setHeaderText(header);
        dialog.initOwner(owner);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER);
        for (int i = 0; i < choices.length; i++) {
            PieceType choice = choices[i];
            Text glyph = new Text();
            PieceArt.paint(glyph, choice, color, GLYPH_SIZE, theme);
            Button button = new Button(captions[i]);
            button.setGraphic(glyph);
            button.setContentDisplay(ContentDisplay.TOP);
            button.setDisable(!allowed[i]);
            button.setAccessibleText(choice.name().toLowerCase());
            button.setOnAction(event -> {
                dialog.setResult(choice);
                dialog.close();
            });
            buttons.getChildren().add(button);
        }
        dialog.getDialogPane().setContent(buttons);
        dialog.getDialogPane().getStylesheets().add(ChessApp.STYLESHEET);
        if (styleClass != null) {
            dialog.getDialogPane().getStyleClass().add(styleClass);
        }
        // A dialog can only be closed if it has a button of a type like
        // Cancel, which leaves the result empty.
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        return dialog.showAndWait().orElse(null);
    }
}
