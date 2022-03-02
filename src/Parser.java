import java.util.List;

/**
 * This class contains the recursive decent parser for the compiler
 */
public class Parser {
    public List<Token> tokenStream;

    // pointer for accessing tokenStream
    public static int current = 0;

    // Arrays of ENUM types to help with the parse
    public static Token.grammar[] statements = {Token.grammar.KEYWORD,
            Token.grammar.TYPE, Token.grammar.ID, Token.grammar.L_BRACE};
    public static Token.grammar[] expressions = {Token.grammar.DIGIT, Token.grammar.QUOTE,
            Token.grammar.L_PARAN, Token.grammar.ID};


    public Parser(List<Token> tokenStream){
        this.tokenStream = tokenStream;
    }

    // Main parse method
    public void parse(){
        // Reset current to 0
        current = 0;
        System.out.println("parse()");
        parseBlock();
        match(Token.grammar.EOP);
    }

    // Parse block
    private void parseBlock(){
        System.out.println("parseBlock()");
        match(Token.grammar.L_BRACE);
        parseStateList();
        match(Token.grammar.R_BRACE);
    }

    // Parse statementlist
    private void parseStateList(){
        System.out.println("parseStateList()");
        while (tokenStream.get(current).contains(statements))
            parseState();
    }

    // Parse statement
    private void parseState(){
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
    }

    // Parse variable declaration
    private void parseVarDec(){
        System.out.println("parseVarDec()");
        match(Token.grammar.TYPE);
        match(Token.grammar.ID);
    }

    // Parse assignment statement
    private void parseAssign(){
        System.out.println("parseAssign()");
        match(Token.grammar.ID);
        match(Token.grammar.ASSIGN_OP);
        parseExprs();
    }

    // Parse print statement
    private void parsePrint(){
        System.out.println("parsePrint()");
        matchString("print");
        match(Token.grammar.L_PARAN);
        if (tokenStream.get(current).contains(expressions))
            parseExprs();

        match(Token.grammar.R_PARAN);
    }

    // Parse expression
    private void parseExprs() {
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
    }

    // Parse boolean expression
    private void parseBoolExpr() {
        System.out.println("parseBoolExpr()");
        if (tokenStream.get(current).type == Token.grammar.BOOL_VAL)
            match(Token.grammar.BOOL_VAL);
        else {
            match(Token.grammar.L_PARAN);
            parseExprs();
            if (tokenStream.get(current).type == Token.grammar.EQUAL_OP)
                match(Token.grammar.EQUAL_OP);
            else
                match(Token.grammar.IN_EQUAL_OP);
            parseExprs();
            match(Token.grammar.R_PARAN);
        }
    }

    // Parse string expression
    private void parseStringExpr() {
        System.out.println("parseStringExpr()");
        match(Token.grammar.QUOTE);
        parseCharList();
        match(Token.grammar.QUOTE);
    }

    // Parse character list
    private void parseCharList() {
        System.out.println("parseCharList()");
        // Add current token to a variable for easier access
        Token token = tokenStream.get(current);
        if (token.type == Token.grammar.CHAR){
            match(Token.grammar.CHAR);
            parseCharList();
        } else if (token.type == Token.grammar.SPACE){
            match(Token.grammar.SPACE);
            parseCharList();
        } else {} // Java doesn't like this
    }

    // Parse int expression
    private void parseIntExpr() {
        match(Token.grammar.DIGIT);
        if (tokenStream.get(current).type == Token.grammar.ADD_OP){
            match(Token.grammar.ADD_OP);
            parseExprs();
        }
        else {} // Java doesn't like this
    }

    // Parse while statement
    private void parseWhile(){
        System.out.println("parseWhile()");
        matchString("while");
        parseBoolExpr();
        parseBlock();
    }

    // Parse if statement
    private void parseIf(){
        System.out.println("parseIf()");
        matchString("if");
        parseBoolExpr();
        parseBlock();
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
            current++;
        } else
            System.out.println("ERROR: expected " + expected.toString() + " got " + token.type.toString() +
                    " on line " + token.lineNumber + " at " + token.linePosition);
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
            current++;
        } else
            System.out.println("ERROR: expected " + expected + " got " + token.attribute +
                    " on line " + token.lineNumber + " at " + token.linePosition);
    }
}
