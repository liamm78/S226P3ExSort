import java.util.*;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.*;
import java.nio.channels.FileChannel;

public class HeapTesting {

    private static byte[] wm;
    
    private static int size;
    
    private static int OUTBUFFER;
    
    static private Random value = new Random();
    
    public static void main(String[] args) throws IOException {
        
        //build an array of bytes
        buildArr(1);
        //clear the temp.bin
        ExternalSort.clearTemp();
        //sort temp.bin
        System.out.println("Original List\n---------------\n");
        print();
        heapify();
        System.out.println("\nAfter Heapifying\n---------------\n");
        print();
        heapSort();

        
        //prepare to read from temp.bin
        File tempfile = new File("temp.bin");
        RandomAccessFile raf = new RandomAccessFile("temp.bin", "rw");
        FileChannel channel = raf.getChannel();        
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        
        //read from temp.bin
        int offset = 0;
        for(int i = 0; i < 11; i++) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) break;
            
            buffer.flip();
            
            buffer.get(wm, offset, 4096);
            offset+= 4096;
            
            buffer.clear();
        }
        
        //print the sorted array
        System.out.println("\nAfter HeapSort\n---------------\n");
        print();
    }
    
    
    public static void buildArr(int blocks) {
        wm = new byte[(blocks + 1) * 4096];
        size = blocks * 512; 
        OUTBUFFER = 4096 * blocks;
        int dataval, keyval;
        
        for (int i = 0; i < blocks; i++) {
            for (int j = 0; j < 512; j++) {
                dataval = random(2000000000) + 1;
                keyval = random(2000000000) + 1;
                int offset = 512 * i + 8 * j;
                ByteBuffer.wrap(wm, offset, 4).putInt(keyval);
                ByteBuffer.wrap(wm, offset + 4, 4).putInt(dataval);
            }
        }
        
    }
    
    private static int random(int n) {
        return Math.abs(value.nextInt()) % n;
    }
    
    private static void heapify() {
        int start = (size - 2) / 2;
        for (int i = start; i >= 0; i--)
        {
            percDown(i);
        }
    }
    
    private static void percDown(int i)
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
            int lKey = ByteBuffer.wrap(wm, l * 8, 4).getInt();
            //right child, check to make sure it exists
            int r = 2 * i + 2;
            //right child 's key integer
            int rKey = ByteBuffer.wrap(wm, r * 8, 4).getInt();
            
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
                System.arraycopy(wm, i * 8, temp, 0, 8);
                System.arraycopy(wm, min * 8, wm, i * 8, 8);
                System.arraycopy(temp, 0, wm, min * 8, 8);
                
                i = min;
            }
            //otherwise, we're done
            else break;
        }
    }
    
    public static void heapSort() throws IOException {
        int oldSize = size;
        int blocks = size / (512);
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        
        for (int i = 0; i < blocks; i++)
        {
            int buffOff = 0;
            for (int j = 0; j < 512; j++) {
                //set the appropriate spot in output buffer equal to the heap root
                System.arraycopy(wm, 0, wm, OUTBUFFER + buffOff, 8);
                //set the last record in the heap to the root, decrement size
                System.arraycopy(wm, (size - 1) * 8, wm, 0, 8);
                size -= 1;
                //percolate down the record to sort it correctly
                percDown(0);
                buffOff += 8;
            }
            buffer.clear();
            //put everything from the output buffer into a bytebuffer
            buffer.put(wm, OUTBUFFER, 4096);
            
            //create (or update) the temp.bin file
            try (DataOutputStream file =
                new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream("temp.bin", true)))) {
                
                buffer.flip();
                file.write(buffer.array());
                
                file.flush();
                file.close();
            }
        }
        size = oldSize;
    }
    
    public static void print() {
        for (int i = 0; i < size; i++) {
            int key = ByteBuffer.wrap(wm, i * 8, 4).getInt();
            int value = ByteBuffer.wrap(wm, i * 8 + 4, 4).getInt();
            System.out.println("Record " + i + " \t Key: " + key + "\t\tValue: " + value);
        }
    }
    
}




