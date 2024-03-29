package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {


	private PriorityQueue<ThreadQueueObject> pQ = new PriorityQueue<ThreadQueueObject>();
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {

		long time = Machine.timer().getTime();
		boolean stat = Machine.interrupt().disable();
		while (!pQ.isEmpty() && pQ.peek().wakeTime <= time){
			KThread toWake = pQ.poll().thread;
			toWake.ready();
		}
		KThread.currentThread().yield();
		Machine.interrupt().restore(stat);
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		KThread currentThread = KThread.currentThread();
		boolean stat = Machine.interrupt().disable();
		if (wakeTime > Machine.timer().getTime()) {
			pQ.add(new ThreadQueueObject(currentThread, wakeTime));
			currentThread.sleep();
		}
		Machine.interrupt().restore(stat);

	}

	private class ThreadQueueObject  implements Comparable<ThreadQueueObject>{
		private KThread thread;
		private long wakeTime;
		public ThreadQueueObject (KThread thread, long wakeTime){
			this.thread = thread;
			this.wakeTime = wakeTime;
		}

		public int compareTo(ThreadQueueObject other){
			if (this.wakeTime > other.wakeTime){
				return 1;
			}else{ if (this.wakeTime == other.wakeTime){
				return 0;
			}else{
				return -1;
			}
			}
		}


	}

	// Add Alarm testing code to the Alarm class

	public static void alarmTest1() {
		int durations[] = {-1, 0, 1000, 10*1000, 100*1000};
		long t0, t1;

		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil (d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	// Implement more test methods here ...

	// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
	public static void selfTest() {
		alarmTest1();

		// Invoke your other test methods here ...
	}
	/**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
        public boolean cancel(KThread thread) {
			for (ThreadQueueObject i : pQ) {
				if (i.thread == thread) {
					pQ.remove(i);
					boolean stat = Machine.interrupt().disable();
					thread.ready();
					Machine.interrupt().restore(stat);
					return true;
				}
			}

			return false;
		}
}
