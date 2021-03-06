import java.util.ArrayList;

/**
 * This class runs Semantic Analysis. It is passed the ast when it is constructed and parses that. SA performs
 * type checking and scope checking. It will build a symbol table as it goes to keep track of the variables
 * that are declared. The symbol table is a tree of hashmaps whose key will be the name of the variable and the value
 * will be a new node object with attributes of type, scope, and linenumber/position. The symbol table will be printed
 * at the end of each program if SA completes with no errors
 *
 * @author Chris Pellerito
 */
public class SA extends Tree {
    public ArrayList<Token> tokenStream;
    public Tree ast;

    // Symbol Table
    public static Tree symbol = new Tree();

    // Global variable to hold scope
    public static int scope = 0;

    // Variable to hold errors
    public static boolean errors = false;

    public SA (ArrayList<Token> tokenStream, Tree ast){
        this.tokenStream = tokenStream;
        this.ast = ast;
    }

    /**
     * Main method everything runs from. Will be called by parser after each program
     * @param programCounter number of current program from parser
     */
    public void semanticAnalysis(int programCounter) {
        // Add root node to symbol table tree
        symbol.addNode(String.valueOf(scope), kind.ROOT);
        traverse(ast.root);
        if (!errors){
            System.out.println("Semantic Analysis completed successfully for program " + programCounter);
            // Print the symbol table
            System.out.println("Symbol Table for program " + programCounter);
            System.out.println("--------------------------");
            System.out.println("Name Type   Scope Line");
            System.out.println("--------------------------");
            ArrayList<String> used = printSymbol(symbol.root);
            for (String warnings: used)
                System.out.println(warnings);
            CG cg = new CG(ast, symbol);
            cg.codeGen(programCounter);
        } else {
            System.out.println("Semantic Analysis for program " + programCounter + " failed due to errors ");
            System.out.println("Symbol table not printing for program " + programCounter);
            errors = false;
        }
    }

    /**
     * Recursive method to traverse the AST
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

    // Parse Branch
    private static void parseBranch(Node node) {
        switch (node.name) {
            case "Block" -> parseBlock(node);
            case "Variable Declaration" -> parseVarDecl(node);
            case "Assignment Statement" -> parseAssign(node);
            case "If Statement" -> parseIf(node);
            case "While Statement" -> parseWhile(node);
            case "Print Statement" -> parsePrint(node);
        }
    }

    // Parse Block
    public static void parseBlock(Node node){
        scope++;
        symbol.addNode(String.valueOf(scope), kind.BRANCH);
        if (!node.children.isEmpty()) {
            for (Node children : node.children)
                traverse(children);
        }
        scope--;
        symbol.moveUp();
    }

    // Parse If Statement
    private static void parseIf(Node node) {
        parseBoolExpr(node);
        parseBlock(node.children.get(1));
    }

    // Parse While statement
    private static void parseWhile(Node node) {
        parseBoolExpr(node);
        parseBlock(node.children.get(1));
    }

    // Parse Bool expr
    private static void parseBoolExpr(Node node) {
        Node expr = node.children.get(0);
        if (expr.name.equals("Is Equal") || expr.name.equals("Not Equal"))
            parseEqual(expr);
        else if (expr.name.equals("Add"))
            parseAdd(expr);
        else if (expr.token != null){
            if (!checkType(expr).equals("boolean")) {
                System.out.println("Error: " + checkType(expr) + " literal must be of type bool in a bool expression" +
                        " on line " + expr.token.lineNumber + ":" + expr.token.linePosition);
                errors = true;
            }
        }
    }

    // Parse Print statement
    private static void parsePrint(Node node) {
        Node child = node.children.get(0);
        if (child.name.equals("Is Equal") || child.name.equals("Not Equal"))
            parseEqual(child);
        else if (child.name.equals("Add"))
            parseAdd(child);
        else if (child.token != null){
            if (child.token.type == Token.grammar.ID) {
                setUsed(child);
                if (checkAssign(child).type == null) {
                    System.out.println("Error: Variable (" + child.name + ") undeclared on line " +
                            child.token.lineNumber + ":" + child.token.linePosition);
                    errors = true;
                }
            }
        }
    }

    // Parse assignment statement
    private static void parseAssign(Node node) {
        Node id = node.children.get(0);
        Node value = node.children.get(1);

        if (value.name.equals("Add"))
            parseAdd(value);

        // Assign current symbol table to variable for easier access
        Node cur = symbol.current;

        // If id is in the current symbol table, check types
        if (cur.st.containsKey(id.name)) {
            if (value.token != null && value.token.type == Token.grammar.ID) {
                setUsed(value);
                if (checkAssign(value).type == null) {
                    System.out.println("Error: variable (" + value.name + ") undeclared in this scope on line: " +
                            id.token.lineNumber + ":" + id.token.linePosition);
                    errors = true;
                } else if (!checkAssign(value).type.equals(cur.st.get(id.name).type)) {
                    System.out.printf("%s%d%s%d%s%s%s%s%n", "Error: type mismatch on line: ", id.token.lineNumber, ":",
                            id.token.linePosition, " can't assign type ", checkAssign(value).type, " to type ",
                            cur.st.get(id.name).type);
                    errors = true;
                }
            } else if (checkType(value).equals(cur.st.get(id.name).type)) {
                cur.st.get(id.name).isInit = true;
                if (value.token != null && value.token.type == Token.grammar.ID)
                    setUsed(value);
            } else {
                System.out.printf("%s%d%s%d%s%s%s%s%n", "Error: type mismatch on line: ", id.token.lineNumber, ":",
                        id.token.linePosition, " can't assign type ", checkType(value), " to type ", checkAssign(id).type);
                errors = true;
            }
        } else if (checkAssign(id).type == null) {
            System.out.println("Error: variable (" + id.name + ") not declared on line: "
                    + id.token.lineNumber + ":" + id.token.linePosition);
            errors = true;
        } else
            setUsed(id);

    }

    // Parse variable declaration
    private static void parseVarDecl(Node node) {
        Node id = node.children.get(1);
        Node type = node.children.get(0);
        if (!symbol.current.st.containsKey(id.name)) {
            // Add the variable to the symbol table
            symbol.current.st.put(id.name, new Node(type.name, scope,
                    id.token.lineNumber, id.token.linePosition));
        }
        else {
            System.out.println("Error on line: (" + id.token.lineNumber + ":" + id.token.linePosition
                    + ") Variable already declared in this scope");
            errors = true;
        }
    }

    // Parse is/not equal
    private static String parseEqual(Node node) {
        if (!parseExpr(node.children.get(0)).equals(parseExpr(node.children.get(1)))) {
            System.out.printf("%s%d%s%d%n%s%s%s%s%n", "Error: type mismatch on line: ", node.lineNumber, ":",
                    node.linePos, "can't compare type ", parseExpr(node.children.get(0)), " with type ",
                    parseExpr(node.children.get(1)));
            errors = true;
            return "error";
        }
        else
            return parseExpr(node.children.get(0));
    }


    /**
     * This method parses the intop expression. Because of the grammar, we know addition must have a digit first
     * and because of the type system we know that you can only add integers to integers,
     * so the first child must be an int, or else it wouldn't have made it through parse. Therefore, all we need to do
     * is check that the second child is an int, an id with type int, or it can be another int expression that must
     * return type int.
     * @param node input node
     */
    private static void parseAdd(Node node) {
        Node child = node.children.get(1);
        if (child.name.equals("Add"))
            parseAdd(child);
        else if (child.token != null){
            if (child.token.type == Token.grammar.ID) {
                if (checkAssign(child).type != null) {
                    setUsed(child);
                    if (!checkAssign(child).type.equals("int")) {
                        System.out.printf("%s%d%s%d%n%s%s%n", "Error: Type mismatch on line: ", child.token.lineNumber,
                                ":", child.token.linePosition, "can't add type int with type ", checkAssign(child).type);
                        errors = true;
                    }
                } else {
                    System.out.println("Error: Undeclared variable on line: " + child.token.lineNumber + ":" +
                            child.token.linePosition);
                    errors = true;
                }
            }
            else if (child.token.type != Token.grammar.DIGIT) {
                System.out.printf("%s%d%s%d%s%s%n", "Error: Type mismatch on line: ", child.token.lineNumber,
                        ":", child.token.linePosition, " can't add type int with type ", checkType(child));
                errors = true;
            }
        }
    }

    /**
     * This method parses expressions and returns their type.
     * @param node Node representing an expression
     * @return The type of the expression (string, int, boolean).
     */
    private static String parseExpr(Node node) {
        if (node.name.equals("Is Equal") || node.name.equals("Not Equal")) {
            return parseEqual(node);
        }
        else if (node.name.equals("Add")) {
            parseAdd(node);
            return "int";
        }
        else if (node.token.type == Token.grammar.ID){
            if (checkAssign(node).type == null) {
                System.out.printf("%s%d%s%d%n", "Error: Variable undeclared on line: ", node.token.lineNumber, ":",
                        node.token.linePosition);
                errors = true;
                return "error";
            } else {
                setUsed(node);
                return checkAssign(node).type;
            }
        }
        else if (node.token.type == Token.grammar.DIGIT)
            return "int";
        else if (node.token.type == Token.grammar.STRING)
            return "string";
        else if (node.token.type == Token.grammar.BOOL_VAL)
            return "boolean";
        else
            return "error";
    }

    /**
     * This method checks the symbol table to see if a variable was declared
     * @param id The node of the variable
     * @return If the variable is in the symbol table, the node from the symbol table will be returned,
     * if it isn't in the symbol table, a node with type null will be returned.
     */
    private static Node checkAssign(Node id){
        // Assign current node of the symbol table to a variable to keep the current node correct after method
        Node cur = symbol.current;
        // Check if node is in current symbol table
        if (cur.st.containsKey(id.name)) {
            return cur.st.get(id.name);
        }
        // If it isn't in the current table check its parents
        else {
            while (symbol.current != symbol.root) {
                if (symbol.current.parent.st.containsKey(id.name)) {
                    // Temp variable to avoid overwriting current node
                    Node temp = symbol.current.parent.st.get(id.name);
                    temp.isInit = true;
                    // Reset the current node
                    symbol.current = cur;
                    return temp;
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
        if (checkAssign(node).type != null)
            return checkAssign(node).type;
        else if (node.name.equals("Add"))
            return "int";
        else if (node.token.type == Token.grammar.BOOL_VAL)
            return "boolean";
        else if (node.token.type == Token.grammar.DIGIT)
            return "int";
        else
            return "string";
        }

    /**
     * This method prints the symbol table. It recursively calls down until it finds a node that has no children
     * and then prints that nodes symbol table. The hashmap gets printed using the forEach method which gets
     * passed a lambda function. It also checks if the variables were used/initialized.
     * @param node node of the symbol table
     * @return ArrayList containing the warning messages
     */
    private static ArrayList<String> printSymbol(Node node) {
        ArrayList<String> used = new ArrayList<>();
        if (node.children.size() > 0) {
            for (Node child : node.children)
                printSymbol(child);
        }

        if (!node.st.isEmpty())
            node.st.forEach((Key, Value) -> {
                System.out.printf("%-5s%-9s%-5d%d%n", Key, Value.type, Value.scope, Value.lineNumber);
                if (!Value.isUsed)
                    used.add(("Warning: Variable (" + Key +  ":" + Value.type + ") is declared but never used"));
                if (!Value.isInit)
                    used.add(("Warning: Variable (" + Key +  ":" + Value.type + ") is declared but never initialized"));
            });

        return used;
    }

//    private static ArrayList<String> print(Node node) {
//        ArrayList<String> used = new ArrayList<>();
//        node.st.forEach((Key, Value) -> {
//            System.out.printf("%-5s%-9s%-5d%d%n", Key, Value.type, Value.scope, Value.lineNumber);
//            if (!Value.isUsed)
//                used.add(("Warning: Variable (" + Key +  ":" + Value.type + ") is declared but never used"));
//            if (!Value.isInit)
//                used.add(("Warning: Variable (" + Key +  ":" + Value.type + ") is declared but never initialized"));
//        });
//        return used;
//    }

    /**
     * This method sets the boolean isUsed attribute to true for nodes in the symbol table
     * @param node Input Node
     */
    private static void setUsed(Node node) {
        Node cur = symbol.current;
        if (cur.st.containsKey(node.name))
            cur.st.get(node.name).isUsed = true;
        else {
            while (symbol.current.parent != null){
                if (symbol.current.parent.st.containsKey(node.name)) {
                    symbol.current.parent.st.get(node.name).isUsed = true;
                    break;
                }
                else
                    symbol.moveUp();
            }
        }
        // Reset current
        symbol.current = cur;
    }

}

