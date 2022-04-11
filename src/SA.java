import java.util.ArrayList;
import java.util.regex.Pattern;

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

    public void semanticAnalysis(int programCounter) {
        // Add root node to symbol table tree
        symbol.addNode(String.valueOf(scope), kind.ROOT);
        traverse(ast.root);
        if (!errors){
            System.out.println("Semantic Analysis completed successfully for program " + programCounter);
            printSymbol(symbol.root, programCounter);
        } else {
            System.out.println("Semantic Analysis for program " + programCounter + " failed due to errors ");
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
        else if (node.name.equals("While Statement"))
            parseWhile(node);
        else if (node.name.equals("Print Statement"))
            parsePrint(node);
    }

    private static void parseIf(Node node) {
        if (node.children.get(0).name.equals("Is Equal") || node.children.get(0).name.equals("Not Equal"))
            parseEqual(node.children.get(0));
        else if (node.children.get(0).name.equals("Add"))
            parseAdd(node.children.get(0));
    }

    private static void parseWhile(Node node) {
        if (node.children.get(0).name.equals("Is Equal") || node.children.get(0).name.equals("Not Equal"))
            parseEqual(node.children.get(0));
        else if (node.children.get(0).name.equals("Add"))
            parseAdd(node.children.get(0));
    }

    private static void parsePrint(Node node) {
        Node child = node.children.get(0);
        if (child.name.equals("Is Equal") || node.children.get(0).name.equals("Not Equal"))
            parseEqual(child);
        else if (child.name.equals("Add"))
            parseAdd(child);
        else {
            if (child.token != null){
                if (child.token.type == Token.grammar.ID) {
                    if (checkAssign(child).type == null) {
                        System.out.println("Error12: Variable (" + child.name + ") undeclared on line " +
                                child.token.lineNumber + ":" + child.token.linePosition);
                        errors = true;
                    }
                }
            }
        }
    }

    private static void parseAssign(Node node) {
        // Assign the children to variables for easier access
        Node id = node.children.get(0);
        Node value = null;
        if (node.children.get(0).name.equals("Add") && parseAdd(node)) {
            value = new Node("int", scope, id.token.lineNumber, id.token.linePosition);
        }
        else {
            value = node.children.get(1);
        }
        // Assign current symbol table to variable for easier access
        Node cur = symbol.current;

        // If id is in the symbol table, check types
        if (cur.st.containsKey(id.name)) {
            if (checkType(value).equals(cur.st.get(id.name).type))
                cur.st.get(id.name).isInit = true;
            else {
                System.out.printf("%s%d%s%d%n%s%s%s%s%n", "Error1: type mismatch on line: ", id.token.lineNumber, ":",
                        id.token.linePosition, "can't assign type ", checkType(value), " to type ", checkAssign(id).type);
                errors = true;
            }
        } else if (checkAssign(id).type != null)
            cur.st.get(id.name).isInit = true;
        else {
            System.out.println("Error2: variable (" + id.name + ") not declared on line: "
                    + id.token.lineNumber + ":" + id.token.linePosition);
            errors = true;
        }
    }

    private static void parseVarDecl(Node node) {
        Node id = node.children.get(1);
        Node type = node.children.get(0);
        if (!symbol.current.st.containsKey(id.name)) {
            // Add the variable to the symbol table
            symbol.current.st.put(id.name, new Node(type.name, scope,
                    id.token.lineNumber, id.token.linePosition));
        }
        else {
            System.out.println("Error3 on line: " + node.token.lineNumber + ":" + node.token.linePosition
                    + "): Variable already declared in this scope");
            errors = true;
        }

    }

    private static String parseEqual(Node node) {
        if (!parseExpr(node.children.get(0)).equals(parseExpr(node.children.get(1)))) {
            System.out.printf("%s%d%s%d%n%s%s%s%s%n", "Error4: type mismatch on line: ", node.lineNumber, ":", node.linePos,
                    "can't compare type ", parseExpr(node.children.get(0)), " with type ",
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
    private static boolean parseAdd(Node node) {
        Node child = node.children.get(1);
        if (child.name.equals("Add"))
            parseAdd(child);
        else if (child.token != null){
            if (child.token.type == Token.grammar.ID) {
                if (checkAssign(child).type != null) {
                    if (!checkAssign(child).type.equals("int")) {
                        System.out.printf("%s%d%s%d%n%s%s%n", "Error7: Type mismatch on line: ", child.token.lineNumber,
                                ":", child.token.linePosition, "can't add type int with type ", checkAssign(child).type);
                        errors = true;
                        return false;
                    }
                } else {
                    System.out.println("Error8: Undeclared variable on line: " + child.token.lineNumber + ":" +
                            child.token.linePosition);
                    errors = true;
                    return false;
                }
            }
            else if (child.token.type != Token.grammar.DIGIT) {
                System.out.printf("%s%d%s%d%n%s%s", "Error9: Type mismatch on line: ", child.token.lineNumber,
                        ":", child.token.linePosition, "can't add type int with type ", checkType(child));
                errors = true;
                return false;
            }
        }
        return true;
    }

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
                System.out.printf("%s%d%s%d%n", "Error0: Variable undeclared on line: ", node.token.lineNumber, ":",
                        node.token.linePosition);
                errors = true;
                return "error";
            } else {
                node.isUsed = true;
                return checkAssign(node).type;
            }
        }
        else if (node.token.type == Token.grammar.DIGIT)
            return "int";
        else if (node.token.type == Token.grammar.STRING)
            return "string";
        else
            return "boolean";
    }


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

        if (node.name.equals("Add"))
            return "int";
        else if (boolExact.matcher(node.name).find())
            return "boolean";
        else if (digit.matcher(node.name).find())
            return "int";
        else
            return "string";
        }

    /**
     * This method prints the symbol table. It recursively calls down until it finds a node that has no children
     * and then prints that nodes symbol table. The hashmap gets printed using the forEach method which gets
     * passed a lambda function.
      * @param node node of the symbol table
     * @param programCounter current program counter
     */
    private static void printSymbol(Node node, int programCounter) {
        System.out.println("Symbol Table for program " + programCounter);
        System.out.println("--------------------------");
        System.out.println("Name Type   Scope Line");
        System.out.println("--------------------------");
        if (node.children.size() > 0)
            for (Node child: node.children)
                printSymbol(child, programCounter);
        else{
            if (!node.st.isEmpty())
                node.st.forEach((Key, Value) -> {
                System.out.printf("%-5s%-9s%-5d%d%n", Key, Value.type, Value.scope, Value.lineNumber);
            });
        }
    }

    }

