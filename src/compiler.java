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
    public static Pattern commentStart = Pattern.compile("/\\*");
    public static Pattern commentEnd = Pattern.compile("\\*/");
    public static Pattern character = Pattern.compile("[a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|s|t|u|v|x|y|z]");
    public static Pattern bool = Pattern.compile("true|false");
    public static Pattern digit = Pattern.compile("[0|1|2|3|4|5|6|7|8|9]");

    // Global variable to help with handling multi line comments
    public static int lineCheck = 0;



    /**
     * main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Use readFile method to read file from stand input into an ArrayList
        //ArrayList<char[]> inputFile = new ArrayList<char[]>();
        
        //char[] inputFile = readFile(args[0]);
        char[] inputFile = readFile("C:\\Users\\cpell\\Documents\\compiler\\src\\test.txt");

        for (char i: inputFile)
            System.out.println(i);

        System.out.println("LEXER");
        lexer(inputFile);

    }

    /**
     * This class reads the input file from standard input and adds it
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

    /**
     * This method runs the lexical analysis of the compiler
     * @param inputFile ArrayList of char arrays that contain the file line by line
     */
    public static List<Token> lexer(char[] inputFile){
        // Create a List to add tokens to when found
        List<Token> tokenStream = new ArrayList<Token>();

        // Initialize input string for easy regex matching
        String inputString = "";

        // Initialize 2 pointers for looping through the file
        int prev = 0;
        int current = 0;
            
        // Line variable to keep track of line numbers
        int line = 1;

        // Create a while loop to loop through the line and end when current pointer is > the last index
        while (current < inputFile.length) {
            // Check if the current char is a boundry
            if (isBoundry(inputFile[current])) {
                if (inputFile[current] == '{') {
                    System.out.println("worked {");
                    tokenStream.add(new Token(line, current, "{", Token.grammar.L_BRACE));
                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == '}') {
                    System.out.println("worked }");
                    tokenStream.add(new Token(line, current, "}", Token.grammar.R_BRACE));
                    current++;
                    prev = current;
                    continue;

                } else if (inputFile[current] == '=') {
                    // Check if next char is an equal sign
                    if (inputFile[current + 1] == '=') {
                        // Add token to tokenStream
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
                        // Add token to tokenStream
                        tokenStream.add(new Token(line, current, "!=", Token.grammar.IN_EQUAL_OP));
                        current = current + 2;
                        prev = current - 1;
                        continue;
                    } else {
                        tokenStream.add(new Token(line, current, "!", Token.grammar.ERROR));
                        current++;
                        prev = current;
                        continue;
                    }

                } else if (inputFile[current] == '$') {
                    tokenStream.add(new Token(line, current, "$", Token.grammar.EOP));
                    current++;
                    prev = current;
                    continue;
                    //break;

                } else if (inputFile[current] == '/') {
                    if (inputFile[current + 1] == '*') {
                        current = current + 2;
                        while (inputFile[current] != '*')
                            current++;

                        if (inputFile[current + 1] == '/')
                            current = current + 2; continue;

                    } else {
                        tokenStream.add(new Token(line, current, "/", Token.grammar.ERROR));
                        current++;
                        prev = current;
                    }

                } else if (inputFile[current] == '\n') {
                    line++;
                    current++;
                    prev = current;
                    continue;

                } else {
                    if (current + 1 < inputFile.length - 1) {
                        prev++;
                        current++;
                        continue;
                    } else
                        inputString = String.copyValueOf(inputFile, prev, (current - prev));
                }
            } else
                inputString = String.copyValueOf(inputFile, prev, (current - prev) + 1);

            System.out.println("Input: " + inputString.toString());
            if (type.matcher(inputString).find()) {
                // Add token to tokenStream
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.TYPE));
                current++;
                prev = current;

            } else if (bool.matcher(inputString).find()) {
                System.out.println("worked true");
                // Add token
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.BOOL_VAL));
                current++;
                prev = current;

            } else if (character.matcher(inputString).find()) {
                try {
                    if (isBoundry(inputFile[current + 1])) {
                        System.out.println("worked char");
                        // Since next char is a boundry this must be an ID, so create token
                        tokenStream.add(new Token(line, prev, String.valueOf(inputFile[current]),
                                Token.grammar.ID));

                        current++;
                        prev = current;
                    } else
                        current++;
                } catch (Exception e) {
                    tokenStream.add(new Token(line, prev, inputString, Token.grammar.ID));
                    current++;
                }

            } else if (digit.matcher(inputString).find()) {
                System.out.println("worked dig");
                tokenStream.add(new Token(line, prev, inputString, Token.grammar.DIGIT));
                current++;
                prev = current;

            } else
                current++;

        }

        
        for (Token output: tokenStream)
            Token.printToken(output);
        return tokenStream;

    }

    /**
     * This method checks if a character is an "operator"
     * Operators are =, !, {}, $, newline characters and white space
     * @param input char from currentLine
     * @return boolean
     */
    public static boolean isBoundry(char input){
        char[] operators = {'=', '!', '\n', ' ', '{', '}', '$', '/'};
        for (char op: operators)
            if (input == op)
                return true;

        return false;
    }

}
