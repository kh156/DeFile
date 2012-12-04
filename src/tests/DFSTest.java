package tests;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
	
	

}
