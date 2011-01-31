package flipper.targets.penguino;

import java.util.HashMap;

import flipper.jtag.TAPStateMachine;

public abstract class Chip {
	@SuppressWarnings("unused")
	protected TAPStateMachine tapState;
	private HashMap<String,Memory> memory = new HashMap<String,Memory>( );
	
	public Chip( TAPStateMachine sm ) {
		this.tapState = sm;
	}
	
	public void addMemory( String name, Memory inst ) {
		memory.put( name, inst );
	}
	
	public Memory getMemory( String name ) {
		if ( memory.containsKey(name) ) {
			return memory.get( name );
		}
		
		throw new Error( "Invalid memory specified" );
	}
	
	public abstract void showInformation( );
}
