import java.util.List;

/**
 * This class contains the recursive decent parser for the compiler
 *
 * @author Chris Pellerito
 */
public class Parser {
    // List for tokenStream from Lexer
    public List<Token> tokenStream;

    // pointer for accessing tokenStream
    public static int current;

    // Variable to hold the state of errors, since the CST shouldn't be printed if there are parse errors,
    // and it doesn't matter if there are more than 1 errors, just that they exist
    static boolean isErrors;

    // Arrays of ENUM types to help with the parse
    public static Token.grammar[] statements = {Token.grammar.KEYWORD,
            Token.grammar.TYPE, Token.grammar.ID, Token.grammar.L_BRACE};
    public static Token.grammar[] expressions = {Token.grammar.DIGIT, Token.grammar.QUOTE,
            Token.grammar.L_PARAN, Token.grammar.ID};

    // Instantiate CST
    Tree cst = new Tree();

    public Parser(List<Token> tokenStream){
        this.tokenStream = tokenStream;
    }

    // Main parse method
    public void parse(){
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
        } else
            System.out.printf("%n%s%n", "CST not printing due to Parse error(s)");

        cst.moveUp();
    }

    // Parse block
    private void parseBlock(){
        cst.addNode("Block", Tree.kind.BRANCH);
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
        System.out.println("parseVarDec()");
        match(Token.grammar.TYPE);
        match(Token.grammar.ID);
        cst.moveUp();
    }

    // Parse assignment statement
    private void parseAssign(){
        cst.addNode("Assignment Statement", Tree.kind.BRANCH);
        System.out.println("parseAssign()");
        match(Token.grammar.ID);
        match(Token.grammar.ASSIGN_OP);
        parseExprs();
        cst.moveUp();
    }

    // Parse print statement
    private void parsePrint(){
        cst.addNode("Print Statement", Tree.kind.BRANCH);
        System.out.println("parsePrint()");
        matchString("print");
        match(Token.grammar.L_PARAN);
        if (tokenStream.get(current).contains(expressions))
            parseExprs();

        match(Token.grammar.R_PARAN);
        cst.moveUp();
    }

    // Parse expression
    private void parseExprs() {
        cst.addNode("Expression", Tree.kind.BRANCH);
        System.out.println("parseExprs()");
        // add current token to a variable for easy access
        Token token = tokenStream.get(current);
        if (token.type == Token.grammar.DIGIT)
            parseIntExpr();
        else if(token.type == Token.grammar.QUOTE)
            parseStringExpr();
        else if (token.type == Token.grammar.L_PARAN)
            parseBoolExpr();
        else if (token.type == Token.grammar.BOOL_VAL)
            parseBoolExpr();
        else
            match(Token.grammar.ID);
        cst.moveUp();
    }

    // Parse boolean expression
    private void parseBoolExpr() {
        cst.addNode("Boolean Expression", Tree.kind.BRANCH);
        System.out.println("parseBoolExpr()");
        if (tokenStream.get(current).type == Token.grammar.L_PARAN) {
            match(Token.grammar.L_PARAN);
            parseExprs();
            if (tokenStream.get(current).type == Token.grammar.EQUAL_OP)
                match(Token.grammar.EQUAL_OP);
            else
                match(Token.grammar.IN_EQUAL_OP);
            parseExprs();
            match(Token.grammar.R_PARAN);
        } else
            match(Token.grammar.BOOL_VAL);

        cst.moveUp();
    }

    // Parse string expression
    private void parseStringExpr() {
        cst.addNode("String Expression", Tree.kind.BRANCH);
        System.out.println("parseStringExpr()");
        match(Token.grammar.QUOTE);
        parseCharList();
        match(Token.grammar.QUOTE);
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
            parseExprs();
        }
        else { } // Java doesn't like this
        cst.moveUp();
    }

    // Parse while statement
    private void parseWhile(){
        cst.addNode("While Statement", Tree.kind.BRANCH);
        System.out.println("parseWhile()");
        matchString("while");
        parseBoolExpr();
        parseBlock();
        cst.moveUp();
    }

    // Parse if statement
    private void parseIf(){
        cst.addNode("If Statement", Tree.kind.BRANCH);
        System.out.println("parseIf()");
        matchString("if");
        parseBoolExpr();
        parseBlock();
        cst.moveUp();
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
            current++;
        } else {
            System.out.println("ERROR: expected " + expected.toString() + " got " + token.type.toString() +
                    " on line " + token.lineNumber + " at " + token.linePosition);
            isErrors = true;
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
    }
}
