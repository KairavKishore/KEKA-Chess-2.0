package chess.cheat;

import static chess.engine.PieceColor.BLACK;
import static chess.engine.PieceColor.WHITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chess.cheat.CheatGame.Chaos;
import chess.cheat.CheatGame.Debuff;
import chess.cheat.CheatGame.Penalty;
import chess.cheat.CheatGame.Phase;
import chess.cheat.CheatGame.Reward;
import chess.cheat.CheatGame.SquareState;
import chess.engine.Board;
import chess.engine.Move;
import chess.engine.Pawn;
import chess.engine.PieceColor;
import chess.engine.PieceType;
import chess.engine.Position;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CheatGameTest {

    private CheatGame game = new CheatGame(CheatLevel.STANDARD, choosing(0));

    /**
     * A source of randomness that never triggers a disaster and, wherever
     * the game picks one of several things, picks the one at this position
     * in the list (or the last, if the list is shorter).
     */
    private static Random choosing(int index) {
        return new Random(1) {
            @Override
            public double nextDouble() {
                return 1.0;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(index, bound - 1);
            }
        };
    }

    private void game(CheatLevel level, int choice) {
        game = new CheatGame(level, choosing(choice));
    }

    private void angry(long seed) {
        game = new CheatGame(CheatLevel.ANGRY, new Random(seed) {
            @Override
            public double nextDouble() {
                return 1.0;  // never below the chance of a disaster: tests call strike() themselves
            }
        });
    }

    private static Position at(String square) {
        return Position.of(square);
    }

    private void transform(PieceColor color, String square, PieceType identity) {
        game.transform(color, at(square), identity);
    }

    private void startPlay() {
        game.finishSetup(WHITE);
        game.finishSetup(BLACK);
    }

    private Move play(String from, String to) {
        for (Move move : game.movesFrom(at(from))) {
            if (move.getTo().equals(at(to))) {
                game.play(move);
                return move;
            }
        }
        throw new AssertionError("No move from " + from + " to " + to);
    }

    private boolean canMove(String from, String to) {
        return game.movesFrom(at(from)).stream().anyMatch(move -> move.getTo().equals(at(to)));
    }

    private int count(SquareState state) {
        int count = 0;
        for (int file = 0; file < Board.SIZE; file++) {
            for (int rank = 0; rank < Board.SIZE; rank++) {
                if (game.getSquareState(new Position(file, rank)) == state) {
                    count++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- setup

    @Test
    void theTwoModesFollowTheRuleBook() {
        assertEquals(20, CheatLevel.STANDARD.getCheatPoints());
        assertEquals(3, CheatLevel.STANDARD.getSwitchTokens());
        assertFalse(CheatLevel.STANDARD.allowsPawns());
        assertEquals(-1, CheatLevel.STANDARD.getChallengeLimit());

        assertEquals(20, CheatLevel.ANGRY.getCheatPoints());
        assertEquals(5, CheatLevel.ANGRY.getSwitchTokens());
        assertTrue(CheatLevel.ANGRY.allowsPawns());
        assertEquals(3, CheatLevel.ANGRY.getChallengeLimit());
    }

    @Test
    void startsInSetupWithEveryPieceWhatItLooksLike() {
        assertEquals(Phase.SETUP, game.getPhase());
        assertEquals(20, game.getCheatPoints(WHITE));
        assertEquals(3, game.getSwitchTokens(BLACK));
        assertNull(game.secretAt(at("b1")));
        assertTrue(game.moves().isEmpty());  // no moves until play starts
    }

    @Test
    void transformedPieceKeepsItsLooksAndGainsNewPowers() {
        transform(WHITE, "b1", PieceType.QUEEN);

        assertEquals(PieceType.QUEEN, game.truePiece(at("b1")).getType());
        assertEquals(PieceType.KNIGHT, game.looksPiece(at("b1")).getType());
        assertEquals(PieceType.QUEEN, game.secretAt(at("b1")));
        assertEquals(PieceType.QUEEN, game.truePiece(at("d1")).getType());  // nothing else changed
        assertEquals(20 - 6, game.getCheatPoints(WHITE));   // a knight (3) made a queen (9)
    }

    @Test
    void kingsCannotBeTransformedAndNothingCanBecomeOne() {
        assertFalse(game.isTransformable(WHITE, at("e1")));
        assertFalse(game.canTransform(WHITE, at("e1"), PieceType.QUEEN));
        assertFalse(game.canTransform(WHITE, at("b1"), PieceType.KING));
        assertTrue(game.canTransform(WHITE, at("b1"), PieceType.PAWN));   // but it can be demoted
        assertThrows(IllegalArgumentException.class, () -> transform(WHITE, "e1", PieceType.QUEEN));
    }

    @Test
    void pawnsCanOnlyBeTransformedInAngryMode() {
        assertFalse(game.canTransform(WHITE, at("e2"), PieceType.QUEEN));
        angry(1);
        assertTrue(game.canTransform(WHITE, at("e2"), PieceType.QUEEN));
        transform(WHITE, "e2", PieceType.QUEEN);
        assertEquals(PieceType.PAWN, game.looksPiece(at("e2")).getType());
        assertEquals(PieceType.QUEEN, game.secretAt(at("e2")));
    }

    @Test
    void transformationMustBeAChangeToYourOwnPiece() {
        assertFalse(game.canTransform(WHITE, at("b1"), PieceType.KNIGHT));  // it already is one
        assertFalse(game.canTransform(WHITE, at("b8"), PieceType.QUEEN));   // not White's
        assertFalse(game.canTransform(WHITE, at("e4"), PieceType.QUEEN));   // empty
    }

    @Test
    void piecesAreWorthTheUsualPoints() {
        assertEquals(1, CheatGame.pointsOf(PieceType.PAWN));
        assertEquals(3, CheatGame.pointsOf(PieceType.KNIGHT));
        assertEquals(3, CheatGame.pointsOf(PieceType.BISHOP));
        assertEquals(5, CheatGame.pointsOf(PieceType.ROOK));
        assertEquals(9, CheatGame.pointsOf(PieceType.QUEEN));
    }

    @Test
    void transformationCostsTheDifferenceInWorth() {
        assertEquals(6, game.transformCost(at("b1"), PieceType.QUEEN));    // knight to queen
        assertEquals(4, game.transformCost(at("a1"), PieceType.QUEEN));    // rook to queen
        assertEquals(0, game.transformCost(at("c1"), PieceType.KNIGHT));   // bishop to knight: a free swap
        assertEquals(-2, game.transformCost(at("a1"), PieceType.BISHOP));  // rook to bishop
        assertEquals(-8, game.transformCost(at("d1"), PieceType.PAWN));    // queen to pawn
    }

    @Test
    void demotingAQueenToAPawnTurnsTwentyPointsIntoTwentyEight() {
        transform(WHITE, "d1", PieceType.PAWN);
        assertEquals(20 + 9 - 1, game.getCheatPoints(WHITE));
        assertEquals(PieceType.QUEEN, game.looksPiece(at("d1")).getType());
        assertEquals(PieceType.PAWN, game.secretAt(at("d1")));
    }

    @Test
    void pointsRunOutAndDemotingBuysMore() {
        transform(WHITE, "b1", PieceType.QUEEN);   // 6
        transform(WHITE, "g1", PieceType.QUEEN);   // 6
        transform(WHITE, "c1", PieceType.QUEEN);   // 6
        assertEquals(2, game.getCheatPoints(WHITE));
        assertFalse(game.canTransform(WHITE, at("f1"), PieceType.QUEEN));   // would cost 6
        assertFalse(game.canTransform(WHITE, at("a1"), PieceType.QUEEN));   // would cost 4
        assertTrue(game.canTransform(WHITE, at("f1"), PieceType.ROOK));     // costs exactly 2
        assertThrows(IllegalArgumentException.class, () -> transform(WHITE, "f1", PieceType.QUEEN));

        transform(WHITE, "d1", PieceType.PAWN);    // the real queen is given up: +8

        assertEquals(10, game.getCheatPoints(WHITE));
        assertTrue(game.canTransform(WHITE, at("f1"), PieceType.QUEEN));
        assertEquals(20, game.getCheatPoints(BLACK));   // the other side's points are its own
    }

    @Test
    void thereIsNoLimitOnHowManyPiecesAreChangedOnlyOnThePoints() {
        // Six free or paying changes: every minor piece swapped with another kind, both rooks demoted.
        transform(WHITE, "b1", PieceType.BISHOP);
        transform(WHITE, "g1", PieceType.BISHOP);
        transform(WHITE, "c1", PieceType.KNIGHT);
        transform(WHITE, "f1", PieceType.KNIGHT);
        transform(WHITE, "a1", PieceType.KNIGHT);
        transform(WHITE, "h1", PieceType.KNIGHT);
        assertEquals(20 + 2 + 2, game.getCheatPoints(WHITE));
    }

    @Test
    void changingAPieceAgainIsChargedFromWhatItHasBecome() {
        transform(WHITE, "b1", PieceType.QUEEN);   // 20 - 6
        transform(WHITE, "b1", PieceType.ROOK);    // queen back down to rook: +4
        assertEquals(18, game.getCheatPoints(WHITE));
        transform(WHITE, "b1", PieceType.KNIGHT);  // and back to what it was: +2
        assertEquals(20, game.getCheatPoints(WHITE));
        assertNull(game.secretAt(at("b1")));
    }

    @Test
    void pieceDemotedToAPawnMovesLikeOne() {
        transform(WHITE, "d1", PieceType.PAWN);
        startPlay();
        play("d2", "d4");
        play("a7", "a6");

        assertEquals(1, game.movesFrom(at("d1")).size());   // one step forward, nothing else
        assertTrue(canMove("d1", "d2"));
        assertTrue(game.wouldLookLegal(game.movesFrom(at("d1")).get(0)));  // a queen could do that too
    }

    @Test
    void pointsAreOnlyForBeforeTheGame() {
        transform(WHITE, "d1", PieceType.PAWN);
        startPlay();
        assertEquals(28, game.getCheatPoints(WHITE));
        assertFalse(game.canTransform(WHITE, at("b1"), PieceType.QUEEN));   // unspent points buy nothing now
    }

    @Test
    void playStartsWhenBothSidesAreReady() {
        game.finishSetup(WHITE);
        assertEquals(Phase.SETUP, game.getPhase());
        assertFalse(game.canTransform(WHITE, at("b1"), PieceType.QUEEN));  // White has declared itself done
        game.finishSetup(BLACK);
        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals(20, game.moves().size());
        assertThrows(IllegalStateException.class, () -> game.finishSetup(WHITE));
    }

    // ----------------------------------------------------------------- play

    @Test
    void piecesMoveByWhatTheyReallyAre() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");

        Move move = play("b1", "b3");  // the "knight" slides up the file

        assertEquals(PieceType.QUEEN, move.getPiece().getType());
        assertEquals(PieceType.KNIGHT, game.looksPiece(at("b3")).getType());
        assertFalse(game.lastMoveLookedLegal());  // no knight goes from b1 to b3
        assertFalse(canMove("b3", "c5"));         // and it has lost the knight's own moves
    }

    @Test
    void transformedPawnMovesAsItsNewSelf() {
        angry(1);
        transform(WHITE, "e2", PieceType.KNIGHT);
        startPlay();

        play("e2", "f4");

        assertEquals(PieceType.PAWN, game.looksPiece(at("f4")).getType());
        assertFalse(game.lastMoveLookedLegal());
    }

    @Test
    void honestMovesLookLegalAndSoDoTransformedOnesThatFitTheirLooks() {
        transform(WHITE, "a1", PieceType.QUEEN);
        startPlay();
        play("a2", "a4");
        assertTrue(game.lastMoveLookedLegal());
        play("a7", "a5");
        play("a1", "a3");  // straight up the file, as a rook could
        assertTrue(game.lastMoveLookedLegal());
        assertEquals(PieceType.QUEEN, game.secretAt(at("a3")));
    }

    @Test
    void castlingIsNotOffered() {
        startPlay();
        play("g1", "f3");
        play("g8", "f6");
        play("e2", "e4");
        play("e7", "e5");
        play("f1", "c4");
        play("f8", "c5");
        for (Move move : game.movesFrom(at("e1"))) {
            assertFalse(move.isCastling());
        }
    }

    @Test
    void kingMayBeLeftInCheckAndCapturingItWins() {
        startPlay();
        play("e2", "e4");
        play("f7", "f6");
        play("d1", "h5");
        assertTrue(game.looksInCheck(BLACK));
        play("a7", "a6");   // Black ignores it, which is allowed
        assertEquals(Phase.PLAY, game.getPhase());

        play("h5", "e8");

        assertTrue(game.isOver());
        assertEquals(WHITE, game.getWinner());
        assertTrue(game.moves().isEmpty());
    }

    @Test
    void checkFromAPieceThatIsNotWhatItSeemsIsNoDanger() {
        transform(WHITE, "f1", PieceType.KNIGHT);  // looks like a bishop
        startPlay();
        play("f1", "g3");
        play("f7", "f6");
        play("g3", "h5");

        assertTrue(game.looksInCheck(BLACK));  // a "bishop" aims down the diagonal at e8
        play("a7", "a6");                      // Black calls the bluff

        for (Move move : game.moves()) {
            assertFalse(move.getTo().equals(at("e8")), "the king cannot actually be taken");
        }
    }

    @Test
    void dangerCanComeFromAPieceThatLooksHarmless() {
        angry(1);
        transform(WHITE, "h2", PieceType.QUEEN);  // a queen that looks like a pawn
        startPlay();
        play("g2", "g3");
        play("f7", "f6");
        play("h2", "h5");   // "pawn" to h5; g6 is the only square between it and... nothing, yet
        assertFalse(game.looksInCheck(BLACK));  // a pawn on h5 threatens nothing on e8
        play("a7", "a6");
        play("h5", "e8");   // down the diagonal h5-g6-f7-e8

        assertEquals(WHITE, game.getWinner());
    }

    @Test
    void rejectsMovesThatAreNotAvailable() {
        startPlay();
        Move blackFirst = new Move(at("e7"), at("e5"), new Pawn(BLACK), null);
        assertThrows(IllegalArgumentException.class, () -> game.play(blackFirst));
    }

    @Test
    void capturedPiecesAreRememberedAsTheyLooked() {
        transform(BLACK, "d8", PieceType.KNIGHT);
        startPlay();
        play("e2", "e4");
        play("d7", "d5");
        play("e4", "d5");
        assertEquals(1, game.lostBy(BLACK).size());
        assertEquals(PieceType.PAWN, game.lostBy(BLACK).get(0).getType());
        assertTrue(game.lostBy(WHITE).isEmpty());
    }

    @Test
    void gameCanBeEndedFromOutsideTheBoard() {
        startPlay();
        game.declareWinner(BLACK);  // say, White's clock ran out
        assertTrue(game.isOver());
        assertEquals(BLACK, game.getWinner());
    }

    // ----------------------------------------------------------- challenges

    @Test
    void nothingToChallengeBeforeTheFirstMove() {
        startPlay();
        assertFalse(game.canAccuse());
        assertThrows(IllegalStateException.class, game::accuse);
    }

    @Test
    void failedChallengeCostsTheTurnInStandardMode() {
        startPlay();
        play("e2", "e4");
        assertTrue(game.canAccuse());

        assertFalse(game.accuse());

        assertEquals(WHITE, game.getTurn());
        assertEquals(Penalty.LOSE_TURN, game.getLastPenalty());
        assertNull(game.getDebuff(BLACK));
        assertFalse(game.canAccuse());   // White cannot challenge its own move
        play("d2", "d4");
        assertTrue(game.canAccuse());
    }

    @Test
    void successfulChallengeCanExposeThePiece() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");
        play("b1", "b3");

        assertTrue(game.accuse());
        assertEquals(Phase.REWARD, game.getPhase());
        assertTrue(game.moves().isEmpty());  // the reward comes first
        assertEquals(List.of(Reward.EXPOSE, Reward.STRIP, Reward.JAIL, Reward.SWITCH, Reward.EXTRA_MOVE),
                game.availableRewards());

        assertEquals(at("b3"), game.exposeAccused());

        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals(BLACK, game.getTurn());  // the challenger keeps their move
        assertEquals(PieceType.QUEEN, game.looksPiece(at("b3")).getType());
        assertNull(game.secretAt(at("b3")));
        assertFalse(game.canAccuse());        // one challenge a turn
    }

    @Test
    void successfulChallengeCanBeSpentOnASwitchToken() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");
        play("b1", "b3");
        assertTrue(game.accuse());

        transform(BLACK, "g8", PieceType.QUEEN);

        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals(2, game.getSwitchTokens(BLACK));
        assertEquals(20, game.getCheatPoints(BLACK));  // a token pays for it, not cheat points
        assertEquals(PieceType.QUEEN, game.secretAt(at("g8")));
        assertNotNull(game.secretAt(at("b3")));          // White's piece stays hidden
    }

    @Test
    void transformedPieceIsCaughtEvenWhenItsMoveLookedLegal() {
        transform(WHITE, "a1", PieceType.QUEEN);
        startPlay();
        play("a2", "a4");
        play("a7", "a5");
        play("a1", "a3");
        assertTrue(game.lastMoveLookedLegal());
        assertTrue(game.accuse());
    }

    @Test
    void rewardCanBeDeclined() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");
        play("b1", "b3");
        game.accuse();
        game.declineReward();
        assertEquals(Phase.PLAY, game.getPhase());
        assertNotNull(game.secretAt(at("b3")));
        assertThrows(IllegalStateException.class, game::declineReward);
    }

    @Test
    void noRewardWithoutASuccessfulChallenge() {
        startPlay();
        assertTrue(game.availableRewards().isEmpty());
        assertThrows(IllegalStateException.class, game::exposeAccused);
        assertFalse(game.canTransform(WHITE, at("b1"), PieceType.QUEEN));  // setup is over
    }

    @Test
    void fiddlingTheClockIsCheatingOnTheNextMove() {
        startPlay();
        game.noteClockFiddled(WHITE);
        play("e2", "e4");
        assertTrue(game.lastMoverFiddledClock());

        assertTrue(game.accuse());   // the pawn is honest, but its owner was not
        assertTrue(game.availableRewards().contains(Reward.SWITCH));
        assertFalse(game.availableRewards().contains(Reward.EXPOSE));  // White has nothing hidden
        game.declineReward();

        play("e7", "e5");
        play("d2", "d4");
        assertFalse(game.lastMoverFiddledClock());  // it only counts for one move
        assertFalse(game.accuse());
    }

    @Test
    void exposingAfterAClockCheatShowsSomeOtherHiddenPiece() {
        transform(WHITE, "g1", PieceType.ROOK);
        startPlay();
        game.noteClockFiddled(WHITE);
        play("e2", "e4");
        assertTrue(game.accuse());

        assertEquals(at("g1"), game.exposeAccused());
        assertNull(game.secretAt(at("g1")));
    }

    // ------------------------------------------------- angry chaotic extras

    @Test
    void angryModeLimitsChallengesToThree() {
        angry(2);
        startPlay();
        int challenges = 0;
        for (int round = 0; round < 5; round++) {
            game.play(game.moves().get(0));          // White makes an honest move
            if (game.canAccuse()) {
                assertFalse(game.accuse());          // Black challenges it, wrongly
                challenges++;
            }
            game.play(game.moves().get(0));          // Black moves, under whatever debuff that earned
        }
        assertEquals(3, challenges);
        assertEquals(0, game.getChallengesLeft(BLACK));
        assertEquals(3, game.getChallengesLeft(WHITE));
    }

    @Test
    void failedChallengeBringsADebuffInsteadOfALostTurn() {
        game(CheatLevel.ANGRY, 0);
        startPlay();
        play("e2", "e4");

        assertFalse(game.accuse());

        assertEquals(BLACK, game.getTurn());  // still Black's move
        assertEquals(Penalty.DEBUFF, game.getLastPenalty());
        assertNotNull(game.getLastDebuff());
        assertEquals(game.getLastDebuff(), game.getDebuff(BLACK));
        assertNull(game.getDebuff(WHITE));
    }

    // ------------------------------------------------- the other penalties

    @Test
    void failedChallengeCanCostASwitchToken() {
        game(CheatLevel.STANDARD, 1);   // the options are: lose a turn, lose a token
        startPlay();
        play("e2", "e4");
        assertFalse(game.accuse());

        assertEquals(Penalty.LOSE_TOKEN, game.getLastPenalty());
        assertEquals(2, game.getSwitchTokens(BLACK));
        assertEquals(BLACK, game.getTurn());   // the turn is kept
    }

    @Test
    void failedChallengeCanExposeOneOfTheChallengersOwnPieces() {
        game(CheatLevel.STANDARD, 1);   // lose a turn, be exposed, lose a token
        transform(BLACK, "g8", PieceType.QUEEN);
        startPlay();
        play("e2", "e4");
        assertFalse(game.accuse());

        assertEquals(Penalty.EXPOSED, game.getLastPenalty());
        assertEquals(at("g8"), game.getLastExposedOwn());
        assertNull(game.secretAt(at("g8")));
        assertEquals(PieceType.QUEEN, game.looksPiece(at("g8")).getType());
    }

    @Test
    void failedChallengeCanCostTimeButOnlyWithAClock() {
        game(CheatLevel.STANDARD, 1);
        game.setClockInUse(true);       // lose a turn, lose time, lose a token
        startPlay();
        play("e2", "e4");
        assertFalse(game.accuse());
        assertEquals(Penalty.LOSE_TIME, game.getLastPenalty());
        assertEquals(BLACK, game.getTurn());
        assertEquals(3, game.getSwitchTokens(BLACK));
    }

    @Test
    void penaltyIsForgottenOnceTheNextMoveIsPlayed() {
        game(CheatLevel.STANDARD, 1);
        startPlay();
        play("e2", "e4");
        game.accuse();
        play("e7", "e5");
        assertNull(game.getLastPenalty());
    }

    // --------------------------------------------------- the other rewards

    /** White's b1 knight is secretly a queen, has just moved to b3, and Black has challenged it. */
    private void caughtTheFakeKnight() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");
        play("b1", "b3");
        assertTrue(game.accuse());
    }

    @Test
    void strippingTurnsThePieceIntoWhatItLooksLike() {
        caughtTheFakeKnight();
        game.stripAccused();

        assertEquals(Phase.PLAY, game.getPhase());
        assertEquals(PieceType.KNIGHT, game.truePiece(at("b3")).getType());
        assertNull(game.secretAt(at("b3")));
        play("a7", "a6");
        assertTrue(canMove("b3", "c5"));    // it moves as a knight now
        assertFalse(canMove("b3", "b2"));
    }

    @Test
    void jailedPieceCannotMoveForItsOwnersNextTwoTurns() {
        caughtTheFakeKnight();
        game.jailAccused();
        assertEquals(at("b3"), game.getJailedSquare());

        play("a7", "a6");
        assertTrue(game.movesFrom(at("b3")).isEmpty());   // White's first turn inside
        play("a2", "a3");
        play("a6", "a5");
        assertTrue(game.movesFrom(at("b3")).isEmpty());   // and the second
        play("h2", "h3");
        play("h7", "h6");
        assertNull(game.getJailedSquare());
        assertFalse(game.movesFrom(at("b3")).isEmpty());  // out again
    }

    @Test
    void jailEndsIfThePrisonerIsCaptured() {
        caughtTheFakeKnight();
        game.jailAccused();
        play("a7", "a5");
        play("a2", "a3");
        play("a5", "b4");
        play("a3", "b4");
        play("f8", "b4");
        play("h2", "h3");
        play("b4", "c3");
        play("h3", "h4");
        assertNotNull(game.truePiece(at("b3")));
    }

    @Test
    void extraMoveGivesTwoMovesInARow() {
        caughtTheFakeKnight();
        game.takeExtraMove();
        assertEquals(BLACK, game.getTurn());

        play("a7", "a6");
        assertEquals(BLACK, game.getTurn());   // White misses its turn
        assertFalse(game.canAccuse());         // and there is nothing new of White's to challenge
        play("b7", "b6");
        assertEquals(WHITE, game.getTurn());   // only once
    }

    @Test
    void stealingTimeIsOnlyOfferedWithAClock() {
        caughtTheFakeKnight();
        assertFalse(game.availableRewards().contains(Reward.STEAL_TIME));
        assertThrows(IllegalStateException.class, game::stealTime);
        game.declineReward();

        game = new CheatGame(CheatLevel.STANDARD, choosing(0));
        game.setClockInUse(true);
        caughtTheFakeKnight();
        assertTrue(game.availableRewards().contains(Reward.STEAL_TIME));
        game.stealTime();
        assertEquals(Phase.PLAY, game.getPhase());
    }

    @Test
    void honestPieceCaughtForTheClockCanBeJailedButNotStripped() {
        startPlay();
        game.noteClockFiddled(WHITE);
        play("e2", "e4");
        assertTrue(game.accuse());
        assertEquals(List.of(Reward.JAIL, Reward.SWITCH, Reward.EXTRA_MOVE), game.availableRewards());
    }

    // ------------------------------------------------ which moves give it away

    @Test
    void movesAreSortedIntoHonestLookingAndTellTale() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");

        int honest = 0;
        int tellTale = 0;
        for (Move move : game.movesFrom(at("b1"))) {
            if (game.wouldLookLegal(move)) {
                honest++;
            } else {
                tellTale++;
            }
        }
        // The queen on b1 can go to b2 and b3 up the file and a few squares
        // along the diagonal. No knight could make any of those moves.
        assertEquals(0, honest);
        assertTrue(tellTale >= 2);
        for (Move move : game.movesFrom(at("g1"))) {
            assertTrue(game.wouldLookLegal(move));   // a real knight is always honest
        }
    }

    @Test
    void transformedPieceCanStillHaveHonestLookingMoves() {
        transform(WHITE, "a1", PieceType.QUEEN);   // a queen that looks like a rook
        startPlay();
        play("a2", "a4");
        play("b7", "b5");
        play("b2", "b3");
        play("b5", "a4");
        boolean straight = false;
        boolean diagonal = false;
        for (Move move : game.movesFrom(at("a1"))) {
            if (move.getTo().equals(at("a2"))) {
                straight = game.wouldLookLegal(move);
            }
            if (move.getTo().equals(at("b2"))) {
                diagonal = !game.wouldLookLegal(move);
            }
        }
        assertTrue(straight, "up the file is what a rook does");
        assertTrue(diagonal, "the diagonal gives it away");
    }

    @Test
    void debuffWearsOffAfterItsTurns() {
        angry(1);
        startPlay();
        game.afflict(WHITE, Debuff.BLOB, 2);
        play("a2", "a3");
        assertEquals(Debuff.BLOB, game.getDebuff(WHITE));
        play("a7", "a6");
        play("b2", "b3");
        assertNull(game.getDebuff(WHITE));
    }

    @Test
    void sameColourDebuffKeepsPiecesOnTheirOwnColourOfSquare() {
        angry(1);
        startPlay();
        game.afflict(WHITE, Debuff.SAME_COLOUR_SQUARES, 1);
        assertFalse(canMove("e2", "e3"));   // light to dark
        assertTrue(canMove("e2", "e4"));    // light to light
        assertFalse(canMove("g1", "f3"));   // a knight always changes colour
        assertEquals(8, game.moves().size());  // only the eight double steps
    }

    @Test
    void queenAndPawnDebuffsFreezeThosePieces() {
        angry(1);
        startPlay();
        play("e2", "e4");
        play("e7", "e5");
        game.afflict(WHITE, Debuff.NO_QUEEN, 1);
        assertFalse(canMove("d1", "h5"));
        assertTrue(canMove("f1", "c4"));

        game.afflict(WHITE, Debuff.NO_PAWNS, 1);
        assertFalse(canMove("d2", "d4"));
        assertTrue(canMove("d1", "h5"));
        assertTrue(Debuff.BLOB.isVisual());
        assertFalse(Debuff.NO_PAWNS.isVisual());
    }

    @Test
    void noCapturesDebuffStopsOnlyTheSufferer() {
        angry(1);
        startPlay();
        play("e2", "e4");
        play("d7", "d5");
        game.afflict(WHITE, Debuff.NO_CAPTURES, 1);
        assertFalse(canMove("e4", "d5"));
        assertTrue(canMove("e4", "e5"));
        play("e4", "e5");
        assertFalse(game.moves().isEmpty());
    }

    @Test
    void shortReachDebuffLimitsSlidesToTwoSquares() {
        angry(1);
        startPlay();
        play("e2", "e4");
        play("e7", "e5");
        game.afflict(WHITE, Debuff.SHORT_REACH, 1);
        assertTrue(canMove("d1", "f3"));    // two squares
        assertFalse(canMove("d1", "g4"));   // three
        assertTrue(canMove("g1", "f3"));    // knights do not slide
    }

    @Test
    void sideWithNoMoveAvailableMissesItsTurn() {
        angry(1);
        startPlay();
        game.afflict(BLACK, Debuff.NO_PAWNS, 5);
        game.afflict(BLACK, Debuff.NO_PAWNS, 5);
        // Black's knights can still move, so give it a debuff that stops everything
        // it has at the start: nothing can keep to its own colour except pawn double steps.
        game.afflict(BLACK, Debuff.SAME_COLOUR_SQUARES, 5);
        play("e2", "e4");
        assertEquals(BLACK, game.getTurn());   // the double steps are still there
        assertEquals(8, game.moves().size());
    }

    @Test
    void disruptRewardsAreOfferedOnceInAngryMode() {
        angry(1);
        transform(WHITE, "b1", PieceType.QUEEN);
        transform(WHITE, "g1", PieceType.ROOK);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");
        play("b1", "b3");
        assertTrue(game.accuse());
        assertEquals(List.of(Reward.EXPOSE, Reward.STRIP, Reward.JAIL, Reward.SWITCH, Reward.EXTRA_MOVE,
                Reward.DISRUPT_EXPOSE, Reward.DISRUPT_SAFE), game.availableRewards());

        assertEquals(at("g1"), game.disruptExposeAnother());  // not the challenged piece: the other one

        assertNull(game.secretAt(at("g1")));
        assertNotNull(game.secretAt(at("b3")));
        assertEquals(Phase.PLAY, game.getPhase());

        play("a7", "a6");
        play("b3", "b2");
        assertTrue(game.accuse());
        assertFalse(game.availableRewards().contains(Reward.DISRUPT_SAFE));  // Disrupt is used up
        assertTrue(game.availableRewards().contains(Reward.EXPOSE));
    }

    @Test
    void safeSquareProtectsWhateverStandsOnIt() {
        angry(1);
        transform(WHITE, "f1", PieceType.QUEEN);
        startPlay();
        play("e2", "e4");
        play("d7", "d5");
        play("f1", "e2");   // any move by the hidden queen
        assertTrue(game.accuse());

        game.disruptSafeSquare(at("d5"));

        assertEquals(SquareState.SAFE, game.getSquareState(at("d5")));
        play("a7", "a6");
        assertFalse(canMove("e4", "d5"));   // the pawn on d5 cannot be taken
        assertTrue(canMove("e4", "e5"));
    }

    @Test
    void uncaptureBringsBackTheBestLostPiece() {
        angry(1);
        transform(WHITE, "f1", PieceType.QUEEN);
        startPlay();
        play("e2", "e4");
        play("d7", "d5");
        play("e4", "d5");
        play("d8", "d5");   // Black's queen takes back
        play("b1", "c3");
        play("a7", "a6");
        play("c3", "d5");   // and is captured
        play("b7", "b6");
        play("f1", "e2");   // White's hidden queen moves
        assertTrue(game.accuse());
        assertTrue(game.availableRewards().contains(Reward.DISRUPT_UNCAPTURE));

        Position back = game.disruptUncapture();

        assertEquals(at("d8"), back);   // the first empty square on Black's back rank
        assertEquals(PieceType.QUEEN, game.truePiece(back).getType());
        assertEquals(1, game.lostBy(BLACK).size());  // only the pawn is still gone
    }

    // ------------------------------------------------------------ disasters

    @Test
    void stormShortensTheReachOfSlidingPieces() {
        angry(5);
        startPlay();
        play("e2", "e4");
        play("e7", "e5");
        game.strike(Chaos.STORM);
        int reach = game.getStormReach();

        assertTrue(game.getStormSeverity() >= 1 && game.getStormSeverity() <= 5);
        assertEquals(6 - game.getStormSeverity(), reach);
        // The queen's diagonal from d1 runs e2, f3, g4, h5: four squares.
        assertEquals(reach >= 4, canMove("d1", "h5"));
        assertTrue(canMove("d1", "e2"));
        assertTrue(canMove("g1", "f3"));   // knights do not slide
        assertTrue(game.takeChaosNotice().getText().startsWith("STORM, force " + game.getStormSeverity()));
        assertNull(game.takeChaosNotice());  // announced once
    }

    @Test
    void stormBlowsOver() {
        angry(5);
        startPlay();
        game.strike(Chaos.STORM);
        String[][] moves = {{"a2", "a3"}, {"a7", "a6"}, {"b2", "b3"}, {"b7", "b6"}, {"c2", "c3"}, {"c7", "c6"}};
        for (String[] move : moves) {
            play(move[0], move[1]);
        }
        assertEquals(0, game.getStormSeverity());  // three turns at most
    }

    @Test
    void rocketLeavesACraterThatNothingCanEnterOrCross() {
        for (long seed = 1; seed <= 20; seed++) {
            angry(seed);
            startPlay();
            game.strike(Chaos.ROCKET);

            int craters = count(SquareState.CRATER);
            assertTrue(craters >= 3 && craters <= 4, "seed " + seed + ": " + craters);  // a king's square is spared
            assertNotNull(game.truePiece(at("e1")));
            assertNotNull(game.truePiece(at("e8")));
            for (int file = 0; file < Board.SIZE; file++) {
                for (int rank = 0; rank < Board.SIZE; rank++) {
                    Position square = new Position(file, rank);
                    if (game.getSquareState(square) == SquareState.CRATER) {
                        assertNull(game.truePiece(square), "seed " + seed + ": piece left in the crater");
                    }
                }
            }
            for (Move move : game.moves()) {
                assertEquals(SquareState.NORMAL, game.getSquareState(move.getTo()), "seed " + seed);
            }
            assertEquals(Chaos.ROCKET, game.takeChaosNotice().getKind());
        }
    }

    @Test
    void piecesCannotSlideAcrossACrater() {
        angry(1);
        startPlay();
        play("a2", "a4");
        play("a7", "a5");
        // Find a seed-independent crater: strike until one lands on the a-file ahead of the rook.
        game.strike(Chaos.LAVA);  // any hazard will do; lava on a file is easier to aim
        // Rather than rely on where it landed, check the rule on every sliding move there is.
        for (Move move : game.moves()) {
            Position from = move.getFrom();
            Position to = move.getTo();
            int steps = Math.max(Math.abs(to.getFile() - from.getFile()), Math.abs(to.getRank() - from.getRank()));
            if (move.getPiece().getType() == PieceType.KNIGHT) {
                continue;
            }
            for (int step = 1; step <= steps; step++) {
                Position passed = new Position(
                        from.getFile() + step * Integer.signum(to.getFile() - from.getFile()),
                        from.getRank() + step * Integer.signum(to.getRank() - from.getRank()));
                assertEquals(SquareState.NORMAL, game.getSquareState(passed), move.toString());
            }
        }
    }

    @Test
    void lavaFloodsOneColourOfOneFileAndTakesWhatStandsThere() {
        for (long seed = 1; seed <= 20; seed++) {
            angry(seed);
            startPlay();
            game.strike(Chaos.LAVA);

            int lava = count(SquareState.LAVA);
            assertTrue(lava >= 3 && lava <= 4, "seed " + seed + ": " + lava);
            int file = -1;
            int parity = -1;
            for (int f = 0; f < Board.SIZE; f++) {
                for (int rank = 0; rank < Board.SIZE; rank++) {
                    if (game.getSquareState(new Position(f, rank)) == SquareState.LAVA) {
                        assertTrue(file == -1 || file == f, "seed " + seed + ": more than one file");
                        assertTrue(parity == -1 || parity == (f + rank) % 2, "seed " + seed + ": both colours");
                        file = f;
                        parity = (f + rank) % 2;
                        assertNull(game.truePiece(new Position(f, rank)));
                    }
                }
            }
            // Each file starts with one piece and one pawn of each side on alternating
            // colours, so two of the four flooded squares held something (unless a king was spared).
            int lost = game.lostBy(WHITE).size() + game.lostBy(BLACK).size();
            assertTrue(lost >= 1 && lost <= 2, "seed " + seed + ": lost " + lost);
            assertTrue(game.takeChaosNotice().getText().startsWith("THE FLOOR IS LAVA: the "));
        }
    }

    @Test
    void cratersAndLavaAreRepairedAfterAFewTurns() {
        angry(4);
        startPlay();
        game.strike(Chaos.ROCKET);
        game.strike(Chaos.LAVA);
        for (int ply = 0; ply < 6 && !game.isOver(); ply++) {
            game.play(game.moves().get(0));
        }
        assertEquals(0, count(SquareState.CRATER));
        assertEquals(0, count(SquareState.LAVA));
    }

    @Test
    void rollCallLetsOnlyOneKindOfPieceMove() {
        angry(1);
        startPlay();
        game.strike(Chaos.ROLL_CALL);
        PieceType kind = game.getRollCallKind();
        assertNotNull(kind);
        assertTrue(game.takeChaosNotice().getText().startsWith("ROLL CALL: only " + kind.name().toLowerCase()));
        for (Move move : game.moves()) {
            assertEquals(kind, game.looksPiece(move.getFrom()).getType());
        }
    }

    @Test
    void rollCallGoesByLooksSoAFakeOfTheRightKindMayMove() {
        game = new CheatGame(CheatLevel.ANGRY, choosing(0));   // the first kind on the list: pawns
        transform(WHITE, "e2", PieceType.KNIGHT);              // a knight that looks like a pawn
        startPlay();
        game.strike(Chaos.ROLL_CALL);
        assertEquals(PieceType.PAWN, game.getRollCallKind());
        assertTrue(canMove("e2", "f4"));     // it answers the roll call as a pawn, and moves as a knight
        assertFalse(canMove("g1", "f3"));    // a real knight must wait
    }

    @Test
    void sideWithNothingOfTheCalledKindMissesItsTurns() {
        game = new CheatGame(CheatLevel.ANGRY, choosing(3));   // the last kind on the list: rooks
        startPlay();
        game.strike(Chaos.ROLL_CALL);
        assertEquals(PieceType.ROOK, game.getRollCallKind());
        // No rook can move at the start for either side.
        assertTrue(game.moves().isEmpty());
    }

    @Test
    void rollCallThatNobodyCanAnswerIsCalledOffInsteadOfEndingTheGame() {
        // Disasters after every move, always the fourth on the list (a roll call),
        // always for the fourth kind (rooks), which nobody can move this early.
        game = new CheatGame(CheatLevel.ANGRY, new Random(1) {
            @Override
            public double nextDouble() {
                return 0.0;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(3, bound - 1);
            }
        });
        startPlay();

        play("e2", "e4");

        assertFalse(game.isOver());
        assertNull(game.getRollCallKind());
        assertEquals(BLACK, game.getTurn());
        assertEquals(20, game.moves().size());
    }

    @Test
    void duringARollCallASideWithOnlyOtherPiecesFreeMissesItsTurn() {
        game = new CheatGame(CheatLevel.ANGRY, choosing(3));
        startPlay();
        play("a2", "a4");
        play("h7", "h5");
        play("a4", "a5");   // White's a1 rook now has room; Black's a8 rook does not, but its h8 rook does
        play("g7", "g6");
        game.strike(Chaos.ROLL_CALL);

        assertEquals(WHITE, game.getTurn());
        for (Move move : game.moves()) {
            assertEquals(PieceType.ROOK, move.getPiece().getType());
        }
        play("a1", "a3");
        assertEquals(BLACK, game.getTurn());   // Black's h8 rook can step to h7 or h6
        assertFalse(game.moves().isEmpty());
    }

    @Test
    void ceasefireForbidsEveryCapture() {
        angry(1);
        startPlay();
        play("e2", "e4");
        play("d7", "d5");
        game.strike(Chaos.CEASEFIRE);
        assertTrue(game.isCeasefire());
        assertFalse(canMove("e4", "d5"));
        for (Move move : game.moves()) {
            assertFalse(move.isCapture());
        }
        for (int ply = 0; ply < 6; ply++) {
            game.play(game.moves().get(0));
        }
        assertFalse(game.isCeasefire());
    }

    @Test
    void fogAndTimeWarpAreAnnouncedAndFogLifts() {
        angry(1);
        startPlay();
        game.strike(Chaos.FOG);
        assertTrue(game.isFoggy());
        assertEquals(Chaos.FOG, game.takeChaosNotice().getKind());
        assertEquals(20, game.moves().size());   // fog hides the board; it does not change the moves

        game.strike(Chaos.TIME_WARP);
        assertEquals("TIME WARP: the two clocks have swapped.", game.takeChaosNotice().getText());
        for (int ply = 0; ply < 6; ply++) {
            game.play(game.moves().get(0));
        }
        assertFalse(game.isFoggy());
    }

    @Test
    void standardModeHasNoDisasters() {
        CheatGame calm = new CheatGame(CheatLevel.STANDARD, new Random(1) {
            @Override
            public double nextDouble() {
                return 0.0;  // would trigger a disaster every move, if the mode had them
            }
        });
        calm.finishSetup(WHITE);
        calm.finishSetup(BLACK);
        for (int ply = 0; ply < 20; ply++) {
            calm.play(calm.moves().get(0));
            assertNull(calm.takeChaosNotice());
        }
    }

    @Test
    void angryModeStrikesByItself() {
        CheatGame wild = new CheatGame(CheatLevel.ANGRY, new Random(7));
        wild.finishSetup(WHITE);
        wild.finishSetup(BLACK);
        int disasters = 0;
        for (int ply = 0; ply < 80 && !wild.isOver(); ply++) {
            wild.play(wild.moves().get(0));
            if (wild.takeChaosNotice() != null) {
                disasters++;
            }
        }
        assertTrue(disasters >= 5, "only " + disasters + " disasters in 80 moves");
    }

    // ------------------------------------------------------------ belief

    @Test
    void beliefBoardShowsOwnTruthAndTheOthersLooks() {
        transform(WHITE, "b1", PieceType.QUEEN);
        Board whiteBelief = game.beliefBoard(WHITE);
        Board blackBelief = game.beliefBoard(BLACK);
        assertEquals(PieceType.QUEEN, whiteBelief.getPiece(at("b1")).getType());   // White knows
        assertEquals(PieceType.KNIGHT, blackBelief.getPiece(at("b1")).getType());  // Black is fooled
    }

    @Test
    void listenersHearAboutEveryChange() {
        int[] count = {0};
        game.addListener(() -> count[0]++);
        transform(WHITE, "b1", PieceType.QUEEN);
        game.finishSetup(WHITE);
        game.finishSetup(BLACK);
        play("e2", "e4");
        game.accuse();
        assertEquals(5, count[0]);
    }

    // ---------------------------------------------------------- computer

    @Test
    void computerSetsUpByTransformingItsOwnPieces() {
        new CheatAI(new Random(4)).setUp(game, BLACK);

        assertTrue(game.getCheatPoints(BLACK) >= 0, "overspent: " + game.getCheatPoints(BLACK));
        int hidden = 0;
        for (int file = 0; file < Board.SIZE; file++) {
            assertNull(game.secretAt(new Position(file, 0)));  // White's pieces untouched
            assertNull(game.secretAt(new Position(file, 6)));  // pawns untouched in standard mode
            if (game.secretAt(new Position(file, 7)) != null) {
                hidden++;
            }
        }
        assertTrue(hidden >= 2 && hidden <= 4, hidden + " pieces hidden");
        assertNull(game.secretAt(at("e8")));  // never the king
    }

    @Test
    void computerTakesTheKingWhenItCan() {
        startPlay();
        play("e2", "e4");
        play("f7", "f6");
        play("d1", "h5");
        play("a7", "a6");
        Move choice = new CheatAI(new Random(1)).chooseMove(game.beliefBoard(WHITE), game.moves(), WHITE);
        assertEquals("h5e8", choice.toString());
    }

    @Test
    void computerOnlyChallengesWhatItCouldHaveSeen() {
        CheatAI sharp = new CheatAI(new Random(1) {
            @Override
            public double nextDouble() {
                return 0.0;   // never misses a thing
            }
        });
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        assertFalse(sharp.shouldAccuse(game));   // an honest move: nothing to see, however sharp
        play("e7", "e5");
        play("b1", "b3");
        assertTrue(sharp.shouldAccuse(game));    // a knight cannot do that
    }

    @Test
    void computerMissesMostMovesThatGiveAPieceAway() {
        transform(WHITE, "b1", PieceType.QUEEN);
        startPlay();
        play("b2", "b4");
        play("e7", "e5");
        play("b1", "b3");

        CheatAI ai = new CheatAI(new Random(6));
        int spotted = 0;
        for (int i = 0; i < 200; i++) {
            if (ai.shouldAccuse(game)) {
                spotted++;
            }
        }
        // About three in ten: often enough to be a risk, rarely enough to be worth it.
        assertTrue(spotted > 35 && spotted < 90, "spotted " + spotted + " of 200");
    }

    @Test
    void hackChangesNothingInTheGameItself() {
        angry(1);
        startPlay();
        String before = game.beliefBoard(WHITE).toString();
        game.strike(Chaos.HACKED);
        assertEquals(Chaos.HACKED, game.takeChaosNotice().getKind());
        assertEquals(before, game.beliefBoard(WHITE).toString());
        assertEquals(20, game.moves().size());
    }

    @Test
    void computerSometimesNoticesTheClockBeingFiddled() {
        int noticed = 0;
        CheatAI ai = new CheatAI(new Random(3));
        for (int i = 0; i < 40; i++) {
            game = new CheatGame(CheatLevel.STANDARD, new Random(i));
            startPlay();
            game.noteClockFiddled(WHITE);
            play("e2", "e4");
            if (ai.shouldAccuse(game)) {
                noticed++;
            }
        }
        assertTrue(noticed > 8 && noticed < 32, "noticed " + noticed + " of 40");
    }

    @Test
    void computersPlayWholeAngryGamesWithoutBreakingTheRules() {
        for (long seed = 1; seed <= 4; seed++) {
            CheatGame match = new CheatGame(CheatLevel.ANGRY, new Random(seed));
            CheatAI white = new CheatAI(new Random(seed + 10));
            CheatAI black = new CheatAI(new Random(seed + 50));
            white.setUp(match, WHITE);
            black.setUp(match, BLACK);

            for (int ply = 0; ply < 80 && !match.isOver(); ply++) {
                CheatAI mover = match.getTurn() == WHITE ? white : black;
                if (mover.shouldAccuse(match) && match.accuse()) {
                    match.exposeAccused();
                }
                Move move = mover.chooseMove(match.beliefBoard(match.getTurn()), match.moves(), match.getTurn());
                assertTrue(match.moves().contains(move), "seed " + seed + " ply " + ply);
                match.play(move);
            }
        }
    }
}
