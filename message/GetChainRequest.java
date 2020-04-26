/**
 *
 *      File Name -     GetChainRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a GetBlockChain API call
 */

 package message;

public class GetChainRequest {
    private int chain_id;

    public GetChainRequest(int chain_id) {
        this.chain_id = chain_id;
    }

    public int getChainId() { return chain_id; }
}
