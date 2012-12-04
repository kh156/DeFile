package dfs;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import common.Constants;

public class Inode implements Serializable{
	
	private static final long serialVersionUID = -2718967990195874435L;
	
	private int index;                      // Need set lock on these variables??????????????
    private List<Integer> blockList;
    private int size;
    private boolean isUsed;
    
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    public final Lock read  = readWriteLock.readLock();
    public final Lock write = readWriteLock.writeLock();
    
    public Inode(int index) {
        this.index = index;
        blockList = new ArrayList<Integer>();
        size = 0;
        isUsed = false;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean addBlock(int b) {
        if (blockList.size() < Constants.NUM_OF_BLOCK_IN_DFILE) {
            blockList.add(b);
            return true;
        }
        else {
            return false;
        }
    }
    
    public List<Integer> getBlockList() {
        return blockList;
    }

    public void clearContent() {
        blockList.clear();
        size = 0;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public boolean isUsed() {
        return isUsed;
    }
    
    public void setUsed(boolean f) {
        isUsed = f;
    }
    
    public void initializeFromDisk() {
        
    }
    
    public void updateDisk() {
    
    }
    
}
