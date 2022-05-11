import java.net.IDN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * This class will generate 6502a op codes based on the AST that was produced and type checked in Semantic Analysis
 *
 * @author Chris Pellerito
 */
public class CG extends Tree {
    // AST
    Tree ast;
    // Symbol Table
    static Tree symbol;

    // This variable represents the executable image
    public static String[] exec = new String[256];

    // Variable to keep track of current position in executable image
    public static int current = 0;
    // Variable to keep track of the current location of the heap. Start at 254 to allow for null termination of string
    public static int heap = 254;
    // Variable to keep track of scope
    public static int scope = 0;

    public static HashMap<String, String> tempStatic = new HashMap<>();
    public static int temp = 0;

    public static HashMap<String, Integer> jump = new HashMap<>();
    public static int j = 0;

    public static ArrayList<Integer> bool = new ArrayList<>();
    public static ArrayList<Integer> boolPrint = new ArrayList<>();

    // This is used to initialize strings since an initialized string should be null. FE will always be null.
    // If FE isn't null that means that the program is too big and will fail since this is where the heap starts.
    public static String nullptr = "FF";


    // Regex matchers
    public static Pattern digit = Pattern.compile("[0-9]");
    public static Pattern boolExact = Pattern.compile("^true$|^false$");
    public static Pattern character = Pattern.compile("[a-z]");

    public CG(Tree ast, Tree symbolTable) {
        this.ast = ast;
        symbol = symbolTable;
    }

    public void codeGen(int programCounter) {
        System.out.println("Beginning Code Gen for program " + programCounter);
        for (int i = 0; i < 256; i++)
            exec[i] = "00";
        symbol.current = symbol.root;
        traverse(ast.root);
        // Add the break code
        current++;
        if (setStatic()) {
            if (heap - current >= 11)
                setBool();
            printExec(exec);
        }
        else
            System.out.println("Error: program size exceeded");
    }
    private static void traverse(Node node) {
        for (Node child: node.children){
            switch (child.name) {
                case "Block" -> {
                    scope++;
                    setSymbol(String.valueOf(scope));
                    traverse(child);
                    symbol.moveUp();
                    scope--;
                }
                case "Variable Declaration" -> genVarDecl(child);
                case "Assignment Statement" -> genAssign(child);
                case "Print Statement" -> genPrint(child);
                case "If Statement" -> genIf(child);
                case "While Statement" -> genWhile(child);
            }
        }
    }

    private static void genVarDecl(Node child) {
        Node type = child.children.get(0);
        Node var = child.children.get(1);
        // Load the accumulator
        exec[current] = "A9";
        current++;
        if (type.name.equals("int") || type.name.equals("boolean")){
            // With 00 if it is an int or boolean
            exec[current] = "00";
            current++;
        } else if (type.name.equals("string")) {
            exec[current] = nullptr;
            current++;
        }

        // Store the var in a temp location and add that location to the temp hashmap
        exec[current] = "8D";
        current++;
        exec[current] = "T" + temp;
        current++;
        exec[current] = "XX";
        current++;
        tempStatic.put(var.name + ":" + scope, "T" + temp);
        temp++;

    }

    private static void genAssign(Node child) {
        Node var = child.children.get(0);
        Node value = child.children.get(1);
        if (value.name.equals("Add")){
            genAdd(value);
            exec[current] = "8D";
            current++;
            exec[current] = findTemp(var, scope);
            current++;
            exec[current] = "XX";
            current++;
        } else if (value.name.equals("Is Equal") || value.name.equals("Not Equal")){
            genEqual(value);
        } else if (value.token != null && value.token.type == Token.grammar.ID){
            exec[current] = "AD";
            current++;
            exec[current] = findTemp(value, scope);
            current++;
            exec[current] = "XX";
            current++;
        } else {
            exec[current] = "A9";
            current++;
            if (getType(var).equals("int")) {
                exec[current] = "0" + value.name;
                current++;
            } else if (getType(var).equals("boolean")) {
                if (value.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                current++;
            } else {
                exec[current] = setString(value);
                current++;
            }
        }
        exec[current] = "8D";
        current++;
        exec[current] = findTemp(var, scope);
        current++;
        exec[current] = "XX";
        current++;
    }

    private static void genPrint(Node child) {
        if (child.children.get(0).name.equals("Add")){
            exec[current] = "AC";
            current++;
            genAdd(child.children.get(0));
        } else if (child.children.get(0).name.equals("Is Equal") || child.children.get(0).name.equals("Not Equal")){
            exec[current] = "AC";
            current++;
            genEqual(child.children.get(0));
        } else if (child.children.get(0).token != null) {
            if (child.children.get(0).token.type == Token.grammar.ID){
                exec[current] = "AC";
                current++;
                exec[current] = findTemp(child.children.get(0), scope);
                current++;
                exec[current] = "XX";
                current++;
                exec[current] = "A2";
                current++;
                if (getType(child.children.get(0)).equals("string"))
                    exec[current] = "02";
                else {
                    exec[current] = "01";
                    if (getType(child.children.get(0)).equals("boolean"))
                        boolPrint.add(current);
                }
                current++;
            } else if (child.children.get(0).token.type == Token.grammar.DIGIT){
                exec[current] = "A0";
                current++;
                exec[current] = "0" + child.children.get(0).name;
                current++;
                exec[current] = "A2";
                current++;
                exec[current] = "01";
                current++;
            } else if (child.children.get(0).token.type == Token.grammar.BOOL_VAL){
                if (child.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                current++;
                exec[current] = "A2";
                current++;
                exec[current] = "01";
                boolPrint.add(current);
                current++;
            } else if (child.children.get(0).token.type == Token.grammar.STRING){
                exec[current] = "A0";
                current++;
                exec[current] = setString(child.children.get(0));
                current++;
                exec[current] = "A2";
                current++;
                exec[current] = "02";
                current++;
            }
        }

        exec[current] = "FF";
        current++;
    }

    private static void genIf(Node child) {
        System.out.println("if");
        Node value = child.children.get(0);
        if (value.name.equals("Is Equal"))
            genEqual(value);
        else if (value.name.equals("Not Equal"))
            genNotEqual(value);

        exec[current] = "D0";
        current++;
        exec[current] = "J" + j;
        jump.put(exec[current], current);
        current++;
        j++;
        traverse(child.children.get(child.children.size() - 1));
        j--;
        int temp = jump.get("J" + j);
        int dist = (current - temp) - 1;
        if (dist > 9)
            exec[temp] = Integer.toHexString(dist);
        else
            exec[temp] = "0" + dist;
    }

    private static void genWhile(Node child) {
    }

    private static void genAdd(Node child) {
        Node num = child.children.get(0);
        if (num.name.equals("Add"))
            genAdd(num);
        else {
            Node val = child.children.get(1);
            if (val.token != null && val.token.type == Token.grammar.ID){
                exec[current] = "A9";
                current++;
                exec[current] = 0 + num.name;
                current++;
                exec[current] = "6D";
                current++;
                exec[current] = findTemp(val, scope);
                current++;
                exec[current] = "XX";
                current++;
            } else {
                exec[current] = "A9";
                current++;
                exec[current] = 0 + val.name;
                current++;
                exec[current] = "8D";
                current++;
                tempStatic.put("T:" + scope, "T" + temp);
                exec[current] = "T" + temp;
                current++;
                exec[current] = "XX";
                current++;
                exec[current] = "A9";
                current++;
                exec[current] = 0 + num.name;
                current++;
                exec[current] = "6D";
                current++;
                exec[current] = tempStatic.get("T:" + scope);
                current++;
                exec[current] = "XX";
                current++;
                temp++;
            }
        }

    }

    private static void genEqual(Node child) {
        Node first = child.children.get(0);
        Node second = child.children.get(1);

        if (first.name.equals("Add"))
            genAdd(first);
        else if (first.name.equals("Is Equal"))
            genEqual(first);
        else if (first.name.equals("Not Equal"))
            genNotEqual(first);
        else if (first.token != null) {
            if (first.token.type == Token.grammar.ID) {
                exec[current] = "AE";
                current++;
                exec[current] = findTemp(first, scope);
                current++;
                exec[current] = "XX";
            } else {
                exec[current] = "A2";
                current++;
                if (first.token.type == Token.grammar.DIGIT)
                    exec[current] = "0" + first.name;
                else if (first.token.type == Token.grammar.BOOL_VAL)
                    exec[current] = first.name;
                else if (first.token.type == Token.grammar.STRING)
                    exec[current] = setString(first);
            }
            current++;
        }

        if (second.name.equals("Add"))
            genAdd(second);
        else if (second.name.equals("Is Equal"))
            genEqual(second);
        else if (second.name.equals("Not Equal"))
            genNotEqual(second);
        else if (second.token != null) {
            if (second.token.type == Token.grammar.ID) {
                exec[current] = "EC";
                current++;
                exec[current] = findTemp(second, scope);
                current++;
                exec[current] = "XX";
                current++;
            } else {
                exec[current] = "A9";
                current++;
                if (second.token.type == Token.grammar.DIGIT)
                    exec[current] = "0" + second.name;
                else if (second.token.type == Token.grammar.BOOL_VAL)
                    exec[current] = second.name;
                else if (second.token.type == Token.grammar.STRING)
                    exec[current] = setString(second);
                current++;
                exec[current] = "8D";
                current++;
                tempStatic.put("T:" + scope, "T" + temp);
                exec[current] = "T" + temp;
                current++;
                exec[current] = "XX";
                current++;
                exec[current] = "EC";
                current++;
                exec[current] = tempStatic.get("T:" + scope);
                temp++;
                current++;
                exec[current] = "XX";
                current++;
            }
        }
    }

    private static void genNotEqual(Node child) {
        Node first = child.children.get(0);
        Node second = child.children.get(1);

        if (first.name.equals("Add")){
            genAdd(first);
        } else if (first.token != null){
            if (first.token.type == Token.grammar.DIGIT){
                exec[current] = "A9";
                current++;
                exec[current] = "0" + first.name;
                current++;
            } else if (first.token.type == Token.grammar.BOOL_VAL){
                exec[current] = "A9";
                current++;
                if (first.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                current++;
            } else if (first.token.type == Token.grammar.STRING){
                exec[current] = "A9";
                current++;
                exec[current] = setString(first);
            } else if (first.token.type == Token.grammar.ID){
                exec[current] = "AD";
                current++;
                exec[current] = findTemp(first, scope);
                current++;
                exec[current] = "XX";
                current++;
            }
        }
        exec[current] = "8D";
        current++;
        tempStatic.put(first.name, "T" + temp);
        exec[current] = "T" + temp;
        current++;
        temp++;
        exec[current] = "XX";
        current++;

        // copy compare to value
        if (second.name.equals("Add"))
            genAdd(second);
        else if (second.token != null){
            if (second.token.type == Token.grammar.ID){
                exec[current] = "AD";
                current++;
                exec[current] = findTemp(second, scope);
                current++;
                exec[current] = "XX";
                current++;
            } else if (second.token.type == Token.grammar.DIGIT){
                exec[current] = "A9";
                current++;
                exec[current] = "0" + first.name;
                current++;
            } else if (second.token.type == Token.grammar.BOOL_VAL){
                exec[current] = "A9";
                current++;
                if (second.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                current++;
            } else if (second.token.type == Token.grammar.STRING){
                exec[current] = "A9";
                current++;
                exec[current] = setString(second);
            }
        }
        exec[current] = "8D";
        current++;
        tempStatic.put(second.name, "T" + temp);
        exec[current] = "T" + temp;
        current++;
        temp++;
        exec[current] = "XX";
        current++;

        // Compare the two newly created temp variables
        exec[current] = "AE";
        current++;
        exec[current] = tempStatic.get(first.name);
        current++;
        exec[current] = "XX";
        current++;
        exec[current] = "EC";
        current++;
        exec[current] =tempStatic.get(second.name);
        current++;
        exec[current] = "XX";
        current++;

        // Load the accumulator with 0
        exec[current] = "A9";
        current++;
        exec[current] = "00";
        current++;
        // Branch 2 bytes if t1 != t2
        exec[current] = "D0";
        current++;
        exec[current] = "02";
        current++;
        // If t1 == t2, load accumulator with 1
        exec[current] = "A9";
        current++;
        exec[current] = "01";
        current++;
        // Load x reg with 0
        exec[current] = "A2";
        current++;
        exec[current] = "00";
        current++;
        // Store accumulator in second temp
        exec[current] = "8D";
        current++;
        exec[current] = tempStatic.get(second.name);
        current++;
        exec[current] = "XX";
        current++;
        // Compare second temp and x reg
        exec[current] = "EC";
        current++;
        exec[current] = tempStatic.get(second.name);
        current++;
        exec[current] = "XX";
        current++;
    }

    private static String setString(Node var) {
        for (int c = var.name.length()-1; c > -1; c--){
            exec[heap] = Integer.toHexString(var.name.charAt(c));
            heap--;
        }
        // Decrement to terminate the next string that will be added and start the next
        heap--;
        // Return pointer to the start of the string
        return Integer.toHexString(heap+2);
    }

    /**
     * This method sets the memory address of the static variables. It looks through the executable image and looks
     * for any index that starts with a T. When it finds one it then iterates through the rest of the array, starting
     * at the index it was found, and replaces any other indexes that contains the same string with the hex
     * representation of the current index.
     */
    private static boolean setStatic() {
        for (int i = 0; i < 256; i++) {
            if (exec[i].charAt(0) == 'T') {
                // Set the temp string equal to a variable to avoid overwriting
                String old = exec[i];
                for (int k = i; k < 255; k++) {
                    if (exec[k].equals(old)) {
                        exec[k] = Integer.toHexString(current);
                        exec[k + 1] = "00";
                    }
                }
                // Move to next address in the stack
                current++;
                // Check if the stack has collided with the heap
                if (current >= heap)
                    return false;

            }
        }
        return true;
    }

    /**
     * This method dynamically sets boolean values. Since printing the string version of true and false takes up space
     * in the limited size heap. The default for bool values will be 00-false and 01-true but if there is room in the
     * heap, the values will instead be set to the string version.
     */
    private static void setBool() {
        String t = "true";
        String f = "false";
        for (int i = t.length()-1; i > -1; i--){
            exec[heap] = Integer.toHexString(t.charAt(i));
            heap--;
        }
        String truePtr = Integer.toHexString(heap + 1);
        System.out.println("true: " + truePtr);
        heap--;

        for (int i = f.length()-1; i > -1; i--){
            exec[heap] = Integer.toHexString(f.charAt(i));
            heap--;
        }
        String falsePtr = Integer.toHexString(heap + 1);
        System.out.println("False: " + falsePtr);

        bool.forEach((Integer) -> {
            if (exec[Integer].equals("01"))
                exec[Integer] = truePtr;
            else
                exec[Integer] = falsePtr;

            exec[Integer - 1] = "A9";
        });

        boolPrint.forEach((Integer) -> exec[Integer] = "02");
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
    }

    private static String findTemp(Node node, int s) {
        while (!tempStatic.containsKey(node.name + ":" + s))
            s--;

        return tempStatic.get(node.name + ":" + s);
    }

    private static String getType(Node var) {
        Node cur = symbol.current;
        if (cur.st.containsKey(var.name))
            return cur.st.get(var.name).type;
        else {
            while (symbol.current.parent != null) {
                if (symbol.current.parent.st.containsKey(var.name))
                    return symbol.current.parent.st.get(var.name).type;
                else
                    symbol.moveUp();
            }
        }
        // Reset current symbol
        symbol.current = cur;
        // This return should never happen since SA already checked the AST. The variable must exist in the symbol table
        return "Error";
    }

    private static void setSymbol(String scope) {
        if (!symbol.current.name.equals(scope)){
            for (Node child: symbol.current.children) {
                if (child.name.equals(scope))
                    symbol.current = child;
            }
        }
    }

}
