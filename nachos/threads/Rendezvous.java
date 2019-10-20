package nachos.threads;

import nachos.machine.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    private class WaitingThread {
      KThread kt;
      int value;
      Lock lock;
      Condition2 cv;

      WaitingThread(KThread kt, int value, Lock lock) {
        this.kt = kt;
        this.value = value;
        this.lock = new Lock();
        // this.lock = lock;
        this.cv = new Condition2(this.lock);
      }
    }

    Map<Integer, WaitingThread> waitingMap;
    Lock rendezvousLock;
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
      waitingMap = new HashMap();
      rendezvousLock = new Lock();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        rendezvousLock.acquire();
        int target = 0;
        if (waitingMap.containsKey(tag)) {
          // have a thread to pair with
          WaitingThread wt = waitingMap.get(tag);
          waitingMap.remove(tag);
          wt.lock.acquire();

          target = wt.value;
          wt.value = value;
          // wake the first exchanger
          wt.cv.wake();

          wt.lock.release();
        } else {
          WaitingThread wt = new WaitingThread(KThread.currentThread(), value, rendezvousLock);
          waitingMap.put(tag, wt);
          wt.lock.acquire();

          wt.cv.wake();
          rendezvousLock.release();
          wt.cv.sleep();
          rendezvousLock.acquire();
          // waken by the second exchanger
          target = wt.value;

          wt.lock.release();
        }
        rendezvousLock.release();
	      return target;
    }

        // Place Rendezvous test code inside of the Rendezvous class.

    private static KThread createNewThread(String name, int send, int expt, int tag, Rendezvous r, List<KThread> list) {
       KThread t = new KThread( new Runnable () {
            public void run() {
      		      System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
      		      int recv = r.exchange (tag, send);
      		      Lib.assertTrue (recv == expt, name + " was expecting " + expt + " but received " + recv);
      		      System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
      		  }
        });
        t.setName(name);
        list.add(t);
        return t;
    }

    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();
        List<KThread> list = new ArrayList<>();

      	KThread t1 = createNewThread("t1", 1, -1, 0, r, list);
      	KThread t2 = createNewThread("t2", -1, 1, 0, r, list);
      	t2.setName("t2");

	      t1.fork(); t2.fork();
      	// assumes join is implemented correctly
        // t1.join(); t2.join();
        for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
    }

    public static void rendezTest2() {
        final Rendezvous r1 = new Rendezvous(), r2 = new Rendezvous();
        List<KThread> list = new ArrayList<>();

        // r1, tag1
      	KThread t1 = createNewThread("t1", 1, 2, 1, r1, list);
      	KThread t2 = createNewThread("t2", 2, 1, 1, r1, list);
      	KThread t3 = createNewThread("t3", 3, 4, 1, r1, list);
      	KThread t4 = createNewThread("t4", 4, 3, 1, r1, list);
        // KThread t5 = createNewThread("t5", 5, 5, 1, r1, list); // should not receive anything
        // r1, tag2
      	KThread t6 = createNewThread("t6", 6, 7, 2, r1, list);
      	KThread t7 = createNewThread("t7", 7, 6, 2, r1, list);
        // KThread t8 = createNewThread("t8", 8, 7, 2, r1, list); // should not receive anything

        // r2, tag1
        KThread t9 = createNewThread("t9", 9, 10, 1, r2, list);
      	KThread t10 = createNewThread("t10", 10, 9, 1, r2, list);
      	KThread t11 = createNewThread("t11", 11, 12, 1, r2, list);
      	KThread t12 = createNewThread("t12", 12, 11, 1, r2, list);
        // KThread t13 = createNewThread("t13", 13, 13, 1, r2, list); // na
        // r2, tag2
      	KThread t14 = createNewThread("t14", 8, 7, 2, r2, list);
      	KThread t15 = createNewThread("t15", 7, 8, 2, r2, list);
        // KThread t16 = createNewThread("t16", 6, 6, 2, r2, list); // na

        for (int i = 0; i < list.size(); i++)
          list.get(i).fork();

      	// assumes join is implemented correctly
        // t1.join(); t2.join();
        for (int i = 0; i < list.size(); i++)
          list.get(i).join();
        // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
    }

    public static void selfTest() {
      System.out.println("Rendezvous test start");
	    // place calls to your Rendezvous tests that you implement here
      // rendezTest1();
      rendezTest2();
      System.out.println("Rendezvous test end");
    }
}
