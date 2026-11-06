# Engine report

Draft text for the engine sections of the project report. It covers the
`chess.engine` package and the console front end; the GUI and AI are
described separately. The class diagram is in
[class-diagram.md](class-diagram.md).

## 1. Overview

The engine is a complete implementation of the rules of chess with no
dependency on any user interface. It knows how every piece moves, including
castling, en passant and promotion; it offers only legal moves; it detects
check, checkmate, stalemate and three kinds of draw; it can take moves back
and replay them; and it reads and writes positions in the standard FEN
format.

Everything else in the project sits on top of it. The JavaFX board and the
AI both work through the same small set of calls: ask a `Game` for its legal
moves, choose one, and hand it back.

| | |
|---|---|
| Source | 19 classes, interfaces and enums, about 1,900 lines including comments |
| Tests | 198 JUnit 5 tests in 14 classes, about 2,500 lines (engine and console; the GUI and AI have their own) |
| Coverage | 99% of lines and 99% of branches (JaCoCo) |
| Language level | Builds as Java 17 with the rest of the project; the engine itself uses no libraries beyond the JDK |

## 2. Design

### 2.1 Responsibilities

Each class has one job.

| Class | Responsibility |
|---|---|
| `Position` | A square such as `e4`. Immutable. |
| `Piece` and six subclasses | How one kind of piece moves and what it attacks. Immutable. |
| `Move` | One move: from, to, the piece, what it captured, any promotion. Immutable. |
| `Board` | Where the pieces are, plus the castling rights, en passant square and half-move clock. Plays and undoes moves. Answers questions about attacks, check and legal moves. |
| `Game` | A board plus whose turn it is and the moves played so far. Enforces legality, reports the status, and provides undo and redo. |
| `Fen` | Converts a game to and from FEN text. |
| `PieceColor`, `PieceType`, `CastlingSide`, `GameStatus` | Enums. |

The split between `Board` and `Game` is deliberate. A `Board` is a position:
it can be copied, searched and thrown away, which is what an AI needs. A
`Game` is a history: turn order, undo and redo, and the draws that depend on
what has already happened.

### 2.2 Object-oriented principles

**Encapsulation.** The 8x8 grid is a private field of `Board`. Code outside
the engine can read squares and ask for moves, but the only public ways to
change a board are `makeMove` and `undoMove`. Methods that place pieces
directly (`setPiece`, `setCastlingRight`) are package-private, so only the
engine and its tests can use them.

**Inheritance.** `Piece` is abstract and holds what all pieces share: colour,
type, printing and equality. `SlidingPiece` sits between `Piece` and the
rook, bishop and queen, because those three move identically apart from
their directions. Each of the three is a constructor and nothing else.

**Polymorphism.** `Board` generates moves with
`piece.pseudoLegalMoves(this, position)` and detects attacks with
`piece.attacks(this, from, target)`. It never asks what kind of piece it is
holding. There is no `switch` on piece type anywhere in move generation.

**Abstraction.** The GUI and AI see `Game` and never deal with castling
rights, en passant squares or check detection. When the engine moved from
pseudo-legal to fully legal moves in week 2, `Game.legalMoves()` kept its
signature and the GUI code written against week 1 did not change.

**Immutability.** `Position`, `Move` and every `Piece` are immutable. This is
why a piece does not store its own square: the board is the only owner of
piece locations, so the two can never disagree, and a copied board can share
piece objects with the original safely.

### 2.3 Design patterns

**Command (undo and redo).** A `Move` is a command object. It carries
everything needed to perform an action and to reverse it, including the
captured piece. `Board.makeMove` executes it and `Board.undoMove` reverses
it. `Game` keeps two stacks, moves played and moves taken back; `undo` moves
a command from the first stack to the second, `redo` moves it back, and
playing a new move empties the second.

A move cannot reverse everything by itself. The castling rights, en passant
square and half-move clock from before the move are not part of it, so
`Board` saves them in a small private `Snapshot` as it plays each move and
restores them on undo.

**Factory method.** `Piece.create(type, color)` and `Piece.fromSymbol('N')`
build a piece from a label. Promotion and FEN parsing use them, so neither
needs to name the six subclasses.

**Template method (in miniature).** `SlidingPiece` implements the sliding
algorithm once and each subclass supplies only its directions.

**Observer (the engine's side of it).** `Game` keeps a list of
`GameListener`s and calls `gameChanged` on each after every move, undo and
redo. The engine knows only the interface, so it can tell a board on screen
to redraw without depending on any GUI code. The listeners themselves are
described in the GUI section.

The Strategy pattern (interchangeable AI algorithms) belongs to the AI
section.

## 3. How the rules are implemented

### 3.1 Legal moves

Legal moves are found in two steps.

1. **Pseudo-legal generation.** Each piece lists the moves that follow its
   movement pattern, stopping at blockers and capturing enemy pieces, without
   considering whether its own king is safe. A single piece cannot know that;
   it depends on the whole board.
2. **Filtering.** The board plays each candidate on a copy of itself and
   discards it if the mover's king is attacked afterwards.

The one filter covers every case that would otherwise need special code:
pinned pieces, a king stepping onto an attacked square, a king capturing a
defended piece, failing to answer a check, double check, and an en passant
capture that removes two pawns from a rank and exposes the king behind them.

### 3.2 Attacks and check

"Can this piece move there?" and "does this piece attack that square?" are
different questions. A pawn attacks the two squares diagonally ahead even
when they are empty, and does not attack the square in front of it. So
`Piece.attacks` is a separate abstract method from `pseudoLegalMoves`.
`Board.isSquareAttacked` asks every piece of one colour, and `isInCheck`
asks it about the king's square.

Keeping attacks separate also avoids a circular definition. Castling depends
on which squares are attacked; if attacks were computed from the move list,
and the move list included castling, each would need the other.

### 3.3 The special moves

**Castling.** The board keeps four castling rights. A right is lost when a
piece moves from the king's or that rook's home square, or when a piece lands
on the rook's home square (the rook was captured). `King` offers castling
when the right is held, the rook is in its corner, the squares between are
empty, the king is not in check, and the square it crosses is not attacked.
The square it lands on is checked by the legal-move filter like any other
king move. Castling is represented as the king's two-square move, and
`makeMove` moves the rook as well.

**En passant.** After a pawn's double step the board records the square it
skipped, for one move only. A pawn that could capture onto that square is
given an en passant move, which is the one move in chess where the captured
piece is not on the destination square.

**Promotion.** A pawn reaching the last rank generates four moves to the
same square, one for each of queen, rook, bishop and knight. The `Move`
carries the choice and `makeMove` places the new piece.

### 3.4 Game status and draws

`Game.getStatus()` returns one of seven values. If the side to move has no
legal moves it is checkmate when in check and stalemate otherwise. Failing
that, three draw rules are tested:

- **Insufficient material:** only kings; a single bishop or knight; or
  bishops that all stand on squares of one colour.
- **Fifty-move rule:** 100 half-moves without a capture or pawn move.
- **Threefold repetition:** the current position has occurred three times.
  Positions are compared by the first four FEN fields: pieces, side to move,
  castling rights and en passant square.

Draws are applied automatically rather than claimed by a player, which keeps
the GUI and AI-versus-AI games simple: once the game is over, `legalMoves()`
is empty.

### 3.5 FEN

`Fen.parse` builds a `Game` from the six FEN fields and `Fen.format` writes
one back. Invalid input raises an `IllegalArgumentException` whose message
names the faulty field. FEN lets tests start from any position in one line,
lets the AI be tested on chosen positions, and gives the repetition rule its
position key.

## 4. Testing

### 4.1 Unit tests

Every rule has tests named for the behaviour they check, for example
`cannotCastleThroughAnAttackedSquare`, `enPassantIsIllegalWhenItUncoversTheKing`
and `underpromotionAvoidsTheStalemateAQueenWouldCause`. Most set up a small
position, often from FEN, and assert the exact set of moves or the resulting
position.

| Test class | Tests | Covers |
|---|---|---|
| `PositionTest`, `PieceTest`, `MoveTest` | 18 | The value classes |
| `PieceMovementTest` | 27 | Each piece's movement, blocking and captures |
| `BoardTest` | 11 | Set-up, playing moves, copying, printing |
| `CheckTest` | 13 | Attacks, check, pins and the legal-move filter |
| `SpecialMovesTest` | 32 | Castling, en passant, promotion |
| `GameTest` | 22 | Turns, status, checkmates, a complete master game |
| `UndoTest` | 21 | Undo for every kind of move; redo |
| `FenTest` | 14 | Reading, writing, round trips, invalid input |
| `DrawTest` | 10 | The three draw rules |
| `GameListenerTest` | 8 | Listeners are told of moves, undo and redo |
| `PerftTest` | 8 | Move counts against published figures (below) |
| `MainTest` | 14 | The console front end, driven by scripted input |

### 4.2 Perft

Unit tests only check the cases someone thought of. Perft ("performance
test") checks everything at once: it counts every sequence of legal moves of
a given length from a position and compares the total with published
figures. One missing or extra move anywhere changes the count.

| Position | Depth | Sequences |
|---|---|---|
| Starting position | 5 | 4,865,609 |
| "Kiwipete" (castling both sides, pins, en passant) | 4 | 4,085,603 |
| Endgame with en passant pins | 5 | 674,624 |
| Promotions while in check, White to move | 4 | 422,333 |
| The same with colours swapped, Black to move | 4 | 422,333 |
| Promotion by capture | 4 | 2,103,487 |
| Crowded middlegame | 4 | 3,894,594 |

All counts match at every depth up to those shown. The positions are the
standard set from the Chess Programming Wiki. They matter because nobody
castles or promotes within five moves of the starting position, so the first
row alone would not test those rules.

The perft search plays and takes back every move on a single board, and the
test then checks that the board's FEN is unchanged. An undo that restored
anything wrongly would alter a count or the final position, so perft
verifies undo as thoroughly as it verifies move generation.

### 4.3 Coverage

Measured with JaCoCo, the tests execute 99% of lines and 99% of branches.
What remains is mainly the `main` method that connects the console to
`System.in`, and two `default` branches that exist only to reject values
that cannot currently occur.

## 5. Design decisions and trade-offs

**Copying the board to test legality, rather than make and undo.** The
legal-move filter copies the board for each candidate move. Playing and
undoing on the board itself would avoid the copies, but would mean that
merely asking for the legal moves briefly changes the board. With the GUI
drawing from the same board that an AI thread is examining, that is a bug
waiting to happen. A copy is 64 references and a few flags, and with it the
engine counts the 4.9 million sequences of perft depth 5 in under three
seconds on a laptop.

**Undo history lives in the board, but copies start empty.** `undoMove` needs
the state from before the move, so the board keeps a history. A copy does
not take the history with it, which keeps copying cheap; the cost is that a
copy cannot undo moves played before it was made.

**A 2D array of objects, not bitboards.** Fast chess programs pack the board
into 64-bit integers. An array of `Piece` objects is slower but lets each
piece own its behaviour, which is the point of an object-oriented design and
is fast enough for the search depths the AI uses.

**Two questions per piece.** Giving `Piece` both `pseudoLegalMoves` and
`attacks` duplicates a little geometry, but it handles pawns correctly and
keeps castling from depending on itself (section 3.2).

**Moves validated by membership.** `Game.makeMove` accepts a move only if it
is in the list of legal moves. This is simple and impossible to get subtly
wrong, at the cost of generating the list once more per move played.

## 6. Limitations

- Repetition compares positions including the en passant square even when no
  pawn could actually capture there, which is slightly stricter than the
  official rule.
- Insufficient material recognises only the clear-cut cases. Positions that
  are drawn in practice, such as king and two knights against king, are
  played on.
- Draws are automatic. The official rules let a player choose whether to
  claim the fifty-move and repetition draws.
- `Game` is not thread-safe. An AI should search a copy of the board on its
  own thread and play its chosen move on the GUI thread.
- There is no algebraic notation (`Nf3`, `O-O`) or PGN; moves are written as
  coordinates (`g1f3`).
