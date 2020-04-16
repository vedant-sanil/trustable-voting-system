/**
 *
 *      File Name -     ChainElement.java
 *      Created By -    Vedant Sanil, Sharath Chellappa
 *      Brief -
 *
 *          Creates the entire blockchain as a doubly linked LinkedList
 */

package blockchain;

import java.lang.String;

public class Chain {
    /** Element pointing to the head of the LinkedList */
    private ChainElement head;
    /** Element pointing to the tail of the LinkedList */
    private ChainElement tail;

    // Constructor to initialize chain
    public Chain() {
        this.head = null;
        this.tail = null;
    }

    /**
     *
     * @param chain : last updated list chain
     * @param block : Input block to be added to the chain
     * @return
     */
    public Chain addBlock(Chain chain, Block block) {
        ChainElement elem = new ChainElement(block);

        // Check if linkedList is empty
        if ((chain.head == null) && (chain.tail == null)) {
            chain.head = elem;
            chain.tail = elem;
        } else {

        }
    }
}