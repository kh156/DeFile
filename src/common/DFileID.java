package common;

/* typedef DFileID to int */
public class DFileID {
	private int _dFID;
	public DFileID(int dFID) {
		_dFID = dFID;
	}
	
	public int getID() {
	    return _dFID;
	}
	
	@Override
	public boolean equals(Object o){
		if(o.getClass() != getClass())
			return false;
		return _dFID == ((DFileID) o)._dFID;
	}
}
