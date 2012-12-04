package tests;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Constants;
import common.DFileID;

import dfs.DFS;

public class DFSTest {

	static DFS dfs;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		dfs = new DFS(true);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		dfs.sync();
	}

	@Test
	public void createAndDeleteFile() {
		List<DFileID> before = dfs.listAllDFiles();
		DFileID dfID = dfs.createDFile();
		List<DFileID> after = dfs.listAllDFiles();
		
		System.out.println("List size before file creation: " + before.size() + " after: " + after.size());
		assertFalse(before.contains(dfID));
		assertTrue(after.size() == before.size()+1);
		assertTrue(after.contains(dfID));
		
		dfs.destroyDFile(dfID);
		List<DFileID> postAfter = dfs.listAllDFiles();
		
		assertFalse(postAfter.contains(dfID));
		assertTrue(before.size() == postAfter.size());
		for(DFileID d : before){
			assertTrue(postAfter.contains(d));
		}
		for(DFileID d : postAfter){
			assertTrue(before.contains(d));
		}
	}
	
	@Test
	public void createFileAndWriteData() {
		DFileID file = dfs.createDFile();
		assertEquals(0, dfs.sizeDFile(file));
		int writeSize = 1024;
		
		byte[] writeBuffer = new byte[writeSize];
		for(int i = 0; i < writeSize; i++)
			writeBuffer[i] = (byte) (i*i);		//dummy data
		dfs.write(file, writeBuffer, 0, writeSize);
		byte[] readBuffer = new byte[writeSize];
		dfs.read(file, readBuffer, 0, writeSize);
		for(int i = 0; i < writeSize; i++){
			System.out.println("wrote: " + writeBuffer[i] + " read: " + readBuffer[i]);
		}
		assertTrue(Arrays.equals(readBuffer, writeBuffer));
	}
	
	@Test
	public void testFormat(){
		dfs.format();
		assertEquals(0, dfs.listAllDFiles().size());
	}
	
	@Test
	public void createMultipleFiles(){
		dfs.format();
		for(int i = 0; i < Constants.NUM_OF_INODE; i++){
			assertNotNull(dfs.createDFile());
		}
		assertNull(dfs.createDFile());
		assertEquals(Constants.NUM_OF_INODE, dfs.listAllDFiles().size());
		
		Random r = new Random(12);
		int remainingBlocks = Constants.NUM_OF_BLOCKS - (Constants.INODE_SIZE * Constants.NUM_OF_INODE) / Constants.BLOCK_SIZE;
		
		HashMap<Integer, byte[]> buffers = new HashMap<Integer,byte[]>();
		
		for(DFileID file : dfs.listAllDFiles()){
			byte[] writeBuffer = new byte[Math.abs(r.nextInt()) % (Constants.NUM_OF_BLOCK_IN_DFILE*Constants.BLOCK_SIZE)];
			remainingBlocks -= (int) Math.ceil((double)writeBuffer.length / Constants.BLOCK_SIZE);
			for(int i = 0; i < writeBuffer.length; i++){
				writeBuffer[i] = (byte) (i*i+file.getID());
			}
			int count = dfs.write(file, writeBuffer, 0, writeBuffer.length);
			if(remainingBlocks >= 0){
				assertEquals(writeBuffer.length, count);
				assertEquals(writeBuffer.length, dfs.sizeDFile(file));
				buffers.put(file.getID(), writeBuffer);
				System.out.println("Wrote " + writeBuffer.length + " bytes to file " + file.getID() + " with remaining blocks: " + remainingBlocks);
			}
			else{
				assertEquals(-1, count);
				System.out.println("Failed to write to file " + file.getID() + " remaining blocks: " + remainingBlocks);
				break;
			}
		}
		
		for(DFileID file : dfs.listAllDFiles()){
			if(!buffers.containsKey(file.getID()))
				continue;
			System.out.println("Starting to read file " + file.getID());
			byte[] writeBuffer = buffers.get(file.getID());
			byte[] readBuffer = new byte[writeBuffer.length];
			assertEquals(writeBuffer.length, dfs.read(file, readBuffer, 0, writeBuffer.length));
			assertTrue(Arrays.equals(writeBuffer, readBuffer));
			System.out.println("Read file " + file.getID() + " successfully");
		}
	}
	
	@Test
	public void createAndDeleteMultipleFiles(){
		dfs.format();
		Random r = new Random(123);
		HashMap<Integer, byte[]> buffers = new HashMap<Integer,byte[]>();
		for(int i = 0; i < 3000; i++){
			int fileCount = dfs.listAllDFiles().size();
			int rand = r.nextInt(4);
			if(fileCount == Constants.NUM_OF_INODE || (rand == 0 && fileCount > 0)){
				deleteRandomFile(r,buffers);
			}else if(rand == 1 && fileCount>0){
				checkRandomFile(r, buffers);
			}
			else{
				putRandomFile(r,buffers);
			}
		}
	}

	private void checkRandomFile(Random r, HashMap<Integer, byte[]> buffers) {
		List<DFileID> dfiles = dfs.listAllDFiles();
		DFileID file = dfiles.get(r.nextInt(dfiles.size()));
		
		byte[] writeBuffer = buffers.get(file.getID());
		byte[] readBuffer = new byte[writeBuffer.length];
		assertEquals(writeBuffer.length, dfs.read(file, readBuffer, 0, writeBuffer.length));
		assertTrue(Arrays.equals(writeBuffer, readBuffer));
		System.out.println("Checked file " + file.getID() + " successfully");
	}

	private void putRandomFile(Random r, HashMap<Integer, byte[]> buffers) {
		DFileID file = dfs.createDFile();
		byte[] writeBuffer = new byte[r.nextInt(Constants.NUM_OF_BLOCK_IN_DFILE*Constants.BLOCK_SIZE)];
		//remainingBlocks -= (int) Math.ceil((double)writeBuffer.length / Constants.BLOCK_SIZE);
		for(int i = 0; i < writeBuffer.length; i++){
			writeBuffer[i] = (byte) (i*i+file.getID());
		}
		int count = dfs.write(file, writeBuffer, 0, writeBuffer.length);
		//if(remainingBlocks >= 0){
			assertEquals(writeBuffer.length, count);
			assertEquals(writeBuffer.length, dfs.sizeDFile(file));
			buffers.put(file.getID(), writeBuffer);
			System.out.println("Wrote " + writeBuffer.length + " bytes to file " + file.getID() );//+ " with remaining blocks: " + remainingBlocks);
		//}
			byte[] readBuffer = new byte[writeBuffer.length];
			assertEquals(writeBuffer.length, dfs.read(file, readBuffer, 0, writeBuffer.length));
			assertTrue(Arrays.equals(writeBuffer, readBuffer));
			System.out.println("Read file " + file.getID() + " successfully");
	}

	private void deleteRandomFile(Random r, HashMap<Integer, byte[]> buffers) {
		List<DFileID> dfiles = dfs.listAllDFiles();
		DFileID delCandidate = dfiles.get(r.nextInt(dfiles.size()));
		dfs.destroyDFile(delCandidate);
		assertTrue(dfiles.size() == dfs.listAllDFiles().size() + 1);
		assertFalse(dfs.listAllDFiles().contains(delCandidate));
	}
	
    @Test
    public void testMultipleThread() {
        dfs.format();
        int threadNum = 100;
        int fileNum = Constants.NUM_OF_INODE / threadNum;
        Thread[] threads = new Thread[threadNum];
        for (int i=0; i<threadNum; i++) {
            threads[i] = new Thread(new TestThread(i, fileNum, dfs));
            threads[i].start();
        }

        for (Thread t:threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class TestThread implements Runnable {
    private int fileNum;
    private int ID;
    private DFS dfs;
    HashMap<Integer, byte[]> buffers = new HashMap<Integer,byte[]>();
    DFileID[] fileIDs;

    public TestThread(int ID, int fileNum, DFS dfs) {
        this.ID = ID;
        this.fileNum = fileNum;
        this.dfs = dfs;
        
        fileIDs = new DFileID[fileNum];
    }

    @Override
    public void run() {
        Random r = new Random(ID * 100);
        
        for (int k=0; k<fileNum; k++) {
            DFileID file = dfs.createDFile();
            fileIDs[k] = file;
            byte[] writeBuffer = new byte[Math.abs(r.nextInt()) % (Constants.NUM_OF_BLOCK_IN_DFILE*Constants.BLOCK_SIZE)];
            for(int i = 0; i < writeBuffer.length; i++){
                writeBuffer[i] = (byte) (i*i + k * ID);
            }
            int count = dfs.write(fileIDs[k], writeBuffer, 0, writeBuffer.length);
            if (count > 0){
                assertEquals(writeBuffer.length, count);
                buffers.put(file.getID(), writeBuffer);
                System.out.println("Thread " + this.ID + " wrote " + writeBuffer.length + "bytes to file " + file.getID());
            }
            else{
                System.out.println("Thread " + ID + " failed to write to file " + file.getID());
                break;
            }
        }
        
        for (int k=0; k<fileNum; k++) {
            DFileID file = fileIDs[k];
            System.out.println("Thread " + ID + " starting to read file " + file.getID());
            byte[] writeBuffer = buffers.get(file.getID());
            byte[] readBuffer = new byte[writeBuffer.length];
            assertEquals(writeBuffer.length, dfs.read(file, readBuffer, 0, writeBuffer.length));
            assertTrue(Arrays.equals(writeBuffer, readBuffer));
            System.out.println("Thread " + ID + " read file " + file.getID() + " successfully");
        }
    }
}
