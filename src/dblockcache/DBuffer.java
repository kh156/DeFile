package dblockcache;


public class DBuffer {

    private Integer blockID;
    private Boolean valid;
    private Boolean busy;

    public DBuffer() {

    }

    /* Start an asynchronous fetch of associated block from the volume */
    public void startFetch() {

    }

    /* Start an asynchronous write of buffer contents to block on volume */
    public void startPush() {

    }

    /* Check whether the buffer has valid data */ 
    public boolean checkValid() {
        synchronized(valid) {
            return valid;
        }
    }

    public void setValid(boolean v) {
        synchronized(valid) {
            valid = v;
        }
    }

    /* Wait until the buffer is free */
    public boolean waitValid() {

    }

    /* Check whether the buffer is dirty, i.e., has modified data to be written back */
    public boolean checkClean() {

    }

    /* Wait until the buffer is clean, i.e., until a push operation completes */
    public boolean waitClean() {

    }

    /* Check if buffer is evictable: not evictable if I/O in progress, or buffer is held */
    public boolean isBusy() {
        synchronized(busy) {
            return busy;
        }
    }

    public void setBusy(boolean b) {
        synchronized(busy) {
            busy = b;
        }
    }

    /*
     * reads into the buffer[] array from the contents of the DBuffer. Check
     * first that the DBuffer has a valid copy of the data! startOffset and
     * count are for the buffer array, not the DBuffer. Upon an error, it should
     * return -1, otherwise return number of bytes read.
     */
    public int read(byte[] buffer, int startOffset, int count) {

    }

    /*
     * writes into the DBuffer from the contents of buffer[] array. startOffset
     * and count are for the buffer array, not the DBuffer. Mark buffer dirty!
     * Upon an error, it should return -1, otherwise return number of bytes
     * written.
     */
    public int write(byte[] buffer, int startOffset, int count) {

    }

    /* An upcall from VirtualDisk layer to inform the completion of an IO operation */
    public void ioComplete() {

    }

    /* An upcall from VirtualDisk layer to fetch the blockID associated with a startRequest operation */
    public int getBlockID() {
        synchronized(blockID) {
            return blockID;
        }
    }

    public void setBlockID(int id) {
        synchronized(blockID) {
            blockID = id;
        }
    }

    /* An upcall from VirtualDisk layer to fetch the buffer associated with DBuffer object*/
    public byte[] getBuffer() {

    }
}