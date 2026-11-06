package chess.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The theatrics of angry chaotic mode, drawn on a layer over the whole
 * window: the glitching "R U SURE" question, the swarm of tiny error
 * pop-ups, the announcements of disasters, the dread that hangs over the
 * whole mode, and the mini-game that ends a hack.
 *
 * <p>None of it affects the game. Passing effects are added to the overlay
 * and removed by a timer, so a moment later the window is as it was.
 */
final class ChaosEffects {

    private static final String[] GLITCH_TITLES = {
        "R U SURE ??", "R U SUR3 ??", "R U  S U R E", "r u sure??", "R_U_SURE//", "R U ░URE ??", "ARE YOU. SURE."
    };

    private static final String[] POPUP_TEXTS = {
        "ERROR", "NO REFUNDS", "!!!", "0xC4E55", "WHY", "TOO LATE", "bad idea", "FATAL: fun", "WARNING",
        "OK?", "not ok", "ROOK.EXE", "run", "HAT ERROR", "CHAOS++", "⚠", "pawn overflow", "AAAAAA"
    };

    private static final int POPUPS = 18;

    /** Things that appear on the screen for a moment, uninvited, in angry chaotic mode. */
    private static final String[] WHISPERS = {
        "IT SEES YOU", "DON'T LOOK BEHIND YOU", "YOUR KING IS ALREADY DEAD", "TICK. TOCK.",
        "WHY DID YOU SAY YES", "HE NEVER BLINKS", "RUN", "THIS IS NOT A GAME", "LET ME OUT",
        "ONE OF YOUR PIECES IS LYING TO YOU"
    };

    /** How many numbered buttons the hack mini-game has. */
    private static final int HACK_BUTTONS = 6;
    private static final int HACK_COLUMNS = 3;

    /** The dread: a dark red edge to the screen that throbs like a pulse, and whatever is haunting it. */
    private final List<Node> dreadNodes = new ArrayList<>();
    private final List<Timeline> dreadTimelines = new ArrayList<>();

    /** What is shaken when the screen shakes: the window's real contents, under the overlay. */
    private Node shaken;

    /** The hack mini-game on screen, or null. */
    private Node hackPanel;

    private final Pane overlay;
    private final Random random = new Random();

    /**
     * @param overlay a pane lying over the whole window, which lets clicks through wherever it is empty
     */
    ChaosEffects(Pane overlay) {
        this.overlay = overlay;
    }

    /**
     * Asks, in a window that cannot hold itself together, whether the
     * player really wants angry chaotic mode.
     *
     * @param onYes run if they say yes
     * @param onNo  run if they back out
     */
    void confirmGlitchy(Runnable onYes, Runnable onNo) {
        // A dark sheet under the question stops clicks reaching the game.
        Rectangle sheet = new Rectangle(0, 0, Color.rgb(0, 0, 0, 0.78));
        sheet.widthProperty().bind(overlay.widthProperty());
        sheet.heightProperty().bind(overlay.heightProperty());

        Label title = new Label(GLITCH_TITLES[0]);
        title.getStyleClass().add("glitch-title");
        Label warning = new Label("th1s w1ll m4ke h1m 4ngry");
        warning.getStyleClass().add("glitch-warning");
        Button yes = new Button("YES");
        Button no = new Button("no, take me back");
        yes.setId("glitch-yes");
        no.setId("glitch-no");
        HBox buttons = new HBox(14, yes, no);
        buttons.setAlignment(Pos.CENTER);
        VBox box = new VBox(10, title, warning, buttons);
        box.getStyleClass().add("glitch-box");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(22, 34, 22, 34));
        box.layoutXProperty().bind(overlay.widthProperty().subtract(box.widthProperty()).divide(2));
        box.layoutYProperty().bind(overlay.heightProperty().subtract(box.heightProperty()).divide(2));

        // The glitch: several times a second the box jumps, the title is
        // rewritten wrongly and the colours flicker.
        Timeline glitch = new Timeline(new KeyFrame(Duration.millis(85), event -> {
            box.setTranslateX(random.nextInt(13) - 6);
            box.setTranslateY(random.nextInt(9) - 4);
            title.setText(GLITCH_TITLES[random.nextInt(GLITCH_TITLES.length)]);
            title.setTextFill(random.nextInt(4) == 0 ? Color.WHITE : Color.web("#ff1a1a"));
            box.setOpacity(random.nextInt(9) == 0 ? 0.55 : 1);
        }));
        glitch.setCycleCount(Timeline.INDEFINITE);
        glitch.play();

        List<Node> shown = List.of(sheet, box);
        overlay.getChildren().addAll(shown);
        yes.setOnAction(event -> {
            glitch.stop();
            overlay.getChildren().removeAll(shown);
            onYes.run();
        });
        no.setOnAction(event -> {
            glitch.stop();
            overlay.getChildren().removeAll(shown);
            onNo.run();
        });
    }

    /** Scatters tiny error windows all over the screen for a couple of seconds. */
    void popupSwarm() {
        List<Node> popups = new ArrayList<>();
        double width = Math.max(overlay.getWidth(), 600);
        double height = Math.max(overlay.getHeight(), 500);
        for (int i = 0; i < POPUPS; i++) {
            Label bar = new Label("✕  keka.exe");
            bar.getStyleClass().add("popup-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            Label text = new Label(POPUP_TEXTS[random.nextInt(POPUP_TEXTS.length)]);
            text.getStyleClass().add("popup-text");
            VBox popup = new VBox(bar, text);
            popup.getStyleClass().add("popup");
            popup.setMouseTransparent(true);
            popup.setLayoutX(random.nextDouble() * (width - 130));
            popup.setLayoutY(random.nextDouble() * (height - 70));
            popup.setRotate(random.nextInt(17) - 8);
            popups.add(popup);
        }
        showFor(popups, 2200);
    }

    /**
     * Announces a disaster in large letters across the top of the window.
     *
     * @param text the announcement
     * @param spin texts to flick through first, like a wheel spinning before it stops; may be empty
     */
    void announce(String text, List<String> spin) {
        Label banner = new Label(spin.isEmpty() ? text : spin.get(0));
        banner.getStyleClass().add("chaos-banner");
        banner.setWrapText(true);
        banner.setMouseTransparent(true);
        banner.setMaxWidth(520);
        banner.setLayoutX(20);
        banner.setLayoutY(70);

        if (!spin.isEmpty()) {
            int[] shown = {0};
            Timeline wheel = new Timeline(new KeyFrame(Duration.millis(110), event -> {
                shown[0]++;
                banner.setText(shown[0] < spin.size() ? spin.get(shown[0]) : text);
            }));
            wheel.setCycleCount(spin.size());
            wheel.play();
        }
        showFor(List.of(banner), 3200);
    }

    // ------------------------------------------------------------ the dread

    /**
     * Makes the window frightening for as long as angry chaotic mode lasts:
     * the edges of the screen darken to red and throb slowly, and every few
     * seconds something happens. The screen shakes, a message flashes up,
     * the picture tears, or a pair of eyes opens somewhere.
     *
     * <p>Nothing here flashes quickly. Each effect appears once and fades,
     * with seconds between them, so it is unsettling without strobing.
     *
     * @param shakeTarget the window's contents, which the shaking moves
     */
    void startDread(Node shakeTarget) {
        stopDread();
        shaken = shakeTarget;

        Rectangle vignette = new Rectangle();
        vignette.widthProperty().bind(overlay.widthProperty());
        vignette.heightProperty().bind(overlay.heightProperty());
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.45, Color.TRANSPARENT), new Stop(1, Color.rgb(120, 0, 0, 0.85))));
        vignette.setMouseTransparent(true);
        vignette.setOpacity(0.45);
        overlay.getChildren().add(0, vignette);
        dreadNodes.add(vignette);

        // A heartbeat: the red edge swells and sinks about once a second.
        Timeline heartbeat = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(vignette.opacityProperty(), 0.35)),
                new KeyFrame(Duration.millis(550), new KeyValue(vignette.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(1100), new KeyValue(vignette.opacityProperty(), 0.35)));
        heartbeat.setCycleCount(Timeline.INDEFINITE);
        heartbeat.play();
        dreadTimelines.add(heartbeat);

        Timeline haunting = new Timeline(new KeyFrame(Duration.seconds(4.5), event -> haunt()));
        haunting.setCycleCount(Timeline.INDEFINITE);
        haunting.play();
        dreadTimelines.add(haunting);
    }

    /** Ends the dread and removes everything it put on screen. */
    void stopDread() {
        for (Timeline timeline : dreadTimelines) {
            timeline.stop();
        }
        dreadTimelines.clear();
        overlay.getChildren().removeAll(dreadNodes);
        dreadNodes.clear();
        if (shaken != null) {
            shaken.setTranslateX(0);
            shaken.setTranslateY(0);
        }
    }

    /** One fright, chosen at random. */
    private void haunt() {
        switch (random.nextInt(4)) {
            case 0:
                shake();
                break;
            case 1:
                whisper();
                break;
            case 2:
                tear();
                break;
            default:
                eyes();
                break;
        }
    }

    /** Shakes the whole window for a third of a second, as if something heavy had landed. */
    void shake() {
        if (shaken == null) {
            return;
        }
        Timeline shaking = new Timeline(new KeyFrame(Duration.millis(40), event -> {
            shaken.setTranslateX(random.nextInt(15) - 7);
            shaken.setTranslateY(random.nextInt(11) - 5);
        }));
        shaking.setCycleCount(8);
        shaking.setOnFinished(event -> {
            shaken.setTranslateX(0);
            shaken.setTranslateY(0);
        });
        shaking.play();
    }

    /** A few words in large letters across the middle of the screen, gone in a second. */
    private void whisper() {
        Label words = new Label(WHISPERS[random.nextInt(WHISPERS.length)]);
        words.getStyleClass().add("whisper");
        words.setMouseTransparent(true);
        words.layoutXProperty().bind(overlay.widthProperty().subtract(words.widthProperty()).divide(2));
        words.setLayoutY(120 + random.nextInt(260));
        words.setRotate(random.nextInt(9) - 4);
        showFor(List.of(words), 1100);
    }

    /** The picture tears: a few black and red bars across the screen for a moment. */
    private void tear() {
        List<Node> bars = new ArrayList<>();
        double height = Math.max(overlay.getHeight(), 500);
        for (int i = 0; i < 5; i++) {
            Rectangle bar = new Rectangle();
            bar.widthProperty().bind(overlay.widthProperty());
            bar.setHeight(3 + random.nextInt(16));
            bar.setY(random.nextDouble() * height);
            bar.setFill(random.nextBoolean() ? Color.BLACK : Color.rgb(200, 0, 0, 0.8));
            bar.setMouseTransparent(true);
            bars.add(bar);
        }
        showFor(bars, 260);
    }

    /** A pair of red eyes opens somewhere on the screen, looks, and closes. */
    private void eyes() {
        double x = 40 + random.nextDouble() * Math.max(overlay.getWidth() - 160, 300);
        double y = 60 + random.nextDouble() * Math.max(overlay.getHeight() - 200, 300);
        List<Node> pair = new ArrayList<>();
        for (int eye = 0; eye < 2; eye++) {
            Ellipse white = new Ellipse(x + eye * 34, y, 11, 6);
            white.setFill(Color.rgb(255, 30, 30));
            white.setEffect(new DropShadow(18, Color.RED));
            Ellipse slit = new Ellipse(x + eye * 34, y, 2.2, 6);
            slit.setFill(Color.BLACK);
            white.setMouseTransparent(true);
            slit.setMouseTransparent(true);
            pair.add(white);
            pair.add(slit);
        }
        showFor(pair, 1500);
    }

    // ------------------------------------------------------------- the hack

    /**
     * Shows the mini-game that ends a hack: six numbered buttons in a
     * muddle, to be pressed in order from 1. A wrong press muddles them
     * again and starts the count over.
     *
     * @param onSolved run when the last button has been pressed
     */
    void showHackPuzzle(Runnable onSolved) {
        closeHackPuzzle();
        HackPuzzle puzzle = new HackPuzzle(HACK_BUTTONS, random);

        Label title = new Label("\u26A0 YOU HAVE BEEN HACKED");
        title.getStyleClass().add("hack-title");
        Label progress = new Label();
        progress.getStyleClass().add("hack-text");
        progress.setId("hack-progress");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);
        List<Button> buttons = new ArrayList<>();
        for (int slot = 0; slot < HACK_BUTTONS; slot++) {
            Button button = new Button();
            button.getStyleClass().add("hack-button");
            button.setId("hack-" + slot);
            buttons.add(button);
            grid.add(button, slot % HACK_COLUMNS, slot / HACK_COLUMNS);
        }
        VBox panel = new VBox(10, title, progress, grid);
        panel.getStyleClass().add("hack-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(16, 24, 18, 24));
        // Near the top, so that the scrambling board can be seen underneath.
        panel.setLayoutX(110);
        panel.setLayoutY(12);

        // Redrawn after every press: a wrong one moves all the numbers.
        Runnable refresh = () -> {
            for (int slot = 0; slot < HACK_BUTTONS; slot++) {
                int number = puzzle.numberAt(slot);
                buttons.get(slot).setText(Integer.toString(number));
                buttons.get(slot).setDisable(number < puzzle.getNext());
            }
            progress.setText("restore the firewall: press 1 to " + HACK_BUTTONS + " in order   ["
                    + puzzle.getProgress() + "/" + HACK_BUTTONS + "]");
        };
        for (int slot = 0; slot < HACK_BUTTONS; slot++) {
            int pressed = slot;
            buttons.get(slot).setOnAction(event -> {
                puzzle.press(pressed);
                if (puzzle.isSolved()) {
                    closeHackPuzzle();
                    onSolved.run();
                } else {
                    refresh.run();
                }
            });
        }
        refresh.run();
        hackPanel = panel;
        overlay.getChildren().add(panel);
    }

    /** Takes the hack mini-game off the screen, if it is there. */
    void closeHackPuzzle() {
        if (hackPanel != null) {
            overlay.getChildren().remove(hackPanel);
            hackPanel = null;
        }
    }

    private void showFor(List<? extends Node> nodes, double millis) {
        overlay.getChildren().addAll(nodes);
        PauseTransition wait = new PauseTransition(Duration.millis(millis));
        wait.setOnFinished(event -> overlay.getChildren().removeAll(nodes));
        wait.play();
    }
}
