package flipper;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

public abstract class FlipperDevice {
	public abstract String toString( );

	public abstract void cleanup();
	
	public abstract TabFolder createUserInterface( Composite parent );
}
