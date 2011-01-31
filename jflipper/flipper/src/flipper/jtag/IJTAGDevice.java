package flipper.jtag;

public interface IJTAGDevice {
	void sendJTAGReset( boolean systemReset, boolean testReset );
	TAPResponse sendJTAGCommand( TAPCommand command );
}
