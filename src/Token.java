/**
 * This class is for the tokens of lexer. It will store their attribute, line number,
 * and any other needed information
 *
 * @author Chris Pellerito
 */

public class Token {

    public int lineNumber;

    public int linePosition;

    public String attribute;

    public void token(int lineNumber, int linePosition, String attribute){
        this.lineNumber = lineNumber;
        this.linePosition = linePosition;
        this.attribute = attribute;
    }

}
