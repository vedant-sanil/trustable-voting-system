/**
 *
 *      File Name -     RegistrationTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This test checks if the public keys of all the voting clients
 *          and voting server have been added to the public key blockchain
 *          after performing the required proof-of-work.
 *
 *          To pass this test, all the nodes must have performed mining,
 *          adding and broadcasting of their respective public keys!
 *
 */


package test.vote;

import java.util.List;

import test.util.TestFailed;

import message.*;

/**
 *      Test the public key registration process of the
 *      voting server and clients
 */
public class RegistrationTest extends VoteTest
{
    /** Test notice. */
    public static final String notice =
            "Testing public key registration";


    /** Performs the tests.

     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testAddKey();
    }

    /**
     *      Test to see if all the keys have been added to the
     *      public key blockchain
     *
     *      @throws TestFailed
     */
    private void testAddKey() throws TestFailed {

        List<Block> blocks = getBlockchain(KEYCHAIN_ID);

        if (blocks.size() < client_ports.size() + 2)
        {
            throw new TestFailed("AddPublicKey failed: " +
                    "Insufficient number of keys");
        }

        // Loop through all the blocks in the Public key blockchain.
        // (ignores the genesis block)
        for (int i = 1; i < blocks.size(); i++)
        {
            Block block = blocks.get(i);

            String pubKey = block.getData().getOrDefault("public_key", "");
            String userName = block.getData().getOrDefault("user_name", "");

            if (pubKey.isEmpty())
                throw new TestFailed("Adding PublicKey failed: " +
                        "public key not found");

            if (userName.isEmpty())
                throw new TestFailed("Adding PublicKey failed: " +
                        "username not found");
        }
    }
}
