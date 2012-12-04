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

    public void startFetch() {
        try {
            clean = false;
            vd.startRequest(this, DiskOperationType.READ);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.waitClean();
    }

    public void startPush() {
        try {
            clean = false;
            vd.startRequest(this, DiskOperationType.WRITE);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.waitClean();
    }

    public synchronized boolean checkValid() {
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
        notifyAll();
    }

    public synchronized boolean checkClean() {
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

    public synchronized boolean isBusy() {
        return busy;
    }

    public synchronized void setBusy(boolean b) {
        busy = b;
    }

    public synchronized int read(byte[] buffer, int startOffset, int count) {
        if(startOffset < 0 || startOffset + count < buffer.length || count > this.size)
            return -1;
        if (!valid) {
            startFetch();
        }
        for(int i = 0; i < count; i++){
            buffer[startOffset+i] = this.buffer[i];
        }
        return count;
    }

    public synchronized int write(byte[] buffer, int startOffset, int count) {
        if(startOffset < 0 || startOffset + count < buffer.length || count > this.size)
            return -1;
        if (!valid) {
            startPush();
        }
        for(int i = 0; i < count; i++){
            this.buffer[i] = buffer[startOffset+i];
        }
        return count;
    }

    public synchronized void ioComplete() {
        clean = true;
        notifyAll();
    }

    public synchronized int getBlockID() {
        return blockID;
    }

    public synchronized void setBlockID(int id) {
        blockID = id;
    }

    public synchronized byte[] getBuffer() {
        //can simply return buffer?? or need to copy?
        return buffer;
    }

}
