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
    public static String[] exec1 = new String[256];

    // Variable to keep track of current position in executable image
    public static int current = 0;

    public static HashMap<String, String> tempStatic = new HashMap<String, String>();
    public static int temp = 0;

    // Regex matchers
    public static Pattern digit = Pattern.compile("[0-9]");
    public static Pattern boolExact = Pattern.compile("^true$|^false$");

    public CG(Tree ast) {
        this.ast = ast;
    }

    public void codeGen(int programCounter) {
        System.out.println("Beginning Code Gen for program " + programCounter);
        for (int i = 0; i < 256; i++)
            exec1[i] = "00";
        traverse(ast.root);
        setStatic();
        printExec(exec1);
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
            exec1[current] = "A9";
            current++;
            exec1[current] = "00";
            current++;
            // Store the integer in a temp location and add that location to the temp hashmap
            exec1[current] = "8D";
            current++;
            exec1[current] = "T" + temp;
            current++;
            exec1[current] = "XX";
            current++;
            tempStatic.put(child.children.get(1).name, "T" + temp);
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
        exec1[current] = "A9";
        current++;
        Node value = child.children.get(1);
        if (digit.matcher(value.name).find()) {
            exec1[current] = 0 + value.name;
            current++;
        }
        exec1[current] = "8D";
        current++;
        exec1[current] = tempStatic.get(child.children.get(0).name).substring(0, 2);
        current++;
        exec1[current] = "XX";
        current++;

    }

    private static void genPrint(Node child) {
        exec1[current] = "AC";
        current++;
        if (child.children.get(0).name.equals("Add")){
            genAdd(child.children.get(0));
        } else if (child.children.get(0).name.equals("Is Equal") || child.children.get(0).name.equals("Not Equal")){
            genEqual(child.children.get(0));
        } else if (child.children.get(0).token != null && child.children.get(0).token.type == Token.grammar.ID){
            exec1[current] = tempStatic.get(child.children.get(0).name);
            current++;
            exec1[current] = "XX";
            current++;
        }
        exec1[current] = "A2";
        current++;
        exec1[current] = "01";
        current++;
        exec1[current] = "FF";
        current++;
//        exec[current] = 0xAC;
//        current++;
//        exec[current] = toInt(child.children.get(0).name);
//        current += 2;
//        exec[current] = 0xA2;
//        current++;
//        exec[current] = 0x01;
//        current++;
//        exec[current] = 0xFF;
//        current++;
    }

    private static void genIf(Node child) {
    }

    private static void genWhile(Node child) {
    }

    private static void genAdd(Node child) {

    }

    private static void genEqual(Node child) {

    }

    private static void setStatic() {
        for (int i = 0; i < 256; i++){
            if (exec1[i].charAt(0) == 'T')
                replace(exec1[i], Integer.toHexString(current));
        }
    }

    private static void replace(String old, String n) {
        for (int i = 0; i < 256; i++)
            if (exec1[i].equals(old)) {
                exec1[i] = n;
                exec1[i+1] = "00";
            }

    }

    private static void printExec(String[] image){
        int count = 1;
        for (String i: image){
            if (count == 16){
                System.out.printf("%S%s%n", i, " ");
                count = 1;
            } else {
                System.out.printf("%S%s", i, " ");
                count++;
            }
        }
//        for (int i: image){
//            if (count == 16) {
//                System.out.printf("%X%s%n", i, " ");
//                count = 1;
//            } else {
//                System.out.printf("%X%s", i, " ");
//                count++;
//            }
//        }
    }

    private static int toInt(String name) {
        switch(name) {
            case "0" -> {return 0;}
            case "1" -> {return 1;}
            case "2" -> {return 2;}
            case "3" -> {return 3;}
            case "4" -> {return 4;}
            case "5" -> {return 5;}
            case "6" -> {return 6;}
            case "7" -> {return 7;}
            case "8" -> {return 8;}
            case "9" -> {return 9;}
        }
        return 0;
    }


}
