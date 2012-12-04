package dblockcache;

import java.io.IOException;

import common.Constants.DiskOperationType;

import virtualdisk.VirtualDisk;

public class DBufferImpl extends DBuffer {

	private byte[] buffer;
	private boolean busy = false;
	private boolean clean = true;
	private boolean valid = false;
	private int blockID;
	private int size;
	private VirtualDisk vd;
	
	public DBufferImpl(VirtualDisk vd, int blockID, int size){
		this.vd = vd;
		this.blockID = blockID;
		this.size = size;
		buffer = new byte[size];
	}
	
	@Override
	public void startFetch() {
		try {
			vd.startRequest(this, DiskOperationType.READ);
			busy = true;
			valid = false;
			clean = false;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void startPush() {
		try {
			vd.startRequest(this, DiskOperationType.WRITE);
			busy = true;
			valid = false;
			clean = false;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public synchronized boolean checkValid() {
		return valid;
	}

	@Override
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

	@Override
	public synchronized boolean checkClean() {
		return clean;
	}

	@Override
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

	@Override
	public synchronized boolean isBusy() {
		return busy;
	}

	@Override
	public synchronized int read(byte[] buffer, int startOffset, int count) {
		if(startOffset < 0 || startOffset + count < buffer.length || count > this.size)
			return -1;
		if(!valid){
			startFetch();
			waitValid();
		}
		for(int i = 0; i < count; i++){
			buffer[startOffset+i] = this.buffer[i];
		}
		return count;
	}

	@Override
	public synchronized int write(byte[] buffer, int startOffset, int count) {
		if(startOffset < 0 || startOffset + count < buffer.length || count > this.size)
			return -1;
		if(!valid){
			startPush();
			waitClean();
		}
		for(int i = 0; i < count; i++){
			this.buffer[i] = buffer[startOffset+i];
		}
		return count;
	}

	@Override
	public synchronized void ioComplete() {
		busy = false;
		valid = true;
		clean = true;
		notifyAll();
	}

	@Override
	public synchronized int getBlockID() {
		return blockID;
	}

	@Override
	public synchronized byte[] getBuffer() {
		//can simply return buffer?? or need to copy?
		return buffer;
	}

}
