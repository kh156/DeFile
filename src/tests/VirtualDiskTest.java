package tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import virtualdisk.VirtualDisk;

import dfs.DFS;

public class VirtualDiskTest {

	static VirtualDisk vd;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		vd = new VirtualDisk("testVolume.dat", true);
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
	
}
