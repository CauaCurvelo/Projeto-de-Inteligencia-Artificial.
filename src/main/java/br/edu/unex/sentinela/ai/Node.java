package br.edu.unex.sentinela.ai;

public class Node implements Comparable<Node> {
    public int col;
    public int row;
    public int gCost; // Cost from start to this node
    public int hCost; // Heuristic cost from this node to target
    public Node parent;
    
    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getFCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(Node other) {
        int compare = Integer.compare(this.getFCost(), other.getFCost());
        if (compare == 0) {
            compare = Integer.compare(this.hCost, other.hCost);
        }
        return compare;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return col == node.col && row == node.row;
    }

    @Override
    public int hashCode() {
        return 31 * col + row;
    }
}
