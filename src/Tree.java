import java.util.ArrayList;
import java.util.List;

/**
 * This class if for the concrete syntax tree of the parser.
 * This code is adapted from code found here: https://www.labouseur.com/projects/jsTreeDemo/treeDemo.js
 *
 * @author Chris Pellerito
 */
public class Tree {
    public enum kind{
        ROOT, BRANCH, LEAF
    }
     Node root = null;
     Node current;

    /**
     * Adds a node to the CST
     * @param name Name of the node
     * @param kind Kind of the node from ENUM kind. (Root, Branch, Leaf)
     */
     public void addNode(String name, kind kind){
        Node n = new Node(name, kind);

        // Since the root node will be assigned the root type in the parser just check the kind to assign the root
         if (kind == Tree.kind.ROOT) {
             this.root = n;
             this.current = n;
         }
         // If it isn't root, make the current node the parent node of n, which was just created
         // Since you made the current node the parent, you have to add n to the children of the current node
         else {
             n.parent = this.current;
             this.current.children.add(n);
         }

         if (kind == Tree.kind.BRANCH)
             this.current = n;

     }

    /**
     * This method helps "move up" from the current node back to the parent. This is what allows for different branches
     * of the tree to be made rather than everything just going down one branch
     */
    public void moveUp(){
         if (this.current.parent != null && this.current.parent.name != null)
             this.current = this.current.parent;
         else {
             // Error Logging
         }

     }

    /**
     * This Method prints the CST using a recursive function to determine the depth
     * @return String representation of CST
     */
    @Override
     public String toString(){
         String traversalResult = "";
         traversalResult = expand(this.root, 0, traversalResult);
         return traversalResult;
     }

    /**
     * This is the recursive function that adds the depth to the CST. It is recursive called
     * after its initial call from the root node and will be called for each of the children of
     * the input node.
     * @param node The node to be added to the CST
     * @param depth The depth of the node
     * @param traversal The String representation of the CST
     * @return The String representation of the CST
     */
     public static String expand(Node node, int depth, String traversal){
         // Add - to the end of traversal, which will be the start of a newline
         traversal = traversal + "-".repeat(depth);

         // If the node has no children it must be a leaf node
         if (node.children == null || node.children.size() == 0)
             traversal += "[" + node.name + "] \n";
         // Otherwise, it is a branch or root node
         else {
             traversal += "<" + node.name + "> \n";

             // This is where the recursion will be called for each of the children of the non-leaf node
             for (int i = 0; i < node.children.size(); i++)
                 traversal = expand(node.children.get(i), depth + 1, traversal);
         }

         return traversal;
     }

    /**
     * Node class
     */
    static class Node {
        String name;
        List<Node> children = new ArrayList<>();
        Node parent;
        Tree.kind kind;

        public Node(String name, kind kind){
            this.name = name;
            this.kind = kind;
        }
    }
}
