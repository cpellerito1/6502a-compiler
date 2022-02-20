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


    public Parser(List<Token> tokenStream){
        this.tokenStream = tokenStream;
    }

    public void parse(){
        parseBlock();
        match(Token.grammar.EOP);
    }

    public void parseBlock(){
        match(Token.grammar.L_BRACE);
        parseStateList();
        match(Token.grammar.R_BRACE);
    }

    public void parseStateList(){
        if (contains(statements))
            parseState();
        else
            // Java doesn't like an empty else, so I put arbitrary code so it would stop throwing an error
            current = current;
    }

    public void parseState(){
        // add current token to a variable for easy access
        Token token = tokenStream.get(current);
        if (token.attribute.equals("print"))
            parsePrint();
        else if (token.attribute.equals("while"))
            parseWhile();
        else if (token.attribute.equals("if"))
            parseIf();

    }

    private void parsePrint(){
        match(Token.grammar.L_PARAN);
        matchString("print");
        match(Token.grammar.R_PARAN);
    }

    public void parseWhile(){
        matchString("while");

    }

    public void match(Token.grammar expected){
        if (tokenStream.get(current).type == expected){
            // Consume token
            current++;
        } else
            System.out.println("error: ");
    }

    public void matchString(String expected){
        if (tokenStream.get(current).attribute.equals(expected)) {
            // Consume token
            current++;
        } else
            System.out.println("error: ");
    }

    public boolean contains(Token.grammar[] input){
        for (Token.grammar type: input)
            if (type == tokenStream.get(current).type)
                return true;

        return false;
    }
}
