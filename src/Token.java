import java.util.List;

/**
 * This class is for the tokens of lexer. It will store their attribute, line number,
 * and any other needed information
 *
 * @author Chris Pellerito
 */

public class Token {
    public enum grammar{
        TYPE, L_BRACE, R_BRACE, ID, CHAR, BOOL_VAL, ADD_OP, EQUAL_OP, ASSIGN_OP,
        IN_EQUAL_OP, ERROR, EOP, DIGIT, WARNING, SPACE, QUOTE, R_PARAN, L_PARAN,
        KEYWORD
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

}
