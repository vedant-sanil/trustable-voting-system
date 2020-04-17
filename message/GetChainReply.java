/**
 *
 *      File Name -     GetChainReply.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The response format for a GetBlockChain API call
 */

package message;

import java.util.List;


public class GetChainReply {
    public int chain_id;
    public int chain_length;
    public List<Block> blocks;

    public GetChainReply(int chain_id, int chain_length, List<Block> blocks) {
        this.chain_id = chain_id;
        this.chain_length = chain_length;
        this.blocks = blocks;
    }

    public int getChainId() { return chain_id; }

    public int getChainLength() { return chain_length; }

    public List<Block> getBlocks() { return blocks; }
}
