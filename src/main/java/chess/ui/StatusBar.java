package chess.ui;

import chess.engine.Game;
import chess.engine.GameListener;

import javafx.geometry.Insets;
import javafx.scene.control.Label;

/**
 * The line under the board saying whose turn it is, whether they are in
 * check, and how the game ended. It listens to the game and updates itself.
 */
public class StatusBar extends Label implements GameListener {

    private final GameController controller;

    /**
     * @param controller knows which player is to move, so the bar can say
     *                   when the computer is thinking
     */
    public StatusBar(Game game, GameController controller) {
        this.controller = controller;
        setPadding(new Insets(8, 12, 8, 12));
        getStyleClass().add("status-bar");
        setMaxWidth(Double.MAX_VALUE);

        game.addListener(this);
        gameChanged(game);
    }

    @Override
    public void gameChanged(Game game) {
        setText(StatusText.describe(game, controller.getPlayerToMove()));
    }
}
