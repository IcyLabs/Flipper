package flipper;

public interface FlipperPlugin {
	String getName( );
	
	void activatePlugin( FlipperCore core );
	void deactivatePlugin( FlipperCore core );
}
