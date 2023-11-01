import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class WaffleMCTSTree {
    // Multiple duplicates can exist in our tree, so we only keep track of the most optimal of these duplicate nodes. The most optimal duplicate node is the one at the lowest level in the tree.
    // These existing nodes will never have a pruned branch that goes to them by design.
    public Map<Integer, WaffleMCTSNode> existingNodes;
    public WaffleMCTSNode root;

    WaffleMCTSTree(WaffleState rootState) {
        this.existingNodes = new HashMap<Integer, WaffleMCTSNode>();
        this.root = this.newNode(null, this.boardHash(rootState.board), rootState);
    }

    public int boardHash(char[][] board) {
        return Arrays.deepHashCode(board);
    }
    
    public WaffleMCTSNode initBranch(WaffleMCTSNode curNode, int branchIndex, WaffleState newState) {
        int newHash = this.boardHash(newState.board);
        WaffleMCTSNode existingNode = this.existingNodes.get(newHash);

        if (existingNode == null) {
            WaffleMCTSNode newNode = this.newNode(curNode, newHash, newState);
            curNode.branches[branchIndex] = newNode;
            // If we're dealing with a unique node, then we want to run the playout off of the new node.
            return newNode;
        } else if (curNode.level < existingNode.level - 1) {
            // If we've reached this case, then we need to prune the existing node at its current path and then move the existing node onto this new, optimal branch.
            existingNode.connectNewParent(curNode, branchIndex);
            // We will then use the updated existing for the playout.
            return existingNode;
        } else {
            // If the existing node is at a lower level, then we need to prune the connection to the existing node at the current node.
            curNode.branchIsPruned[branchIndex] = true;
            // Then we will use the existing node for the playout.
            return existingNode;
        }
    }

    private WaffleMCTSNode newNode(WaffleMCTSNode parent, int newHash, WaffleState newState) {
        // This function assumes that the node has not already been created!
        WaffleMCTSNode node = new WaffleMCTSNode(this, parent, newHash, newState);
        this.existingNodes.put(newHash, node);
        return node;
    }

    public int getBestMoveIndex() {
        double bestUtility = -Double.MAX_VALUE;
        int bestMoveIndex = 0;
        for (int i = 0; i < root.branches.length; i++) {
            if (!root.branchIsPruned[i]) {
                double curUtility = root.branches[i].played;
                if (curUtility > bestUtility) {
                    bestUtility = curUtility;
                    bestMoveIndex = i;
                }
            }
        }
        return bestMoveIndex;
    }
}