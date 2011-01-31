package flipper.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class DeviceTabFactory {
	
	public static TabFolder createEmptyFolder( Composite parent, int style ) {
		TabFolder folder = new TabFolder( parent, style );
		updateLayoutData( folder );
		return folder;
	}
	
	private static void updateLayoutData( TabFolder folder ) {
		GridData deviceData = new GridData( );
		// stretch to fill the cell vertically
		deviceData.verticalAlignment = SWT.FILL;
		deviceData.grabExcessVerticalSpace = true;
		// and horizontally
		deviceData.horizontalAlignment = SWT.FILL;
		deviceData.grabExcessHorizontalSpace = true;
		
		folder.setLayoutData( deviceData );
	}
}
