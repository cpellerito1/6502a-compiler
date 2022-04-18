import java.util.HashMap;

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

    public CG(Tree ast) {
        this.ast = ast;
    }

    private void codeGen(int programCounter) {
        traverse(ast.root);
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

    }

    private static void genAssign(Node child) {
    }

    private static void genPrint(Node child) {
    }

    private static void genIf(Node child) {
    }

    private static void genWhile(Node child) {
    }


}
