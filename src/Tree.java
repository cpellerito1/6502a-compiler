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
         cst.add(new Node(name, kind));

         if (this.root == null || this.root != getLast())
             this.root = getLast();
         else {
             getLast().parent = this.current;
             this.current.children.add(getLast());
         }

         if (kind == Tree.kind.BRANCH)
             this.current = getLast();

     }


    /**
     * This is a convenience method
     * @return last node in CST
     */
     static Node getLast(){ return cst.get(cst.size()-1); }

    /**
     * Node class
     */
    static class Node {
        String name;
        List<Node> children;
        Node parent;
        Tree.kind kind;

        public Node(String name, kind kind){
            this.name = name;
            this.kind = kind;
        }
    }
}
