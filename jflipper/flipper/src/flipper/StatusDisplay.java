package flipper;

public interface StatusDisplay {
	public void setStatus( String status );
	
	public void setProgress( int current, int maximum );

	public void setIndeterminate( boolean b );

	public void setAnimating( boolean b );
}
