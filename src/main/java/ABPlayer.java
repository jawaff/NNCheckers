import java.util.*;
import java.io.*;

public class ABPlayer {

    static Random random=new Random();

    static void setupBoardState(ABState state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        ABHelper.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        ABHelper.FindLegalMoves(state);

        // Randomizes the state moves
        state.shuffleMoves();
    }
    
    static void PerformMove(ABState state, int moveIndex)
    {
        ABHelper.PerformMove(state.board, state.movelist[moveIndex], ABHelper.MoveLength(state.movelist[moveIndex]));
        state.player = state.player%2+1;
        ABHelper.FindLegalMoves(state);
    }

    static double min(ABState state, int depth, double alpha, double beta) {
        if (depth-- <= 0) {
            return -evalBoard(state);
        }
        for (int i=0; i < state.numLegalMoves; i++){
            ABState nextState = new ABState(state);
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

    static double max(ABState state, int depth, double alpha, double beta) {
        if (depth-- <= 0) {
            return evalBoard(state);
        }
        for (int i=0; i < state.numLegalMoves; i++){
            ABState nextState = new ABState(state);
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
        int bestMoveIndex;
        
        ABState state = new ABState(); // , nextstate;
        setupBoardState(state, player, board);

        bestMoveIndex = 0;
        double bestMoveValue = -Double.MAX_VALUE;

        for (int x = 0; x < state.numLegalMoves; x++) {
            ABState nextState = new ABState(state);
            PerformMove(nextState, x);

            double result = min(nextState, 5, bestMoveValue, Double.MAX_VALUE);
            if (result > bestMoveValue) {
                bestMoveValue = result;
                bestMoveIndex = x;
            }

            //printBoard(nextState);
            System.err.println("Eval of board: " + evalBoard(nextState));
        }

        ABHelper.memcpy(bestmove, state.movelist[bestMoveIndex], ABHelper.MoveLength(state.movelist[bestMoveIndex]));
    }

    static void printBoard(ABState state)
    {
        int y,x;

        for(y=0; y<8; y++) 
        {
            for(x=0; x<8; x++)
            {
                if(x%2 != y%2)
                {
                     if(ABHelper.empty(state.board[y][x]))
                     {
                         System.err.print(" ");
                     }
                     else if(ABHelper.king(state.board[y][x]))
                     {
                         if(ABHelper.color(state.board[y][x])==2) System.err.print("B");
                         else System.err.print("A");
                     }
                     else if(ABHelper.piece(state.board[y][x]))
                     {
                         if(ABHelper.color(state.board[y][x])==2) System.err.print("b");
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
    static double evalBoard(ABState state)
    {
        int y,x;
        double score;
        score=0.0;

        for(y=0; y<8; y++) for(x=0; x<8; x++)
        {
            if(x%2 != y%2)
            {
                 if(ABHelper.empty(state.board[y][x]))
                 {
                 }
                 else if(ABHelper.king(state.board[y][x]))
                 {
                     if(ABHelper.color(state.board[y][x])==2) score += 2.0;
                     else score -= 2.0;
                 }
                 else if(ABHelper.piece(state.board[y][x]))
                 {
                     if(ABHelper.color(state.board[y][x])==2) score += 1.0;
                     else score -= 1.0;
                 }
            }
        }

        if(state.player==1) score = -score;

        return score;

    }

}
