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

    /**
     * This method prints the token
     * @param input Token from tokenStream
     */
    public static boolean printToken(List<Token> input){
        int errors = 0;
        int warnings = 0;

        for (Token token : input) {
            if (token.type == grammar.ERROR) {
                errors++;
                System.out.println("Error: " + token.attribute + " at " + token.lineNumber + ":" +token.linePosition);
            } else if (token.type == grammar.WARNING){
                warnings++;
                System.out.println("Warning: " + token.attribute + " at " + token.lineNumber + ":" +token.linePosition);
            }
            else
                System.out.println("VERBOSE Lexer - " + token.type + " [ " + token.attribute + " ] found at (" +
                    token.lineNumber + ":" + token.linePosition + ")");
        }

        System.out.println("Program " + compiler.counter + " finished with " + errors +
                " error(s) and " + warnings + " warning(s)");

        // Add blank line after output
        System.out.println();

        return warnings <= 0;

    }

}
