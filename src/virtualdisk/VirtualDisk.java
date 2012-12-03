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

import common.Constants;
import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

public class VirtualDisk implements IVirtualDisk {

	private String _volName;
	private RandomAccessFile _file;
	private int _maxVolSize;

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
		vdr.start();
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
	}
	
	public class VirtualDiskRequest implements Runnable{

		DBuffer buf;
		DiskOperationType operation;
		
		public VirtualDiskRequest(DBuffer buf, DiskOperationType operation){
			this.buf = buf;
			this.operation = operation;
		}
		
		public void start(){
			Thread t = new Thread(this);
			t.start();
		}
		
		@Override
		public void run(){
			try{
			switch(operation){
			case READ:
				readBlock(buf);
			case WRITE:
				writeBlock(buf);
			}
			}
			catch(IOException ex){
				//TODO: fill this in
			}
		}
		
	}
}