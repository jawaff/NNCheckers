import java.util.*;


public class WafflePlayer {

    static void setupBoardState(WaffleState state, int player, char[][] board)
    {
        /* Set up the current state */
        state.player = player;
        WaffleHelper.memcpy(state.board,board);

        /* Find the legal moves for the current state */
        WaffleHelper.FindLegalMoves(state);

        state.shuffleMoves();
    }

    static class FindMoveTask implements Runnable {
        public boolean isDone;
        public WaffleMCTSTree mctsTree;

        FindMoveTask(WaffleState initialState) {
            this.mctsTree = new WaffleMCTSTree(initialState);
            this.isDone = false;
        }
    
        public void PerformMove(WaffleState state, int moveIndex)
        {
            WaffleHelper.PerformMove(state.board, state.movelist[moveIndex], WaffleHelper.MoveLength(state.movelist[moveIndex]));
            state.player = state.player%2+1;
            WaffleHelper.FindLegalMoves(state);
        }

        private double playoutNN(WaffleState state, int maxMoves) throws InterruptedException {
            WaffleState curState = state;
            double bestMoveValue = 0.0;
            // We assume that maxMoves will always be 1 or greater.
            for (int i = 0; i < maxMoves; i++) {
                bestMoveValue = 0.0;

                // Check to see if the game is over, or if I've run out of moves.
                if (curState.numLegalMoves == 0) {
                    // Game over, this player lost so tell the other player they won.
                    // The first iteration should return a 0, because the provided state's player lost.
                    return i % 2 == 0 ? 0.0 : 1.00;
                }

                curState.shuffleMoves();
                for (int x = 0; x < state.numLegalMoves; x++) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    WaffleState nextState = new WaffleState(state);
                    PerformMove(nextState, x);

                    double result = WaffleNNModel.predict(nextState);
                    if (result > bestMoveValue) {
                        bestMoveValue = result;

                        // By the end of this inner-loop, the curState will be equal to the best state.
                        curState = nextState;
                    }
                }
            }
            double result = 1.0 - bestMoveValue;
            if (state.player == 2) {
                result = 1.0 - result;
            }

            if (curState.player == 2) {
                result = 1.0 - result;
            }
            return result;
        }

        private void expand(WaffleMCTSNode curNode) throws InterruptedException {
            double totalWins = 0;
            for (int i = 0; i < curNode.state.numLegalMoves; i++) {
                WaffleState nextState = new WaffleState(curNode.state);
                PerformMove(nextState, i);
                // The optimal node will usually be the same as the curNode, but may change if a more optimal duplicate node already exists in the tree.
                WaffleMCTSNode newNode = curNode.initBranch(i, nextState);
                double result = playoutNN(nextState, 20);
                //System.err.println("Expand Player: " + nextState.player);
                //System.err.println("RESULT: " + result);
                newNode.won += result;
                totalWins += newNode.won;
                newNode.played++;
            }
            // By doing the backpropagation at the parent, we can hopefully make it a bit more efficient.
            curNode.backprop(curNode.state.numLegalMoves - totalWins, curNode.state.numLegalMoves);
        }

        private double utility(WaffleMCTSNode curNode, WaffleMCTSNode parent) {
            return (((double) curNode.won) / curNode.played) +
                1.4 * Math.sqrt(Math.log(parent.played) / curNode.played);
        }

        private void select(WaffleMCTSNode curNode) throws InterruptedException {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (curNode.isGameOver()) {
                curNode.played++;
                curNode.backprop(0, 1);
                return;
            }
            if (curNode.isLeaf()) {
                //System.err.println("Selected Leaf Player: " + selected.state.player);
                expand(curNode);
                return;
            }
            double bestUtility = -Double.MAX_VALUE;;
            WaffleMCTSNode selected = null;
            for (int i = 0; i< curNode.branches.length; i++) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (!curNode.branchIsPruned[i]) {
                    double curUtility = utility(curNode.branches[i], curNode);
                    if (curUtility > bestUtility) {
                        bestUtility = curUtility;
                        selected = curNode.branches[i];
                    }
                }
            }
            //System.err.println("Selected Player: " + selected.state.player);
            if (selected != null) {
                select(selected);
            }
        }

        @Override
        public void run() {
            while (!isDone) {
                try {
                    //System.err.println("New Select for Player: " + mctsTree.root.state.player);
                    select(mctsTree.root);
                } catch (InterruptedException e) {
                    isDone = true;
                    return;
                }
            }
        }
    }

    static class TimeOutTask extends TimerTask {
        private Thread thread;
        private Timer timer;
    
        public TimeOutTask(Thread thread, Timer timer) {
            this.thread = thread;
            this.timer = timer;
        }
    
        @Override
        public void run() {
            if(thread != null && thread.isAlive()) {
                thread.interrupt();
                timer.cancel();
            }
        }
    }

        /* Employ your favorite search to find the best move. This code is an example */
    /* of an alpha/beta search, except I have not provided the MinVal,MaxVal,EVAL */
    /*
     * functions. This example code shows you how to call the FindLegalMoves
     * function
     */
    /* and the PerformMove function */
    public static void FindBestMove(int player, char[][] board, char[] bestmove) {
        long startTime = System.currentTimeMillis();

        WaffleNNModel.loadPredictor();

        WaffleState state = new WaffleState(); // , nextstate;
        setupBoardState(state, player, board);

        //state.printMoves();
        printBoard(state);

        FindMoveTask finderTask = new FindMoveTask(state);
        Thread thread = new Thread(finderTask);
        thread.start();
        
        Timer timer = new Timer();
        TimeOutTask timeOutTask = new TimeOutTask(thread, timer);

        long now = System.currentTimeMillis();
        long timeLeft = ((long) WaffleHelper.SecPerMove*1000L) - (now - startTime) - 100L;
        timer.schedule(timeOutTask, timeLeft);

        try {
            thread.join();
            while (!finderTask.isDone) {
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {}

        int finalBestMoveIndex = finderTask.mctsTree.getBestMoveIndex();

        WaffleHelper.memcpy(bestmove, state.movelist[finalBestMoveIndex], WaffleHelper.MoveLength(state.movelist[finalBestMoveIndex]));
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
                     if(WaffleHelper.empty(state.board[y][x]))
                     {
                         System.err.print(" ");
                     }
                     else if(WaffleHelper.king(state.board[y][x]))
                     {
                         if(WaffleHelper.color(state.board[y][x])==2) System.err.print("B");
                         else System.err.print("A");
                     }
                     else if(WaffleHelper.piece(state.board[y][x]))
                     {
                         if(WaffleHelper.color(state.board[y][x])==2) System.err.print("b");
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
