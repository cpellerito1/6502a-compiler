/**
 * This class is for the tokens of lexer. It will store their description, line number,
 * and any other needed information
 *
 * @author Chris Pellerito
 */

public class Token {

    public int lineNumber;

    public int linePosition;

    public String description;

    public void token(int lineNumber, int linePosition, String description){
        this.lineNumber = lineNumber;
        this.linePosition = linePosition;
        this.description = description;
    }

}
