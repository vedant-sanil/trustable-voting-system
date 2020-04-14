/**
 *
 *      File Name -     Miningtest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          Tests if the nodes are able to mine and add blocks to the
 *          blockchains
 *
 */

package test.blockchain;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import test.util.TestFailed;

import message.*;

/**
 */
public class MiningTest extends NodeTest
{
    /** Test notice. */
    public static final String notice =
            "Testing blockchain initial setup & basic mining";

    private int[] chain_lengths = {1, 1};

    /** Performs the tests.

     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testInitialState();
        testMineOneBlock();
        testMultiMineOneBlock();
    }

    /**
     *      Test the initial state of the two blockchains.
     *      They each must contain only the genesis block.
     *
     *      @throws TestFailed
     */
    private void testInitialState() throws TestFailed
    {
        for (int c = 0; c < 2; c++)
        {
            int chain_id = CHAIN_IDS[c];
            String hash = "";

            GetChainRequest request = new GetChainRequest(chain_id);

            for (int i = 0; i < nodes.size(); i++) {
                String uri = HOST_URI + nodes.get(i) + GET_CHAIN_URI;
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

                if (reply.getChainLength() != 1)
                {
                    throw new TestFailed("Error: " +
                            "Incorrect initial state, " +
                            "chain length should be 1!");
                }

                Block block = reply.getBlocks().get(0);
                String cur_hash = block.getHash();

                if (hash.isEmpty())
                {
                    hash = cur_hash;
                }
                else if (!hash.equals(cur_hash))
                {
                    throw new TestFailed("Error: " +
                            "Incorrect initial state, " +
                            "inconsistent genesis block!");
                }
            }
        }
    }

    /**
     *      Try to mine and add a single block to each of the blockchains
     *      from a single node
     *
     *      @throws TestFailed
     */
    private void testMineOneBlock() throws TestFailed
    {
        Map<String, String> data = new TreeMap<>();
        data.put("sample_key", "sample_value");

        for (int c = 0; c < 2; c++)
        {
            int chain_id = CHAIN_IDS[c];
            String chain_proof = CHAIN_PROOFS[c];
            int port = nodes.get(0);

            // Mine a new block
            MineBlockRequest mine_request = new MineBlockRequest(
                    chain_id, data);
            String mine_uri = HOST_URI + port + MINE_BLOCK_URI;

            BlockReply mine_reply;
            Block block;
            try
            {
                mine_reply = client.post(mine_uri, mine_request,
                        BlockReply.class);
                if (mine_reply == null) throw new Exception();

                block = mine_reply.getBlock();
                if (block == null) throw new Exception();
            }
            catch (Exception ex)
            {
                throw new TestFailed("MineBlock failed: " +
                        "No response or incorrect format.");
            }

            checkChainLength(nodes, chain_id, chain_lengths[c]);

            if (!block.getHash().startsWith(chain_proof))
            {
                throw new TestFailed("Error: " +
                        "Hash is of incorrect difficulty!");
            }

            // Add the new block
            AddBlockRequest add_request = new AddBlockRequest(
                    chain_id, block);
            String add_uri = HOST_URI + port + ADD_BLOCK_URI;

            StatusReply add_reply;
            try
            {
                add_reply = client.post(add_uri, add_request,
                        StatusReply.class);
                if (add_reply == null) throw new Exception();
            }
            catch (Exception ex)
            {
                throw new TestFailed("AddBlock failed: " +
                        "No response or incorrect format");
            }

            if (!add_reply.getSuccess())
            {
                throw new TestFailed("Error: Failed to add block!");
            }

            try
            {
                Thread.sleep(BROADCAST_TIMEOUT_MS);
            }
            catch (InterruptedException ex) {}

            chain_lengths[c]++;
            checkChainData(nodes, chain_id, chain_lengths[c],
                    chain_lengths[c] - 1, data);
        }
    }

    /**
     *      Each node mines and adds one block onto both blockchains,
     *
     *      @throws TestFailed
     */
    private void testMultiMineOneBlock() throws TestFailed
    {
        Map<String, String> data = new TreeMap<>();
        String key = "data";

        for (int c = 0; c < 2; c++)
        {
            int chain_id = CHAIN_IDS[c];
            String chain_proof = CHAIN_PROOFS[c];

            for (int n = 0; n < nodes.size(); n++) {
                int port = nodes.get(n);

                // Mine a new block
                data.put(key, Integer.toString(n));
                MineBlockRequest mine_request = new MineBlockRequest(
                        chain_id, data);
                String mine_uri = HOST_URI + port + MINE_BLOCK_URI;

                BlockReply mine_reply;
                Block block;
                try
                {
                    mine_reply = client.post(mine_uri, mine_request,
                            BlockReply.class);
                    if (mine_reply == null) throw new Exception();

                    block = mine_reply.getBlock();
                    if (block == null) throw new Exception();
                }
                catch (Exception ex)
                {
                    throw new TestFailed("MineBlock failed: " +
                            "No response or incorrect format.");
                }

                checkChainLength(nodes, chain_id, chain_lengths[c]);

                if (!block.getHash().startsWith(chain_proof))
                {
                    throw new TestFailed("Error: " +
                            "Hash is of incorrect difficulty!");
                }

                // Add the new block
                AddBlockRequest add_request = new AddBlockRequest(
                        chain_id, block);
                String add_uri = HOST_URI + port + ADD_BLOCK_URI;

                StatusReply add_reply;
                try
                {
                    add_reply = client.post(add_uri, add_request,
                            StatusReply.class);
                    if (add_reply == null) throw new Exception();
                }
                catch (Exception ex)
                {
                    throw new TestFailed("AddBlock failed: " +
                            "No response or incorrect format");
                }

                if (!add_reply.getSuccess())
                {
                    throw new TestFailed("Error: Failed to add block!");
                }

                try
                {
                    Thread.sleep(BROADCAST_TIMEOUT_MS);
                }
                catch (InterruptedException ex) {}

                chain_lengths[c]++;
                checkChainData(nodes, chain_id, chain_lengths[c],
                        chain_lengths[c] - 1, data);
            }
        }
    }


    /**
     *      Checks the length of the blockchain with a target_length that
     *      is tracked by the test suite
     *
     *      @throws TestFailed
     */
    protected void checkChainLength(List<Integer> ports, int chain_id,
                                    int target_length) throws TestFailed
    {
        GetChainRequest request = new GetChainRequest(chain_id);

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
        }
    }


    /**
     *      Checks the integrity of a blockchain after mining and block
     *      broadcast
     *
     *      @throws TestFailed
     */
    protected void checkChainData(List<Integer> ports,
                                  int chain_id, int target_length,
                                  int index, Map<String, String> expect_data)
            throws TestFailed
    {
        GetChainRequest request = new GetChainRequest(chain_id);

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
            Block block = blocks.get(index);
            Block prev_block = blocks.get(index - 1);

            if (!block.getData().equals(expect_data))
            {
                throw new TestFailed("Error: Incorrect block data!");
            }
            if (!block.getPreviousHash().equals(prev_block.getHash()))
            {
                throw new TestFailed("Error: Incorrect previous hash!");
            }
            if (block.getTimestamp() <= prev_block.getTimestamp())
            {
                throw new TestFailed("Error: Timestamp should increment!");
            }
        }
    }
}
