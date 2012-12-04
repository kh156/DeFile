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
		dfs = new DFS();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void createAndDeleteFile() {
		List<DFileID> before = dfs.listAllDFiles();
		DFileID dfID = dfs.createDFile();
		List<DFileID> after = dfs.listAllDFiles();
		
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
	    DFileID file0 = dfs.createDFile();
	    System.out.println(file0.getID());
		DFileID file = dfs.createDFile();
		System.out.println(file.getID());
		assertEquals(dfs.sizeDFile(file),0);
		
		int size = Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCK_IN_DFILE;
		byte[] writeBuffer = new byte[size];
		byte[] readBuffer = new byte[size];
		for(int i = 0; i < size; i++)
			writeBuffer[i] = (byte) (i*i);		//dummy data
		dfs.write(file, writeBuffer, 0, size);
		dfs.read(file, readBuffer, 0, size);
		for(int i = 0; i < size; i++){
//			System.out.println("wrote: " + writeBuffer[i] + " read: " + readBuffer[i]);
		}
		assertTrue(Arrays.equals(readBuffer, writeBuffer));
	}
	

}
