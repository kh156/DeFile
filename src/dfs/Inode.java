package dfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import common.Constants;

public class Inode{
	
	/**
	 * 
	 */
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
    
    public void initializeFromSerializedMetadata(byte[] buf, int offset, int length) {
    	ByteArrayInputStream bin = new ByteArrayInputStream(buf,offset,length);
        DataInputStream dis = new DataInputStream(bin);
        clearContent();
    	try{
    		int blockListSize = dis.readInt();
    		size = dis.readInt();
    		for(int i = 0; i < blockListSize; i++){
    			blockList.add(dis.readInt());
    		}
    		dis.close();
    	} catch (IOException e){
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	return;
    }
    
    public byte[] getSerializedMetadata() {
    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	DataOutputStream dos = new DataOutputStream(bout);
    	try {
    		dos.writeInt(blockList.size());
			dos.writeInt(size);
			for(Integer blockID : blockList)
				dos.writeInt(blockID);
			dos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return bout.toByteArray();
    }
    
}
