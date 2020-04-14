/**
 *
 *      File Name -     AddBlockRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a AddBlockRequest API call
 */

package message;


public class AddBlockRequest {
    private int chain_id;
    private Block block;

    public AddBlockRequest(int chain_id, Block block) {
        this.chain_id = chain_id;
        this.block = block;
    }

    public int getChainId() { return chain_id; }

    public Block getBlock() { return block; }
}
