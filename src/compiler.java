import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * This program is a compiler written for the 6502a processor. The grammar for this compiler can be found
 * here: https://www.labouseur.com/courses/compilers/grammar.pdf
 *
 * @author Chris Pellerito
 * @date 27 January 2022
 */
public class compiler {
    // Regular expression definitions
    public static Pattern type = Pattern.compile("int|string|boolean");
    public static Pattern character = Pattern.compile("[a-z]");
    public static Pattern bool = Pattern.compile("true|false");
    public static Pattern digit = Pattern.compile("[0|1|2|3|4|5|6|7|8|9]");
    public static Pattern keyword = Pattern.compile("while|if|print");
    public static Pattern all = Pattern.compile("[a-z|0-9]");

    // Make these global too allow for accurate position tracking across multiple methods
    public static int line = 1;
    public static int current = 0;
    public static int prev = 0;

    // Temp token used to keep the order of the tokens correct in special cases
    public static Token tempToken;

    // Program counter
    public static int counter = 0;

    /**
     * main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Use readFile method to read file from standard input into a char array
        char[] inputFile = readFile(args[0]);
        //char[] inputFile = readFile("C:\\Users\\cpell\\Documents\\compiler\\src\\test.txt");

        System.out.println("LEXER");
        lexer(inputFile);

    }

    /**
     * This method runs the lexical analysis of the compiler
     * @param inputFile ArrayList of char arrays that contain the file line by line
     */
    public static List<Token> lexer(char[] inputFile){
        // Create a List to add tokens to when found
        List<Token> tokenStream = new ArrayList<Token>();

        // Input string for easy regex matching
        String inputString = "";

        // Loop through the entire char array
        while (current < inputFile.length) {
            // Make sure character is in the grammar
            if (!all.matcher(String.copyValueOf(inputFile, current,1)).find() && !isBoundry(inputFile[current])) {
                tokenStream.add(new Token(line, current,
                        "unrecognized token \"" + inputFile[current] +"\"", Token.grammar.ERROR));
                current++;
                prev = current;
                continue;
            }
            // Check if the current char is a boundry
            if (isBoundry(inputFile[current])) {
                if (inputFile[current] == '{') {
                    // Add the token to the tokenStream using the line, current position, attribute, and enum type
                    tokenStream.add(new Token(line, current, "{", Token.grammar.L_BRACE));
                    // Increment the pointers to get next character
                    current++;
                    prev = current;
                    // Go to next iteration of the loop
                    continue;

                } else if (inputFile[current] == '}') {
                    tokenStream.add(new Token(line, current, "}", Token.grammar.R_BRACE));
                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == '=') {
                    // Check if next char is an equal sign
                    if (inputFile[current + 1] == '=') {
                        tokenStream.add(new Token(line, current, "==", Token.grammar.EQUAL_OP));
                        current = current + 2;
                        prev = current - 1;
                        continue;

                    } else {
                        tokenStream.add(new Token(line, current, "=", Token.grammar.ASSIGN_OP));
                        current++;
                        prev = current;
                        continue;
                    }

                } else if (inputFile[current] == '!') {
                    // Check if next char is an equal sign
                    if (inputFile[current + 1] == '=') {
                        tokenStream.add(new Token(line, current, "!=", Token.grammar.IN_EQUAL_OP));
                        current = current + 2;
                        prev = current - 1;
                        continue;
                        
                    } else {
                        tokenStream.add(new Token(line, current,
                                "unrecognized token \"" + inputFile[current] + "\"", Token.grammar.ERROR));
                        current++;
                        prev = current;
                        continue;
                    }

                } else if (inputFile[current] == '$') {
                    tokenStream.add(new Token(line, current, "$", Token.grammar.EOP));
                    current++;
                    prev = current;
                    // Print the tokens at the end of the program
                    Token.printToken(tokenStream);
                    // Empty the tokenStream for the next program
                    tokenStream.clear();
                    // Move to next program
                    counter++;
                    continue;

                } else if (inputFile[current] == '/') {
                    if (inputFile[current + 1] == '*') {
                        // Create a tempToken now with the line positioning of the start of the comment
                        tempToken = new Token(line, current,
                                "unclosed comment end of file", Token.grammar.WARNING);
                        current = current + 2;
                        // Find the end of the comment using a try catch to not get an index out of bounds.
                        try {
                            while (inputFile[current] != '*')
                                current++;
                        } catch (Exception e){
                            // Add token from the temp token and add to tokenStream
                            tokenStream.add(tempToken);
                            tempToken = null;
                            continue;
                        }
                        // Make sure the comment gets closed correctly and doesn't throw an index out of bounds.
                        try {
                            if (inputFile[current + 1] == '/') {
                                current = current + 2;
                                continue;
                            // If the next char isn't /, add a warning token
                            } else{
                                tokenStream.add(tempToken);
                                tempToken = null;
                            }
                        // Add a warning token if the file ends and the comment wasn't closed
                        } catch (Exception e) {
                            tokenStream.add(tempToken);
                            tempToken = null;
                        }

                    // If the next character isn't a * throw an error
                    } else {
                        tokenStream.add(new Token(line, current,
                                "unrecognized token \"/\"", Token.grammar.ERROR));

                        current++;
                        prev = current;
                    }

                } else if (inputFile[current] == '\n') {
                    line++;
                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == '+') {
                    tokenStream.add(new Token(line, current, "+", Token.grammar.ADD_OP));
                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == '"') {
                    // Make sure the tempToken is null from previous iterations
                    tempToken = null;
                    // Add the token for the starting quote
                    tokenStream.add(new Token(line, current, "\"", Token.grammar.QUOTE));
                    current++;
                    prev = current;

                    // Use a temp variable to loop through so current can still be used for proper line positioning
                    int temp = current;
                    // Find the next quote, use a try catch to make sure we don't get an index out of bounds.
                    try {
                        while (inputFile[temp] != '"')
                            temp++;
                    } catch (Exception e) {
                        // If the file ends and the quote isn't terminated create a warning token and add it to
                        // temp token to keep the positioning of the tokens correct
                        tempToken = new Token(line, current - 1,
                                "unclosed quote at end of file", Token.grammar.WARNING);
                    }

                    // Take everything in the quote and add it to a new char array for readibility
                    char[] tempArray = String.copyValueOf(inputFile, prev, temp - prev).toCharArray();
                    // Run stringLexer method using the char array just created
                    tokenStream.addAll(stringLexer(tempArray));

                    // If the temp token was filled add it to the end of the tokenStream and empty it
                    if (tempToken != null) {
                        tokenStream.add(tempToken);
                        tempToken = null;
                    // If it wasn't that means the quote was closed so add the token
                    } else
                        tokenStream.add(new Token(line, current, "\"", Token.grammar.QUOTE));

                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == '(') {
                    tokenStream.add(new Token(line, current, "(", Token.grammar.L_PARAN));
                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == ')'){
                    tokenStream.add(new Token(line, current, ")", Token.grammar.R_PARAN));
                    current++;
                    prev = current;
                    continue;

                // If the current character is a white space
                } else {
                    if (current + 1 < inputFile.length - 1) {
                        prev++;
                        current++;
                        continue;
                    }
                    else
                        inputString = String.copyValueOf(inputFile, prev, (current - prev));
                }
            // If the current character isn't a boundry
            } else
                inputString = String.copyValueOf(inputFile, prev, (current - prev) + 1);

            // int, string, and boolean matcher
            if (type.matcher(inputString).find()) {
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.TYPE));
                current++;
                prev = current;

             // Boolean value matcher (true or false)
            } else if (bool.matcher(inputString).find()) {
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.BOOL_VAL));
                current++;
                prev = current;

            // Keyword matcher (print, while, if)
            } else if (keyword.matcher(inputString).find()){
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.KEYWORD));
                current++;
                prev = current;

            // Character matcher (length check added because I couldn't get regex to work for just one character)
            } else if (character.matcher(inputString).find() && inputString.length() == 1) {
                // Wrap in a try catch so it doesn't throw an index out of bounds
                try {
                    if (isBoundry(inputFile[current + 1])) {
                        // Since next char is a boundry this must be an ID, so create token
                        tokenStream.add(new Token(line, prev, String.valueOf(inputFile[current]),
                                Token.grammar.ID));

                        current++;
                        prev = current;
                    // If it's not a boundry then increment and move on
                    } else
                        current++;

                } catch (Exception e) {
                    tokenStream.add(new Token(line, prev, inputString, Token.grammar.ID));
                    current++;
                }

            // Number matcher
            } else if (digit.matcher(inputString).find()) {
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.DIGIT));
                current++;
                prev = current;

            } else
                current++;

        }

        // Make sure tokenStream is empty and the last token in there is an EOP. If not throw a warning and add the
        // EOP assuming the user meant to put one as this is the end of the file
        if (!tokenStream.isEmpty() && tokenStream.get(tokenStream.size() - 1).type  != Token.grammar.EOP) {
                tokenStream.add(new Token(line, current,
                        "file ended with no end of program, EOP inserted", Token.grammar.WARNING));
                tokenStream.add(new Token(line, current, "$", Token.grammar.EOP));
                Token.printToken(tokenStream);
        }

        return tokenStream;

    }

    /**
     * This method checks if a character is an "operator"
     * Operators are =, !, {}, $, " , +, /, (), newline characters and white space
     * @param input char from currentLine
     * @return boolean
     */
    public static boolean isBoundry(char input){
        char[] operators = {'=', '!', '\n', ' ', '{', '}', '$', '/', '"', '+', '(', ')'};
        for (char op: operators)
            if (input == op)
                return true;

        return false;
    }

    /**
     * This method is a convience method. It is used to lex the contents of a string.
     * Makes the code cleaner and easier to read so the Lexer method doesn't need to worry about
     * checking if every char it finds is a part of a string.
     * @param input char array of all characters in between the quotes
     * @return
     */
    public static List<Token> stringLexer(char[] input){
        // temp array of tokens to return and add to the end of tokenStream
        List<Token> tempToken = new ArrayList<Token>();

        for (int i = 0; i < input.length; i++){
            // String representation of current char so regex can read it
            String inputString = String.copyValueOf(input, i, 1);

            // Check if current is a character or white space, if not add an error token
            // Also checks for new line character to keep line number tracking consistent
            if (character.matcher(inputString).find())
                tempToken.add(new Token(line, current, inputString, Token.grammar.CHAR));

            else if (input[i] == ' ')
                tempToken.add(new Token(line, current, inputString, Token.grammar.SPACE));

            else if (input[i] == '\n') {
                current++;
                line++;
            }
            else
                tempToken.add(new Token(line, current,
                        "unexpected character \"" + inputString + "\"", Token.grammar.ERROR));

            // Increment current to keep the line position consistent
            current++;
        }

        return tempToken;
    }


    /**
     * This method reads the input file from standard input and adds it
     * to a string that will be returned as an array of chars for easy parsing.
     * A \n is added to the end of every line to make counting lines easy.
     * @param input text file from command line
     * @return char array containing the file
     * @throws IOException
     */
    public static char[] readFile(String input) throws IOException {
        // Read file from standard input
        File inputFile = new File(input);

        // Initialize String to read the file to.
        String inputString = "";

        // Use try to catch errors while reading the input file and adding it to a string
        // Append a \n to the end of each line to help with line number counting.
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            while (br.ready())
                inputString = inputString + br.readLine() + '\n';
        }

        // Return the string as an array of characters for easy parsing
        return inputString.toCharArray();
    }

}
