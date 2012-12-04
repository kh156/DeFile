package dfs;

import java.util.*;

import common.Constants;
import common.DFileID;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;

public class DFS {

    private boolean _format;
    private String _volName;

    private DBufferCache cache;
    private Inode[] inodeMap;
    private boolean[] usedBlockMap;


    DFS(String volName, boolean format) {
        _volName = volName;
        _format = format;
        cache = new DBufferCache(Constants.CACHE_SIZE);
        inodeMap = new Inode[Constants.NUM_OF_INODE];
        usedBlockMap = new boolean[Constants.NUM_OF_BLOCKS];        // lock on the map????????????


        initializeInode();
    }

    DFS(boolean format) {
        this(Constants.vdiskName,format);
    }

    DFS() {
        this(Constants.vdiskName,false);
    }

    private void initializeInode() {
        for (int i=0; i<inodeMap.length; i++) {
            inodeMap[i] = new Inode(i);
            inodeMap[i].initializeFromDisk();
        }
    }

    /*
     * If format is true, the system should format the underlying disk contents,
     * i.e., initialize to empty. On success returns true, else return false.
     */
    public boolean format() {
        if (_format) {
            for (Inode f:inodeMap) {
                f.write.lock();
            }
            synchronized(usedBlockMap) {
                Arrays.fill(usedBlockMap, false);
            }
            for (Inode f:inodeMap) {
                f.clearContent();
                f.setUsed(false);
                f.updateDisk();                // need optimized!!!!!!!!!!!!
            }
            for (Inode f:inodeMap) {
                f.write.unlock();
            }
            return true;
        }
        else {
            return false;
        }
    }

    /* creates a new DFile and returns the DFileID, which is useful to uniquely identify the DFile*/
    public DFileID createDFile() {
        for (Inode f:inodeMap) {
            f.write.lock();
            if (!f.isUsed()) {
                f.clearContent();
                f.setUsed(true);
                f.updateDisk();          
                f.write.unlock();
                return new DFileID(f.getIndex());
            }
            f.write.unlock();
        }
        return null;
    }

    /* destroys the file specified by the DFileID */
    public void destroyDFile(DFileID dFID) {
        Inode f = inodeMap[dFID.getID()];
        f.write.lock();
        synchronized(usedBlockMap) {
            for (int i:f.getBlockList()) {
                usedBlockMap[i] = false;
            }
        }
        f.clearContent();
        f.setUsed(false);
        f.updateDisk();
        f.write.unlock();
    }

    /*
     * reads the file dfile named by DFileID into the buffer starting from the
     * buffer offset startOffset; at most count bytes are transferred
     */
    public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
        Inode f = inodeMap[dFID.getID()];
        f.read.lock();
        if (!f.isUsed()) {
            f.read.unlock();
            return -1;
        }

        int readsize = Math.min(f.getSize(), count);
        List<Integer> blocks = f.getBlockList();
        for (int i=0; i<blocks.size(); i++) {
            DBuffer dbuff = cache.getBlock(blocks.get(i));
            if (dbuff.read(buffer, startOffset + i*Constants.BLOCK_SIZE, readsize - i*Constants.BLOCK_SIZE) == -1) {
                cache.releaseBlock(dbuff);
                f.read.unlock();
                return -1;
            }
            cache.releaseBlock(dbuff);
        }
        f.read.unlock();
        return readsize;
    }

    /*
     * writes to the file specified by DFileID from the buffer starting from the
     * buffer offset startOffsetl at most count bytes are transferred
     */
    public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
        Inode f = inodeMap[dFID.getID()];
        f.write.lock();
        if (!f.isUsed()) {
            f.write.unlock();
            return -1;
        }

        // clear used blocks first
        synchronized(usedBlockMap) {
            for (int i:f.getBlockList()) {
                usedBlockMap[i] = false;
            }
        }
        f.clearContent();

        int writesize = Math.min(buffer.length-startOffset, count);
        int numBlocks = writesize / Constants.BLOCK_SIZE;
        if (writesize % Constants.BLOCK_SIZE > 0)
            numBlocks ++;
        
        // find free blocks
        int head = 0;
        synchronized(usedBlockMap) {
            for (int i= Constants.NUM_OF_INODE / (Constants.BLOCK_SIZE / Constants.INODE_SIZE); i<numBlocks; i++) {
                while (head<usedBlockMap.length && usedBlockMap[head] == true) {
                    head++;
                }
                if (head >= usedBlockMap.length) {
                    f.write.unlock();
                    return -1;
                }
                usedBlockMap[head] = true;
                f.addBlock(head);
            }
        }

        // set DFile size
        f.setSize(writesize);

        // update inode region on disk
        f.updateDisk();

        List<Integer> blocks = f.getBlockList();
        for (int i=0; i<blocks.size(); i++) {
            DBuffer dbuff = cache.getBlock(blocks.get(i));
            if (dbuff.write(buffer, startOffset + i*Constants.BLOCK_SIZE, writesize - i*Constants.BLOCK_SIZE) == -1) {
                cache.releaseBlock(dbuff);
                f.write.unlock();
                return -1;
            }
            cache.releaseBlock(dbuff);
        }
        f.write.unlock();
        return writesize;
    }

    /* returns the size in bytes of the file indicated by DFileID. */
    public int sizeDFile(DFileID dFID) {
        Inode f = inodeMap[dFID.getID()];
        f.read.lock();
        if (f.isUsed()) {
            try {
                return f.getSize();    
            } finally {
                f.read.unlock();
            }
        }
        else {
            f.read.unlock();
            return -1;
        }
    }

    /* 
     * List all the existing DFileIDs in the volume
     */
    public List<DFileID> listAllDFiles() {
        List<DFileID> list = new ArrayList<DFileID>();
        for (Inode f:inodeMap) {
            f.read.lock();
            if (f.isUsed()) {
                list.add(new DFileID(f.getIndex()));
            }
            f.read.unlock();
        }
        return list;
    }

    /* Write back all dirty blocks to the volume, and wait for completion. */
    public void sync() {
        cache.sync();               // ?????????????????????????????
    }
}