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
public class Compiler {
    public static char[] inputFile;

    /**
     * main method
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Use readFile method to read file from standard input into a char array
        inputFile = readFile(args[0]);


        // Run the Lexer
        Lexer.lexer(inputFile);

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

        // Make sure the input file is a text file
        String name = inputFile.getName();
        int dot = name.lastIndexOf('.');
        name = name.substring(dot);
        if (!name.equals("txt")) {
            System.out.println("Error: input file must be a text file (.txt)");
            System.exit(-1);
        }

        // Initialize String to read the file to.
        String inputString = "";

        // Use try to catch errors while reading the input file and adding it to a string
        // Append a \n to the end of each line to help with line number counting.
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            while (br.ready())
                inputString = inputString + br.readLine() + '\n';
        }

        // Remove the last \n from the string and return the string as an array of characters for easy parsing
        // This helps with error handling
        return String.copyValueOf(inputString.toCharArray(), 0, inputString.length() - 1).toCharArray();
    }

}
