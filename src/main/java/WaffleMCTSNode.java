import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class WaffleMCTSNode {
    private WaffleMCTSTree tree;
    public int level;
    public int hash;
    public WaffleState state;
    public WaffleMCTSNode parent;
    public WaffleMCTSNode[] branches;
    public boolean[] branchIsPruned;
    public double won = 0;
    public double played = 0;

    public WaffleMCTSNode(WaffleMCTSTree tree, WaffleMCTSNode parent, int hash, WaffleState state) {
        this.tree = tree;
        this.level = parent == null ? 1 : parent.level + 1;
        this.hash = hash;
        this.state = state;
        this.parent = parent;
        this.branches = new WaffleMCTSNode[state.numLegalMoves];
        this.branchIsPruned = new boolean[state.numLegalMoves];
        for (int i = 0; i < state.numLegalMoves; i++) {
            this.branchIsPruned[i] = false;
        }
    }

    public boolean isGameOver() {
        return branches.length == 0;
    }

    public boolean isLeaf() {
        return branches[0] == null && branchIsPruned[0] == false;
    }

    public WaffleMCTSNode initBranch(int branchIndex, WaffleState state) {
        return this.tree.initBranch(this, branchIndex, state);
    }

    public void connectNewParent(WaffleMCTSNode newParent, int branchIndex) {
        if (this.parent != null) {
            // The branch from the old parent needs to be pruned as a prerequisite.
            for (int i = 0; i < this.parent.branches.length; i++) {
                // We have found the branch to this node from the current parent.
                if (this.parent.branches[i] != null && this.parent.branches[i].hash == this.hash) {
                    // We don't need to keep track of this branch anymore, so we prune it.
                    this.parent.branches[i] = null;
                    this.parent.branchIsPruned[i] = true;
                }
            }
        }

        this.parent = newParent;
        newParent.branches[branchIndex] = this;
        // We assume that it's impossible to duplicate the initial state of the board, so newParent will never be null.
        this.level = newParent.level + 1;
        // Since we have a new parent, we can backpropagate a summary of our stats so that the new branch has some new information without there being too much weight on the new branch.
        //this.backprop(this.won / this.played, 1);
    }

    public void backprop(double newWon, double newPlayed) {
        this.won += newWon;
        this.played += newPlayed;
        if (parent != null) {
            this.parent.backprop(newPlayed - newWon, newPlayed);
        }
    }

    public void backprop() {
        if (parent != null) {
            this.parent.backprop(this.played - this.won, this.played);
        }
    }
}