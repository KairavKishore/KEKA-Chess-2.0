package chess.ui;

import chess.ui.JockyLines.Mood;

import javafx.animation.TranslateTransition;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * Jocky, the mascot: a round red and yellow fellow in a black hat who
 * cheers the player on. Clicking his hat is the way into cheating chess, at
 * which point he turns purple and green and a good deal less wholesome. In
 * angry chaotic mode he is red and black and beside himself.
 *
 * <p>He is drawn from simple shapes. His moods are the same shapes with
 * different colours, eyelids, eyebrows and mouth, and his hat at a
 * different angle.
 */
public class Jocky extends Pane {

    private static final double WIDTH = 84;
    private static final double HEIGHT = 92;

    private final Ellipse body = new Ellipse(42, 60, 30, 28);
    private final Ellipse belly = new Ellipse(42, 68, 18, 16);
    private final Circle leftCheek = new Circle(22, 62, 4.5);
    private final Circle rightCheek = new Circle(62, 62, 4.5);
    private final Circle leftPupil = new Circle(32, 52, 3.2, Color.BLACK);
    private final Circle rightPupil = new Circle(52, 52, 3.2, Color.BLACK);
    private final Rectangle leftLid = new Rectangle(24, 43, 16, 8);
    private final Rectangle rightLid = new Rectangle(44, 43, 16, 8);
    private final Line leftBrow = new Line(24, 40, 38, 46);
    private final Line rightBrow = new Line(60, 40, 46, 46);
    private final Arc mouth = new Arc(42, 64, 11, 8, 200, 140);
    private final Rectangle hatBand = new Rectangle(27, 27, 30, 5);
    private final Group hat = new Group();

    public Jocky() {
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        body.setStroke(Color.BLACK);
        body.setStrokeWidth(2);
        Circle leftEye = new Circle(32, 51, 7, Color.WHITE);
        Circle rightEye = new Circle(52, 51, 7, Color.WHITE);
        leftEye.setStroke(Color.BLACK);
        rightEye.setStroke(Color.BLACK);
        for (Line brow : new Line[] {leftBrow, rightBrow}) {
            brow.setStrokeWidth(3.5);
            brow.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        mouth.setType(ArcType.OPEN);
        mouth.setFill(Color.TRANSPARENT);
        mouth.setStroke(Color.BLACK);
        mouth.setStrokeWidth(2.5);
        mouth.setStrokeLineCap(StrokeLineCap.ROUND);

        Rectangle crown = new Rectangle(27, 6, 30, 26);
        crown.setArcWidth(6);
        crown.setArcHeight(6);
        Rectangle brim = new Rectangle(16, 30, 52, 6);
        brim.setArcWidth(6);
        brim.setArcHeight(6);
        // A grey edge, or a black hat would vanish against the dark window.
        crown.setStroke(Color.gray(0.45));
        brim.setStroke(Color.gray(0.45));
        hat.getChildren().addAll(crown, brim, hatBand);
        hat.setCursor(Cursor.HAND);
        hat.setId("jocky-hat");

        getChildren().addAll(body, belly, leftCheek, rightCheek, leftEye, rightEye, leftPupil, rightPupil,
                leftLid, rightLid, leftBrow, rightBrow, mouth, hat);
        setMood(Mood.FRIENDLY);
    }

    /** Sets what happens when his hat is clicked. */
    public void setOnHatClicked(Runnable action) {
        hat.setOnMouseClicked(event -> action.run());
    }

    /** Changes his colours and his face to suit a mood. */
    public void setMood(Mood mood) {
        boolean shady = mood == Mood.SHADY;
        boolean angry = mood == Mood.ANGRY;

        Color skin = Color.web(shady ? "#7b2cbf" : angry ? "#c1121f" : "#e10600");
        Color trim = Color.web(shady ? "#39ff14" : angry ? "#111111" : "#ffd60a");
        Color cheek = Color.web(shady ? "#5a189a" : angry ? "#7a0000" : "#ff7a70");
        body.setFill(skin);
        belly.setFill(trim);
        hatBand.setFill(angry ? Color.web("#ff1a1a") : trim);
        leftCheek.setFill(cheek);
        rightCheek.setFill(cheek);

        // Shady: heavy eyelids, eyes cut to one side, and the hat pulled low.
        leftLid.setFill(skin);
        rightLid.setFill(skin);
        leftLid.setVisible(shady);
        rightLid.setVisible(shady);
        leftPupil.setCenterX(shady ? 35.5 : 32);
        rightPupil.setCenterX(shady ? 55.5 : 52);
        leftPupil.setCenterY(shady ? 54 : 52);
        rightPupil.setCenterY(shady ? 54 : 52);
        hat.setTranslateY(shady ? 6 : 0);
        hat.setRotate(shady ? -9 : angry ? 7 : 0);

        // Angry: eyebrows slanting down to the middle, pinprick pupils, a deep scowl.
        leftBrow.setVisible(angry);
        rightBrow.setVisible(angry);
        leftPupil.setRadius(angry ? 2.2 : 3.2);
        rightPupil.setRadius(angry ? 2.2 : 3.2);

        // Friendly: a wide open grin. Shady: a small smirk off to one side.
        // Angry: the grin turned upside down. Angles run anticlockwise from
        // three o'clock, so 200 to 340 is the bottom of an oval (a smile)
        // and 20 to 160 is the top of one (a frown).
        mouth.setCenterX(shady ? 47 : 42);
        mouth.setCenterY(angry ? 74 : 64);
        mouth.setRadiusX(shady ? 7 : 11);
        mouth.setRadiusY(shady ? 4 : 8);
        mouth.setStartAngle(shady ? 220 : angry ? 20 : 200);
        mouth.setLength(shady ? 110 : 140);
        mouth.setStroke(angry ? Color.WHITE : Color.BLACK);
    }

    /** A little hop, for when he has something to say. */
    public void hop() {
        TranslateTransition hop = new TranslateTransition(Duration.millis(130), this);
        hop.setFromY(0);
        hop.setToY(-7);
        hop.setAutoReverse(true);
        hop.setCycleCount(2);
        hop.play();
    }
}
