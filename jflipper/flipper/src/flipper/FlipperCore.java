package flipper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import flipper.usb.DevicePlugMonitor;
import flipper.usb.USBDeviceManager;

public class FlipperCore {
	ArrayList<FlipperPlugin> loadedPlugins = new ArrayList<FlipperPlugin>();

	ArrayList<FlipperDevice> activeDevices = new ArrayList<FlipperDevice>();
	
	public void initialise() {
		loadPlugins();
	}
	
	public void beginThreads() {
		USBDeviceManager manager = USBDeviceManager.getDeviceManager();
		
		manager.addPlugMonitor(new DevicePlugMonitor() {

			public void deviceConnected(FlipperDevice device) {
				activeDevices.add( device );
			}

			public void deviceDisconnected(FlipperDevice device) {
				activeDevices.remove( device );
			}
			
		});
		
		manager.runAsThread( );
	}
	
	public void cleanup( ) {
		for ( FlipperDevice device : activeDevices ) {
			device.cleanup( );
		}
	}

	private void loadPlugins() {
		File dir = new File( "plugins/" );
		
		File[] files = dir.listFiles();
		
		for ( int i = 0; i < files.length; i++ ) {
			if ( files[i].getName().endsWith(".jar") ) {
				FlipperPlugin plugin = loadPlugin(files[i]);
				if ( plugin != null ) {
					loadedPlugins.add( plugin );
					
					plugin.activatePlugin( this );
				}
			}
		}
	}

	private FlipperPlugin loadPlugin(File jarFile) {
		URL jarURL = null;
		try {
			jarURL = new URL( "jar", "", "file:" + jarFile.getAbsolutePath()+"!/" );
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		URLClassLoader cl = URLClassLoader.newInstance( new URL[] { jarURL } );
		Class<?> loadedClass = null;
		try {
			loadedClass = cl.loadClass( getPluginClassName(jarFile) );
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		
		FlipperPlugin plugin = null;
		try {
			plugin = (FlipperPlugin) loadedClass.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return plugin;
	}
	
	private String getPluginClassName(File jarFile) {
		JarInputStream jar = null;
		try {
			jar = new JarInputStream( new FileInputStream( jarFile.getAbsolutePath() ) );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JarEntry jarEntry;
		
		try {
			while ( (jarEntry = jar.getNextJarEntry()) != null ) {
				if ( jarEntry.getName().endsWith("Plugin.class") ) {
					return jarEntry.getName().replace( "/", "." ).replace( ".class", "" );
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public void addPlugMonitor(DevicePlugMonitor plugMonitor) {
		USBDeviceManager.getDeviceManager().addPlugMonitor( plugMonitor );
	}

}
