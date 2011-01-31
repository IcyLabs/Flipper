package flipper.gui;

import java.util.HashMap;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import flipper.FlipperCore;
import flipper.FlipperDevice;
import flipper.usb.DevicePlugMonitor;

public class FlipperGUI implements DevicePlugMonitor {

	private Shell shell;
	private Composite deviceInfo;
	private Tree deviceList;
	private Label statusBar;
	private Display display;
	
	
	private FlipperCore core;
	
	private FlipperGUI( ) {
		core = new FlipperCore( );
		core.initialise( );
		core.beginThreads( );
		core.addPlugMonitor( this );
	}
	
	public Shell open( Display display_ ) {
		display = display_;
		
		// create new window
		shell = new Shell( display );
		
		// handle quits to clean up flipper core
		shell.addShellListener(new ShellListener() {

			public void shellActivated(ShellEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void shellClosed(ShellEvent arg0) {
				core.cleanup( );
				System.exit(0);
			}

			public void shellDeactivated(ShellEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void shellDeiconified(ShellEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void shellIconified(ShellEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		display.addListener( SWT.Close, new Listener() {

			public void handleEvent(Event arg0) {
				core.cleanup( );
			}
			
		});

		// window title
		shell.setText( "Flipper" );
		
		// 2 column grid layout
		GridLayout mainLayout = new GridLayout( 2, false );
		shell.setLayout( mainLayout );
		
		createDeviceList( );
		createDeviceInfo( );
		createStatusBar( );
		createAppMenu( );
		
		// pack up that window
		shell.pack( );
		shell.setSize( 640, 480 );

		shell.open( );	
		
		return shell;
	}
	
	private void clearDeviceInfo( ) {
		for ( Control control : deviceInfo.getChildren( ) ) {
			control.dispose( );
		}
	}
	
	private void createStatusBar( ) {
		// status bar, for good measure
		statusBar = new Label( shell, SWT.NONE );
		// span by 2 cells
		statusBar.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 2, 1 ) );
		statusBar.setAlignment( SWT.LEFT );
	}

	private void createDeviceList( ) {
		deviceList = new Tree( shell, SWT.BORDER | SWT.V_SCROLL );
		deviceList.setHeaderVisible( true );
		GridData treeData = new GridData( );
		// stretch to fill the cell vertically
		treeData.verticalAlignment = SWT.FILL;
		treeData.grabExcessVerticalSpace = true;
		treeData.minimumWidth = 150;
		//treeData.grabExcessHorizontalSpace = true;
		deviceList.setLayoutData( treeData );
		
		deviceList.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				if ( event.item != null ) {
					statusBar.setText( ((TreeItem)event.item).getText( ) );
					clearDeviceInfo( );
					//DeviceTabFactory.createPenguinoAVRFolder( deviceInfo, SWT.BORDER );
					FlipperDevice device = (FlipperDevice)event.item.getData();
					device.createUserInterface( deviceInfo );
					deviceInfo.layout( );
				} else {
					clearDeviceInfo( );
					DeviceTabFactory.createEmptyFolder( deviceInfo, SWT.BORDER );
					deviceInfo.layout( );
				}
			}
		});
	}
	
	private void createDeviceInfo( ) {
		// A container
		deviceInfo = new Composite( shell, SWT.NONE );
		deviceInfo.setLayout( new GridLayout( 1, false ) );
		GridData deviceData = new GridData( );
		// stretch to fill the cell vertically
		deviceData.verticalAlignment = SWT.FILL;
		deviceData.grabExcessVerticalSpace = true;
		// and horizontally
		deviceData.horizontalAlignment = SWT.FILL;
		deviceData.grabExcessHorizontalSpace = true;
		DeviceTabFactory.createEmptyFolder( deviceInfo, SWT.BORDER );
		deviceInfo.setLayoutData( deviceData );
		
		TreeColumn column = new TreeColumn( deviceList, SWT.LEFT );
		column.setText( "Devices" );
		column.setResizable( false );
		column.setWidth( 150 );
	}
	
	private void createAppMenu( ) {
		// "File" menu
		Menu bar = new Menu( shell, SWT.BAR );
		shell.setMenuBar( bar );
		MenuItem menuItem = new MenuItem( bar, SWT.CASCADE );
		menuItem.setText( "Device" );
		Menu menu = new Menu( bar );
		menuItem.setMenu( menu );
		for (int i = 0; i < 5; i++) {
			MenuItem item = new MenuItem (menu, SWT.PUSH);
			item.setText ("Item " + i);
			item.addArmListener(new ArmListener() {
				public void widgetArmed(ArmEvent e) {
					statusBar.setText(((MenuItem)e.getSource()).getText());
				}
			});
		}
	}
	
	HashMap<FlipperDevice,TreeItem> deviceColumns = new HashMap<FlipperDevice,TreeItem>( );
	
	class DeviceConnectedProxy implements Runnable {
		FlipperDevice target;
		
		public DeviceConnectedProxy( FlipperDevice target ) {
			this.target = target;
		}
		
		public void run() {
			TreeItem treeItem = new TreeItem( deviceList, SWT.NONE );
			String[] itemText = new String[]{ target.toString(), "?" };
			treeItem.setText( itemText );
			
			treeItem.setData( target );
			
			deviceColumns.put( target, treeItem );
		}
	}
	
	public void deviceConnected(FlipperDevice device) {
		display.syncExec( new DeviceConnectedProxy(device) );
	}
	
	class DeviceDisonnectedProxy implements Runnable {
		FlipperDevice target;
		
		public DeviceDisonnectedProxy( FlipperDevice target ) {
			this.target = target;
		}
		
		public void run() {
			deviceColumns.get(target).dispose();
		}
	}

	public void deviceDisconnected(FlipperDevice device) {
		display.syncExec( new DeviceDisonnectedProxy(device) );
	}
	
	public static void main(String[] args) {
		// make the menu at the top say Flipper
		Display.setAppName( "Flipper" );
		
		Display display = new Display( );

		FlipperGUI app = new FlipperGUI( );
		
		Shell shell = app.open( display );
		
		// main loop
		while ( !shell.isDisposed( ) ) {
			// while main window is open
			
			if ( !display.readAndDispatch( ) ) {
				display.sleep( );
			}
		}
		
		display.dispose( );
	}
	
}
