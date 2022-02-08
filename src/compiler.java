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
    public static Pattern character = Pattern.compile("^a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|s|t|u|v|x|y|z*");
    public static Pattern bool = Pattern.compile("true|false");
    public static Pattern digit = Pattern.compile("0|1|2|3|4|5|6|7|8|9");

    // Global variable to help with handling multi line comments
    public static int lineCheck = 0;



    /**
     * main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Use readFile method to read file from stand input into an ArrayList
        ArrayList<char[]> inputFile = new ArrayList<char[]>();
        //inputFile = readFile(args[0]);
        inputFile = readFile("C:\\Users\\cpell\\Documents\\compiler\\src\\test.txt");

        for (int i = 0; i < inputFile.size(); i++)
            System.out.println(inputFile.get(i));

        System.out.println("LEXER");
        lexer(inputFile);

    }

    /**
     * This class reads the input file from standard input and adds it
     * to an ArrayList line by line as an array of characters
     * @param input
     * @return ArrayList containing the file line by line
     * @throws IOException
     */
    public static ArrayList<char[]> readFile(String input) throws IOException {
        // Read file from standard input
        File inputFile = new File(input);

        // Initialize ArrayList to read the file to
        ArrayList<char[]> inputString = new ArrayList<char[]>();

        // Use try to catch errors while reading the input file line by line to the new ArrayList
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            while (br.ready())
                inputString.add(br.readLine().toCharArray());
        }

        return inputString;
    }

    /**
     * This method runs the lexical analysis of the compiler
     * @param inputFile ArrayList of char arrays that contain the file line by line
     */
    public static List<Token> lexer(ArrayList<char[]> inputFile){
        // Create a List to add tokens to when found
        List<Token> tokenStream = new ArrayList<Token>();

        // Loop through the array list by each line
        for (int line = 0; line < inputFile.size(); line++) {
            // Add current line to new array for easy access
            char[] currentLine = inputFile.get(line);
            String inputString = String.valueOf(currentLine);
            System.out.println(currentLine);

            // Initialize 2 pointers for looping through each line
            int prev = 0;
            int current = 0;

            // Create a while loop to loop through the line and end when current pointer is > the last index
            while (current < currentLine.length) {
                // Check if the current char is a boundry
                if (isBoundry(currentLine[current])){
                    if (currentLine[current] == '{') {
                        System.out.println("worked {");
                        tokenStream.add(new Token(line + 1, current, "{", Token.grammar.L_BRACE));
                        current++;
                        prev = current;
                        continue;

                    } else if (currentLine[current] == '}') {
                        System.out.println("worked }");
                        tokenStream.add(new Token(line + 1, current, "}", Token.grammar.R_BRACE));
                        current++;
                        prev = current;
                        continue;

                    } else if (currentLine[current] == '='){
                        // Check if next char is an equal sign
                        if (currentLine[current + 1] == '='){
                            // Add token to tokenStream
                            tokenStream.add(new Token(line + 1, current, "==", Token.grammar.EQUAL_OP));
                            current = current + 2;
                            prev = current - 1;
                        }
                        else {
                            tokenStream.add(new Token(line + 1, current, "=", Token.grammar.ASSIGN_OP));
                            current++;
                            prev = current;
                        }

                    } else if (currentLine[current] == '!'){
                        // Check if next char is an equal sign
                        if (currentLine[current + 1] == '='){
                            // Add token to tokenStream
                            tokenStream.add(new Token(line + 1, current, "!=", Token.grammar.IN_EQUAL_OP));
                            current = current + 2;
                            prev = current - 1;
                        }
                        else {
                            tokenStream.add(new Token(line + 1, current, "!", Token.grammar.ERROR));
                            current++;
                            prev = current;
                        }

                    } else if (currentLine[current] == '$'){
                        tokenStream.add(new Token(line + 1, current, "$", Token.grammar.EOP));
                        break;

                    } else if (currentLine[current] == '/'){
                        if (currentLine[current + 1] == '*'){
                            int[] lineInfo = commentHandler(inputFile, line, current + 1);
                            if (lineInfo[0] == line){
                                current = lineInfo[1];
                                prev = current;
                            } else{
                                line = lineInfo[0] - 1;
                                current = lineInfo[1];
                                prev = current;
                            }
                        }
                        else {
                            tokenStream.add(new Token(line + 1, current, "/", Token.grammar.ERROR));
                            current++;
                            prev = current;
                        }
                    }


                    else {
                        if (current + 1 < currentLine.length-1){
                            prev++;
                            current++;
                            //inputString = String.copyValueOf(currentLine, prev, (current-prev)+1);
                            continue;
                        }
                        else
                            inputString = String.copyValueOf(currentLine, prev, (current-prev));
                    }
                }
                else
                    inputString = String.copyValueOf(currentLine, prev, (current-prev)+1);

                System.out.println("Input: " + inputString.toString());
                if (type.matcher(inputString).find()){
                    // Add token to tokenStream
                    tokenStream.add(new Token(line + 1, prev, inputString, Token.grammar.TYPE));
                    current++;
                    prev = current;

                } else if (bool.matcher(inputString).find()) {
                    System.out.println("worked true");
                    // Add token
                    tokenStream.add(new Token(line + 1, prev, inputString, Token.grammar.BOOL_VAL));
                    current++;
                    prev = current;

                } else if (character.matcher(inputString).find()) {
                    if (isBoundry(currentLine[current+1])){
                        System.out.println("worked char");
                        // Since next char is a boundry this must be an ID, so create token
                        tokenStream.add(new Token(line + 1, prev, String.valueOf(currentLine[current]),
                                Token.grammar.ID));

                        current++;
                        prev = current;
                    }
                    else
                        current++;

                } else if (digit.matcher(inputString).find()){
                    System.out.println("worked dig");
                    tokenStream.add(new Token(line + 1, prev, inputString, Token.grammar.DIGIT));
                    current++;
                    prev = current;

                } else
                    current++;


            }
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

    /**
     * This method handles comments. This method is needed becuase of how the lexer method handles the lines.
     * This method allows for multi line comments
     * @param input ArrayList containing input file
     * @param lineNumber line number the comment started on
     * @param index index of the * of the comment
     * @return int array containing the line number and index of the / of the comment
     */
    public static int[] commentHandler(ArrayList<char[]> input, int lineNumber, int index){
        // Make sure the correct line number and index were passed to the method
        assert(input.get(lineNumber)[index] == '*');

        index++;
        while (input.get(lineNumber)[index] != '*'){
            index++;
            if (index == input.get(lineNumber).length){
                lineNumber++;
                index = 0;
            }
        }
        // Add line number and index to an array for output so the lexer method knows where the comment ended
        int[] lineInfo = new int[2];
        lineInfo[0] = lineNumber;
        // Add 1 to get the index of the / not the *
        lineInfo[1] = index + 1;

        return lineInfo;
    }

}

