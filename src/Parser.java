import java.util.List;

/**
 * This class contains the recursive decent parser for the compiler
 */
public class Parser {
    public List<Token> tokenStream;

    // pointer for accessing tokenStream
    public static int current = 0;


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
//        if is epsilon
//        else{
//            parseState();
//            parseStateList();
//        }
    }

    public void parseState(){

    }

    public void match(Token.grammar expected){
        if (tokenStream.get(current).type == expected){
            // Consume token
            current++;
        }


    }
}
