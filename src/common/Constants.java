package common;
/*
 * This class contains the global constants used in DFS
 */

public class Constants {

	public static final int NUM_OF_BLOCKS = 16384; // 2^14
	public static final int BLOCK_SIZE = 1024; // 1kB
	
	public static final int CACHE_SIZE = 16; 
	public static final int NUM_OF_INODE = 512;
	public static final int INODE_SIZE = 128;
    public static final int NUM_OF_BLOCK_IN_DFILE = 50;
	
	
	/* DStore Operation types */
	public enum DiskOperationType {
		READ, WRITE
	};

	/* Virtual disk file/store name */
	public static final String vdiskName = "DSTORE.dat";
}