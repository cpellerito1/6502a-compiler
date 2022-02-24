import java.util.List;

/**
 * This class contains the recursive decent parser for the compiler
 */
public class Parser {
    public List<Token> tokenStream;

    // pointer for accessing tokenStream
    public static int current = 0;

    public static Token.grammar[] statements = {Token.grammar.KEYWORD,
            Token.grammar.TYPE, Token.grammar.ID, Token.grammar.L_BRACE};
    public static Token.grammar[] expressions = {Token.grammar.DIGIT, Token.grammar.QUOTE,
            Token.grammar.L_PARAN, Token.grammar.ID};


    public Parser(List<Token> tokenStream){
        this.tokenStream = tokenStream;
    }

    public void parse(){
        // Reset current to 0
        current = 0;
        System.out.println("parse()");
        parseBlock();
        match(Token.grammar.EOP);
    }

    private void parseBlock(){
        System.out.println("parseBlock()");
        match(Token.grammar.L_BRACE);
        parseStateList();
        match(Token.grammar.R_BRACE);
    }

    private void parseStateList(){
        System.out.println("parseStateList()");
        while (contains(statements))
            parseState();

    }

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

    private void parseVarDec(){
        System.out.println("parseVarDec()");
        match(Token.grammar.TYPE);
        match(Token.grammar.ID);
        //if (tokenStream.get(current).type == Token.grammar.ASSIGN_OP)
          //  parseAssign();
    }

    private void parseAssign(){
        System.out.println("parseAssign()");
        match(Token.grammar.ID);
        match(Token.grammar.ASSIGN_OP);
        parseExprs();
    }

    private void parsePrint(){
        System.out.println("parsePrint()");
        matchString("print");
        match(Token.grammar.L_PARAN);
        if (contains(expressions)){
            parseExprs();
        }
        match(Token.grammar.R_PARAN);
    }

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

    private void parseStringExpr() {
        System.out.println("parseStringExpr()");
        match(Token.grammar.QUOTE);
        parseCharList();
        match(Token.grammar.QUOTE);
    }

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

    private void parseIntExpr() {
        match(Token.grammar.DIGIT);
        if (tokenStream.get(current).type == Token.grammar.ADD_OP){
            match(Token.grammar.ADD_OP);
            parseExprs();
        }
        // Java doesn't like this
        else {}
    }

    private void parseWhile(){
        System.out.println("parseWhile()");
        matchString("while");
        parseBoolExpr();
        parseBlock();

    }

    private void parseIf(){
        System.out.println("parseIf()");
        matchString("if");
        parseBoolExpr();
        parseBlock();
    }

    private void match(Token.grammar expected){
        if (tokenStream.get(current).type == expected){
            // Consume token
            current++;
        } else
            System.out.println("ERROR: expected " + expected.toString() +
                    " got " + tokenStream.get(current).type.toString());
    }

    private void matchString(String expected){
        if (tokenStream.get(current).attribute.equals(expected)) {
            // Consume token
            current++;
        } else
            System.out.println("ERROR: expected " + expected +
                    " got " + tokenStream.get(current).attribute);
    }

    private boolean contains(Token.grammar[] input){
        for (Token.grammar type: input)
            if (type == tokenStream.get(current).type)
                return true;

        return false;
    }
}
