/**
 *
 *      File Name -     ChainElement.java
 *      Created By -    Vedant Sanil, Sharath Chellappa
 *      Brief -
 *
 *          A single element within a block chain
 */

package blockchain;

import message.Block;

public class ChainElement {
    /** A single block of the blockchain */
    private Block block;
    /** Next element in the chain */
    private ChainElement next;
    /** Previous element in the chain */
    private ChainElement prev;

    // Construtor to add an element to the chain
    public ChainElement(Block block) {
        this.block = block;
        this.next = null;
        this.prev = null;
    }

    // Constructor to add an element along with prev and next
    public ChainElement(Block block, ChainElement next, ChainElement prev) {
        this.block = block;
        this.next = next;
        this.prev = prev;
    }

    public Block getBlock() {
        return block;
    }

    public blockchain.ChainElement getNext() {
        return next;
    }

    public blockchain.ChainElement getPrev() {
        return prev;
    }
}