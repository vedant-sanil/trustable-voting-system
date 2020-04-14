/**
 *
 *      File Name -     SleepRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a Sleep API call
 */

package message;

public class SleepRequest {
    private int timeout;

    public SleepRequest(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() { return timeout; }
}
