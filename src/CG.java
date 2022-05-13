import java.util.ArrayList;
import java.util.HashMap;

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

    // Hashmap to keep track of static variables
    public static HashMap<String, String> tempStatic = new HashMap<>();
    public static int temp = 0;

    // Hashmap to keep track of jumps
    public static HashMap<String, Integer> jump = new HashMap<>();
    public static int j = 0;

    // Arraylists to keep track of the indexes of where bool values and the print values are too
    public static ArrayList<Integer> bool;
    public static ArrayList<Integer> boolLiteral;
    public static ArrayList<Integer> boolPrint;

    // This is used to initialize strings since an initialized string should be null. FF will always 00 since the
    // program needs to end with a 00
    public static String nullptr = "FF";
    // Hashmap to keep track of strings. This will allow the compiler to assign like strings the same pointer
    public static HashMap<String, String> stringTable;


    public CG(Tree ast, Tree symbolTable) {
        this.ast = ast;
        symbol = symbolTable;
        // Initialize these variables in the constructor to make sure they are reset to starting values when a new
        // program starts
        current = 0;
        heap = 254;
        scope = 0;
        temp = 0;
        j = 0;
        bool = new ArrayList<>();
        boolLiteral = new ArrayList<>();
        boolPrint = new ArrayList<>();
        stringTable = new HashMap<>();
    }

    public void codeGen(int programCounter) {
        System.out.println();
        System.out.println("Beginning Code Gen for program " + programCounter);
        for (int i = 0; i < 256; i++)
            exec[i] = "00";
        symbol.current = symbol.root;
        traverse(ast.root);
        // Add the break code
        incrementCurrent();
        if (setStatic()) {
            if (heap - current >= 11)
                setBool();
            printExec(exec);
        }
        else
            System.out.println("Error: program size exceeded, stack collided with heap");
    }

    /**
     * This method traverses the ast and calls acom
     * method based on the type of node
     * @param node input node
     */
    private static void traverse(Node node) {
        for (Node child: node.children){
            switch (child.name) {
                case "Block" -> {
                    // Increment the scope and set the new symbol table
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

    // Gen VarDecl
    private static void genVarDecl(Node child) {
        Node type = child.children.get(0);
        Node var = child.children.get(1);
        // Load the accumulator
        exec[current] = "A9";
        incrementCurrent();
        if (type.name.equals("int") || type.name.equals("boolean")) {
            // With 00 if it is an int or boolean
            exec[current] = "00";
            incrementCurrent();
        } else if (type.name.equals("string")) {
            exec[current] = nullptr;
            incrementCurrent();
        }
        // Store the var in a temp location and add that location to the temp hashmap
        exec[current] = "8D";
        incrementCurrent();
        exec[current] = "T" + temp;
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
        tempStatic.put(var.name + ":" + scope, "T" + temp);
        temp++;
    }

    // Gen Assignment Statement
    private static void genAssign(Node child) {
        Node var = child.children.get(0);
        Node value = child.children.get(1);
        if (value.name.equals("Add")){
            genAdd(value);
            exec[current] = "8D";
            incrementCurrent();
            exec[current] = findTemp(var, scope);
            incrementCurrent();
            exec[current] = "XX";
            incrementCurrent();
        } else if (value.name.equals("Is Equal") || value.name.equals("Not Equal")){
            genEqual(value);
        } else if (value.token != null && value.token.type == Token.grammar.ID){
            exec[current] = "AD";
            incrementCurrent();
            exec[current] = findTemp(value, scope);
            incrementCurrent();
            exec[current] = "XX";
            incrementCurrent();
        } else {
            exec[current] = "A9";
            incrementCurrent();
            if (getType(var).equals("int")) {
                exec[current] = "0" + value.name;
                incrementCurrent();
            } else if (getType(var).equals("boolean")) {
                if (value.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                incrementCurrent();
            } else {
                exec[current] = setString(value);
                incrementCurrent();
            }
        }
        exec[current] = "8D";
        incrementCurrent();
        exec[current] = findTemp(var, scope);
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
    }

    // Gen Print Statement
    private static void genPrint(Node child) {
        if (child.children.get(0).name.equals("Add")){
            exec[current] = "AC";
            incrementCurrent();
            genAdd(child.children.get(0));
        } else if (child.children.get(0).name.equals("Is Equal") || child.children.get(0).name.equals("Not Equal")){
            exec[current] = "AC";
            incrementCurrent();
            genEqual(child.children.get(0));
        } else if (child.children.get(0).token != null) {
            if (child.children.get(0).token.type == Token.grammar.ID){
                exec[current] = "AC";
                incrementCurrent();
                exec[current] = findTemp(child.children.get(0), scope);
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
                exec[current] = "A2";
                incrementCurrent();
                if (getType(child.children.get(0)).equals("string"))
                    exec[current] = "02";
                else {
                    exec[current] = "01";
                    if (getType(child.children.get(0)).equals("boolean"))
                        boolPrint.add(current);
                }
                incrementCurrent();
            } else if (child.children.get(0).token.type == Token.grammar.DIGIT){
                exec[current] = "A0";
                incrementCurrent();
                exec[current] = "0" + child.children.get(0).name;
                incrementCurrent();
                exec[current] = "A2";
                incrementCurrent();
                exec[current] = "01";
                incrementCurrent();
            } else if (child.children.get(0).token.type == Token.grammar.BOOL_VAL){
                exec[current] = "A0";
                incrementCurrent();
                if (child.children.get(0).name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                boolLiteral.add(current);
                incrementCurrent();
                exec[current] = "A2";
                incrementCurrent();
                exec[current] = "01";
                boolPrint.add(current);
                incrementCurrent();
            } else if (child.children.get(0).token.type == Token.grammar.STRING){
                exec[current] = "A0";
                incrementCurrent();
                exec[current] = setString(child.children.get(0));
                incrementCurrent();
                exec[current] = "A2";
                incrementCurrent();
                exec[current] = "02";
                incrementCurrent();
            }
        }

        exec[current] = "FF";
        incrementCurrent();
    }

    // Gen If statement
    private static void genIf(Node child) {
        Node value = child.children.get(0);
        if (value.name.equals("Is Equal"))
            genEqual(value);
        else if (value.name.equals("Not Equal"))
            genNotEqual(value);

        exec[current] = "D0";
        incrementCurrent();
        exec[current] = "J" + j;
        jump.put(exec[current], current);
        incrementCurrent();
        j++;
        traverse(child.children.get(child.children.size() - 1));
        j--;
        int temp = jump.get("J" + j);
        int dist = (current - temp) - 1;
        if (dist > 9) {
            if (dist < 16)
                exec[temp] = "0" + Integer.toHexString(dist);
            else
                exec[temp] = Integer.toHexString(dist);
        }
        else
            exec[temp] = "0" + dist;
    }

    // Gen While Statement
    private static void genWhile(Node child) {
        int start = current;
        if (child.children.get(0).name.equals("Is Equal"))
            genEqual(child.children.get(0));
        else if (child.children.get(0).name.equals("Not Equal"))
            genNotEqual(child.children.get(0));
        exec[current] = "D0";
        incrementCurrent();
        exec[current] = "J" + j;
        jump.put(exec[current], current);
        incrementCurrent();
        j++;
        traverse(child.children.get(child.children.size() - 1));
        j--;
        // Create the unconditional branch by storing 01 in one of the temp vars and comparing it to 00 in the x reg
        exec[current] = "A9";
        incrementCurrent();
        exec[current] = "01";
        incrementCurrent();
        exec[current] = "8D";
        incrementCurrent();
        tempStatic.put("T:" + temp, "T" + temp);
        exec[current] = tempStatic.get("T:" + temp);
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
        exec[current] = "A2";
        incrementCurrent();
        exec[current] = "00";
        incrementCurrent();
        exec[current] = "EC";
        incrementCurrent();
        exec[current] = tempStatic.get("T:" + temp);
        temp++;
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
        exec[current] = "D0";
        incrementCurrent();
        // Jump to the start of the loop
        exec[current] = Integer.toHexString(255 - (current - start));
        incrementCurrent();
        int temp = jump.get("J" + j);
        int dist = (current - temp) - 1;
        if (dist > 9) {
            if (dist < 16)
                exec[temp] = "0" + Integer.toHexString(dist);
            else
                exec[temp] = Integer.toHexString(dist);
        }
        else
            exec[temp] = "0" + dist;
    }

    // Gen Add
    private static void genAdd(Node child){
        Node num = child.children.get(0);
        if (num.name.equals("Add"))
            genAdd(num);
        else {
            Node val = child.children.get(1);
            if (val.token != null && val.token.type == Token.grammar.ID) {
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = 0 + num.name;
                incrementCurrent();
                exec[current] = "6D";
                incrementCurrent();
                exec[current] = findTemp(val, scope);
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
            } else {
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = 0 + val.name;
                incrementCurrent();
                exec[current] = "8D";
                incrementCurrent();
                tempStatic.put("T:" + scope, "T" + temp);
                exec[current] = "T" + temp;
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = 0 + num.name;
                incrementCurrent();
                exec[current] = "6D";
                incrementCurrent();
                exec[current] = tempStatic.get("T:" + scope);
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
                temp++;
            }
        }
    }

    // Gen Equal
    private static void genEqual(Node child) {
        Node first = child.children.get(0);
        Node second = child.children.get(1);

        if (first.name.equals("Add")) {
            genAdd(first);
            exec[current] = "8D";
            incrementCurrent();
            exec[current] = "T" + temp;
            tempStatic.put("T" + temp, "T" + temp);
            incrementCurrent();
            exec[current] = "XX";
            incrementCurrent();
            exec[current] = "AE";
            incrementCurrent();
            exec[current] = tempStatic.get("T" + temp);
            temp++;
            incrementCurrent();
            exec[current] = "00";
            incrementCurrent();
        }
        else if (first.name.equals("Is Equal"))
            genEqual(first);
        else if (first.name.equals("Not Equal"))
            genNotEqual(first);
        else if (first.token != null) {
            if (first.token.type == Token.grammar.ID) {
                exec[current] = "AE";
                incrementCurrent();
                exec[current] = findTemp(first, scope);
                incrementCurrent();
                exec[current] = "XX";
            } else {
                exec[current] = "A2";
                incrementCurrent();
                if (first.token.type == Token.grammar.DIGIT)
                    exec[current] = "0" + first.name;
                else if (second.token.type == Token.grammar.BOOL_VAL) {
                    if (second.name.equals("true"))
                        exec[current] = "01";
                    else
                        exec[current] = "02";
                }
                else if (first.token.type == Token.grammar.STRING)
                    exec[current] = setString(first);
            }
            incrementCurrent();
        }

        if (second.name.equals("Add")) {
            genAdd(second);
            exec[current] = "8D";
            incrementCurrent();
            exec[current] = "T" + temp;
            tempStatic.put("T" + temp, "T" + temp);
            incrementCurrent();
            exec[current] = "XX";
            incrementCurrent();
            exec[current] = "EC";
            incrementCurrent();
            exec[current] = tempStatic.get("T" + temp);
            temp++;
            incrementCurrent();
            exec[current] = "00";
            incrementCurrent();
        }
        else if (second.name.equals("Is Equal"))
            genEqual(second);
        else if (second.name.equals("Not Equal"))
            genNotEqual(second);
        else if (second.token != null) {
            if (second.token.type == Token.grammar.ID) {
                exec[current] = "EC";
                incrementCurrent();
                exec[current] = findTemp(second, scope);
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
            } else {
                exec[current] = "A9";
                incrementCurrent();
                if (second.token.type == Token.grammar.DIGIT)
                    exec[current] = "0" + second.name;
                else if (second.token.type == Token.grammar.BOOL_VAL) {
                    if (second.name.equals("true"))
                        exec[current] = "01";
                    else
                        exec[current] = "02";
                }
                else if (second.token.type == Token.grammar.STRING)
                    exec[current] = setString(second);
                incrementCurrent();
                exec[current] = "8D";
                incrementCurrent();
                tempStatic.put("T:" + scope, "T" + temp);
                exec[current] = "T" + temp;
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
                exec[current] = "EC";
                incrementCurrent();
                exec[current] = tempStatic.get("T:" + scope);
                temp++;
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
            }
        }
    }

    // Gen not equal
    private static void genNotEqual(Node child) {
        Node first = child.children.get(0);
        Node second = child.children.get(1);

        if (first.name.equals("Add")){
            genAdd(first);
        } else if (first.token != null){
            if (first.token.type == Token.grammar.DIGIT){
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = "0" + first.name;
                incrementCurrent();
            } else if (first.token.type == Token.grammar.BOOL_VAL){
                exec[current] = "A9";
                incrementCurrent();
                if (first.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                incrementCurrent();
            } else if (first.token.type == Token.grammar.STRING){
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = setString(first);
                incrementCurrent();
            } else if (first.token.type == Token.grammar.ID){
                exec[current] = "AD";
                incrementCurrent();
                exec[current] = findTemp(first, scope);
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
            }
        }
        exec[current] = "8D";
        incrementCurrent();
        tempStatic.put(first.name, "T" + temp);
        exec[current] = "T" + temp;
        incrementCurrent();
        temp++;
        exec[current] = "XX";
        incrementCurrent();

        // copy compare to value
        if (second.name.equals("Add"))
            genAdd(second);
        else if (second.token != null){
            if (second.token.type == Token.grammar.ID){
                exec[current] = "AD";
                incrementCurrent();
                exec[current] = findTemp(second, scope);
                incrementCurrent();
                exec[current] = "XX";
                incrementCurrent();
            } else if (second.token.type == Token.grammar.DIGIT){
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = "0" + second.name;
                incrementCurrent();
            } else if (second.token.type == Token.grammar.BOOL_VAL){
                exec[current] = "A9";
                incrementCurrent();
                if (second.name.equals("true"))
                    exec[current] = "01";
                else
                    exec[current] = "00";
                bool.add(current);
                incrementCurrent();
            } else if (second.token.type == Token.grammar.STRING){
                exec[current] = "A9";
                incrementCurrent();
                exec[current] = setString(second);
                incrementCurrent();
            }
        }
        exec[current] = "8D";
        incrementCurrent();
        tempStatic.put(second.name, "T" + temp);
        exec[current] = "T" + temp;
        incrementCurrent();
        temp++;
        exec[current] = "XX";
        incrementCurrent();

        // Compare the two newly created temp variables
        exec[current] = "AE";
        incrementCurrent();
        exec[current] = tempStatic.get(first.name);
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
        exec[current] = "EC";
        incrementCurrent();
        exec[current] =tempStatic.get(second.name);
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();

        // Load the accumulator with 0
        exec[current] = "A9";
        incrementCurrent();
        exec[current] = "00";
        incrementCurrent();
        // Branch 2 bytes if t1 != t2
        exec[current] = "D0";
        incrementCurrent();
        exec[current] = "02";
        incrementCurrent();
        // If t1 == t2, load accumulator with 1
        exec[current] = "A9";
        incrementCurrent();
        exec[current] = "01";
        incrementCurrent();
        // Load x reg with 0
        exec[current] = "A2";
        incrementCurrent();
        exec[current] = "00";
        incrementCurrent();
        // Store accumulator in second temp
        exec[current] = "8D";
        incrementCurrent();
        exec[current] = tempStatic.get(second.name);
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
        // Compare second temp and x reg
        exec[current] = "EC";
        incrementCurrent();
        exec[current] = tempStatic.get(second.name);
        incrementCurrent();
        exec[current] = "XX";
        incrementCurrent();
    }

    /**
     * This method sets the string in the heap. It first checks the string table to see if the same string has already
     * been added to the heap. If it has it just returns the pointer to that string. If not it adds it to the heap
     * in reverse order and returns the pointer to the start of the string.
     * @param var the node of the string
     * @return the value of the pointer in the heap
     */
    private static String setString(Node var) {
        if (stringTable.containsKey(var.name))
            return stringTable.get(var.name);

        for (int c = var.name.length()-1; c > -1; c--){
            exec[heap] = Integer.toHexString(var.name.charAt(c));
            heap--;
        }
        // Decrement to terminate the next string that will be added and start the next
        heap--;
        stringTable.put(var.name, Integer.toHexString(heap + 2));
        // Return pointer to the start of the string
        return Integer.toHexString(heap + 2);
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
                incrementCurrent();
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
        heap--;

        for (int i = f.length()-1; i > -1; i--){
            exec[heap] = Integer.toHexString(f.charAt(i));
            heap--;
        }
        String falsePtr = Integer.toHexString(heap + 1);



        boolLiteral.forEach((Integer) -> {
            if (exec[Integer].equals("01"))
                exec[Integer] = truePtr;
            else
                exec[Integer] = falsePtr;
        });

        boolPrint.forEach((Integer) -> exec[Integer] = "02");
    }

    /**
     * This method prints the executable image in a 16x16 grid
     * @param image The array of opcodes
     */
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

    /**
     * This method finds a temp variable in the static table. It then returns the value from the hashmap.
     * @param node input node
     * @param s current scope
     * @return the value from the temp hashmap
     */
    private static String findTemp(Node node, int s) {
        while (!tempStatic.containsKey(node.name + ":" + s))
            s--;

        return tempStatic.get(node.name + ":" + s);
    }

    /**
     * Traverse the symbol table to find the type of variables
     * @param var variable
     * @return type of variable
     */
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

    /**
     * This method sets the current symbol table. Since the nodes of the symbol table are named after their scope, this
     * looks for the node with the name of the current scope and makes that node the current node.
     * @param scope current scope
     */
    private static void setSymbol(String scope) {
        if (!symbol.current.name.equals(scope)){
            for (Node child: symbol.current.children) {
                if (child.name.equals(scope))
                    symbol.current = child;
            }
        }
    }

    /**
     * This method increments current. Using this method helps with the transition between programs and does error
     * checking. If current becomes larger than 255, print out the error message and then re-call lexer.
     */
    private static void incrementCurrent(){
        current += 1;
        if (current > 255) {
            System.out.println("Error: size limit exceeded");
            // If this isn't the last program in the text file, continue
            if (Compiler.inputFile.length > Lexer.current)
                Lexer.lexer(Compiler.inputFile);

            // After this terminate the program
            System.exit(-1);
        }
    }
}
