import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by chamod on 2/8/18.
 */
public class Main {
    private static int riders_count = 0;
    private static Semaphore mutex = new Semaphore(1);
    private static Semaphore multiplex = new Semaphore(50);
    private static Semaphore bus = new Semaphore(0);
    private static Semaphore allAboard = new Semaphore(0);

    private static ArrayList<Bus> busses = new ArrayList();
    private static ArrayList<Rider> riders = new ArrayList();

    //  mean time in milliseconds
    private static double BUS_MEAN_TIME;
    private static double RIDER_MEAN_TIME;

    private static ExponentialDistribution busExponentialDistribution;
    private static ExponentialDistribution riderExponentialDistribution;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please provide <BUS_MEAN_TIME(s)> <RIDER_MEAN_TIME(s)> as first two arguments!");
        } else {
            BUS_MEAN_TIME = Double.valueOf(args[0]) * 1000;
            RIDER_MEAN_TIME = Double.valueOf(args[1]) * 1000;
            System.out.println("Mean arrival time between buses : " + args[0] + "s");
            System.out.println("Mean arrival time between riders : " + args[1] + "s");

            busExponentialDistribution = new ExponentialDistribution(BUS_MEAN_TIME);
            riderExponentialDistribution = new ExponentialDistribution(RIDER_MEAN_TIME);
            startSimulation();
        }
    }


    private static void startSimulation() {
        Thread busGenerator = new Thread(new Runnable() {
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

        busGenerator.start();
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
                System.out.print("Riders - ");
                if (riders_count > 0) {
                    bus.release();
                    allAboard.acquire();
                }
                mutex.release();
                System.out.println();
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
                System.out.print(this.id + ",");
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