package chess.ui;

import chess.engine.Game;
import chess.engine.GameListener;
import chess.engine.Move;
import chess.engine.PieceColor;

import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The record of the game beside the board: every move played, and the
 * pieces each side has captured. It listens to the game and updates itself.
 */
public class MoveListPanel extends VBox implements GameListener {

    private final ListView<String> moves = new ListView<>();
    private final Label whiteCaptures = new Label();
    private final Label blackCaptures = new Label();

    public MoveListPanel(Game game) {
        super(6);
        moves.setFocusTraversable(false);
        // A list asks for 400 pixels by default, which would stretch the
        // window. Ask for little and let the panel give it what is left over.
        moves.setPrefHeight(80);
        moves.setMinHeight(60);
        moves.setPlaceholder(new Label("No moves yet"));
        VBox.setVgrow(moves, Priority.ALWAYS);
        whiteCaptures.getStyleClass().add("captures");
        blackCaptures.getStyleClass().add("captures");
        getChildren().addAll(moves, whiteCaptures, blackCaptures);

        game.addListener(this);
        gameChanged(game);
    }

    @Override
    public void gameChanged(Game game) {
        List<Move> history = game.getMoveHistory();
        moves.getItems().setAll(MoveListText.lines(history));
        moves.scrollTo(moves.getItems().size() - 1);
        whiteCaptures.setText("White took: " + MoveListText.capturedBy(PieceColor.WHITE, history));
        blackCaptures.setText("Black took: " + MoveListText.capturedBy(PieceColor.BLACK, history));
    }
}
