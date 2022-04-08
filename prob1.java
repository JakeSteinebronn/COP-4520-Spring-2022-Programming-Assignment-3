import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.lang.Math;
import java.util.*;

public class prob1 {

    static int[] gifts;
    static AtomicInteger[] giftInserted;
    static int n;
    static AtomicInteger giftIdx, notesWritten;

    static WaitFreeList list;

    public static void main(String[] args){
        n = 100000;

        // Set up the static variables. The "unordered set" of gifts is just going to be
        // a random permutation, because really, a bunch of removals from an unordered set
        // can be expressed as a permutation anyway.
        shuffleArray(gifts = IntStream.range(0, n).toArray());
        giftIdx = new AtomicInteger(); notesWritten = new AtomicInteger();
        Arrays.setAll(giftInserted = new AtomicInteger[n], a -> new AtomicInteger());
        list = new WaitFreeList();

        // Create and spin off the servant threads
        threadEx[] servants = new threadEx[4];
        for(int i = 0; i < servants.length; i++){
            servants[i] = new threadEx();
            servants[i].start();
        }

        // Join the servant threads
        for(int i = 0; i < servants.length; i++){
            try {
                servants[i].join();
                System.out.println("Joined " + i);
            } catch (Throwable ignored) {
                System.out.println("Failed to join thread " + i);
            }
        }


        // Sanity check, there shouldn't be anything left over in the list.
        for(int i = 0; i < n; i++){
            if(list.contains(i)){
                System.out.println(i + " leftover ");
            }
        }
    }

    static class threadEx extends Thread {

        @Override
        public void run() {
            while (notesWritten.get() < n) {
                int act = (int)(Math.random() * 12);
                // 0-4 is add, 5-9 is remove, 10+ is query
            
                if(act < 5) {
                    // Try to put in a gift
                    if(giftIdx.get() < n){
                        int curIdx = giftIdx.getAndIncrement();
                        if(curIdx < n) {
                            boolean res = list.add(gifts[curIdx]);
                            if(!res) System.out.println("Failed to toss in " + gifts[curIdx]);
                            else {
                                System.out.println("Toss in " + gifts[curIdx]);
                                giftInserted[curIdx].set(1);
                            }
                        }
                    }
                } else if (act < 10) {
                    int curIdx = notesWritten.get();
                    if(curIdx < giftIdx.get() && curIdx < n){
                        // It might have changed since we checked the bounds, so
                        // make sure it's still the expected number and if so, go
                        // this will definitely work for at least one thread and
                        // ensures no two threads will try to remove the same guy

                        // Using compareAndSet here makes sure we increment atomically
                        // only if we are actually using the thing

                        if(giftInserted[curIdx].get() > 0 && notesWritten.compareAndSet(curIdx, curIdx + 1)){
                            boolean res = list.remove(gifts[curIdx]);
                            if(!res) System.out.println("Tried and failed to remove " + gifts[curIdx]);
                            else System.out.println("Thanks, " + gifts[curIdx]);
                        }
                    }
                } else {
                    // query
                    int q = (int)(Math.random() * n);
                    System.out.println("Minotaur asks if " + q + " is in there, and it " + (list.contains(q) ? "is!" : "isn't!"));
                }
            }
        }
    }

    // Taken from StackOverflow since it's a pretty trivial piece of code
    private static void shuffleArray(int[] array) {
        int index;
        java.util.Random random = new java.util.Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            if (index != i) {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
    }

}

// Lifted straight from the textbook but I removed the generics since
// we don't need them.
class Window {
    public Node pred, curr;
    Window(Node myPred, Node myCurr) {
        pred = myPred; curr = myCurr;
    }

    public static Window find(Node head, int key) {
        Node pred = null, curr = null, succ = null;
        boolean[] marked = {false};
        boolean snip;
        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();
            while (true) {
                succ = curr.next.get(marked);
                while (marked[0]) {
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) continue retry;
                    curr = succ;
                    succ = curr.next.get(marked);
                }
                if (curr.key >= key)
                    return new Window(pred, curr);
                pred = curr;
                curr = succ;
            }
        }
    }
}

// Lifted straight from the textbook (plus fixes some bugs it has) 
// but I removed the generics since we don't need them.
class WaitFreeList{
    Node head;
    public WaitFreeList() {
        head = new Node(Integer.MIN_VALUE);
        head.next.set(new Node(Integer.MAX_VALUE), false);
    }

    public boolean add(int item) {
        // int key = item.hashCode();
        int key = item;
        while (true) {
            Window window = Window.find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key == key) {
                return false;
            } else {
                Node node = new Node(item);
                node.next = new AtomicMarkableReference<Node>(curr, false);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(int item) {
        // int key = item.hashCode();
        int key = item;
        boolean snip;
        while (true) {
            Window window = Window.find(head, key);
            Node pred = window.pred, curr = window.curr;
            if (curr.key != key) {
                return false;
            } else {
                Node succ = curr.next.getReference();
                snip = curr.next.compareAndSet(succ, succ, false, true);
                if (!snip)
                    continue;
                pred.next.compareAndSet(curr, succ, false, false);
                return true;
            }
        }
    }
    public boolean contains(int item) {
        boolean[] marked = {false};
        // int key = item.hashCode();
        int key = item;
        Node curr = head;
        while (curr.key < key) {
            curr = curr.next.getReference();
            Node succ = curr.next.get(marked);
        }
        return (curr.key == key && !marked[0]);
    }
}

class Node {
    int item;
    int key;
    AtomicMarkableReference<Node> next;
    public Node(int item){
        this.item = item;
        this.key = item;
        this.next = new AtomicMarkableReference<Node>(null, false);
    }
}
