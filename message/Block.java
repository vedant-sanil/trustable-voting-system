/**
 *
 *      File Name -     Block.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The structure of a single block in a blockchain
 */

package message;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.TreeMap;


// Blockchain class
public class Block {

    private long id = 0;
    private Map<String, String> data = new TreeMap<String, String>();
    private long timestamp = 0;
    private long nonce = 0;
    private String previous_hash = "";
    private String hash = "dummy";

    // Constructor
    public Block() {}

    // Parameterized constructor
    public Block(long id, Map<String, String> data, long timestamp,
                 long nonce, String previous_hash, String hash) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.previous_hash = previous_hash;
        this.hash = hash;
    }

    // Return ID of the block
    public long getId() { return id; }

    // Return the block's data
    public Map<String, String> getData() {
        return data;
    }

    // Returns the block's timestamp
    public long getTimestamp() { return timestamp; }

    // Returns the block's nonce value
    public long getNonce() {
        return nonce;
    }

    // Returns the hash of the previous block
    public String getPreviousHash() {
        return previous_hash;
    }

    // Returns the hash value of this block
    public String getHash() {
        return hash;
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }

    // Override the toString method
    public String toString() {
        String key = "$" + Long.toString(timestamp) + "$" + Long.toString(nonce)
                + "$" + previous_hash + "$";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            key += entry.getKey() + ":" + entry.getValue() + "$";
        }
        return key;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof Block) &&
                this.toString().equals(obj.toString());
    }

    // Get the encrypted hash of a block using SHA-256
    public static String computeHash(Block block)
    {
        return DigestUtils.sha256Hex(block.toString());
    }
}
