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
    
    private static final int OUTBUFFER = 45056;

    //Allocate 50,000 bytes of working memory
    private static byte[] wm;
    
    //tracks the amount of data in working memory
    private static int size;
    
    /**
     * Create a new ExternalSort object.
     * @param theFileName The name of the file to be sorted
     *
     * @throws IOException
     */
    public static void sort(String theFileName)
        throws IOException
    {
        
        wm = new byte[MEMBYTES];
        size = 0;
        
        //the offset of working memory
        int offset = 0;
        
        //create a randomAccessFile, file channel, and byteBuffer
        RandomAccessFile raf = new RandomAccessFile(theFileName, "rw");
        FileChannel channel = raf.getChannel();        
        ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);
        
        //load up to 11 blocks into the workingMem
        for(int i = 0; i < 11; i++) {
            int bytesRead = channel.read(buffer);
            size += bytesRead;
            if (bytesRead == -1) break;
            
            buffer.flip();
            
            buffer.get(wm, offset, BLOCKSIZE);
            offset+= BLOCKSIZE;
            
            buffer.clear();
        }
        size /= 8;
    }
        
        // Call heapify on the entire working memory to initially sort the data
        //heapify()
        // in a loop, call heapSort, which will fill output buffer and then flush it. it does it up to 11 times
        //heapSort() 
    
    
    //takes the ith record and sorts it in the heap
    public void percDown(int i)
    {
        //integer at the ith key's record
        int iKey = ByteBuffer.wrap(wm, i * 8, 4).getInt();
        
        //while we're not at the end
        while(2 * i < size) {
            int min;
            int minKey;
            //left child
            int l = 2 * i + 1;
            //left child's key integer
            int lKey = ByteBuffer.wrap(wm, l, 4).getInt();
            //right child, check to make sure it exists
            int r = 2 * i + 2;
            //right child 's key integer
            int rKey = ByteBuffer.wrap(wm, l, 4).getInt();
            
            //find the smallest child. default to left child if right is empty
            min = l;
            minKey = l;
            if (r < size && Math.min(lKey, rKey) == rKey) {                
                min = r;
                minKey = rKey;                
            }
            
            //if the smallest child is smaller than the parent, swap them
            if (minKey < iKey) {
                
                byte[] temp = new byte[8];
                System.arraycopy(wm, i, temp, 0, 8);
                System.arraycopy(wm, min, wm, i, 8);
                System.arraycopy(temp, 0, wm, min, 8);
                
                i = min;
            }
            //otherwise, we're done
            else break;
        }
    }
    
    //calculate the last non-leaf node, then percDown that and every node before it
    public void heapify() {
        int start = (size - 2) / 2;
        for (int i = start; i >= 0; i--)
        {
            percDown(i);
        }
    }
    
    //fill output buffer with sorted data by swapping the min(root) value of heap into the output buffer, putting the last node a the root, then percDown to sort it.
    public void heapSort() {
        int buffOff = 0;
        int blocks = size / (BLOCKSIZE / 8);
        
        for (int i = 0; i < blocks; i++)
        {
            for (int j = 0; 0 < BLOCKSIZE / 8; j++) {
                //set the appropriate spot in output buffer equal to the heap root
                System.arraycopy(wm, 0, wm, OUTBUFFER + buffOff, 8);
                //set the last record in the heap to the root, decrement size
                System.arraycopy(wm, (size - 1) * 4, wm, 0, 8);
                size -= 8;
                //percolate down the record to sort it correctly
                percDown(0);
            }
            
            //here is where we need to flush the output buffer to the file. I don't know enough about creating and writing to files right now
            
        }
        
        
    }
}











