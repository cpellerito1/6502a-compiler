/**
 * This class if for the concrete syntax tree part of the parser.
 * This code is adapted from code found here:
 *
 * @author Chris Pellerito
 */
public class Tree {
    public enum kind{
        ROOT, BRANCH, LEAF
    }
     Node root = null;
     Node current;

     public static void addNode(String name, kind kind){
         Node node = new Node(name, kind);
     }




    static class Node {
        String name;
        Node[] children;
        Node parent;
        Tree.kind kind;

        public Node(String name, kind kind){
            this.name = name;
            this.kind = kind;
        }
    }
}
