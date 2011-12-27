package flipper.targets.penguino;

import flipper.ProgramBinary;
import flipper.StatusDisplay;

public abstract class Memory {

	public Memory() {
	}

	protected int memoryBytes = 0; // in bytes
	protected int pageBytes = 0; // in bytes

	int getNumPages( ) {
		return memoryBytes / pageBytes;
	}
	
	abstract void writePage( int pageIndex, byte[] data );
	
	abstract byte[] readPage( int pageIndex );
	
	public abstract void erase();

	public boolean writeStream(ProgramBinary program, StatusDisplay statusDisplay) {
		int offset = program.getProgramStartIndex( );
		int startPage = offset / pageBytes;
		int currPage = startPage;
		
		int totalBytes = program.getTotalBytes();
		
		statusDisplay.setProgress( 0, totalBytes );
		
		while ( (offset-program.getProgramStartIndex()) < totalBytes ) {
			assert( currPage < getNumPages() );
			
			statusDisplay.setProgress( (currPage-startPage) * pageBytes, totalBytes );
			
			byte[] pageBuf = program.getBytes( offset, offset+pageBytes );
			writePage( currPage, pageBuf );
			
			currPage++;
			offset += pageBytes;
		}
		
		statusDisplay.setProgress( totalBytes, totalBytes );
		
		return true;
	}

	public boolean verifyStream(ProgramBinary program, StatusDisplay statusDisplay) {
		int offset = program.getProgramStartIndex();
		int startPage = offset / pageBytes;
		int currPage = startPage;
		
		int totalBytes = program.getTotalBytes();
		
		boolean success = true;
		
		statusDisplay.setProgress( 0, totalBytes );
		
		while ( (offset-program.getProgramStartIndex()) < totalBytes ) {
			assert( currPage < getNumPages() );
			
			statusDisplay.setProgress( (currPage-startPage) * pageBytes, totalBytes );
			
			byte[] pageBuf = program.getBytes( offset, offset+pageBytes );
			byte[] actualBuf = readPage( currPage );
			
			for ( int j = 0; j < pageBuf.length; j++ ) {
				if ( actualBuf[j] != pageBuf[j] ) {
					System.out.printf( "[%d + %d] %d ~ %d\n", offset, j, actualBuf[j], pageBuf[j] );
					//throw new Error( "Verify failed!" );
					statusDisplay.setStatus( "VERIFY FAILED!" );
					return false;
				}
			}
			
			currPage++;
			offset += pageBytes;
		}
		
		statusDisplay.setProgress( totalBytes, totalBytes );
		
		return success;
	}

	public abstract void finished();

}
