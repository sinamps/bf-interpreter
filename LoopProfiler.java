import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.HashMap;


public class LoopProfiler {
    private Map<String, Integer> simpleLoops;
    private Map<String, Integer> nonSimpleLoops;

    public LoopProfiler() {
        this.simpleLoops = new HashMap<>();
        this.nonSimpleLoops = new HashMap<>();
    }

    private String loopKey(int loopStart, int loopEnd) {
        return loopStart + "-" + loopEnd;
    }

    public void startLoop(int loopStart, int loopEnd, boolean isSimple) {
        String key = loopKey(loopStart, loopEnd);
        if (isSimple) {
            simpleLoops.putIfAbsent(key, 0);
        } else {
            nonSimpleLoops.putIfAbsent(key, 0);
        }
    }

    public void incrementLoop(int loopStart, int loopEnd, boolean isSimple) {
        String key = loopKey(loopStart, loopEnd);
        if (isSimple) {
            simpleLoops.put(key, simpleLoops.getOrDefault(key, 0) + 1);
        } else {
            nonSimpleLoops.put(key, nonSimpleLoops.getOrDefault(key, 0) + 1);
        }
    }

    public void printSortedLoops() {
        // Sort simple loops by number of executions in descending order
        System.out.println("\nSimple Loop Profiling Report:");
        simpleLoops.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> 
                    System.out.println("Simple Loop [" + entry.getKey() + "] executed " + entry.getValue() + " times")
                );

        // Sort non-simple loops by number of executions in descending order
        System.out.println("\nNon-Simple Loop Profiling Report:");
        nonSimpleLoops.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> 
                    System.out.println("Non-Simple Loop [" + entry.getKey() + "] executed " + entry.getValue() + " times")
                );
    }
}
