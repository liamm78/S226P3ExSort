import java.nio.*;
import java.nio.channels.FileChannel;
import java.io.*;

// The External Sort implementation
// -------------------------------------------------------------------------
/**
 *
 * @author {Your Name Here}
 * @version Spring 2026
 */
public class ExternalSort {

    /**
     * The working memory available to the program: 50,000 bytes
     */
    private static final int MEMBYTES = 50000;
    
    private static final int BLOCKSIZE = 4096;

    /**
     * Create a new ExternalSort object.
     * @param theFileName The name of the file to be sorted
     *
     * @throws IOException
     */
    public static void sort(String theFileName)
        throws IOException
    {
        
        //Allocate 50,000 bytes of working memory
        byte[] workingMem = new byte[MEMBYTES];
        //the offset of working memory
        int offset = 0;
        
        //create a randomAccessFile, file channel, and byteBuffer
        RandomAccessFile raf = new RandomAccessFile(theFileName, "rw");
        FileChannel channel = raf.getChannel();        
        ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);
        
        //load up to 11 blocks into the workingMem
        for(int i = 0; i < 11; i++) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) break;
            
            buffer.flip();
            
            buffer.get(workingMem, offset, BLOCKSIZE);
            offset+= BLOCKSIZE;
            
            buffer.clear();
        }       
    }
    
    public void percDown(byte[] wm)
    {
        
    }
}
