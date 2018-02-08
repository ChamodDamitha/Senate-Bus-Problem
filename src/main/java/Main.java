import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * Created by chamod on 2/8/18.
 */
public class Main {
    static int riders_count = 0;
    static Semaphore mutex = new Semaphore(1);
    static Semaphore multiplex = new Semaphore(50);
    static Semaphore bus = new Semaphore(0);
    static Semaphore allAboard = new Semaphore(0);

    static ArrayList<Bus> busses = new ArrayList();
    static ArrayList<Rider> riders = new ArrayList();

//  mean time in seconds
    static final int BUS_MEAN_TIME = 20 * 60;
    static final int RIDER_MEAN_TIME = 30;

    static ExponentialDistribution busExponentialDistribution = new ExponentialDistribution(BUS_MEAN_TIME);
    static ExponentialDistribution riderExponentialDistribution = new ExponentialDistribution(RIDER_MEAN_TIME);

    public static void main(String[] args) {
        update();
    }


    private static void update() {
        Thread busGenerater = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep((long) busExponentialDistribution.sample());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Bus bus = new Bus(busses.size());
                    busses.add(bus);
                    bus.start();
                }
            }
        });

        Thread riderGenerator = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep((long) riderExponentialDistribution.sample());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Rider rider = new Rider(riders.size());
                    riders.add(rider);
                    rider.start();
                }
            }
        });

        busGenerater.start();
        riderGenerator.start();
    }


    private static class Bus extends Thread {
        private int id;

        Bus(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                mutex.acquire();
                System.out.println("Bus " + this.id + " arrived - " + riders_count + " riders to get in");
                if (riders_count > 0) {
                    bus.release();
                    allAboard.acquire();
                }
                mutex.release();
                System.out.println("Bus " + this.id + " departed!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private static class Rider extends Thread {
        private int id;

        Rider(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                multiplex.acquire();
                mutex.acquire();
                riders_count++;
                mutex.release();
                bus.acquire();
                multiplex.release();
//                System.out.println("Rider " + this.id + " boarded to bus!");
                riders_count--;
                if (riders_count == 0) {
                    allAboard.release();
                } else {
                    bus.release();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}


