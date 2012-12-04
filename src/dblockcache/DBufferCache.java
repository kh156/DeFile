package dblockcache;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import virtualdisk.VirtualDisk;

import common.Constants;

public class DBufferCache {

    private int _cacheSize;
    private LinkedList<DBuffer> buffers;
    //    private Boolean[] held = new Boolean[Constants.NUM_OF_BLOCKS];

    /*
     * Constructor: allocates a cacheSize number of cache blocks, each
     * containing BLOCK-size bytes data, in memory
     */
    public DBufferCache(String volName, boolean format, int cacheSize) {
        _cacheSize = cacheSize * Constants.BLOCK_SIZE;
        buffers = new LinkedList<DBuffer>();
        
        VirtualDisk vdk = null;
        try {
            vdk = new VirtualDisk(volName, format);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        for (int i=0; i<cacheSize; i++) {
            buffers.add(new DBuffer(vdk, -1, Constants.BLOCK_SIZE));
        }
    }

    /*
     * Get buffer for block specified by blockID. The buffer is "held" until the
     * caller releases it. A “held” buffer cannot be evicted: its block ID
     * cannot change.
     */
    public synchronized DBuffer getBlock(int blockID) {
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
            boolean foundBlock = false;
            for (DBuffer b:buffers) {
                if (b.getBlockID() == blockID) {
                    foundBlock = true;
                    if (b.isBusy()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    else {
                        // blockID matched
                        b.setBusy(true);
                        buffers.remove(b);
                        buffers.add(b);
                        return b;
                    }
                }
            }
            if (foundBlock) {
                continue;
            }
            for (DBuffer b:buffers) {
                if (!b.isBusy() && !b.checkValid()) {
                    // unused
                    b.setBusy(true);
                    b.setBlockID(blockID);
                    b.setValid(false);
                    buffers.remove(b);
                    buffers.add(b);
                    return b;
                }
            }

            for (DBuffer b:buffers) {
                if (!b.isBusy()) {
                    // evict
                    b.setBusy(true);
                    if (b.checkValid() && !b.checkClean()) {
                        b.startPush();
                    }
                    b.setBlockID(blockID);
                    b.setValid(false);
                    buffers.remove(b);
                    buffers.add(b);
                    return b;
                }
            }
            // all buffers held!!!
            try {
                buffers.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /* Release the buffer so that others waiting on it can use it */
    public synchronized void releaseBlock(DBuffer buf) {
        buf.setBusy(false);
        notifyAll();
    }

    /*
     * sync() writes back all dirty blocks to the volume and wait for completion.
     * The sync() method should maintain clean block copies in DBufferCache.
     */
    public synchronized void sync() {
        boolean[] heldmap = new boolean[buffers.size()];
        while (true) {
            boolean check = true;
            for (int i=0; i<heldmap.length; i++ ) {
                if (heldmap[i] == false) {
                    check = false;
                    break;
                }
            }
            if (!check) {
                for (int i=0; i<buffers.size(); i++) {
                    while (buffers.get(i).isBusy()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    buffers.get(i).setBusy(true);
                    heldmap[i] = true;
                }
            }
            else {
                break;
            }
        }
        for (DBuffer b:buffers) {
            if (b.checkValid() && !b.checkClean()) {
                b.startPush();
            }
            b.setBusy(false);
        }
    }
}
