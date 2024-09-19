import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;


public class BFInterpreter {
    public static String langCommands = "><+-.,[]";
    private static boolean doProfiling = false;
    private static LoopProfiler prof = null;

    public static void main(String[] args) {
        if (args.length == 1){
            System.out.println("---> Interpreter: Running " + args[0] + "\n");
            run(args[0]);
        }
        else if ((args.length == 2) && (args[0].equals("-p"))) {
            System.out.println("---> Interpreter: Running " + args[1] + "\n");
            doProfiling = true;
            run(args[1]);
        }
        else if ((args.length == 2) && (args[1].equals("-p"))) {
            System.out.println("---> Interpreter: Running " + args[0] + "\n");
            doProfiling = true;
            run(args[0]);
        }
        else {
            System.out.println("Usage: java BFInterpreter <bf src file path> [<-p>]");
        }
    }

    public static void run(String bfSrcFilename) {
        StringBuilder srcCode = new StringBuilder();
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(bfSrcFilename));
            String line;
            while ((line = fileReader.readLine()) != null) {
                srcCode.append(line);
            }
            fileReader.close();
        } catch (IOException e) {
            System.out.println("Cannot read " + bfSrcFilename);
            e.printStackTrace();
        }
        if (srcCode.length() == 0) {
            throw new RuntimeException("BF source code is empty.");
        }
        StringBuilder cleanedCode = clean_code(srcCode);
        HashMap<Integer, Integer> braceMap = build_brace_map(cleanedCode);
        // System.out.println("len of code: " + cleanedCode.length());
        interpreter_loop(cleanedCode, braceMap);
    }

    public static StringBuilder clean_code(StringBuilder code) {
        StringBuilder cleanedCode = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (langCommands.indexOf(c) != -1) {
                cleanedCode.append(c);
            }
        }
        return cleanedCode;
    }

    public static HashMap<Integer, Integer> build_brace_map(StringBuilder code) {
        HashMap<Integer, Integer> map = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < code.length(); i++) {
            char curCmd = code.charAt(i);
            if (curCmd == '[') {
                stack.push(i);
            } else if (curCmd == ']') {
                int matchingBegin = stack.pop();
                map.put(matchingBegin, i);
                map.put(i, matchingBegin);
            }
        }
        return map;
    }

    public static void interpreter_loop(StringBuilder code, HashMap<Integer, Integer> braceMap) {
        List<Integer> tape = new ArrayList<>();
        tape.add(0);
        int progCounter = 0;
        int head = 0;
        if (doProfiling) {
            prof = new LoopProfiler();
        }
        int cMR = 0;
        int cML = 0;
        int cP = 0;
        int cM = 0;
        int cO = 0;
        int cI = 0;
        int cJZ = 0;
        int cJnZ = 0;

        while (true) {
            char curCmd = code.charAt(progCounter);
            switch (curCmd) {
                case '>':
                    cMR++;
                    head = head + 1;
                    if (head == tape.size()){
                        tape.add(0);
                    }
                    break;
                case '<':
                    cML++;
                    if (head > 0) {
                        head = head - 1;
                    } else {
                        head = 0;
                        // throw new RuntimeException("Your BF program tried to move left on the tape from index 0!");
                        System.out.println("Your BF program tried to move left on the tape from index 0! We set the pointer to 0 and move on.");
                    }
                    break;
                case '+':
                    cP++;
                    if (tape.get(head) < 255) {
                        tape.set(head, tape.get(head) + 1);
                    } else {
                        tape.set(head, 0);
                    }
                    break;
                case '-':
                    cM++;
                    if (tape.get(head) > 0){
                        tape.set(head, tape.get(head) - 1);
                    } else {
                        tape.set(head, 255);
                    }
                    break;
                case '.':
                    cO++;
                    System.out.print((char) (int) tape.get(head));
                    break;
                case ',':
                    cI++;
                    try {
                        tape.set(head, System.in.read());
                    } catch (IOException e) {
                        throw new RuntimeException("Input value caused: " + e.getMessage());
                    }
                    break;
                case '[':
                    cJZ++;
                    if (doProfiling && prof != null){
                        int loopEnd = braceMap.get(progCounter);
                        if (is_innermost(code.toString(), progCounter, loopEnd)) {
                            if (is_simple(code.toString(), progCounter, loopEnd)) {
                                prof.startLoop(progCounter, loopEnd, true);
                            } else {
                                prof.startLoop(progCounter, loopEnd, false);
                            }
                        }
                    }
                    if (tape.get(head) == 0) {
                        progCounter = braceMap.get(progCounter);
                    }
                    break;
                case ']':
                    cJnZ++;
                    if (doProfiling && prof != null){
                        int loopStart = braceMap.get(progCounter);
                        if (is_innermost(code.toString(), loopStart, progCounter)) {
                            if (is_simple(code.toString(), loopStart, progCounter)) {
                                prof.incrementLoop(loopStart, progCounter, true);
                            } else {
                                prof.incrementLoop(loopStart, progCounter, false);
                            }
                        }
                    }
                    if (tape.get(head) != 0) {
                        progCounter = braceMap.get(progCounter);
                    }
                    break;
                default:
                    break;
            }
            progCounter = progCounter + 1;
            if (progCounter >= code.length()){
                break;
            }
        }
        System.out.println("\n---> Interpreter: Normal Execution\n");
        if (doProfiling && prof != null) {
            System.out.println("Number of times each instrcution was executed:");
            System.out.println("> : " + cMR);
            System.out.println("< : " + cML);
            System.out.println("+ : " + cP);
            System.out.println("- : " + cM);
            System.out.println(". : " + cO);
            System.out.println(", : " + cI);
            System.out.println("[ : " + cJZ);
            System.out.println("] : " + cJnZ);
            System.out.println();
            prof.printSortedLoops();
        }
    }

    
    // Profiling:
    public static boolean is_innermost(String code, int loopStart, int loopEnd) {
        String loopBody = code.substring(loopStart + 1, loopEnd);
        return !loopBody.contains("[");
    }
    
    public static boolean is_simple(String code, int loopStart, int loopEnd) {
        String loopBody = code.substring(loopStart + 1, loopEnd);
        int inBodyPointer = 0;
        int startCellChange = 0;
        for (char command : loopBody.toCharArray()) {
            switch (command) {
                case '[':
                case ']':
                    System.out.println("You have called is_simple on a loop that is not innermost!");
                    throw new RuntimeException("Not supported!");
                    // break;
                case '>':
                    inBodyPointer++;
                    break;
                case '<':
                    inBodyPointer--;
                    break;
                case '+':
                    // Change the value of the cell that the pointer is currently on
                    // cells.put(pointer, cells.getOrDefault(pointer, 0) + 1);
                    if (inBodyPointer == 0) startCellChange++;
                    break;
                case '-':
                    // Change the value of the cell that the pointer is currently on
                    // cells.put(pointer, cells.getOrDefault(pointer, 0) - 1);
                    if (inBodyPointer == 0) startCellChange--;
                    break;
                case '.':
                case ',':
                    // If there's I/O, this is not a simple loop
                    return false;
            }
        }
        // boolean isSimple = false;
        return (inBodyPointer == 0) && ((startCellChange == 1) || (startCellChange == -1));
    }

}