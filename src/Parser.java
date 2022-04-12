import java.util.ArrayList;

/**
 * This class contains the recursive decent parser for the compiler. It also adds node to the Concrete Syntax
 * Tree
 *
 * @author Chris Pellerito
 */
public class Parser {
    // List for tokenStream from Lexer
    public ArrayList<Token> tokenStream;

    // pointer for accessing tokenStream
    public static int current;

    // Variable to hold the string representation of characterlists
    public static String charToString = "";

    // Variable to hold the state of errors, since the CST shouldn't be printed if there are parse errors,
    // and it doesn't matter if there are more than 1 errors, just that they exist
    static boolean isErrors;

    // Arrays of ENUM types to help with the parse
    public static Token.grammar[] statements = {Token.grammar.KEYWORD,
            Token.grammar.TYPE, Token.grammar.ID, Token.grammar.L_BRACE};
    public static Token.grammar[] expressions = {Token.grammar.DIGIT, Token.grammar.L_QUOTE,
            Token.grammar.L_PARAN, Token.grammar.ID, Token.grammar.BOOL_VAL};
    public static Token.grammar[] abs = {Token.grammar.ID, Token.grammar.DIGIT,
            Token.grammar.TYPE, Token.grammar.BOOL_VAL};

    // Instantiate CST
    Tree cst = new Tree();
    // Instantiate AST
    Tree ast = new Tree();

    public Parser(ArrayList<Token> tokenStream){
        this.tokenStream = tokenStream;
    }

    // Main parse method
    public void parse(int programCounter){
        // Reset counter and errors
        current = 0;
        isErrors = false;

        // Add the root node
        cst.addNode("Program", Tree.kind.ROOT);

        System.out.println("parse()");
        parseBlock();
        match(Token.grammar.EOP);
        if (!isErrors) {
            System.out.printf("%n%s%n", "CST");
            System.out.println(cst.toString());
            System.out.printf("%n%s%n", "AST");
            System.out.println(ast.toString());
            // Run Semantic Analysis on the AST that was produced
            SA sa = new SA(tokenStream, ast);
            System.out.println("Beginning Semantic Analysis for program " + programCounter);
            sa.semanticAnalysis(programCounter);
        } else
            System.out.printf("%n%s%n%s%d%n", "CST and AST not printing due to Parse error(s)",
                    "Skipping Semantic Analysis for program ", programCounter);

        cst.moveUp();

    }

    // Parse block
    private void parseBlock(){
        cst.addNode("Block", Tree.kind.BRANCH);
        ast.addNode("Block", Tree.kind.BRANCH);
        System.out.println("parseBlock()");
        match(Token.grammar.L_BRACE);
        parseStateList();
        match(Token.grammar.R_BRACE);
        cst.moveUp();
    }

    // Parse statementlist
    private void parseStateList(){
        cst.addNode("Statement List", Tree.kind.BRANCH);
        System.out.println("parseStateList()");
        if (tokenStream.get(current).contains(statements)) {
            parseState();
            parseStateList();
        } else { } // Java doesn't like this
        cst.moveUp();
    }

    // Parse statement
    private void parseState(){
        cst.addNode("Statement", Tree.kind.BRANCH);
        System.out.println("parseState()");
        // add current token to a variable for easy access
        Token token = tokenStream.get(current);
        if (token.attribute.equals("print"))
            parsePrint();
        else if (token.attribute.equals("while"))
            parseWhile();
        else if (token.attribute.equals("if"))
            parseIf();
        else if (token.type == Token.grammar.ID)
            parseAssign();
        else if (token.type == Token.grammar.TYPE)
            parseVarDec();
        else
            parseBlock();
        cst.moveUp();
    }

    // Parse variable declaration
    private void parseVarDec(){
        cst.addNode("Variable Declaration", Tree.kind.BRANCH);
        ast.addNode("Variable Declaration", Tree.kind.BRANCH);
        System.out.println("parseVarDec()");
        cst.addNode("Type", Tree.kind.BRANCH);
        match(Token.grammar.TYPE);
        cst.addNode("ID", Tree.kind.BRANCH);
        match(Token.grammar.ID);
        cst.moveUp();
        ast.moveUp();
    }

    // Parse assignment statement
    private void parseAssign(){
        cst.addNode("Assignment Statement", Tree.kind.BRANCH);
        ast.addNode("Assignment Statement", Tree.kind.BRANCH);
        System.out.println("parseAssign()");
        cst.addNode("ID", Tree.kind.BRANCH);
        match(Token.grammar.ID);
        match(Token.grammar.ASSIGN_OP);
        parseExprs();
        cst.moveUp();
        ast.moveUp();
    }

    // Parse print statement
    private void parsePrint(){
        cst.addNode("Print Statement", Tree.kind.BRANCH);
        ast.addNode("Print Statement", Tree.kind.BRANCH);
        System.out.println("parsePrint()");
        matchString("print");
        match(Token.grammar.L_PARAN);
        if (tokenStream.get(current).contains(expressions))
            parseExprs();

        match(Token.grammar.R_PARAN);
        cst.moveUp();
        ast.moveUp();
    }

    // Parse expression
    private void parseExprs() {
        cst.addNode("Expression", Tree.kind.BRANCH);
        System.out.println("parseExprs()");
        // add current token to a variable for easy access
        Token token = tokenStream.get(current);
        if (token.type == Token.grammar.DIGIT)
            parseIntExpr();
        else if(token.type == Token.grammar.L_QUOTE)
            parseStringExpr();
        else if (token.type == Token.grammar.L_PARAN)
            parseBoolExpr();
        else if (token.type == Token.grammar.BOOL_VAL)
            parseBoolExpr();
        else {
            cst.addNode("ID", Tree.kind.BRANCH);
            match(Token.grammar.ID);
        }
        cst.moveUp();
    }

    // Parse boolean expression
    private void parseBoolExpr() {
        cst.addNode("Boolean Expression", Tree.kind.BRANCH);
        System.out.println("parseBoolExpr()");
        if (tokenStream.get(current).type == Token.grammar.L_PARAN) {
            match(Token.grammar.L_PARAN);
            parseExprs();
            if (tokenStream.get(current).type == Token.grammar.EQUAL_OP) {
                ast.addNode("Is Equal", Tree.kind.BRANCH);
                ast.current.lineNumber = tokenStream.get(current).lineNumber;
                ast.current.linePos = tokenStream.get(current).linePosition;
                match(Token.grammar.EQUAL_OP);
            }
            else {
                ast.addNode("Not Equal", Tree.kind.BRANCH);
                ast.current.lineNumber = tokenStream.get(current).lineNumber;
                ast.current.linePos = tokenStream.get(current).linePosition;
                match(Token.grammar.IN_EQUAL_OP);
            }
            ast.restructure();
            parseExprs();
            match(Token.grammar.R_PARAN);
            ast.moveUp();
        } else {
            cst.addNode("Bool Val", Tree.kind.BRANCH);
            match(Token.grammar.BOOL_VAL);
        }
        cst.moveUp();
    }

    // Parse string expression
    private void parseStringExpr() {
        cst.addNode("String Expression", Tree.kind.BRANCH);
        System.out.println("parseStringExpr()");
        match(Token.grammar.L_QUOTE);
        parseCharList();
        match(Token.grammar.R_QUOTE);
        cst.moveUp();
    }

    // Parse character list
    private void parseCharList() {
        cst.addNode("Character List", Tree.kind.BRANCH);
        System.out.println("parseCharList()");
        // Add current token to a variable for easier access
        Token token = tokenStream.get(current);
        if (token.type == Token.grammar.CHAR){
            match(Token.grammar.CHAR);
            parseCharList();
        } else if (token.type == Token.grammar.SPACE){
            match(Token.grammar.SPACE);
            parseCharList();
        } else { } // Java doesn't like this
        cst.moveUp();
    }

    // Parse int expression
    private void parseIntExpr() {
        cst.addNode("Integer Expression", Tree.kind.BRANCH);
        match(Token.grammar.DIGIT);
        if (tokenStream.get(current).type == Token.grammar.ADD_OP){
            match(Token.grammar.ADD_OP);
            ast.addNode("Add", Tree.kind.BRANCH);
            if (ast.current.parent.name.equals("Assignment Statement"))
                ast.assignRestructure();
            else {
                ast.restructure();
            }
            parseExprs();
            ast.moveUp();
        }
        else { } // Java doesn't like this
        cst.moveUp();
    }

    // Parse while statement
    private void parseWhile(){
        cst.addNode("While Statement", Tree.kind.BRANCH);
        ast.addNode("While Statement", Tree.kind.BRANCH);
        System.out.println("parseWhile()");
        matchString("while");
        parseBoolExpr();
        parseBlock();
        cst.moveUp();
        ast.moveUp();
    }

    // Parse if statement
    private void parseIf(){
        cst.addNode("If Statement", Tree.kind.BRANCH);
        ast.addNode("If Statement", Tree.kind.BRANCH);
        System.out.println("parseIf()");
        matchString("if");
        parseBoolExpr();
        parseBlock();
        cst.moveUp();
        ast.moveUp();
    }

    /**
     * Method to match the terminal tokens using the ENUM type from the Token class
     * @param expected Expected Token type
     */
    private void match(Token.grammar expected){
        // Assign current token to variable for easier access
        Token token = tokenStream.get(current);
        if (token.type == expected){
            // Consume token
            cst.addNode(token.attribute, Tree.kind.LEAF);
            // If the token is a char, add it to a string for the AST
            if (expected == Token.grammar.CHAR || expected == Token.grammar.SPACE)
                charToString += token.attribute;
            // Add the node when you see an end quote and reset the string
            else if (expected == Token.grammar.R_QUOTE) {
                ast.addNode(charToString, Tree.kind.LEAF,
                        new Token(token.lineNumber, token.linePosition, charToString, Token.grammar.STRING));
                charToString = "";
            }
            current++;
        } else {
            System.out.println("ERROR: expected " + expected.toString() + " got " + token.type.toString() +
                    " on line " + token.lineNumber + " at " + token.linePosition);
            isErrors = true;
        }

        if (token.contains(abs)){
            ast.addNode(token.attribute, Tree.kind.LEAF, token);
        }
    }

    /**
     * This method is the same as the regular match method but uses a different input. This is needed
     * because of the way I handled keywords in Lex. Instead of making a KEYWORD_PRINT ENUM and creating
     * a regex for each keyword, I used just one regex and made the ENUM just KEYWORD and then stored the
     * type of keyword in the attribute field.
     * @param expected String representation of the expected Token
     */
    private void matchString(String expected){
        // Assign current token to variable for easier access
        Token token = tokenStream.get(current);

        if (token.attribute.equals(expected)) {
            // Consume token
            cst.addNode(token.attribute, Tree.kind.LEAF);
            current++;
        } else {
            System.out.println("ERROR: expected " + expected + " got " + token.attribute +
                    " on line " + token.lineNumber + " at " + token.linePosition);
            isErrors = true;
        }
        // Check if token should be added to AST
        if (token.contains(abs)) {
            ast.addNode(token.attribute, Tree.kind.LEAF, token);
        }
    }
}
