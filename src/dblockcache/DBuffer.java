package dblockcache;

import java.io.IOException;

import common.Constants.DiskOperationType;

import virtualdisk.VirtualDisk;

public class DBuffer {

    private byte[] buffer;
    private boolean busy = false;
    private boolean clean = true;
    private boolean valid = false;
    private int blockID;
    private int size;
    private VirtualDisk vd;

    public DBuffer(VirtualDisk vd, int blockID, int size){
        this.vd = vd;
        this.blockID = blockID;
        this.size = size;
        buffer = new byte[size];
    }

    private void startFetch() {
        try {
            vd.startRequest(this, DiskOperationType.READ);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void startPush() {
        try {
            vd.startRequest(this, DiskOperationType.WRITE);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void evict() {
        if (this.clean = false) {
            startPush();
            this.waitClean();
        }
    }

    public boolean checkValid() {
        return valid;
    }

    public synchronized boolean waitValid() {
        while(valid == false){
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return valid;
    }

    public synchronized void setValid(boolean v) {
        valid = v;
        if (v = true) {
            notifyAll();
        }
    }

    public boolean checkClean() {
        return clean;
    }

    public synchronized boolean waitClean() {
        while(clean == false){
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return clean;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean b) {
        busy = b;
    }

    public int read(byte[] buffer, int startOffset, int count) {
        if(startOffset < 0 || startOffset + count > buffer.length || count > this.size)
            return -1;
        if (!valid) {
            startFetch();
            waitValid();
        }
        for(int i = 0; i < count; i++){
            buffer[startOffset+i] = this.buffer[i];
        }
        return count;
    }

    public int write(byte[] buffer, int startOffset, int count) {
        if(startOffset < 0 || startOffset + count > buffer.length || count > this.size)
            return -1;
        for(int i = 0; i < count; i++){
            this.buffer[i] = buffer[startOffset+i];
        }
        // erase the rest bytes
        for (int i=count; i<this.buffer.length; i++) {
            this.buffer[i] = 0; 
        }
        
        // mark valid!!!!
        this.valid = true;
        // mark dirty!!!!
        this.clean = false;
        return count;
    }

    public synchronized void ioComplete() {
        clean = true;
        valid = true;
        notifyAll();
    }

    public int getBlockID() {
        return blockID;
    }

    public void setBlockID(int id) {
        blockID = id;
    }

    public byte[] getBuffer() {
        //can simply return buffer?? or need to copy?
        return buffer;
    }

}
