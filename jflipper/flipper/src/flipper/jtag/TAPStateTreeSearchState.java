package flipper.jtag;

public class TAPStateTreeSearchState {
	public TAPState[] path;

	public TAPStateTreeSearchState( TAPState state ) {
		path = new TAPState[1];
		path[0] = state;
	}
	
	private TAPStateTreeSearchState( ) {
		
	}

	public TAPStateTreeSearchState fork( TAPState to ) {
		TAPStateTreeSearchState ss = new TAPStateTreeSearchState( );

		ss.path = new TAPState[path.length + 1];

		//Array.Copy( path, 0, ss.path, 0, path.length );
		//ss.path[0..path.length] = path;
		System.arraycopy( path, 0, ss.path, 0, path.length );
		ss.path[path.length] = to;

		return ss;
	}

	public TAPState currentState( ) {
		return path[path.length - 1];
	}
}