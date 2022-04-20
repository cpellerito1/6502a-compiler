import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This class will generate 6502a op codes based on the AST that was produced and type checked in Semantic Analysis
 *
 * @author Chris Pellerito
 */
public class CG extends Tree {
    // AST
    Tree ast;

    // This variable represents the executable image
    public static int[] exec = new int[256];

    // Variable to keep track of current position in executable image
    public static int current = 0;

    public static HashMap<String, Integer> tempStatic = new HashMap<String, Integer>();
    public static int temp = 1;

    // Regex matchers
    public static Pattern digit = Pattern.compile("[0-9]");
    public static Pattern boolExact = Pattern.compile("^true$|^false$");

    public CG(Tree ast) {
        this.ast = ast;
    }

    public void codeGen(int programCounter) {
        traverse(ast.root);
        printExec(exec);
    }
    private static void traverse(Node node) {
        for (Node child: node.children){
            switch (child.name) {
                case "Block" -> traverse(child);
                case "Variable Declaration" -> genVarDecl(child);
                case "Assignment Statement" -> genAssign(child);
                case "Print Statement" -> genPrint(child);
                case "If Statement" -> genIf(child);
                case "While Statement" -> genWhile(child);
            }
        }
    }

    private static void genVarDecl(Node child) {
        if (child.children.get(0).name.equals("int")){
            // Load the accumulator with 00 for int init
            exec[current] = 0xA9;
            current++;
            exec[current] = 0x00;
            current++;
            // Store the integer in a temp location and add that location to the temp hashmap
            exec[current] = 0X8D;
            current++;
            exec[current] = temp;
            current += 2;
            tempStatic.put(child.children.get(1).name, temp);
            temp++;
        }
//        else if (child.children.get(0).name.equals("string")){
//            exec[current] = 0xA9;
//            current++;
//            // exec[current] = string pointer
//            current++;
        //}

    }

    private static void genAssign(Node child) {
        exec[current] = 0xA9;
        current++;
        Node value = child.children.get(1);
        if (digit.matcher(value.name).find()) {
            exec[current] = toInt(child.children.get(1));
            current++;
        }
        exec[current] = 0x8D;
        current++;
        exec[current] = tempStatic.get(child.children.get(0).name);
        current += 2;

    }

    private static void genPrint(Node child) {
    }

    private static void genIf(Node child) {
    }

    private static void genWhile(Node child) {
    }

    private static void printExec(int[] image){
        int count = 1;
        for (int i: image){
            if (count == 16) {
                System.out.printf("%X%s%n", i, " ");
                count = 1;
            } else {
                System.out.printf("%X%s", i, " ");
                count++;
            }
        }
    }

    private static int toInt(Node child) {
        switch(child.name) {
            case "0" -> {return 0x00;}
            case "1" -> {return 0x01;}
            case "2" -> {return 0x02;}
            case "3" -> {return 0x03;}
            case "4" -> {return 0x04;}
            case "5" -> {return 0x05;}
            case "6" -> {return 0x06;}
            case "7" -> {return 0x07;}
            case "8" -> {return 0x08;}
            case "9" -> {return 0x09;}
        }
        return 0;
    }


}
