/**
 *
 *      File Name -     BlockReply.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The response format for a MineBlock API call
 */

package message;


public class BlockReply {
    private int chain_id;
    private Block block;

    public BlockReply(int chain_id, Block block) {
        this.chain_id = chain_id;
        this.block = block;
    }

    public int getChainId() { return chain_id; }

    public Block getBlock() { return block; }
}
