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

public class compiler{
    public enum grammar{
        TYPES
    }

    public static void main(String[] args) throws Exception {
        // Use readFile method to read file from stand input into an ArrayList
        ArrayList<char[]> inputFile = new ArrayList<char[]>();
        inputFile = readFile(args[0]);

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
        List<Token> tokenStream = null;

        // Regular expression definitions
        Pattern typeInt = Pattern.compile("int");
        Pattern typeString = Pattern.compile("string");
        Pattern typeBool = Pattern.compile("boolean");

        // Loop through the array list by each line
        for (int line = 0; line < inputFile.size(); line++){
            // Add current line to new array for easy access
            char[] currentLine = inputFile.get(line);

            // Initialize 2 pointers for looping through each line
            int prev = 0;
            int current = 1;

            // Create a while loop to loop through the line and end when current pointer is > the last index
            while (current <= currentLine.length){
                // use String.copyValueOf to go character by character and match against reg expressions
                Matcher intMatch = typeInt.matcher(String.copyValueOf(currentLine, prev, current-prev));
                Matcher stringMatch = typeString.matcher(String.copyValueOf(currentLine, prev, current-prev));
                Matcher boolMatch = typeBool.matcher(String.copyValueOf(currentLine, prev, current-prev));

                if (intMatch.find()){
                    // Add token to tokenStream
                    tokenStream.add(new Token(line, prev, "int", Token.grammar.TYPE));
                    System.out.printf("DEBUG", "Lexer - ","I_TYPE", "[ int ]", "Found at " );

                    prev = current + 1;
                    current = current + 2;
                } else if (stringMatch.find()){
                    tokenStream.add(new Token(line, prev, "string", Token.grammar.TYPE));
                }



                current++;
            }

        }

        return tokenStream;

    }
}

