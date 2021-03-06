package virtualdisk;
/*
 * VirtualDisk.java
 *
 * A virtual asynchronous disk.
 *
 */

import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import common.Constants;
import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

public class VirtualDisk implements IVirtualDisk, Runnable {

    private String _volName;
    private RandomAccessFile _file;
    private int _maxVolSize;
    private Queue<VirtualDiskRequest> queue = new LinkedList<VirtualDiskRequest>();
    private Object requestPoolLock = new Object();
    private Thread requestManager;
    /*
     * VirtualDisk Constructors
     */
    public VirtualDisk(String volName, boolean format) throws FileNotFoundException,
    IOException {

        _volName = volName;
        _maxVolSize = Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS;

        /*
         * mode: rws => Open for reading and writing, as with "rw", and also
         * require that every update to the file's content or metadata be
         * written synchronously to the underlying storage device.
         */
        _file = new RandomAccessFile(_volName, "rws");

        /*
         * Set the length of the file to be NUM_OF_BLOCKS with each block of
         * size BLOCK_SIZE. setLength internally invokes ftruncate(2) syscall to
         * set the length.
         */
        _file.setLength(Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS);
        if(format) {
            formatStore();
        }
        /* Other methods as required */
        
        requestManager = new Thread(this);
        requestManager.start();
    }

    public VirtualDisk(boolean format) throws FileNotFoundException,
    IOException {
        this(Constants.vdiskName, format);
    }

    public VirtualDisk() throws FileNotFoundException,
    IOException {
        this(Constants.vdiskName, false);
    }

    /*
     * Start an asynchronous request to the underlying device/disk/volume. 
     * -- buf is an DBuffer object that needs to be read/write from/to the volume.	
     * -- operation is either READ or WRITE  
     */
    public void startRequest(DBuffer buf, DiskOperationType operation) throws IllegalArgumentException,
    IOException{
        VirtualDiskRequest vdr = new VirtualDiskRequest(buf,operation);
        enqueue(vdr);
    }

    private void enqueue(VirtualDiskRequest vdr) {
        synchronized(requestPoolLock){
            queue.add(vdr);
            requestPoolLock.notify();
        }
    }

    /*
     * Clear the contents of the disk by writing 0s to it
     */
    private void formatStore() {
        byte b[] = new byte[Constants.BLOCK_SIZE];
        setBuffer((byte) 0, b, Constants.BLOCK_SIZE);
        for (int i = 0; i < Constants.NUM_OF_BLOCKS; i++) {
            try {
                int seekLen = i * Constants.BLOCK_SIZE;
                _file.seek(seekLen);
                _file.write(b, 0, Constants.BLOCK_SIZE);
            } catch (Exception e) {
                System.out
                .println("Error in format: WRITE operation failed at the device block "
                        + i);
            }
        }
    }

    /*
     * helper function: setBuffer
     */
    private static void setBuffer(byte value, byte b[], int bufSize) {
        for (int i = 0; i < bufSize; i++) {
            b[i] = value;
        }
    }

    /*
     * Reads the buffer associated with DBuffer to the underlying
     * device/disk/volume
     */
    private int readBlock(DBuffer buf) throws IOException {
        int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
        /* Boundary check */
        if (_maxVolSize < seekLen + Constants.BLOCK_SIZE) {
            return -1;
        }
        _file.seek(seekLen);
//        System.err.println("read");

        return _file.read(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
    }

    /*
     * Writes the buffer associated with DBuffer to the underlying
     * device/disk/volume
     */
    private void writeBlock(DBuffer buf) throws IOException {
        int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
        _file.seek(seekLen);
        _file.write(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
//        System.err.println("wrote");
    }

    @Override
    public void run() {
        while(true){
            VirtualDiskRequest vdr = null;
            synchronized(requestPoolLock){
                while (queue.isEmpty()) {
                    try {
                        requestPoolLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                vdr = queue.poll();
            }
            if(vdr != null)
            	commitRequest(vdr);
        }
    }

    private void commitRequest(VirtualDiskRequest vdr) {
        try{
            switch(vdr.operation){
            case READ:
                readBlock(vdr.buf);
                break;
            case WRITE:
                writeBlock(vdr.buf);
                break;
            }
            vdr.buf.ioComplete();
        }catch(IOException ex){
            //TODO do something
        }
    }

    public class VirtualDiskRequest{

        DBuffer buf;
        DiskOperationType operation;

        public VirtualDiskRequest(DBuffer buf, DiskOperationType operation){
            this.buf = buf;
            this.operation = operation;
        }
    }
}