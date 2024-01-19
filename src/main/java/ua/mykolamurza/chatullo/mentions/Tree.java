package ua.mykolamurza.chatullo.mentions;

import javafx.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tree {

    private Tree() {}

    // When no player IGN starts with said char (corresponding to index), it's null
    public static Branch[][] base = new Branch[123][];

    public static void construct(List<String> strings) {
        Branch[][] newbase = new Branch[123][];
        for (String string: strings) {
            if (string.isEmpty())
                continue;

            char c = string.charAt(0);
            if (newbase[c] == null) {
                String left = string.substring(1);
                Branch current = new Branch((char)0,null);
                newbase[c] = new Branch[]{current};
                while (!left.isEmpty()) {
                    current.c = left.charAt(0);
                    if (left.length() < 2)
                        break;

                    Branch next = new Branch((char)0, null);
                    current.sub = new Branch[]{next};
                    current = next;
                    left = left.substring(1);
                }
                current.isEnd = true;
            } else {
                if (string.length() == 1)
                    continue;

                Branch[] trunk = newbase[c];
                String left = string.substring(1);
                char t = left.charAt(0);
                Branch previous = null;
                // Find first branch stemming from first char
                for (Branch branch: trunk) {
                    if (branch.c == t) {
                        previous = branch;
                        left = left.substring(1);
                        t = left.charAt(0);
                        break;
                    }
                }

                // Branch found, follow it
                if (previous != null) {
                    while (!left.isEmpty()) {
                        if (previous.sub == null)
                            break;

                        boolean found = false;
                        for (Branch branch: previous.sub) {
                            if (branch.c == t) {
                                found = true;
                                previous = branch;
                                left = left.substring(1);
                                t = left.charAt(0);
                                break;
                            }
                        }
                        if (!found)
                            break;
                    }
                }

                if (left.length() == 0)
                    continue;

                // Branched off right at the trunk
                if (previous == null) {
                    previous = new Branch(t,null);
                    Branch[] branches = newbase[c];
                    Branch[] augmented = new Branch[branches.length + 1];
                    System.arraycopy(branches, 0, augmented, 0, branches.length);
                    augmented[branches.length] = previous;
                    newbase[c] = augmented;
                    left = left.substring(1);
                    t = left.charAt(0);
                }

                Branch next = new Branch(t, null);
                if (previous.sub == null){
                    previous.sub = new Branch[]{next};
                } else {
                    Branch[] augmented = new Branch[previous.sub.length + 1];
                    System.arraycopy(previous.sub, 0, augmented, 0, previous.sub.length);
                    augmented[previous.sub.length] = next;
                    previous.sub = augmented;
                }
                previous = next;
                left = left.substring(1);

                while (!left.isEmpty()) {
                    t = left.charAt(0);
                    next = new Branch(t, null);
                    previous.sub = new Branch[]{next};
                    previous = next;
                    left = left.substring(1);
                }
                next.isEnd = true;
            }
        }
        base = newbase;
    }

    /**
     * Returns whether message contains any of the strings.
     * @param message Input message to search through.
     * @return Returns true if found a match, false if not.
     */
    public static boolean contains(String message) {
        Branch[] current = null;
        for (int j = 0; j < message.length(); j++) {
            char c = message.charAt(j);
            if (c > 122){
                current = null;
                continue;
            }
            if (current == null){
                if (base[c] != null)
                    current = base[c];
            } else {
                for (Branch branch: current) {
                    if (branch.c == c) {
                        if (branch.isEnd)
                            return true;

                        current = branch.sub;

                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns whether message equals any of the strings.
     * @param message Input message to search through.
     * @return Returns true if found a match, false if not.
     */
    public static boolean containsExact(String message) {
        Branch[] current = null;
        for (int j = 0; j < message.length(); j++) {
            char c = message.charAt(j);
            if (c > 122){
                current = null;
                continue;
            }
            if (current == null){
                if (j > 0)
                    return false;

                if (base[c] != null)
                    current = base[c];
            } else {
                boolean found = false;
                for (Branch branch: current) {
                    if (branch.c == c) {
                        if (branch.isEnd && j == message.length()-1)
                            return true;

                        current = branch.sub;
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return false;
            }
        }
        return false;
    }

    /**
     * Returns start and end index of the found strings in the message.
     * @param message Input message to search through.
     * @return List of long containing start indexes and lengths.
     */
    @NotNull
    public static List<Long> findMultiple(@NotNull String message) {
        Branch[] current = null;
        int start = 0;
        int lenght = 0;
        List<Long> foundsofar = new ArrayList<>();
        int found = 0;

        for (int j = 0; j < message.length(); j++) {
            char c = message.charAt(j);
            if (c > 122){
                current = null;
                lenght = 0;
                continue;
            }
            if (current == null){
                if (base[c] != null) {
                    current = base[c];
                    start = j;
                }
            } else {
                for (Branch branch: current) {
                    boolean success = false;
                    if (branch.c == c) {
                        success = true;
                        lenght++;
                        current = branch.sub;
                        if (branch.isEnd) {
                            if (found > 1) {
                                if ((int)(foundsofar.get(found - 1) >> 32) == start) {
                                    foundsofar.set(found - 1, (((long)start) << 32) | ((lenght+1) & 0xffffffffL));
                                    break;
                                }
                            }
                            foundsofar.add((((long)start) << 32) | ((lenght+1) & 0xffffffffL));
                            found++;
                        }
                        break;
                    }
                    if (!success) {
                        current = null;
                        lenght = 0;
                    }
                }
            }
        }
        return foundsofar;
    }

    /**
     * Returns start index and length index of the first found string in the message or null if not found.
     * @param message Input message to search through.
     * @return long containing start index and length or 0 if not found.
     */
    public static long findFirst(@NotNull String message) {
        //int messagesize = message.length();
        //if (messagesize <= 1)
        //    return 0;

        Branch[] current = null;
        int start = 0;
        int length = 0;

        for (int j = 0; j < /*messagesize*/message.length(); j++) {
            char c = message.charAt(j);
            if (c > 122){
                current = null;
                start = 0;
                length = 0;
                continue;
            }
            if (current == null){
                if (base[c] != null) {
                    current = base[c];
                    start = j;
                    length = 1;
                } else {
                    length = 0;
                }
            } else {
                boolean found = false;
                for (Branch branch: current) {
                    if (branch.c == c) {
                        found = true;
                        length++;
                        current = branch.sub;
                        if (branch.isEnd) {
                            //System.out.println("start: " + start + " length: " + length + " word: " + message.substring(start, start+length));
                            return (((long)start) << 32) | ((length) & 0xffffffffL);
                        }
                        break;
                    }
                }
                if (!found) {
                    start = 0;
                    length = 0;
                    current = null;
                }

            }
        }
        return 0;
    }

    // Do it yourself in place, it will be faster than calling two methods
    public static int startIndex(long returned) {
        return (int)(returned >> 32);
    }

    public static int endIndex(long returned) {
        return (int) returned;
    }

}
