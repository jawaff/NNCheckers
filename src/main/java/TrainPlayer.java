public class TrainPlayer {

    static void setupBoardState(WaffleState state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        TrainHelper.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        TrainHelper.FindLegalMoves(state);

        state.shuffleMoves();
    }

    static void PerformMove(WaffleState state, int moveIndex)
    {
        TrainHelper.PerformMove(state.board, state.movelist[moveIndex], TrainHelper.MoveLength(state.movelist[moveIndex]));
        state.player = state.player%2+1;
        TrainHelper.FindLegalMoves(state);
    }

    /* Employ your favorite search to find the best move. This code is an example */
    /* of an alpha/beta search, except I have not provided the MinVal,MaxVal,EVAL */
    /*
     * functions. This example code shows you how to call the FindLegalMoves
     * function
     */
    /* and the PerformMove function */
    public static void FindBestMove(int player, char[][] board, char[] bestmove) {
        WaffleNNModel.loadPredictor();

        WaffleState state = new WaffleState(); // nextstate;
        setupBoardState(state, player, board);

        //state.printMoves();
        printBoard(state);

        int bestMoveIndex = 0;
        double bestMoveValue = -Double.MAX_VALUE;

        for (int x = 0; x < state.numLegalMoves; x++) {
            WaffleState nextState = new WaffleState(state);
            PerformMove(nextState, x);

            double result = WaffleNNModel.predict(nextState);
            if (result > bestMoveValue) {
                bestMoveValue = result;
                bestMoveIndex = x;
            }

            //printBoard(nextState);
            System.err.println("Eval of board: " + evalBoard(nextState));
        }

        // This saves our move so that it can be saved for training later.
        WaffleState nextState = new WaffleState(state);
        PerformMove(nextState, bestMoveIndex);
        WaffleNNModel.storeBoardState(nextState);

        TrainHelper.memcpy(bestmove, state.movelist[bestMoveIndex], TrainHelper.MoveLength(state.movelist[bestMoveIndex]));
    }


    /* An example of how to walk through a board and determine what pieces are on it*/
    static double evalBoard(WaffleState state)
    {
        int y,x;
        double score;
        score=0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                if(TrainHelper.empty(state.board[y][x]))
                {
                }
                else if(TrainHelper.king(state.board[y][x]))
                {
                    if(TrainHelper.color(state.board[y][x])==2) score += 2.0;
                    else score -= 2.0;
                }
                else if(TrainHelper.piece(state.board[y][x]))
                {
                    if(TrainHelper.color(state.board[y][x])==2) score += 1.0;
                    else score -= 1.0;
                }
            }
        }

        if(state.player==1) score = -score;

        return score;

    }

    static void printBoard(WaffleState state)
    {
        int y,x;

        for(y=0; y<8; y++) 
        {
            for(x=0; x<8; x++)
            {
                if(x%2 != y%2)
                {
                     if(TrainHelper.empty(state.board[y][x]))
                     {
                         System.err.print(" ");
                     }
                     else if(TrainHelper.king(state.board[y][x]))
                     {
                         if(TrainHelper.color(state.board[y][x])==2) System.err.print("B");
                         else System.err.print("A");
                     }
                     else if(TrainHelper.piece(state.board[y][x]))
                     {
                         if(TrainHelper.color(state.board[y][x])==2) System.err.print("b");
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
}
