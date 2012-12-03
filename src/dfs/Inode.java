package dfs;

import java.util.*;

import common.Constants;

public class Inode {

    private int index;                      // Need set lock on these variables??????????????
    private List<Integer> blockList;
    private int size;
    private boolean isUsed;
    
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
    
    public void updateDisk() {
    
    }
    
}
