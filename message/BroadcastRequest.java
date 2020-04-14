/**
 *
 *      File Name -     BroadcastRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a BroadcastBlock API call
 */

package message;


public class BroadcastRequest {
    private int chain_id;
    String request_type;
    private Block block;

    public BroadcastRequest(int chain_id, String request_type, Block block) {
        this.chain_id = chain_id;
        if (request_type.toLowerCase().equals("commit")) {
            this.request_type = "COMMIT";
        }
        else {
            this.request_type = "PRECOMMIT";
        }
        this.block = block;
    }

    public int getChainId() { return chain_id; }

    public String getRequestType() { return request_type; }

    public Block getBlock() { return block; }
}
