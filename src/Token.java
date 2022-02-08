/**
 * This class is for the tokens of lexer. It will store their attribute, line number,
 * and any other needed information
 *
 * @author Chris Pellerito
 */

public class Token {
    public enum grammar{
        TYPE, L_BRACE, R_BRACE, ID, CHAR, BOOL_VAL, BOOL_OP, EQUAL_OP, ASSIGN_OP,
        IN_EQUAL_OP, ERROR, EOP, DIGIT
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

    /**
     * This method prints the token
     * @param input Token from tokenStream
     */
    public static void printToken(Token input){
       System.out.printf("%s%s%s%s%s%s%s%d%s%d%s%n","VERBOSE", "Lexer -", input.type, "[", input.attribute, "]",
                "found at (", input.lineNumber,":",input.linePosition,")");
        //System.out.println("VERBOSE  Lexer -  " + input.type + "  [  " + input.attribute + "  ]  found at (" +
          //      input.lineNumber + ":" + input.linePosition + ")");

    }

}
