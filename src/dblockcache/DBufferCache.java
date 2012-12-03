package dblockcache;

import java.util.LinkedList;
import java.util.Queue;

import common.Constants;

public class DBufferCache {

    private int _cacheSize;
    private Queue<DBuffer> buffers;
//    private Boolean[] held = new Boolean[Constants.NUM_OF_BLOCKS];

    /*
     * Constructor: allocates a cacheSize number of cache blocks, each
     * containing BLOCK-size bytes data, in memory
     */
    public DBufferCache(int cacheSize) {
        _cacheSize = cacheSize * Constants.BLOCK_SIZE;
        buffers = new LinkedList<DBuffer>();
        for (int i=0; i<cacheSize; i++) {
            buffers.add(new DBuffer());
        }
    }

    /*
     * Get buffer for block specified by blockID. The buffer is "held" until the
     * caller releases it. A “held” buffer cannot be evicted: its block ID
     * cannot change.
     */
    public DBuffer getBlock(int blockID) {
        //        synchronized(held[blockID]) {
        //            while (held[blockID]) {
        //                held[blockID].wait();
        //            }
        //            held[blockID] = true;
        //        }
        //        DBuffer buff = null;
        //        synchronized(buffers) {
        //            for (DBuffer b:buffers) {
        //                if (b.getBlockID() == blockID) {
        //                    return b;
        //                }
        //            }
        //            for (DBuffer b:buffers) {
        //                synchronized(held[b.getBlockID()]) {
        //                    if (!held[b.getBlockID()]) {
        //                        b.setBlockID(blockID);
        //                        b.setValid(false);
        //                        return b;
        //                    }
        //                }
        //            }
        //        }
        while (true) {
            synchronized(buffers) {                                 
                for (DBuffer b:buffers) {
                    if (!b.isBusy() && b.getBlockID() == blockID) {
                        b.setBusy(true);
                        buffers.remove(b);
                        buffers.add(b);
                        return b;
                    }
                }
                for (DBuffer b:buffers) {
                    if (!b.isBusy()) {
                        // evict
                        b.setBusy(true);
                        b.setBlockID(blockID);
                        b.setValid(false);
                        buffers.remove(b);
                        buffers.add(b);
                        return b;
                    }
                }
                try {
                    buffers.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* Release the buffer so that others waiting on it can use it */
    public void releaseBlock(DBuffer buf) {
        buf.setBusy(false);
        buffers.notify();
    }

    /*
     * sync() writes back all dirty blocks to the volume and wait for completion.
     * The sync() method should maintain clean block copies in DBufferCache.
     */
    public void sync() {
        synchronized(buffers) {
            
        }
    }
}