package chess.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * What Jocky the mascot says. He has three moods: friendly and loud in
 * ordinary chess, shady and quiet in cheating chess, and furious in angry
 * chaotic mode. Kept apart from JavaFX so it can be tested without a window.
 */
final class JockyLines {

    /** Jocky's moods, one for each side of the application. */
    enum Mood {
        FRIENDLY, SHADY, ANGRY
    }

    /** The things that happen in a game that Jocky has something to say about. */
    enum Event {
        WELCOME, MOVE, CAPTURE, CHECK, GAME_WON, DRAW, OUT_OF_TIME,
        // The rest only happen in cheating chess.
        MARKET_OPEN, ANGRY_OPEN, SWITCH, FIDDLE, CAUGHT_THEM, WRONG_ACCUSATION, CAUGHT_YOU, DEBUFF,
        ROCKET, STORM, LAVA, ROLL_CALL, CEASEFIRE, TIME_WARP, FOG, HACKED, UNHACKED,
        YOU_WIN, YOU_LOSE
    }

    private static final Map<Mood, Map<Event, List<String>>> LINES = new EnumMap<>(Mood.class);

    static {
        Map<Event, List<String>> friendly = new EnumMap<>(Event.class);
        friendly.put(Event.WELCOME, List.of(
                "Hi, I'm Jocky! Let's play some chess!",
                "Welcome back, champ! Ready when you are!",
                "New game, new glory! Let's go!"));
        friendly.put(Event.MOVE, List.of(
                "Nice move!",
                "Ooh, I like it!",
                "Keep going, you've got this!",
                "Solid. Very solid.",
                "That's the spirit!",
                "Whatever you do, don't touch my hat."));
        friendly.put(Event.CAPTURE, List.of(
                "Yoink! Got one!",
                "That piece is going home early!",
                "Munch munch munch!"));
        friendly.put(Event.CHECK, List.of(
                "Check! Heads up!",
                "The king is sweating!",
                "Check! Somebody do something!"));
        friendly.put(Event.GAME_WON, List.of(
                "CHECKMATE! What a finish!",
                "Game over! Hats off! No wait, not my hat."));
        friendly.put(Event.DRAW, List.of(
                "A draw! Everybody wins. Sort of.",
                "Nobody lost! I call that a good day."));
        friendly.put(Event.OUT_OF_TIME, List.of(
                "Time's up! The clock wins this one.",
                "Tick tock... and that's the flag!"));

        Map<Event, List<String>> shady = new EnumMap<>(Event.class);
        shady.put(Event.MARKET_OPEN, List.of(
                "Psst. You touched the hat. Welcome to the black market.",
                "Cheating chess :)  You didn't hear it from me."));
        shady.put(Event.WELCOME, List.of(
                "Back for more? Keep your voice down.",
                "Fresh board. Same rules: there aren't many."));
        shady.put(Event.SWITCH, List.of(
                "Done. Nobody saw a thing.",
                "A queen in a knight costume. Classic.",
                "Lovely. They'll never check the label."));
        shady.put(Event.FIDDLE, List.of(
                "Time is money. You just printed some.",
                "What clock? I didn't see a clock."));
        shady.put(Event.MOVE, List.of(
                "Play it cool.",
                "Act natural.",
                "Nothing to see here.",
                "I know a guy who knows a knight.",
                "Trust nobody. Especially bishops."));
        shady.put(Event.CAPTURE, List.of(
                "It fell off the back of a truck.",
                "One less witness.",
                "Finders keepers."));
        shady.put(Event.CAUGHT_THEM, List.of(
                "Busted! I never trusted that piece.",
                "Caught red-handed. Well, green-handed."));
        shady.put(Event.WRONG_ACCUSATION, List.of(
                "Ouch. Innocent. That cost you a turn.",
                "An honest piece? In here? Unlucky."));
        shady.put(Event.CAUGHT_YOU, List.of(
                "They saw you! Deny everything.",
                "Cover's blown. I was never here."));
        shady.put(Event.YOU_WIN, List.of(
                "You got the king. Pleasure doing business.",
                "The king is yours. Tell no one how."));
        shady.put(Event.YOU_LOSE, List.of(
                "They got your king. We never met.",
                "Gone. Happens to the best crooks."));
        shady.put(Event.DRAW, List.of(
                "A draw. Everyone walks. Suspicious."));
        shady.put(Event.OUT_OF_TIME, List.of(
                "Out of time. Should have fiddled harder."));

        Map<Event, List<String>> angry = new EnumMap<>(Event.class);
        angry.put(Event.ANGRY_OPEN, List.of(
                "YOU SAID YES. NOW LOOK WHAT YOU'VE DONE.",
                "I WARNED YOU! NOBODY EVER LISTENS!"));
        angry.put(Event.WELCOME, List.of(
                "AGAIN?! FINE. AGAIN!",
                "NEW BOARD. I'LL BREAK THIS ONE TOO."));
        angry.put(Event.SWITCH, List.of(
                "CHEAT HARDER!",
                "GOOD. LIE TO THEM!"));
        angry.put(Event.FIDDLE, List.of(
                "STEAL ALL THE TIME! ALL OF IT!"));
        angry.put(Event.MOVE, List.of(
                "MOVE FASTER!",
                "IS THAT IT?!",
                "HIT SOMETHING!",
                "I'VE SEEN PAWNS WITH MORE NERVE!",
                "WHO PUT THESE SQUARES HERE?!"));
        angry.put(Event.CAPTURE, List.of(
                "YES! SMASH IT!",
                "MORE! TAKE MORE!"));
        angry.put(Event.CAUGHT_THEM, List.of(
                "GOT YOU, YOU CHEATING LUMP OF WOOD!",
                "LIAR! I KNEW IT!"));
        angry.put(Event.WRONG_ACCUSATION, List.of(
                "WRONG! NOW YOU PAY!",
                "INNOCENT?! I HATE INNOCENT!"));
        angry.put(Event.DEBUFF, List.of(
                "SUFFER!",
                "THAT'S WHAT YOU GET!"));
        angry.put(Event.CAUGHT_YOU, List.of(
                "THEY SAW YOU, YOU CLUMSY OAF!"));
        angry.put(Event.ROCKET, List.of(
                "ROCKET! I LOVE ROCKETS!",
                "BOOM! WHO NEEDS THAT CORNER ANYWAY!"));
        angry.put(Event.STORM, List.of(
                "RAIN! NOBODY GOES ANYWHERE!",
                "I ORDERED THIS WEATHER MYSELF!"));
        angry.put(Event.LAVA, List.of(
                "THE FLOOR IS LAVA! JUMP!",
                "SPIN THE WHEEL! BURN THE BOARD!"));
        angry.put(Event.ROLL_CALL, List.of(
                "ROLL CALL! EVERYONE ELSE SIT DOWN!",
                "ONLY THEM! NOBODY ELSE MOVES A MUSCLE!"));
        angry.put(Event.CEASEFIRE, List.of(
                "CEASEFIRE?! WHO ALLOWED PEACE?!",
                "NO CAPTURING! I HATE THIS ONE!"));
        angry.put(Event.TIME_WARP, List.of(
                "TIME WARP! YOUR CLOCK IS MINE NOW!",
                "SWAP THE CLOCKS! SWAP EVERYTHING!"));
        angry.put(Event.FOG, List.of(
                "FOG! GOOD LUCK SEEING ANYTHING!",
                "WHO'S IN THERE? NOBODY KNOWS!"));
        angry.put(Event.HACKED, List.of(
                "HACKED! IT WASN'T ME! ...IT WAS ME!",
                "THEY'RE IN THE BOARD! THEY'RE IN THE BOARD!"));
        angry.put(Event.UNHACKED, List.of(
                "FINE. YOU CAN COUNT. CONGRATULATIONS.",
                "FIREWALL UP. I'LL GET IN AGAIN."));
        angry.put(Event.YOU_WIN, List.of(
                "YOU WON?! ...FINE. GOOD. WHATEVER."));
        angry.put(Event.YOU_LOSE, List.of(
                "YOU LOST THE KING! THE KING!!"));
        angry.put(Event.DRAW, List.of(
                "A DRAW?! NOBODY WINS? UNACCEPTABLE!"));
        angry.put(Event.OUT_OF_TIME, List.of(
                "TOO SLOW! TOO SLOW! TOO SLOW!"));

        LINES.put(Mood.FRIENDLY, friendly);
        LINES.put(Mood.SHADY, shady);
        LINES.put(Mood.ANGRY, angry);
    }

    private JockyLines() {
    }

    /**
     * Something for Jocky to say about an event, in his current mood. If the
     * mood has nothing for that event, he says what he would about a move.
     */
    static String pick(Event event, Mood mood, Random random) {
        List<String> lines = linesFor(event, mood);
        return lines.get(random.nextInt(lines.size()));
    }

    /** Every line Jocky might say about an event in one mood; never empty. */
    static List<String> linesFor(Event event, Mood mood) {
        Map<Event, List<String>> lines = LINES.get(mood);
        return lines.getOrDefault(event, lines.get(Event.MOVE));
    }
}
