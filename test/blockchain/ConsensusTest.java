/**
 *
 *      File Name -     ConsensusTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          Check different situations where there should/shouldn't be
 *          consensus amongst nodes in the network.
 *
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
 *      Check different situations where there should/shoundn't be
 *      consensus amongst nodes in the network
 */
public class ConsensusTest extends NodeTest
{
    /** Test notice. */
    public static final String notice =
            "Testing blockchain consensus";

    private int[] chain_lengths = {1, 1};

    Random rand = new Random();

    /** Performs the tests.

     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testAgree();
        testFailAgree();
        testFailNoAgree();
    }

    /**
     *      Tests situations where multiple blocks mined with the
     *      same previous block are added at the same time.
     *      Only one block will be agreed and added.
     *
     *      @throws TestFailed
     */
    private void testAgree() throws TestFailed
    {
        Map<String, String> data = new TreeMap<>();

        int c = selectRandomChain();

        int chain_id = CHAIN_IDS[c];

        List<Block> mined_blocks = new ArrayList<>();

        // mine multiple blocks with the same previous block
        for (int n = 0; n < nodes.size(); n++) {
            data.put("data", Integer.toString(rand.nextInt(500)));
            Block block = mineBlock(n, chain_id, data);
            mined_blocks.add(block);
        }

        // add the new blocks at the same time
        for (int n = 0; n < nodes.size(); n++)
        {
            Block block = mined_blocks.get(n);
            boolean success = addBlock(n, chain_id, block);
        }

        try
        {
            Thread.sleep(BROADCAST_TIMEOUT_MS);
        }
        catch (InterruptedException ex) {}

        chain_lengths[c]++;

        // test consensus
        checkChainConsensus(nodes, chain_id, chain_lengths[c]);

    }


    /**
     *      Tests situations where the system should reach consensus
     *      with failure of some nodes.
     *      The tests puts some nodes to sleep but the nodes alive
     *      are able to form a quorum.
     *
     *      @throws TestFailed
     */
    private void testFailAgree() throws TestFailed
    {
        Map<String, String> data = new TreeMap<>();

        int c = selectRandomChain();

        int chain_id = CHAIN_IDS[c];

        int num_alive = (int) Math.ceil(nodes.size() * 2.0 / 3);
        int num_failed = nodes.size() - num_alive;
        List<Integer> nodes_alive = new ArrayList<>(nodes);

        // puts some nodes to sleep
        for (int n = 0; n < num_failed; n++) {
            disconnect((nodes.size() - 1) - n);
            nodes_alive.remove((nodes.size() - 1) - n);
        }

        // mine and add multiple blocks sequentially
        for (int n = 0; n < num_alive; n++)
        {
            data.put("data", Integer.toString(rand.nextInt(500)));
            Block block = mineBlock(n, chain_id, data);
            addBlock(n, chain_id, block);

            try
            {
                Thread.sleep(BROADCAST_TIMEOUT_MS);
            }
            catch (InterruptedException ex) {}

            chain_lengths[c]++;
        }

        // wait for the nodes asleep to wake up
        try
        {
            int time_remaining = SLEEP_TIMEOUT * 1000 -
                    BROADCAST_TIMEOUT_MS * num_alive;
            Thread.sleep(time_remaining);
        }
        catch (InterruptedException ex) {}

        // test consensus on nodes that did not sleep
        checkChainConsensus(nodes_alive, chain_id, chain_lengths[c]);

        // mine and add a block
        data.put("data", Integer.toString(rand.nextInt(500)));
        Block final_block = mineBlock(nodes.size() - 1, chain_id, data);
        addBlock(nodes.size() - 1, chain_id, final_block);

        try
        {
            Thread.sleep(BROADCAST_TIMEOUT_MS);
        }
        catch (InterruptedException ex) {}

        chain_lengths[c]++;

        // test consensus on all nodes
        checkChainConsensus(nodes, chain_id, chain_lengths[c]);
    }


    /**
     *      Test situations where the system should not have reached
     *      consensus.
     *      This test puts some nodes to sleep so as to ensure that
     *      quorum is not reached.
     *
     *      @throws TestFailed
     */
    private void testFailNoAgree() throws TestFailed
    {
        Map<String, String> data = new TreeMap<>();
        data.put("data", Integer.toString(rand.nextInt(500)));

        int c = selectRandomChain();

        int chain_id = CHAIN_IDS[c];

        int num_alive = (int) Math.ceil(nodes.size() * 2.0 / 3) - 1;
        int num_failed = nodes.size() - num_alive;

        // mine a new block
        Block block = mineBlock(0, chain_id, data);

        // puts some nodes to sleep
        for (int n = 0; n < num_failed; n++) {
            disconnect((nodes.size() - 1) - n);
        }

        // add the new block
        for (int n = 0; n < num_alive; n++)
        {
            boolean success = addBlock(n, chain_id, block);

            if (success)
            {
                throw new TestFailed("Error: " +
                        "Nodes shouldn't have reached consensus!");
            }
        }

        try
        {
            Thread.sleep(SLEEP_TIMEOUT * 1000);
        }
        catch (InterruptedException ex) {}

        // test consensus on all nodes
        checkChainConsensus(nodes, chain_id, chain_lengths[c]);
    }


    /**
     *      Selects a random blockchain id
     */
    private int selectRandomChain() {
        return (int) (System.currentTimeMillis() % 2);
    }


    /**
     *      Check if the all the nodes have consistent view of the
     *      blockchain
     *
     *      @throws TestFailed
     */
    protected void checkChainConsensus(List<Integer> ports, int chain_id,
                                       int target_length) throws TestFailed
    {
        GetChainRequest request = new GetChainRequest(chain_id);

        List<Block> ref_blocks = null;

        for (int port : ports)
        {
            String uri = HOST_URI + port + GET_CHAIN_URI;

            GetChainReply reply;
            try
            {
                reply = client.post(uri, request, GetChainReply.class);

                if (reply == null) throw new Exception();
            }
            catch (Exception ex)
            {
                throw new TestFailed("GetBlockChain failed: " +
                        "No response or incorrect format.");
            }

            if (reply.getChainLength() != target_length)
            {
                throw new TestFailed("Error: Incorrect chain length, " +
                        " should be " + target_length +
                        " instead of " + reply.getChainLength() +"!");
            }

            List<Block> blocks = reply.getBlocks();
            if (ref_blocks == null)
            {
                ref_blocks = blocks;
            }
            else if (!ref_blocks.equals(blocks))
            {
                throw new TestFailed("Error: Chain inconsistent across nodes!");
            }
        }
    }
}
