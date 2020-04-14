/**
 *
 *      File Name -     BlockReply.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a MineBlock API call
 */

package message;

import java.util.Map;


public class MineBlockRequest {
    private int chain_id;
    private Map<String, String> data;

    public MineBlockRequest(int chain_id, Map<String, String> data) {
        this.chain_id = chain_id;
        this.data = data;
    }

    public int getChainId() { return chain_id; }

    public Map<String, String> getData() { return data; }
}
