package flipper.targets.penguino;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

public class StatusDisplay {
	private ProgressBar progressBar;
	private Label statusBox;
	
	public StatusDisplay( ProgressBar progressBar, Label statusBox ) {
		this.progressBar = progressBar;
		this.statusBox = statusBox;
	}
	
	public void setStatus( String status ) {
		this.statusBox.setText( status );
		while (Display.getCurrent().readAndDispatch());
	}
	
	public void setProgress( int current, int maximum ) {
		this.progressBar.setMinimum( 0 );
		this.progressBar.setMaximum( maximum );
		this.progressBar.setSelection( current );
		while (Display.getCurrent().readAndDispatch());
	}

	public void setIndeterminate(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setAnimating(boolean b) {
		// TODO Auto-generated method stub
		
	}
}
