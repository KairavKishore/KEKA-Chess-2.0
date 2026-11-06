package chess.ui;

import chess.ai.MeasuredStrategy;
import chess.engine.Game;
import chess.engine.GameListener;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Shows who is playing each side and how hard each computer player is
 * working: moves chosen, average thinking time and average positions
 * examined. There is always one line per side, so the window stays the same
 * size whoever is playing.
 */
public class StatsPanel extends VBox implements GameListener {

    private final MeasuredStrategy white;
    private final MeasuredStrategy black;
    private final Label whiteLine = new Label();
    private final Label blackLine = new Label();

    /**
     * @param white the strategy playing White, or null if a person is
     * @param black the strategy playing Black, or null if a person is
     */
    public StatsPanel(Game game, MeasuredStrategy white, MeasuredStrategy black) {
        this.white = white;
        this.black = black;
        setPadding(new Insets(0, 12, 8, 12));
        whiteLine.getStyleClass().add("stats-line");
        blackLine.getStyleClass().add("stats-line");
        getChildren().addAll(whiteLine, blackLine);

        game.addListener(this);
        gameChanged(game);
    }

    @Override
    public void gameChanged(Game game) {
        whiteLine.setText(StatsText.describe("White", white));
        blackLine.setText(StatsText.describe("Black", black));
    }
}
