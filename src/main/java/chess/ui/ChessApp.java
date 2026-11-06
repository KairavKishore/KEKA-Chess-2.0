package chess.ui;

import chess.ai.Difficulty;
import chess.ai.MeasuredStrategy;
import chess.cheat.CheatAI;
import chess.cheat.CheatGame;
import chess.cheat.CheatGame.Chaos;
import chess.cheat.CheatGame.ChaosNotice;
import chess.cheat.CheatGame.Reward;
import chess.cheat.CheatLevel;
import chess.engine.Game;
import chess.engine.GameStatus;
import chess.engine.Move;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;
import chess.player.AIPlayer;
import chess.player.Player;
import chess.ui.JockyLines.Event;
import chess.ui.JockyLines.Mood;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The JavaFX application, KEKA CHESS COM: a window with a chess board,
 * played by clicking, against another person or the computer, or with the
 * computer playing itself. Start it with {@code mvn javafx:run}.
 *
 * <p>The board is on the left, with Jocky the mascot and the status
 * underneath. The panel on the right has the game settings, the view
 * options and the list of moves.
 *
 * <p>There are three sides to the application. It opens on ordinary chess,
 * in red, yellow and black. Clicking Jocky's hat switches everything to
 * cheating chess, in purple, green and white, and clicking it again
 * switches back. Inside cheating chess, choosing the angry chaotic mode
 * (and saying yes to a question that tries to talk you out of it) turns
 * everything red and black.
 */
public class ChessApp extends Application {

    /** The name in the title bar and at the top of the side panel. */
    static final String TITLE = "KEKA CHESS COM";

    /** The colours and shapes of the controls. */
    static final String STYLESHEET = ChessApp.class.getResource("keka.css").toExternalForm();

    /** The three sides of the application, each with its own colours and its own Jocky. */
    private enum Look {
        ORDINARY(null, Mood.FRIENDLY),
        MARKET("market", Mood.SHADY),
        ANGRY("angry", Mood.ANGRY);

        /** The style class that recolours the whole window, or null for the ordinary colours. */
        private final String styleClass;
        private final Mood mood;

        Look(String styleClass, Mood mood) {
            this.styleClass = styleClass;
            this.mood = mood;
        }
    }

    /** How often the clocks are brought up to date. */
    private static final double CLOCK_TICK_MILLIS = 100;

    /** A clock with less than this left is outlined in red. */
    private static final long LOW_TIME_MILLIS = 20_000;

    /** How long the computer takes over a move at the least, so its moves can be followed. */
    private static final long COMPUTER_PAUSE_MILLIS = 400;

    private static final double SIDE_PANEL_WIDTH = 276;
    private static final double INFO_AREA_HEIGHT = 108;

    /** Jocky comments on about this share of ordinary moves, so that he does not talk non-stop. */
    private static final double CHATTINESS = 0.4;

    private static final String CHEAT_RULES =
            "Click a piece and choose what it secretly is. It keeps its looks; the badge shows "
            + "you its real power. You have 20 cheat points: stronger costs, weaker pays back.\n\n"
            + "Nobody has to answer a check. Capture the king to win.\n\n"
            + "Green marks are moves the piece could make honestly. Red marks give it away.\n\n"
            + "Challenge! says the last move was a cheat: a transformed piece or a fiddled "
            + "clock. Right: pick a reward. Wrong: pay a penalty.";

    private static final String ANGRY_RULES =
            "20 cheat points, and pawns can be transformed too. 5 switch tokens, 3 challenges.\n\n"
            + "Green marks are honest moves, red ones give the piece away.\n\n"
            + "Right challenge: pick a reward, including one Disrupt a game. Wrong: a debuff "
            + "or worse.\n\n"
            + "Expect rockets, storms, lava, fog, roll calls, ceasefires, time warps, and "
            + "getting hacked.";

    /** Who plays each side in ordinary chess. */
    private enum Mode {
        TWO_PLAYERS("Two players", true, true),
        PLAY_WHITE("Play White", true, false),
        PLAY_BLACK("Play Black", false, true),
        COMPUTERS("Computer v computer", false, false);

        private final String label;
        private final boolean whiteIsHuman;
        private final boolean blackIsHuman;

        Mode(String label, boolean whiteIsHuman, boolean blackIsHuman) {
            this.label = label;
            this.whiteIsHuman = whiteIsHuman;
            this.blackIsHuman = blackIsHuman;
        }

        @Override
        public String toString() {
            return label;  // what the drop-down list shows
        }
    }

    /**
     * The one background thread the computer thinks on. A daemon thread does
     * not keep the program alive after the window is closed.
     */
    private final ExecutorService thinker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "chess-ai");
        thread.setDaemon(true);
        return thread;
    });

    private final Random random = new Random();
    private final BorderPane root = new BorderPane();

    /** Lies over the whole window, for the theatrics of angry chaotic mode. Empty, it lets clicks through. */
    private final Pane overlay = new Pane();
    private final StackPane window = new StackPane(root, overlay);
    private final ChaosEffects effects = new ChaosEffects(overlay);

    private final ComboBox<TimeControl> timeChoice = new ComboBox<>();
    private final Label whiteClock = new Label();
    private final Label blackClock = new Label();

    // Ordinary chess controls.
    private final ComboBox<Mode> modeChoice = new ComboBox<>();
    private final ComboBox<Difficulty> whiteLevel = new ComboBox<>();
    private final ComboBox<Difficulty> blackLevel = new ComboBox<>();
    private final Button undoButton = new Button("Undo");
    private final Button redoButton = new Button("Redo");
    private final Button copyFenButton = new Button("Copy position (FEN)");
    private final VBox normalControls = new VBox(8);

    // Cheating chess controls.
    private final ComboBox<CheatLevel> cheatLevel = new ComboBox<>();
    private final Button startButton = new Button("Start");
    private final Button accuseButton = new Button("Challenge!");
    private final Button fiddleButton = new Button();
    private final VBox cheatControls = new VBox(8);
    private final Label subtitle = new Label("cheating chess :)");
    private final Label lowerSection = section("MOVES");

    // The view options live here, not in the board, so that they carry over to the next game.
    private final CheckBox perspectiveOption = new CheckBox("3D board");
    private final CheckBox flipOption = new CheckBox("View from Black's side");
    private final CheckBox hintsOption = new CheckBox("Show legal moves");
    private final ComboBox<BoardTheme> themeChoice = new ComboBox<>();

    /** Where the move list, or the rules of cheating chess, goes in the side panel. */
    private final StackPane lowerSlot = new StackPane();

    private final Jocky jocky = new Jocky();
    private final Label speech = new Label();

    /** Where the status line and whatever goes under it are shown, beside Jocky. */
    private final VBox statusSlot = new VBox();

    /** Which side of the application is showing. */
    private Look look = Look.ORDINARY;

    /** True while the mode list is being set by the program, so that its handler does not react. */
    private boolean settingLevel;

    /** The clock of the game on screen, or null if it is not timed. */
    private ChessClock clock;
    private long lastClockTick;

    /** True once a clock has run out, which ends the game whatever the board says. */
    private boolean timedOut;

    /** How many moves of the ordinary game the clock has given an increment for. */
    private int movesClocked;

    /** The last move of the cheating game that the clock gave an increment for. */
    private Move lastClockedMove;

    /** Stand-ins for the dialogs, so that tests can answer without a window; null to show the real ones. */
    private Function<Position, PieceType> identityAnswer;
    private Function<List<Reward>, Reward> rewardAnswer;

    /** The theme to go back to when leaving cheating chess. */
    private BoardTheme ordinaryTheme = BoardTheme.KEKA;

    private BoardView boardView;

    /** The ordinary game on screen and what runs it; null in cheating chess. */
    private Game game;
    private GameController controller;
    private StatusBar statusBar;

    /** The game of cheating chess on screen and what runs it; null in ordinary chess. */
    private CheatGame cheatGame;
    private CheatSession cheatSession;

    @Override
    public void start(Stage stage) {
        stage.setTitle(TITLE);
        stage.setScene(createScene());
        stage.setResizable(false);
        stage.show();
    }

    /** Builds the whole window's contents and starts the first game. */
    Scene createScene() {
        buildOrdinaryControls();
        buildCheatControls();
        buildViewOptions();

        VBox.setVgrow(lowerSlot, Priority.ALWAYS);
        timeChoice.setId("time");
        timeChoice.getItems().addAll(TimeControl.values());
        timeChoice.setValue(TimeControl.RAPID);
        timeChoice.setOnAction(event -> newGame());

        VBox sidePanel = new VBox(
                logo(), subtitle,
                section("GAME"), normalControls, cheatControls, labelled("Clock", timeChoice),
                section("VIEW"), perspectiveOption, flipOption, hintsOption, fullWidth(themeChoice),
                lowerSection, lowerSlot, fullWidth(copyFenButton));
        sidePanel.getStyleClass().add("side-panel");
        sidePanel.setPrefWidth(SIDE_PANEL_WIDTH);
        subtitle.getStyleClass().add("subtitle");
        root.setRight(sidePanel);

        jocky.setOnHatClicked(this::toggleMarket);
        speech.getStyleClass().add("speech");
        speech.setId("speech");
        whiteClock.setId("white-clock");
        blackClock.setId("black-clock");
        whiteClock.getStyleClass().add("clock");
        blackClock.getStyleClass().add("clock");
        overlay.setPickOnBounds(false);

        showSide();
        newGame();
        say(Event.WELCOME);

        Timeline ticking = new Timeline(new KeyFrame(Duration.millis(CLOCK_TICK_MILLIS), event -> tickClock()));
        ticking.setCycleCount(Timeline.INDEFINITE);
        ticking.play();

        Scene scene = new Scene(window);
        scene.getStylesheets().add(STYLESHEET);
        addShortcuts(scene);
        return scene;
    }

    private void buildOrdinaryControls() {
        modeChoice.setId("mode");
        modeChoice.getItems().addAll(Mode.values());
        modeChoice.setValue(Mode.TWO_PLAYERS);
        modeChoice.setOnAction(event -> {
            // Someone playing Black wants to sit on Black's side of the board.
            flipOption.setSelected(modeChoice.getValue() == Mode.PLAY_BLACK);
            newGame();
        });
        for (ComboBox<Difficulty> level : List.of(whiteLevel, blackLevel)) {
            level.getItems().addAll(Difficulty.values());
            level.setValue(Difficulty.MEDIUM);
            level.setOnAction(event -> newGame());
        }
        whiteLevel.setId("white-level");
        blackLevel.setId("black-level");

        Button newGameButton = new Button("New game");
        newGameButton.setOnAction(event -> newGame());
        undoButton.setOnAction(event -> controller.undo());
        redoButton.setOnAction(event -> controller.redo());
        undoButton.setId("undo");
        redoButton.setId("redo");
        copyFenButton.setOnAction(event -> copyFen());

        normalControls.getChildren().addAll(
                fullWidth(modeChoice),
                labelled("White", whiteLevel),
                labelled("Black", blackLevel),
                buttonRow(newGameButton, undoButton, redoButton));
    }

    private void buildCheatControls() {
        cheatLevel.setId("cheat-level");
        cheatLevel.getItems().addAll(CheatLevel.values());
        cheatLevel.setValue(CheatLevel.STANDARD);
        // Listening to the value itself, not to the list's action event,
        // which is only sent once the list has been drawn on screen.
        cheatLevel.valueProperty().addListener((property, before, now) -> levelChosen());

        Button newGameButton = new Button("New game");
        newGameButton.setOnAction(event -> newGame());
        startButton.setId("start");
        accuseButton.setId("accuse");
        fiddleButton.setId("fiddle");
        startButton.setOnAction(event -> {
            cheatSession.finishSwitching();
            updateButtons();
        });
        accuseButton.setOnAction(event -> {
            cheatSession.accuse();
            updateButtons();
        });
        fiddleButton.setOnAction(event -> {
            cheatSession.fiddleClock();
            updateButtons();
        });

        cheatControls.getChildren().addAll(
                fullWidth(cheatLevel),
                buttonRow(newGameButton, startButton),
                fullWidth(accuseButton),
                fullWidth(fiddleButton));
    }

    private void buildViewOptions() {
        perspectiveOption.setId("perspective");
        flipOption.setId("flip");
        hintsOption.setId("hints");
        themeChoice.setId("theme");
        perspectiveOption.setSelected(true);
        hintsOption.setSelected(true);
        themeChoice.getItems().addAll(BoardTheme.values());
        themeChoice.setValue(BoardTheme.KEKA);
        perspectiveOption.setOnAction(event -> boardView.setPerspective(perspectiveOption.isSelected()));
        flipOption.setOnAction(event -> boardView.setFlipped(flipOption.isSelected()));
        hintsOption.setOnAction(event -> boardView.setShowHints(hintsOption.isSelected()));
        themeChoice.setOnAction(event -> boardView.setTheme(themeChoice.getValue()));
    }

    // ------------------------------------------------- the two sides of the app

    /** Jocky's hat was clicked: go from ordinary chess to cheating chess, or from either kind of that back. */
    private void toggleMarket() {
        if (look == Look.ORDINARY) {
            ordinaryTheme = themeChoice.getValue();
            flipOption.setSelected(false);  // the person plays White in cheating chess
            setLevel(CheatLevel.STANDARD);
            enter(Look.MARKET, BoardTheme.BLACK_MARKET, Event.MARKET_OPEN);
        } else {
            enter(Look.ORDINARY, ordinaryTheme, Event.WELCOME);
        }
    }

    /** A mode was chosen from the list in cheating chess. Angry chaotic mode has to be asked for twice. */
    private void levelChosen() {
        if (settingLevel) {
            return;
        }
        if (cheatLevel.getValue() == CheatLevel.ANGRY) {
            effects.confirmGlitchy(
                    () -> {
                        enter(Look.ANGRY, BoardTheme.DANGER, Event.ANGRY_OPEN);
                        effects.popupSwarm();
                    },
                    () -> setLevel(CheatLevel.STANDARD));
        } else if (look == Look.ANGRY) {
            enter(Look.MARKET, BoardTheme.BLACK_MARKET, Event.MARKET_OPEN);
        } else {
            newGame();
        }
    }

    /** Sets the mode list without it being treated as the player's choice. */
    private void setLevel(CheatLevel level) {
        settingLevel = true;
        cheatLevel.setValue(level);
        settingLevel = false;
    }

    /** Switches to one of the three sides of the application and starts a game there. */
    private void enter(Look newLook, BoardTheme theme, Event greeting) {
        look = newLook;
        themeChoice.setValue(theme);
        showSide();
        newGame();
        say(greeting);
    }

    /** Shows the colours, mascot and controls of whichever side of the app is current. */
    private void showSide() {
        for (Look other : Look.values()) {
            window.getStyleClass().remove(other.styleClass);
        }
        if (look.styleClass != null) {
            window.getStyleClass().add(look.styleClass);
        }
        jocky.setMood(look.mood);
        if (look == Look.ANGRY) {
            effects.startDread(root);
        } else {
            effects.stopDread();
        }
        boolean cheating = look != Look.ORDINARY;
        show(normalControls, !cheating);
        show(copyFenButton, !cheating);
        show(cheatControls, cheating);
        show(subtitle, cheating);
        subtitle.setText(look == Look.ANGRY ? "ANGRY CHAOTIC CHEATING" : "cheating chess :)");
        lowerSection.setText(cheating ? "HOW TO CHEAT" : "MOVES");
    }

    /** Shows or hides a part of the window; a hidden part takes up no room. */
    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** Throws the current game away and starts another, of whichever kind is current. */
    private void newGame() {
        if (controller != null) {
            controller.stop();  // if the computer is still thinking, its move is no longer wanted
        }
        if (cheatSession != null) {
            cheatSession.stop();
        }
        controller = null;
        cheatSession = null;
        game = null;
        cheatGame = null;
        effects.closeHackPuzzle();  // a hack belongs to the game it struck

        clock = timeChoice.getValue().createClock();
        timedOut = false;
        movesClocked = 0;
        lastClockedMove = null;
        lastClockTick = System.nanoTime();

        if (look == Look.ORDINARY) {
            newOrdinaryGame();
        } else {
            newCheatGame();
        }
        showClocks(null);
        updateButtons();
    }

    private void newOrdinaryGame() {
        Mode mode = modeChoice.getValue();
        // A level can only be chosen for a side the computer is playing.
        whiteLevel.setDisable(mode.whiteIsHuman);
        blackLevel.setDisable(mode.blackIsHuman);

        game = new Game();
        showBoard(new BoardView(game));

        MeasuredStrategy whiteStrategy = mode.whiteIsHuman ? null : measured(whiteLevel);
        MeasuredStrategy blackStrategy = mode.blackIsHuman ? null : measured(blackLevel);
        Player white = mode.whiteIsHuman ? new HumanPlayer(boardView) : computer(whiteStrategy);
        Player black = mode.blackIsHuman ? new HumanPlayer(boardView) : computer(blackStrategy);
        controller = new GameController(game, white, black);

        statusBar = new StatusBar(game, controller);
        statusSlot.getChildren().setAll(statusBar, new StatsPanel(game, whiteStrategy, blackStrategy));
        lowerSlot.getChildren().setAll(new MoveListPanel(game));

        game.addListener(changed -> {
            // A move has been made if the game is longer than when the clock last looked.
            // Its maker gains the increment. (Taking a move back gives nothing.)
            int played = changed.getMoveHistory().size();
            if (clock != null && played > movesClocked) {
                clock.moveMade(changed.getLastMove().getPiece().getColor());
            }
            movesClocked = played;
            updateButtons();
            cheerFor(changed);
        });
        controller.start();
    }

    private void newCheatGame() {
        CheatLevel level = look == Look.ANGRY ? CheatLevel.ANGRY : CheatLevel.STANDARD;
        cheatGame = new CheatGame(level);
        showBoard(new BoardView(new CheatBoardSource(cheatGame, CheatSession.HUMAN)));

        Label status = new Label();
        status.setId("cheat-status");
        status.getStyleClass().add("status-bar");
        status.setWrapText(true);
        status.setPadding(new Insets(4, 12, 4, 12));
        statusSlot.getChildren().setAll(status);

        Label rules = new Label(level.isChaotic() ? ANGRY_RULES : CHEAT_RULES);
        rules.getStyleClass().add("rules");
        rules.setWrapText(true);
        StackPane.setAlignment(rules, Pos.TOP_LEFT);
        lowerSlot.getChildren().setAll(rules);

        CheatGame thisGame = cheatGame;
        cheatSession = new CheatSession(cheatGame, new CheatAI(), boardView, thinker, Platform::runLater,
                COMPUTER_PAUSE_MILLIS, new CheatHost(status), random);
        cheatGame.addListener(() -> {
            Move last = thisGame.getLastMove();
            if (clock != null && last != null && last != lastClockedMove) {
                clock.moveMade(last.getPiece().getColor());
            }
            lastClockedMove = last;
            updateButtons();
        });
        cheatSession.start();
    }

    /** What a game of cheating chess needs from the window: its dialogs, its status line, Jocky and the clock. */
    private final class CheatHost implements CheatSession.Host {
        private final Label status;

        CheatHost(Label status) {
            this.status = status;
        }

        @Override
        public PieceType chooseIdentity(Position square) {
            if (identityAnswer != null) {
                return identityAnswer.apply(square);
            }
            // Each choice says what it would cost in cheat points, or give back.
            // Before the game the ones that cannot be afforded are greyed out;
            // a switch token, spent as a reward, buys any of them.
            boolean spendingPoints = cheatGame.getPhase() == CheatGame.Phase.SETUP;
            PieceType[] choices = CheatGame.identities();
            String[] captions = new String[choices.length];
            boolean[] allowed = new boolean[choices.length];
            for (int i = 0; i < choices.length; i++) {
                int cost = cheatGame.transformCost(square, choices[i]);
                captions[i] = !spendingPoints ? "" : cost > 0 ? "costs " + cost : cost < 0 ? "gives " + -cost : "as it is";
                allowed[i] = cheatGame.canTransform(CheatSession.HUMAN, square, choices[i]);
            }
            String header = "What is the piece on " + square + " really?"
                    + (spendingPoints ? "  You have " + cheatGame.getCheatPoints(CheatSession.HUMAN)
                            + " cheat points." : "");
            return PieceChoiceDialog.choose(window.getScene().getWindow(), header, CheatSession.HUMAN,
                    choices, captions, allowed, themeChoice.getValue(), look.styleClass);
        }

        @Override
        public Reward chooseReward(List<Reward> options) {
            return rewardAnswer != null ? rewardAnswer.apply(options) : askForReward(options);
        }

        @Override
        public void status(String text) {
            status.setText(text);
        }

        @Override
        public void jocky(Event event) {
            say(event);
        }

        @Override
        public void announce(ChaosNotice notice) {
            // For lava, flick through the wheel's other outcomes before it stops on the real one.
            List<String> spin = new ArrayList<>();
            if (notice.getKind() == Chaos.LAVA) {
                for (int i = 0; i < 12; i++) {
                    spin.add("SPINNING THE WHEEL...  " + (random.nextBoolean() ? "dark" : "light") + "  "
                            + (char) ('a' + random.nextInt(8)));
                }
            }
            if (notice.getKind() == Chaos.ROCKET) {
                effects.shake();
            }
            effects.announce(notice.getText(), spin);
        }

        @Override
        public boolean hasClock() {
            return clock != null;
        }

        @Override
        public void addTime(PieceColor color, long millis) {
            if (clock != null) {
                clock.add(color, millis);
                showClocks(clockOwner());
            }
        }

        @Override
        public void hacked() {
            // Only the picture is scrambled. The mini-game puts it right.
            BoardView hackedBoard = boardView;
            hackedBoard.setScrambled(true);
            effects.shake();
            effects.showHackPuzzle(() -> {
                hackedBoard.setScrambled(false);
                say(Event.UNHACKED);
            });
        }

        @Override
        public void swapClocks() {
            if (clock != null) {
                long white = clock.getRemaining(PieceColor.WHITE);
                long black = clock.getRemaining(PieceColor.BLACK);
                clock.add(PieceColor.WHITE, black - white);
                clock.add(PieceColor.BLACK, white - black);
                showClocks(clockOwner());
            }
        }
    }

    /** Asks which reward to take for a successful challenge; null if the dialog is closed without choosing. */
    private Reward askForReward(List<Reward> options) {
        Dialog<Reward> dialog = new Dialog<>();
        dialog.setTitle("Caught!");
        dialog.setHeaderText("Caught cheating! Choose your reward:");
        dialog.initOwner(window.getScene().getWindow());

        VBox buttons = new VBox(8);
        for (Reward option : options) {
            Button button = new Button(describe(option));
            button.getStyleClass().add("reward-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                dialog.setResult(option);
                dialog.close();
            });
            buttons.getChildren().add(button);
        }
        dialog.getDialogPane().setContent(buttons);
        dialog.getDialogPane().getStylesheets().add(STYLESHEET);
        dialog.getDialogPane().getStyleClass().add(look.styleClass);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Nothing", ButtonType.CANCEL.getButtonData()));
        return dialog.showAndWait().orElse(null);
    }

    private String describe(Reward reward) {
        switch (reward) {
            case EXPOSE:
                return "Reveal the piece for good";
            case STRIP:
                return "Strip it: it becomes what it looks like";
            case JAIL:
                return "Jail it: it cannot move for two turns";
            case EXTRA_MOVE:
                return "Take two moves in a row";
            case STEAL_TIME:
                return "Steal 30 seconds from its clock";
            case SWITCH:
                return "Spend a switch token to transform one of mine ("
                        + cheatGame.getSwitchTokens(CheatSession.HUMAN) + " left)";
            case DISRUPT_EXPOSE:
                return "Disrupt: expose another hidden piece";
            case DISRUPT_SAFE:
                return "Disrupt: make a square safe for a few turns";
            default:
                return "Disrupt: bring back my best captured piece";
        }
    }

    /** Puts a new board on screen with the current view options applied. */
    private void showBoard(BoardView view) {
        boardView = view;
        boardView.setPerspective(perspectiveOption.isSelected());
        boardView.setFlipped(flipOption.isSelected());
        boardView.setShowHints(hintsOption.isSelected());
        boardView.setTheme(themeChoice.getValue());

        StackPane boardArea = new StackPane(boardView);
        boardArea.getStyleClass().add("board-area");

        VBox words = new VBox(4, speech, statusSlot);
        HBox.setHgrow(words, Priority.ALWAYS);
        // It takes whatever width is left over and no more: a long status
        // line must wrap inside it, not push the window wider.
        words.setMinWidth(0);
        words.setPrefWidth(0);
        VBox clocks = new VBox(6, blackClock, whiteClock);  // Black's on top, as its side of the board is
        clocks.setAlignment(Pos.CENTER_RIGHT);
        clocks.setMinWidth(Region.USE_PREF_SIZE);  // a long status line must wrap, not squeeze the clocks
        HBox infoArea = new HBox(6, jocky, words, clocks);
        infoArea.getStyleClass().add("info-area");
        infoArea.setAlignment(Pos.CENTER_LEFT);
        infoArea.setPadding(new Insets(6, 8, 6, 8));
        // A fixed height, so that the window is the same size whatever the status says.
        infoArea.setMinHeight(INFO_AREA_HEIGHT);
        infoArea.setPrefHeight(INFO_AREA_HEIGHT);
        infoArea.setMaxHeight(INFO_AREA_HEIGHT);
        root.setCenter(new VBox(boardArea, infoArea));
    }

    // ------------------------------------------------------------ the clock

    /** Called several times a second: runs down the clock of whoever is thinking, and ends the game at zero. */
    private void tickClock() {
        long now = System.nanoTime();
        long elapsedMillis = (now - lastClockTick) / 1_000_000;
        lastClockTick = now;
        PieceColor running = clockOwner();
        if (running != null) {
            clock.advance(running, elapsedMillis);
            if (clock.isOutOfTime(running)) {
                timeUp(running);
                running = null;
            }
        }
        showClocks(running);
    }

    /**
     * The side whose clock should be running, or null if none should: there
     * is no clock, the game is over, or nobody has moved yet. Waiting for
     * the first move lets the players settle in before time starts.
     */
    private PieceColor clockOwner() {
        if (clock == null || timedOut) {
            return null;
        }
        if (game != null) {
            boolean started = !game.getMoveHistory().isEmpty();
            return started && !game.getStatus().isGameOver() ? game.getTurn() : null;
        }
        if (cheatGame != null && cheatGame.getLastMove() != null && !cheatGame.isOver()) {
            return cheatGame.getTurn();
        }
        return null;
    }

    private void showClocks(PieceColor running) {
        for (PieceColor color : PieceColor.values()) {
            Label label = color == PieceColor.WHITE ? whiteClock : blackClock;
            show(label, clock != null);
            if (clock == null) {
                continue;
            }
            label.setText((color == PieceColor.WHITE ? "White " : "Black ") + ChessClock.format(clock.getRemaining(color)));
            label.getStyleClass().removeAll("clock-running", "clock-low");
            if (color == running) {
                label.getStyleClass().add("clock-running");
            }
            if (clock.getRemaining(color) < LOW_TIME_MILLIS) {
                label.getStyleClass().add("clock-low");
            }
        }
    }

    /** A side's time has run out: the game is over and the other side has won. */
    private void timeUp(PieceColor loser) {
        timedOut = true;
        boardView.setBannerOverride("OUT OF TIME");
        if (cheatSession != null) {
            cheatSession.timeUp(loser);
        } else {
            controller.stop();
            boardView.requestMove(null);
            String winner = loser == PieceColor.WHITE ? "Black" : "White";
            statusBar.setText((loser == PieceColor.WHITE ? "White" : "Black") + " ran out of time. "
                    + winner + " wins.");
            say(Event.OUT_OF_TIME);
        }
        updateButtons();
    }

    // --------------------------------------------------------------- Jocky

    /** Jocky says something about what just happened. */
    private void say(Event event) {
        speech.setText(JockyLines.pick(event, look.mood, random));
        jocky.hop();
    }

    /** Jocky reacts to a move in an ordinary game: always to the big moments, now and then to the rest. */
    private void cheerFor(Game changed) {
        GameStatus status = changed.getStatus();
        Move lastMove = changed.getLastMove();
        if (status == GameStatus.CHECKMATE) {
            say(Event.GAME_WON);
        } else if (status.isDraw()) {
            say(Event.DRAW);
        } else if (status == GameStatus.CHECK) {
            say(Event.CHECK);
        } else if (lastMove != null && lastMove.isCapture()) {
            say(Event.CAPTURE);
        } else if (random.nextDouble() < CHATTINESS) {
            say(Event.MOVE);
        }
    }

    // ------------------------------------------------------------- helpers

    private static MeasuredStrategy measured(ComboBox<Difficulty> level) {
        return new MeasuredStrategy(level.getValue().createStrategy());
    }

    /**
     * A computer player that thinks on the background thread and answers on
     * the JavaFX thread, the only one allowed to touch the game and the window.
     */
    private Player computer(MeasuredStrategy strategy) {
        return new AIPlayer(strategy, thinker, Platform::runLater, COMPUTER_PAUSE_MILLIS);
    }

    private void updateButtons() {
        undoButton.setDisable(controller == null || timedOut || !controller.canUndo());
        redoButton.setDisable(controller == null || timedOut || !controller.canRedo());
        startButton.setDisable(cheatSession == null || !cheatSession.isSettingUp());
        accuseButton.setDisable(cheatSession == null || !cheatSession.canAccuse());
        fiddleButton.setDisable(cheatSession == null || !cheatSession.canFiddleClock());
        if (cheatSession != null) {
            int challenges = cheatGame.getChallengesLeft(CheatSession.HUMAN);
            accuseButton.setText(challenges < 0 ? "Challenge!" : "Challenge!  (" + challenges + " left)");
            fiddleButton.setText("Fiddle the clock: +20 s  (" + cheatSession.getFiddlesLeft() + " left)");
        }
    }

    /** Replaces the two dialogs of cheating chess with ready answers, so tests can play without a window. */
    void answerDialogsWith(Function<Position, PieceType> identity, Function<List<Reward>, Reward> reward) {
        identityAnswer = identity;
        rewardAnswer = reward;
    }

    private void copyFen() {
        ClipboardContent content = new ClipboardContent();
        content.putString(game.toFen());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /** Ctrl+N new game, Ctrl+Z undo, Ctrl+Y redo, Ctrl+F flip the board, Ctrl+D switch between 3D and flat. */
    private void addShortcuts(Scene scene) {
        shortcut(scene, KeyCode.N, this::newGame);
        shortcut(scene, KeyCode.Z, () -> {
            if (controller != null && !timedOut && controller.canUndo()) {
                controller.undo();
            }
        });
        shortcut(scene, KeyCode.Y, () -> {
            if (controller != null && !timedOut && controller.canRedo()) {
                controller.redo();
            }
        });
        shortcut(scene, KeyCode.F, flipOption::fire);
        shortcut(scene, KeyCode.D, perspectiveOption::fire);
    }

    private static void shortcut(Scene scene, KeyCode key, Runnable action) {
        scene.getAccelerators().put(new KeyCodeCombination(key, KeyCombination.SHORTCUT_DOWN), action);
    }

    /** The name of the game in its three colours. */
    private static Node logo() {
        TextFlow logo = new TextFlow(logoWord("KEKA ", "logo-red"), logoWord("CHESS ", "logo-yellow"),
                logoWord("COM", "logo-white"));
        logo.getStyleClass().add("logo");
        return logo;
    }

    private static Text logoWord(String word, String styleClass) {
        Text text = new Text(word);
        text.getStyleClass().add(styleClass);
        return text;
    }

    private static Label section(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("section");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static <T extends Control> T fullWidth(T control) {
        control.setMaxWidth(Double.MAX_VALUE);
        return control;
    }

    /** Buttons side by side, each as wide as its text needs and no narrower. */
    private static HBox buttonRow(Button... buttons) {
        for (Button button : buttons) {
            button.setMinWidth(Region.USE_PREF_SIZE);
        }
        return new HBox(6, buttons);
    }

    /** A control with a fixed-width label in front of it, so that a column of them lines up. */
    private static HBox labelled(String text, ComboBox<?> control) {
        Label label = new Label(text);
        label.setMinWidth(44);
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox row = new HBox(6, label, fullWidth(control));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @Override
    public void stop() {
        // Stop the games first: a computer move already on its way to this
        // thread must be ignored, not answered with a request to a thread
        // that has shut down.
        if (controller != null) {
            controller.stop();
        }
        if (cheatSession != null) {
            cheatSession.stop();
        }
        thinker.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
