package flipper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public class ProgramBinary {
	
	class ProgramChunk {
		public int offset;
		public byte[] bytes;
		
		public ProgramChunk( int o, byte[] b ) {
			offset = o;
			bytes = b;
		}
	}
	
	private ProgramChunk chunk;

	public ProgramBinary(int offset, byte[] bytes) {
		chunk = new ProgramChunk( offset, bytes );
	}

	public static ProgramBinary loadFromPath(String filename) {
		if ( filename.endsWith( ".bin" ) ) {
			return loadFileBinary( filename );
		} else if ( filename.endsWith( ".hex" ) ) {
			return loadFileIHex( filename );
		} else {
			return null;
		}
	}

	public int getTotalBytes() {
		return chunk.bytes.length;
	}

	public int getProgramStartIndex() {
		return chunk.offset;
	}

	public byte[] getBytes(int start, int end) {
		int offset = chunk.offset;
		
		int startRelative = start - offset;
		int endRelative = end - offset;
		
		if ( endRelative > chunk.bytes.length )
			endRelative = chunk.bytes.length;
		
		int numBytes = endRelative - startRelative; 
		
		byte[] bytes = new byte[numBytes];
		
		for ( int i = 0; i < numBytes; i++ ) {
			bytes[i] = chunk.bytes[startRelative+i];
		}
		
		return bytes;
	}
	
	
	// http://www.java-tips.org/java-se-tips/java.io/reading-a-file-into-a-byte-array.html
	private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();
    
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
    
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
	
	private static ProgramBinary loadFileBinary( String filename ) {
		try {
			return new ProgramBinary( 0, getBytesFromFile( new File(filename) ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private static byte[] hexToBytes( String hex ) {
		byte[] ret = new byte[hex.length() / 2];
		
		for ( int i = 0; i < ret.length; i++ ) {
			ret[i] = (byte) Integer.parseInt( hex.substring( i*2, (i+1)*2 ), 16 );
		}
		
		return ret;
	}
	
	private static ProgramBinary loadFileIHex( String filename ) {
		FileReader fileReader;
		BufferedReader reader;
		try {
			fileReader = new FileReader( filename );
			reader = new BufferedReader( fileReader);
			
			int offset = -1;
			
			byte[] totalBytes = new byte[1024*1024];
			int byteOffset = 0;
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				if ( line.length() < 5 )
					continue;
				
				//String byteCount = line.substring(1, 3);
				String address = line.substring(3, 7);
				String recordType = line.substring(7, 9);
				String data = line.substring(9, line.length()-2);
				//String checksum = line.substring(line.length()-2, line.length());
				
				// TODO: checksum?
				short addr = (short) Integer.parseInt( address, 16 );
				byte[] bytes = hexToBytes( data );
				
				if ( recordType == "00" ) {
					System.out.printf( "Read in %d bytes at offset 0x%x (%d), currently read %d (%d)\n", bytes.length, address, addr, totalBytes.length, offset+totalBytes.length );
					
					if ( offset == -1 ) {
						offset = addr;
					} else {
						// make sure regions are consecutive
						if ( offset+totalBytes.length != addr ) {
							throw new Error( "Regions must be consecutive" );
						}
					}
					
					for ( int i = 0; i < bytes.length; i++ ) {
						totalBytes[byteOffset] = bytes[i];
						byteOffset++;
					}
					//totalBytes ~= bytes;
				}
			}
			
			System.out.printf( "Read complete. %d bytes total at offset %d.\n", byteOffset, offset );
			
			byte[] finalBytes = new byte[byteOffset];
			
			for ( int i = 0; i < byteOffset; i++ ) {
				finalBytes[i] = totalBytes[i];
			}
			
			return new ProgramBinary( offset, finalBytes );
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
		
	}

}
