package flipper.jtag;

public class TAPStateTreeNode {
	public TAPState highTMS;
	public TAPState lowTMS;
	
	public TAPStateTreeNode( TAPState high, TAPState low ) {
		highTMS = high;
		lowTMS = low;
	}
}
