/**
 * This class is for the tokens of lexer. It will store their attribute, line number,
 * and any other needed information
 *
 * @author Chris Pellerito
 */

public class Token {
    public enum grammar{
        TYPE
    }

    public int lineNumber;

    public int linePosition;

    public String attribute;

    public grammar type;

    public Token(int lineNumber, int linePosition, String attribute, grammar type){
        this.lineNumber = lineNumber;
        this.linePosition = linePosition;
        this.attribute = attribute;
        this.type = type;
    }

    public void printToken(Token input){

    }

}
