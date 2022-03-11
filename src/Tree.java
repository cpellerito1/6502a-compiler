import java.util.ArrayList;
import java.util.List;

/**
 * This class if for the concrete syntax tree part of the parser.
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

     static List<Node> cst;

     public void addNode(String name, kind kind){
        Node n = new Node(name, kind);

         if (kind == Tree.kind.ROOT || this.root == null) {
             this.root = n;
             this.current = n;
         }
         else {
             n.parent = this.current;
             this.current.children.add(n);
         }

         if (kind == Tree.kind.BRANCH)
             this.current = n;

     }

     public void moveUp(){
         if (this.current.parent != null && this.current.parent.name != null)
             this.current = this.current.parent;
         else {
             // Error Logging
         }

     }

     public String toString(){
         String traversalResult = "";
         traversalResult = expand(this.root, 0, traversalResult);
         return traversalResult;
     }

     public static String expand(Node node, int depth, String traversal){
         traversal = traversal + "-".repeat(Math.max(0, depth));

         if (node.children == null || node.children.size() == 0)
             traversal += "[" + node.name + "] \n";
         else {
             traversal += "<" + node.name + "> \n";

             for (int i = 0; i < node.children.size(); i++)
                 expand(node.children.get(i), depth + 1, traversal);
         }

         return traversal;
     }


    /**
     * This is a convenience method
     * @return last node in CST
     */
    // static Node getLast(){ return cst.get(cst.size()-1); }

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
