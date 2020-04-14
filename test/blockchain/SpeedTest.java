/**
 *
 *      File Name -     SpeedTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          Tests the performance of the blockchain nodes by performing
 *          simultaneous mine requests.
 */

package test.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Random;

import test.util.TestFailed;

import message.*;

/**
 */
public class SpeedTest extends NodeTest
{
    /** Test notice. */
    public static final String notice =
            "Testing blockchain performance";
    
    private final int SPEED_TEST_TIMEOUT_MS = 45000;

    private final int TARGET_NUM = 8;

    /** Performs the tests.
     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testConcurrentMine();
    }

    /**
     * Concurrently mines multiple blocks and tests if
     * it can be finished within a specified timeout
     *
     * @throws TestFailed
     */
    private void testConcurrentMine() throws TestFailed
    {
        List<Thread> threads = new ArrayList<>();

        for (int n = 0; n < TARGET_NUM; n++)
        {
            final int local_n = n;

            // create a thread to perform mining
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    int node_n = local_n % nodes.size();

                    Random rand = new Random();

                    Map<String, String> data = new TreeMap<>();
                    data.put("data", Integer.toString(rand.nextInt(TARGET_NUM)));

                    while (true)
                    {
                        // retry until success
                        try {
                            Block block = mineBlock(node_n, 1, data);
                            boolean success = addBlock(node_n, 1, block);

                            if (success) break;
                        }
                        catch (TestFailed tf) {
                            // if one thread fails, this test will fail by time out
                            while (true) ;
                        }

                        // wait for a random amount of time before retry
                        try
                        {
                            Thread.sleep(rand.nextInt(40) * 500);
                        }
                        catch (InterruptedException ex) {}
                    }
                }
            });
            threads.add(t);
        }

        // start concurrent mining
        double start_timer = System.currentTimeMillis();
        for (Thread t :threads)
        {
            t.start();
        }

        // wait for all threads to finish
        for (Thread t :threads)
        {
            try {
                t.join();
            }
            catch (InterruptedException ex) {}
        }

        double end_timer = System.currentTimeMillis();
        double time_elapsed = end_timer - start_timer;

        // check if time is out
        if (time_elapsed < SPEED_TEST_TIMEOUT_MS)
        {
            String msg = String.format("%.3f", time_elapsed / 1000);
            System.out.println(" success in " + msg + " seconds");
        }
        else
        {
            throw new TestFailed("Speed test failed: " +
                    "Concurrent mining timed out.");
        }
    }
}
