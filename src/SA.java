import java.util.ArrayList;
import java.util.regex.Pattern;

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
        else if (node.name.equals("If Statement"))
            parseIf(node);
    }

    private static void parseIf(Node node) {
        if (node.children.get(0).name.equals("Is Equal"))
            parseEqual(node.children.get(0));
    }

    private static void parseAssign(Node node) {
        // Assign the children to variables for easier access
        Node id = node.children.get(0);
        Node value = node.children.get(1);
        // Assign current symbol table to variable for easier access
        Node cur = symbol.current;

        // If id is in the symbol table, check types
        if (cur.st.containsKey(id.name)) {
            if (checkType(value).equals(cur.st.get(id.name).type)) {
                cur.st.replace(id.name, new Node(value.name, scope, id.token.lineNumber, id.token.linePosition));
                cur.st.get(id.name).isInit = true;
            }
            else
                System.out.printf("%s%d%s%d%n%s%s%s%s","Error1: type mismatch on line: ", id.token.lineNumber,  ":",
                        id.token.linePosition, "can't assign type ", checkType(value), " to type ", checkType(id));
        } else if (checkAssign(id).type != null) {
            cur.st.put(id.name, new Node(checkAssign(id).type, scope, id.token.lineNumber, id.token.linePosition));
            cur.st.get(id.name).isInit = true;
        } else {
            System.out.println("Error2: variable not declared on line: "
                    + id.token.lineNumber + ":" + id.token.linePosition);
        }
    }

    private static void parseVarDecl(Node node) {
        Node id = node.children.get(1);
        Node type = node.children.get(0);
        if (!symbol.current.st.containsKey(node.children.get(1).name)) {
            // Add the variable to the symbol table
            symbol.current.st.put(node.children.get(1).name, new Node(type.name, scope,
                    id.token.lineNumber, id.token.linePosition));
        }
        else
            System.out.println("Error3 on line: " + node.token.lineNumber + ":" + node.token.linePosition
                    + "): Variable already declared in this scope");

    }

    private static void parseEqual(Node node) {
        if (!parseExpr(node.children.get(0)).equals(parseExpr(node.children.get(1))))
            System.out.printf("%s%d%s%d%n%s%s%s%s%n","Error4: type mismatch on line: ", node.lineNumber, ":", node.linePos,
                    "can't compare type ", parseExpr(node.children.get(0)), " with type ",
                    parseExpr(node.children.get(1)));


//        // If first node is a variable, check assignment and type
//        else if (node.children.get(0).token.type == Token.grammar.ID && !node.children.get(1).name.equals("Add"))
//            checkEquality(node.children.get(0), node.children.get(1));
//        else if (node.children.get(1).token.type == Token.grammar.ID && !node.children.get(1).name.equals("Add"))
//            checkEquality(node.children.get(1), node.children.get(0));
//        else if (checkType(node.children.get(0)).equals("int"))
//                if (!checkType(node.children.get(1)).equals("int"))
//                    System.out.printf("%s%d%n%s%s","Error: Type mismatch on line: ", node.children.get(0).lineNumber,
//                            "can't compare type int with ", checkType(node.children.get(1)));
//        else if (checkType(node.children.get(0)).equals("boolean"))
//            if (!checkType(node.children.get(1)).equals("boolean"))
//                System.out.printf("%s%d%n%s%s","Error: Type mismatch on line: ", node.children.get(0).lineNumber,
//                        "can't compare type boolean with ", checkType(node.children.get(1)));
//        else if (checkType(node.children.get(0)).equals("string"))
//            if (!checkType(node.children.get(1)).equals("string"))
//                System.out.printf("%s%d%n%s%s","Error: Type mismatch on line: ", node.children.get(0).lineNumber,
//                        "can't compare type string with ", checkType(node.children.get(1)));
        }

//    private static void checkEquality(Node id, Node value) {
//        if (checkAssign(id).type != null)
//            if (!checkType(value).equals(checkAssign(id).type))
//                System.out.printf("%s%d%s%d%n%s%s%s%s", "Error5: type mismatch on line: ", id.token.lineNumber, ":",
//                        id.token.linePosition, " can't compare type ", checkAssign(id), " to type ", checkType(value));
//        else
//            System.out.println("Error6: Variable undeclared on line: " +
//                    id.token.lineNumber + ":" + id.token.linePosition);
//    }


    /**
     * This method parses the intop expression. Because of the grammar, we know addition must have a digit first
     * and because of the type system we know that you can only add integers to integers,
     * so the first child must be an int, or else it wouldn't have made it through parse. Therefore all we need to do
     * is check that the second child is an int, an id with type int or it can be another int expression that must
     * return type int.
     * @param node input node
     */
    private static void parseAdd(Node node) {
        Node child = node.children.get(1);
        if (child.name.equals("Add"))
            parseAdd(child);
        else if (child.token != null){
            if (child.token.type == Token.grammar.ID) {
                if (checkAssign(child).type != null)
                    if (!checkType(child).equals("int"))
                        System.out.printf("%s%d%s%d%n%s%s", "Error7: Type mismatch on line: ", child.token.lineNumber,
                                ":", child.token.linePosition, "can't add type int with type ", checkType(child));
                else
                    System.out.println("Error8: Undeclared variable on line: " + child.token.lineNumber + ":" +
                            child.token.linePosition);
            }
            else if (child.token.type != Token.grammar.DIGIT)
                System.out.printf("%s%d%s%d%n%s%s", "Error9: Type mismatch on line: ", child.token.lineNumber,
                        ":", child.token.linePosition, "can't add type int with type ", checkType(child));
        }
    }

    private static String parseExpr(Node node) {
        if (node.name.equals("Is Equal") || node.name.equals("Not Equal")) {
            parseEqual(node);
        }
        else if (node.name.equals("Add")) {
            parseAdd(node);
            return "int";
        }
        else if (node.token.type == Token.grammar.ID){
            if (checkAssign(node).type == null) {
                System.out.printf("%s%d%s%d%n", "Error0: Variable undeclared on line: ", node.token.lineNumber, ":",
                        node.token.linePosition);
                return "error";
            }
            else {
                node.isUsed = true;
                return checkType(node);
            }
        }
        else if (node.token.type == Token.grammar.DIGIT)
            return "int";
        else if (node.token.type == Token.grammar.STRING)
            return "string";
        else
            return "boolean";

        return "error";
    }


    private static Node checkAssign(Node id){
        // Assign current node of the symbol table to a variable to keep the current node correct after method
        Node cur = symbol.current;
        // Check if node is in current symbol table
        if (cur.st.containsKey(id.name))
            return cur.st.get(id.name);
        // If it isn't in the current table check its parents
        else {
            while (symbol.current != symbol.root) {
                if (symbol.current.parent.st.containsKey(id.name)) {
                    return symbol.current.parent.st.get(id.name);
                } else
                    symbol.moveUp();
            }
        }
        // Reset the current node
        symbol.current = cur;
        // If id does not exist in any of the parent scopes, return a Node with a null type and 0s for all other params
        return new Node(null, 0, 0, 0);
    }

    /**
     * This method does type checking. It checks if the value being assigned, compared or added to a variable
     * is the correct type
     * @param node Node of the variable being type checked
     * @return True if types match false if not
     */
    private static String checkType(Node node) {
        // Regex patterns from Lex to match booleans and digits
        Pattern boolExact = Pattern.compile("^true$|^false$");
        Pattern digit = Pattern.compile("[0-9]");

        if (boolExact.matcher(node.name).find())
            return "boolean";
        else if (digit.matcher(node.name).find())
            return "int";
        else
            return "string";

//        if (symbol.current.st.get(id.name).type.equals("int"))
//            return digit.matcher(value.name).find();
//        else if (symbol.current.st.get(id.name).type.equals("boolean"))
//            return boolExact.matcher(value.name).find();
//        else {
//            // Check String
//            return value.token.type == Token.grammar.STRING;
        }
    }

