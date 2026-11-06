package chess.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.cheat.CheatGame.Chaos;
import chess.ui.JockyLines.Event;
import chess.ui.JockyLines.Mood;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JockyLinesTest {

    @Test
    void hasSomethingToSayAboutEveryEventInEveryMood() {
        for (Event event : Event.values()) {
            for (Mood mood : Mood.values()) {
                List<String> lines = JockyLines.linesFor(event, mood);
                assertFalse(lines.isEmpty(), event + " " + mood);
                for (String line : lines) {
                    assertFalse(line.isBlank());
                    assertTrue(line.length() <= 60, "too long for the speech bubble: " + line);
                }
            }
        }
    }

    @Test
    void picksOneOfTheLinesForTheEvent() {
        Random random = new Random(1);
        for (int i = 0; i < 50; i++) {
            assertTrue(JockyLines.linesFor(Event.CAPTURE, Mood.FRIENDLY)
                    .contains(JockyLines.pick(Event.CAPTURE, Mood.FRIENDLY, random)));
        }
    }

    @Test
    void variesWhatHeSays() {
        Random random = new Random(2);
        Set<String> said = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            said.add(JockyLines.pick(Event.MOVE, Mood.FRIENDLY, random));
        }
        assertEquals(JockyLines.linesFor(Event.MOVE, Mood.FRIENDLY).size(), said.size());
    }

    @Test
    void theMoodsDoNotShareLines() {
        for (Event event : new Event[] {Event.WELCOME, Event.MOVE, Event.CAPTURE, Event.DRAW}) {
            Set<String> seen = new HashSet<>();
            for (Mood mood : Mood.values()) {
                for (String line : JockyLines.linesFor(event, mood)) {
                    assertTrue(seen.add(line), "said in two moods: " + line);
                }
            }
        }
    }

    @Test
    void angryJockyShouts() {
        for (Event event : Event.values()) {
            for (String line : JockyLines.linesFor(event, Mood.ANGRY)) {
                assertEquals(line.toUpperCase(), line, "not shouted: " + line);
            }
        }
    }

    @Test
    void friendlyJockyDropsAHintAboutTheHat() {
        assertTrue(JockyLines.linesFor(Event.MOVE, Mood.FRIENDLY).stream().anyMatch(line -> line.contains("hat")));
    }

    @Test
    void openingTheMarketMentionsCheatingChess() {
        assertTrue(JockyLines.linesFor(Event.MARKET_OPEN, Mood.SHADY).stream()
                .anyMatch(line -> line.toLowerCase().contains("cheating chess")));
    }

    @Test
    void everyDisasterHasAnEventOfTheSameName() {
        // CheatSession turns a disaster into Jocky's event by name.
        for (Chaos chaos : Chaos.values()) {
            assertFalse(JockyLines.linesFor(Event.valueOf(chaos.name()), Mood.ANGRY)
                    .equals(JockyLines.linesFor(Event.MOVE, Mood.ANGRY)), chaos.name());
        }
    }

    @Test
    void eventsWithNoLineOfTheirOwnFallBackToMoveLines() {
        // Friendly Jocky never sees a challenge, so he has nothing special for one.
        assertEquals(JockyLines.linesFor(Event.MOVE, Mood.FRIENDLY),
                JockyLines.linesFor(Event.CAUGHT_THEM, Mood.FRIENDLY));
    }
}
