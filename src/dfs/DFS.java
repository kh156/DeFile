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
    private Inode[] dfileMap;
    private boolean[] blockMap;

    DFS(String volName, boolean format) {
        _volName = volName;
        _format = format;
        cache = new DBufferCache(Constants.CACHE_SIZE);
        dfileMap = new Inode[Constants.NUM_OF_INODE];
        blockMap = new boolean[Constants.NUM_OF_BLOCKS];        // lock on the map????????????

        initializeDFile();
    }

    DFS(boolean format) {
        this(Constants.vdiskName,format);
    }

    DFS() {
        this(Constants.vdiskName,false);
    }

    public void initializeDFile() {

    }

    /*
     * If format is true, the system should format the underlying disk contents,
     * i.e., initialize to empty. On success returns true, else return false.
     */
    public boolean format() {
        if (_format) {
            Arrays.fill(blockMap, false);
            for (Inode f:dfileMap) {
                f.clearContent();
                f.setUsed(false);
                f.updateDisk();                // need optimized!!!!!!!!!!!!
            }
            return true;
        }
        else {
            return false;
        }
    }

    /* creates a new DFile and returns the DFileID, which is useful to uniquely identify the DFile*/
    public DFileID createDFile() {
        for (Inode f:dfileMap) {
            if (!f.isUsed()) {
                f.clearContent();
                f.setUsed(true);
                f.updateDisk();              
                return new DFileID(f.getIndex());
            }
        }
        return null;
    }

    /* destroys the file specified by the DFileID */
    public void destroyDFile(DFileID dFID) {
        Inode f = dfileMap[dFID.getID()];
        for (int i:f.getBlockList()) {
            blockMap[i] = false;
        }
        f.clearContent();
        f.setUsed(false);
        f.updateDisk();
    }

    /*
     * reads the file dfile named by DFileID into the buffer starting from the
     * buffer offset startOffset; at most count bytes are transferred
     */
    public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
        Inode dfile = dfileMap[dFID.getID()];
        if (!dfile.isUsed()) return -1;
        
        int readsize = Math.min(dfile.getSize(), count);
        List<Integer> blocks = dfile.getBlockList();
        for (int i=0; i<blocks.size(); i++) {
            DBuffer dbuff = cache.getBlock(blocks.get(i));
            if (dbuff.read(buffer, startOffset + i*Constants.BLOCK_SIZE, readsize - i*Constants.BLOCK_SIZE) == -1) {
                return -1;
            }
        }
        return readsize;
    }

    /*
     * writes to the file specified by DFileID from the buffer starting from the
     * buffer offset startOffsetl at most count bytes are transferred
     */
    public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
        Inode dfile = dfileMap[dFID.getID()];
        if (!dfile.isUsed()) return -1;
        
        // clear used blocks first
        for (int i:dfile.getBlockList()) {
            blockMap[i] = false;
        }
        dfile.clearContent();
        
        int writesize = Math.min(buffer.length-startOffset, count);
        int numBlocks = writesize / Constants.BLOCK_SIZE;
        
        // find free blocks
        int head = 0;
        for (int i=0; i<numBlocks; i++) {
            while (head<blockMap.length && blockMap[head] == true) {
                head++;
            }
            if (head >= blockMap.length) {
                return -1;
            }
            blockMap[head] = true;
            dfile.addBlock(head);
        }
        
        // set DFile size
        dfile.setSize(writesize);
        
        // update inode
        dfile.updateDisk();

        List<Integer> blocks = dfile.getBlockList();
        for (int i=0; i<blocks.size(); i++) {
            DBuffer dbuff = cache.getBlock(blocks.get(i));
            if (dbuff.write(buffer, startOffset + i*Constants.BLOCK_SIZE, writesize - i*Constants.BLOCK_SIZE) == -1) {
                return -1;
            }
        }
        return writesize;
    }

    /* returns the size in bytes of the file indicated by DFileID. */
    public int sizeDFile(DFileID dFID) {
        if (dfileMap[dFID.getID()].isUsed()) {
            return dfileMap[dFID.getID()].getSize();    
        }
        else {
            return -1;
        }
    }

    /* 
     * List all the existing DFileIDs in the volume
     */
    public List<DFileID> listAllDFiles() {
        List<DFileID> list = new ArrayList<DFileID>();
        for (int i=0; i<dfileMap.length; i++) {
            if (dfileMap[i].isUsed()) {
                list.add(new DFileID(i));
            }
        }
        return list;
    }
    
    /* Write back all dirty blocks to the volume, and wait for completion. */
    public void sync() {
        cache.sync();               // ?????????????????????????????
    }
}