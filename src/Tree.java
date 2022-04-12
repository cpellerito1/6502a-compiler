import java.util.ArrayList;
import java.util.HashMap;
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
     * Adds a node to the tree. This method is used for adding branch nodes
     * @param name Name of the node
     * @param kind Kind of the node from ENUM kind. (Root, Branch, Leaf)
     */
     public void addNode(String name, kind kind){
        Node n = new Node(name, kind);
        // Set the node's position in the tree
        this.setNode(n);

     }

    /**
     * Adds a node to the tree with a token pointer. This method is used to add leaf nodes to the tree because
     * they have a corresponding token.
     * @param name Name of the node
     * @param kind kind of the node from ENUM kind. (ROOT, BRANCH, LEAF)
     * @param token Pointer to the token the Node is created from. This will help during Semantic Analysis
     */
    public void addNode(String name, kind kind, Token token){
        Node n = new Node(name, kind, token);
        // Set the node's position in the tree
        this.setNode(n);
    }

    /**
     * This method sets the node's position in the tree. This is needed to set the position for each of the addNode
     * methods.
     * @param node new node being added to the tree
     */
    private void setNode(Node node) {
        // Since the root node will be assigned the root type in the parser just check the kind to assign the root
        if (node.kind == Tree.kind.ROOT || this.root == null) {
            this.root = node;
            this.current = node;

            // For the ast, since blocks are being assigned kind.BRANCH but can also be the root
            node.kind = Tree.kind.ROOT;
        }
        // If it isn't root, make the current node the parent node of n, which was just created
        // Since you made the current node the parent, you have to add n to the children of the current node
        else {
            node.parent = this.current;
            this.current.children.add(node);
        }

        if (node.kind == Tree.kind.BRANCH)
            this.current = node;
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
     * This Method prints the tree using a recursive function to determine the depth
     * @return String representation of CST
     */
    @Override
     public String toString(){
         String traversalResult = "";
         traversalResult = expand(this.root, 0, traversalResult);
         return traversalResult;
     }

    /**
     * This is the recursive function that adds the depth to the CST. It is recursively called
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
         if (node.kind == kind.LEAF)
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
     * This method helps keep the order of the AST correct. Since I am building the AST at the same time as the CST,
     * I need to rearrange the order of the nodes in certain scenarios. This method takes the current node, adds all of
     * its parent's children to its children. It then removes all other children from its parent.
     */
    public void restructure() {

        // Add current node and parent of current node to variables for easy access
        Node current = this.current;
        Node parent = this.current.parent;
        // Temp variable to keep track of Add nodes
        Node temp = null;
        // If the parent has
        if (current.name.equals("Add") || current.name.equals("Is Equal") || current.name.equals("Not Equal")) {
            for (Node child : parent.children)
                if ((child.name.equals("Add") || child.name.equals("Is Equal")) || child.name.equals("Not Equal"))
                    if (!current.equals(child))
                        temp = child;
        }

        // Add all the children of parent to the children of current
        current.children.addAll(parent.children);
        // Remove current from its own children because that would be weird
        current.children.remove(current);
        // Clear all the children from the parent
        parent.children.clear();
        // If temp is not null, add it back to children of parent
        if (temp != null && current.children.size() > 1) {
            current.children.remove(temp);
            parent.children.add(temp);
        }
        // Re-add the current node to the children of parent
        parent.children.add(current);
    }

    public void assignRestructure() {
        Node current = this.current;
        Node parent = this.current.parent;

        current.children.add(parent.children.get(parent.children.indexOf(current) - 1));
        parent.children.remove(parent.children.get(parent.children.indexOf(current) - 1));
    }

    /**
     * Node class. This class will be used to add nodes to the CST, AST, and the symbol table.
     */
    static class Node {
        String name;
        // For CST and AST
        ArrayList<Node> children = new ArrayList<Node>();
        Node parent;
        Tree.kind kind;
        // Pointer to the token this node is based on (only for leaf nodes/keywords)
        Token token;
        // For nodes in the symbol table
        String type;
        int scope;
        int lineNumber;
        int linePos;
        Boolean isUsed = false;
        Boolean isInit = false;
        HashMap<String, Node> st = new HashMap<String, Node>();

        // For nodes in the CST and AST
        public Node(String name, kind kind) {
            this.name = name;
            this.kind = kind;
        }

        // For leaf and keyword nodes in the CST and AST
        public Node(String name, kind kind, Token token) {
            this.name = name;
            this.kind = kind;
            this.token = token;

        }

        // For nodes in the Symbol table
        public Node(String type, int scope, int line, int linePos) {
            this.type = type;
            this.scope = scope;
            this.lineNumber = line;
            this.linePos = linePos;
            this.isInit = false;
            this.isUsed = false;
        }
    }
}
