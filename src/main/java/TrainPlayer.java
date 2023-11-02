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

    private static double min(WaffleState state, int depth, double alpha, double beta) {
        if (depth-- <= 0) {
            // evalBoard returns a [0,1] value, so negating its result means that we subtract it from one.
            return 1.0 - WaffleNNModel.predict(state);
        }
        for (int i=0; i < state.numLegalMoves; i++){
            WaffleState nextState = new WaffleState(state);
            PerformMove(nextState, i);
            double result = max(nextState, depth, alpha, beta);
            if (result < beta) {
                beta = result;
                if (alpha >= beta) {
                    return alpha;
                }
            }
        }
        return beta;
    }

    private static double max(WaffleState state, int depth, double alpha, double beta) {
        if (depth-- <= 0) {
            return WaffleNNModel.predict(state);
        }
        for (int i=0; i < state.numLegalMoves; i++){
            WaffleState nextState = new WaffleState(state);
            PerformMove(nextState, i);
            double result = min(nextState, depth, alpha, beta);
            if (result > alpha) {
                alpha = result;
                if (alpha >= beta) {
                    return beta;
                }
            }
        }
        return alpha;
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
            System.err.println("Eval of board: " + evalBoard(nextState));

            double result = min(nextState, 5, bestMoveValue, 1.01);
            if (result > bestMoveValue) {
                System.err.println("Better Result: " + result);
                bestMoveValue = result;
                bestMoveIndex = x;
            }
        }

        if (player == 1) {
            // This saves our move so that it can be saved for training later.
            WaffleState nextState = new WaffleState(state);
            PerformMove(nextState, bestMoveIndex);
            System.err.println("Chosen Eval: " + evalBoard(nextState));
            WaffleNNModel.storeBoardState(nextState, evalBoard(nextState));
        }

        TrainHelper.memcpy(bestmove, state.movelist[bestMoveIndex], TrainHelper.MoveLength(state.movelist[bestMoveIndex]));
    }


    /* An example of how to walk through a board and determine what pieces are on it*/
    private static double evalBoard(WaffleState state)
    {
        int y,x;
        double score;
        score=0.0;
        int pieces1 = 0;
        int pieces2 = 0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                if(WaffleHelper.empty(state.board[y][x]))
                {
                }
                else if(WaffleHelper.king(state.board[y][x]))
                {
                    if(WaffleHelper.color(state.board[y][x])==2) {
                        //score += 2.0;//WaffleHelper.edge(x) || WaffleHelper.edge(y) ? 1.5 : 2.0;
                        pieces2 += 2;
                    } else {
                        score -= 2.0;//WaffleHelper.edge(x) || WaffleHelper.edge(y) ? 1.5 : 2.0;
                        pieces1 += 2;
                    }
                }
                else if(WaffleHelper.piece(state.board[y][x]))
                {
                    if(WaffleHelper.color(state.board[y][x])==2) {
                        //score += 1.0;//(WaffleHelper.edge(x) ? 1.05 : 1.0);
                        pieces2 += 1;
                    } else {
                        //score -= 1.0;//(WaffleHelper.edge(x) ? 1.05 : 1.0);
                        pieces1 += 1;
                    }
                }
            }
        }

        if (state.player == 1) {
            if (pieces2 == 0) {
                score = 1.0;
            } else {
                // The score needs to be positive (for a good score) from player1's perspective.
                // For pieces1 == 12 and pieces2 == 24, we get 0.25;
                // For pieces1 == 24 and pieces2 == 12, we get 0.75;
                // This is equivalent to: ((pieces1 / 24.0) - (pieces2 / 24.0) + 1) / 2.0
                score =  (pieces1 / 48.0) - (pieces2 / 48.0) + 0.5;
            }
        } else {
            if (pieces1 == 0) {
                score = 1.0;
            } else {
                // The score needs to be positive (for a good score) from player2's perspective.
                // For pieces1 == 12 and pieces2 == 24, we get 0.75;
                // For pieces1 == 24 and pieces2 == 12, we get 0.25;
                // This is equivalent to: ((pieces2 / 24.0) - (pieces1 / 24.0) + 1) / 2.0
                score = (pieces2 / 48.0) - (pieces1 / 48.0) + 0.5;
            }
        }

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
