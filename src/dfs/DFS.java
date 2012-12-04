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


    public DFS(String volName, boolean format) {
        _volName = volName;
        _format = format;
        cache = new DBufferCache(_volName, format, Constants.CACHE_SIZE);
        inodeMap = new Inode[Constants.NUM_OF_INODE];
        usedBlockMap = new boolean[Constants.NUM_OF_BLOCKS];        // lock on the map????????????

        for (int i=0; i<inodeMap.length; i++) {
            inodeMap[i] = new Inode(i);
        }
        if (format) {
            format();
        }
        else {
            initializeAllInodes();
        }
    }

    public DFS(boolean format) {
        this(Constants.vdiskName,format);
    }

    public DFS() {
        this(Constants.vdiskName,false);
    }

    private void initializeAllInodes() {
        for (int i=0; i<inodeMap.length; i++) {
            int blockID = i / (Constants.BLOCK_SIZE/Constants.INODE_SIZE);
            int inodeOffset = i % (Constants.BLOCK_SIZE/Constants.INODE_SIZE);
            byte[] buffer = new byte[Constants.BLOCK_SIZE];
            DBuffer dbuf = cache.getBlock(blockID);
            dbuf.read(buffer, 0, Constants.BLOCK_SIZE);
            cache.releaseBlock(dbuf);

            inodeMap[i].initializeFromSerializedMetadata(buffer, inodeOffset*Constants.INODE_SIZE, Constants.INODE_SIZE);

            if(inodeMap[i].isUsed()){
                for(int k : inodeMap[i].getBlockList()){
                    if(usedBlockMap[k])
                        System.err.println("A block is being used by two different inodes");
                    else
                        usedBlockMap[k] = true;
                }
            }
        }
    }

    private void updateInode(Inode f) {
        int index = f.getIndex();
        int blockID = index / (Constants.BLOCK_SIZE/Constants.INODE_SIZE);
        int inodeOffset = index % (Constants.BLOCK_SIZE/Constants.INODE_SIZE);
        byte[] buffer = new byte[Constants.BLOCK_SIZE];
        byte[] metadata = f.getSerializedMetadata();

        DBuffer dbuf = cache.getBlock(blockID);
        dbuf.read(buffer, 0, Constants.BLOCK_SIZE);
        for(int i = 0 ; i < metadata.length; i++){
            buffer[inodeOffset*Constants.INODE_SIZE + i] = metadata[i];
        }
        dbuf.write(buffer, 0, Constants.BLOCK_SIZE);
        cache.releaseBlock(dbuf);
    }

    private void markInodeRegionAsUsed() {
		for(int i = 0; i < (Constants.NUM_OF_INODE * Constants.INODE_SIZE) / Constants.BLOCK_SIZE; i++){
			usedBlockMap[i] = true;
		}
	}

	/*
     * If format is true, the system should format the underlying disk contents,
     * i.e., initialize to empty. On success returns true, else return false.
     */
    public synchronized boolean format() {
    	for (Inode f:inodeMap) {
    		f.write.lock();
    	}
    	synchronized(usedBlockMap) {
    		Arrays.fill(usedBlockMap, false);
    		markInodeRegionAsUsed();
    	}
    	
    	for (Inode f:inodeMap) {
    		f.clearContent();
    		f.setUsed(false);
    		updateInode(f);              // need optimized!!!!!!!!!!!!
    	}
    	for (Inode f:inodeMap) {
    		f.write.unlock();
    	}
    	return true;
    }

    /* creates a new DFile and returns the DFileID, which is useful to uniquely identify the DFile*/
    public DFileID createDFile() {
        for (Inode f:inodeMap) {
            f.write.lock();
            if (!f.isUsed()) {
                f.clearContent();
                f.setUsed(true);
                updateInode(f);          
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
        updateInode(f);
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
            if (dbuff.read(buffer, startOffset + i*Constants.BLOCK_SIZE, Math.min(readsize - i*Constants.BLOCK_SIZE, Constants.BLOCK_SIZE)) == -1) {
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
        int head = Constants.NUM_OF_INODE / (Constants.BLOCK_SIZE / Constants.INODE_SIZE);
        synchronized(usedBlockMap) {
            for (int i = 0; i<numBlocks; i++) {
                while (head<usedBlockMap.length && usedBlockMap[head] == true) {
                    head++;
                }
                if (head >= usedBlockMap.length) {
                    f.write.unlock();
                    return -1;
                }
                //                System.out.println("head = " + head);
                usedBlockMap[head] = true;
                f.addBlock(head);
            }
        }

        // set DFile size
        f.setSize(writesize);

        // update inode region on disk
        updateInode(f);



        List<Integer> blocks = f.getBlockList();

        //        System.out.println("block size = " + blocks.size());

        for (int i=0; i<blocks.size(); i++) {
//            System.out.println("Here: " + i);

            DBuffer dbuff = cache.getBlock(blocks.get(i));
            if (dbuff.write(buffer, startOffset + i*Constants.BLOCK_SIZE, Math.min(writesize - i*Constants.BLOCK_SIZE, Constants.BLOCK_SIZE)) == -1) {
                //                System.out.println("Here");

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
        for(Inode f: inodeMap)
            updateInode(f);
        cache.sync();               // ?????????????????????????????
    }
}