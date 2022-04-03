import java.util.ArrayList;

public class SA extends Tree {
    public ArrayList<Token> tokenStream;
    public Tree ast;

    // Symbol Table
    public static Tree symbol = new Tree();

    // Global variable to hold scope
    public static int scope = 0;
    // Pointer to access the tokenStream
    public static int current = 0;

    public SA (ArrayList<Token> tokenStream, Tree ast){
        this.tokenStream = tokenStream;
        this.ast = ast;
    }

    public void semanticAnalysis() {
        // Add root node to symbol table tree
        symbol.addNode(String.valueOf(scope), kind.ROOT);
        traverse(ast.root);
    }

    /**
     * Recursive method to traverse the CST and create the AST
     * @param node input node
     */
    private static void traverse(Node node) {
        if (node.kind == kind.ROOT) {
            node.scope = scope;
            for (Node children : node.children)
                traverse(children);
        }
        else if (node.kind == kind.BRANCH){
            parseBranch(node);
        }
    }

    private static void parseBranch(Node node) {
        if (node.name.equals("Block")){
            scope++;
            node.scope = scope;
            symbol.addNode(String.valueOf(scope), kind.BRANCH);
            for (Node children: node.children)
                traverse(children);
            scope--;
            symbol.moveUp();
        }
        else if (node.name.equals("Variable Declaration"))
            parseVarDecl(node);
        else if (node.name.equals("Assignment Statement"))
            parseAssign(node);
    }

    private static void parseAssign(Node node) {
        if (symbol.current.st.containsKey(node.children.get(0).name)) {
            node.children.get(0).value = node.children.get(1).name;
        } else {
            System.out.println("Error: variable not declared at (line:position) "
                    + node.token.lineNumber + ":" + node.token.linePosition);
        }
    }

    private static void parseVarDecl(Node node) {
        if (!symbol.current.st.containsKey(node.children.get(1).name))
            // Add the variable to the symbol table
            symbol.current.st.put(node.children.get(1).name, node.children.get(1));
        else
            System.out.println("Error: variable already declared at (line:position) "
                    + node.token.lineNumber + ":" + node.token.linePosition);

    }

    private static Node checkAssign(Node node){
        // Assign current node on the symbol table to a variable for easier access and to keep the order of the tree.
        Node cur = symbol.current;
        while (symbol.current != symbol.root){
            if (symbol.current.parent.st.containsKey(node.name)){
                return symbol.current.parent;
            } else {
                symbol.current = symbol.current.parent;
            }
        }
        symbol.current = cur;
    }
}
