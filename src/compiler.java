import java.io.*;
import java.util.ArrayList;

/**
 * This program is a compiler written for the 6502a processor. The grammar for this compiler can be found
 * here: https://www.labouseur.com/courses/compilers/grammar.pdf
 *
 * @author Chris Pellerito
 * @date 27 January 2022
 */

public class compiler{
    public static void main(String[] args) throws Exception {
        // Use readFile method to read file from stand input into an ArrayList
        ArrayList<String> inputFile = new ArrayList<String>();
        inputFile = readFile(args[0]);

    }

    /**
     * This class reads the input file from standard input and adds it
     * to an ArrayList line by line
     * @param input
     * @return ArrayList containing the file line by line
     * @throws IOException
     */
    public static ArrayList<String> readFile(String input) throws IOException {
        // Read file from standard input
        File inputFile = new File(input);

        // Initialize ArrayList to read the file to
        ArrayList<String> inputString = new ArrayList<String>();

        // Use try to catch errors while reading the input file line by line to the new ArrayList
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            while (br.ready())
                inputString.add(br.readLine());
        }

        return inputString;
    }
}

