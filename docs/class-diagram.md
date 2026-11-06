# Engine class diagram

The `chess.engine` package. GitHub draws the diagram below automatically. To
get an image for the report, paste the block into <https://mermaid.live> and
export it as PNG or SVG.

Getters, `equals`/`hashCode`/`toString`, the package-private set-up methods
(`setPiece` and friends) and some parameter lists are left out to keep it
readable. `Snapshot` is a private class nested inside `Board`.

```mermaid
classDiagram
    direction TB

    class Piece {
        <<abstract>>
        -PieceColor color
        -PieceType type
        +pseudoLegalMoves(Board, Position)* List~Move~
        +attacks(Board, Position, Position)* boolean
        +getSymbol() char
        +create(PieceType, PieceColor)$ Piece
        +fromSymbol(char)$ Piece
        #isEnemy(Piece) boolean
        #addStepMove()
    }
    class SlidingPiece {
        <<abstract>>
        -int[][] directions
        +pseudoLegalMoves(Board, Position) List~Move~
        +attacks(Board, Position, Position) boolean
    }
    class Rook
    class Bishop
    class Queen
    class Knight {
        +pseudoLegalMoves(Board, Position) List~Move~
        +attacks(Board, Position, Position) boolean
    }
    class King {
        +pseudoLegalMoves(Board, Position) List~Move~
        +attacks(Board, Position, Position) boolean
        -addCastlingMoves()
    }
    class Pawn {
        +pseudoLegalMoves(Board, Position) List~Move~
        +attacks(Board, Position, Position) boolean
        -addAdvance()
    }

    Piece <|-- SlidingPiece
    Piece <|-- Knight
    Piece <|-- King
    Piece <|-- Pawn
    SlidingPiece <|-- Rook
    SlidingPiece <|-- Bishop
    SlidingPiece <|-- Queen

    class Board {
        +int SIZE$
        -Piece[][] squares
        -boolean[][] castlingRights
        -Position enPassantTarget
        -int halfmoveClock
        -Deque~Snapshot~ history
        +startingPosition()$ Board
        +getPiece(Position) Piece
        +makeMove(Move)
        +undoMove(Move)
        +pseudoLegalMoves(PieceColor) List~Move~
        +legalMoves(PieceColor) List~Move~
        +legalMovesFrom(Position) List~Move~
        +findKing(PieceColor) Position
        +isSquareAttacked(Position, PieceColor) boolean
        +isInCheck(PieceColor) boolean
        +hasCastlingRight(PieceColor, CastlingSide) boolean
        +hasInsufficientMaterial() boolean
    }
    class Snapshot {
        <<nested>>
        Move move
        boolean[][] castlingRights
        Position enPassantTarget
        int halfmoveClock
    }
    class Game {
        -Board board
        -PieceColor turn
        -int fullmoveNumber
        -Deque~Move~ undoStack
        -Deque~Move~ redoStack
        -List~String~ positionKeys
        +fromFen(String)$ Game
        +toFen() String
        +legalMoves() List~Move~
        +legalMovesFrom(Position) List~Move~
        +makeMove(Move)
        +undo() Move
        +redo() Move
        +getStatus() GameStatus
        +isInCheck() boolean
        +getMoveHistory() List~Move~
        +addListener(GameListener)
        +removeListener(GameListener)
    }
    class Move {
        <<immutable>>
        -Position from
        -Position to
        -Piece piece
        -Piece captured
        -PieceType promotion
        -boolean enPassant
        +enPassant()$ Move
        +isCapture() boolean
        +isPromotion() boolean
        +isCastling() boolean
        +isEnPassant() boolean
    }
    class Position {
        <<immutable>>
        -int file
        -int rank
        +of(String)$ Position
        +isValid(int, int)$ boolean
    }
    class GameListener {
        <<interface>>
        +gameChanged(Game)
    }
    class Fen {
        <<utility>>
        +String STARTING_POSITION$
        +parse(String)$ Game
        +format(Game)$ String
    }
    class PieceColor {
        <<enumeration>>
        WHITE
        BLACK
        +opposite() PieceColor
    }
    class PieceType {
        <<enumeration>>
        KING
        QUEEN
        ROOK
        BISHOP
        KNIGHT
        PAWN
        +fromSymbol(char)$ PieceType
    }
    class CastlingSide {
        <<enumeration>>
        KING_SIDE
        QUEEN_SIDE
    }
    class GameStatus {
        <<enumeration>>
        IN_PROGRESS
        CHECK
        CHECKMATE
        STALEMATE
        DRAW_BY_FIFTY_MOVES
        DRAW_BY_REPETITION
        DRAW_BY_INSUFFICIENT_MATERIAL
        +isGameOver() boolean
        +isDraw() boolean
    }

    Game "1" *-- "1" Board : owns
    Game "1" o-- "*" Move : undo and redo stacks
    Game ..> GameStatus : reports
    Game "1" o-- "*" GameListener : notifies
    Game ..> Fen : reads and writes through
    Board "1" o-- "0..32" Piece : squares
    Board "1" *-- "*" Snapshot : history
    Board ..> Move : plays and undoes
    Snapshot --> Move
    Move --> "2" Position : from and to
    Move --> "1..2" Piece : moved and captured
    Move --> PieceType : promotion
    Piece --> PieceColor
    Piece --> PieceType
    Piece ..> Board : reads when generating moves
    Piece ..> Move : creates
    Board ..> CastlingSide
    Fen ..> Game : creates
```

## How to read it

- **Inheritance** (hollow triangle): `Rook`, `Bishop` and `Queen` add nothing
  but their directions to `SlidingPiece`. `Knight`, `King` and `Pawn` each
  implement the two abstract methods of `Piece` themselves.
- **Composition** (filled diamond): a `Game` owns its `Board`, and a `Board`
  owns its undo `Snapshot`s. Neither part is shared with anything else.
- **Aggregation** (hollow diamond): a `Board` holds `Piece` objects, but
  pieces are immutable and a copied board shares them with the original.
- **Observer**: `Game` holds `GameListener`s and tells them when it changes.
  The classes that implement the interface are in the GUI, outside this
  package, which is the point: the engine does not know them.
- **Dependency** (dashed arrow): one class uses another without keeping it.
  `Piece` reads the `Board` it is asked about but never stores it, which is
  what keeps the board the only owner of where pieces are.
