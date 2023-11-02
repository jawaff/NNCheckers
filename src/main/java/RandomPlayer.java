import java.util.Random;

public class RandomPlayer {

    static Random random=new Random();

    static void setupBoardState(RandomState state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        RandomHelper.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        RandomHelper.FindLegalMoves(state);

        // Randomizes the state moves
        state.shuffleMoves();
    }

    
    static void PerformMove(RandomState state, int moveIndex)
    {
        RandomHelper.PerformMove(state.board, state.movelist[moveIndex], RandomHelper.MoveLength(state.movelist[moveIndex]));
        state.player = state.player%2+1;
        RandomHelper.FindLegalMoves(state);
    }


        /* Employ your favorite search to find the best move. This code is an example */
    /* of an alpha/beta search, except I have not provided the MinVal,MaxVal,EVAL */
    /*
     * functions. This example code shows you how to call the FindLegalMoves
     * function
     */
    /* and the PerformMove function */
    public static void FindBestMove(int player, char[][] board, char[] bestmove) {
        int bestMoveIndex;
        
        RandomState state = new RandomState(); // , nextstate;
        setupBoardState(state, player, board);
        printBoard(state);

        bestMoveIndex = random.nextInt(state.numLegalMoves);

        for (int x = 0; x < state.numLegalMoves; x++) {
            RandomState nextState = new RandomState(state);
            PerformMove(nextState, x);
            System.err.println("Eval of board: " + evalBoard(nextState));
        }
        RandomHelper.memcpy(bestmove, state.movelist[bestMoveIndex], RandomHelper.MoveLength(state.movelist[bestMoveIndex]));
    }

    static void printBoard(RandomState state)
    {
        int y,x;

        for(y=0; y<8; y++) 
        {
            for(x=0; x<8; x++)
            {
                if(x%2 != y%2)
                {
                     if(RandomHelper.empty(state.board[y][x]))
                     {
                         System.err.print(" ");
                     }
                     else if(RandomHelper.king(state.board[y][x]))
                     {
                         if(RandomHelper.color(state.board[y][x])==2) System.err.print("B");
                         else System.err.print("A");
                     }
                     else if(RandomHelper.piece(state.board[y][x]))
                     {
                         if(RandomHelper.color(state.board[y][x])==2) System.err.print("b");
                         else System.err.print("a");
                     }
                }
                else
                {
                    System.err.print("@");
                }
            }
            System.err.print("\n");
        }
    }

    /* An example of how to walk through a board and determine what pieces are on it*/
    static double evalBoard(RandomState state)
    {
        int y,x;
        double score;
        score=0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                 if(RandomHelper.empty(state.board[y][x]))
                 {
                 }
                 else if(RandomHelper.king(state.board[y][x]))
                 {
                     if(RandomHelper.color(state.board[y][x])==2) score += 2.0;
                     else score -= 2.0;
                 }
                 else if(RandomHelper.piece(state.board[y][x]))
                 {
                     if(RandomHelper.color(state.board[y][x])==2) score += 1.0;
                     else score -= 1.0;
                 }
            }
        }

        if(state.player==1) score = -score;

        return score;

    }

}
