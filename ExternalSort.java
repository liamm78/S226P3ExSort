import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    // Allocate 50,000 bytes of working memory
    private static byte[] wm;

    // tracks the amount of data in working memory
    private static int size;

    /**
     * Create a new ExternalSort object.
     * 
     * @param theFileName
     *            The name of the file to be sorted
     *
     * @throws IOException
     */
    public static void sort(String theFileName) throws IOException {
        // for debugging, delete later FIXME
        clearTemp();
        File tempfile = new File(theFileName);
        System.out.println("Input file size: " + tempfile.length());

        wm = new byte[MEMBYTES];
        size = 0;

        // create a randomAccessFile, file channel, and byteBuffer
        RandomAccessFile raf = new RandomAccessFile(theFileName, "rw");
        FileChannel channel = raf.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);

        // the offset of working memory
        int offset = 0;
        // load up to 11 blocks into the workingMem
        for (int i = 0; i < 11; i++) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1)
                break;
            size += bytesRead;

            buffer.flip();

            buffer.get(wm, offset, BLOCKSIZE);
            offset += BLOCKSIZE;

            buffer.clear();
        }
        size /= 8;

        // Sorting file
        heapify();
        heapSort();

        raf.close();

        // We need to write back into the input file
        SortUtils.copyFile("temp.bin", theFileName);
    }


    // takes the ith record and sorts it in the heap
    private static void percDown(int i) {
        // integer at the ith key's record
        int iKey = ByteBuffer.wrap(wm, i * 8, 4).getInt();

        // while we're not at the end
        while (2 * i < size) {
            int min;
            int minKey;
            // left child
            int l = 2 * i + 1;
            // left child's key integer
            int lKey = ByteBuffer.wrap(wm, l * 8, 4).getInt();
            // right child, check to make sure it exists
            int r = 2 * i + 2;
            // right child 's key integer

            // find the smallest child. default to left child if right is empty
            min = l;
            minKey = lKey;
            // Checks if right child exists and the right key is less of the two
            if (r < size) {
                int rKey = ByteBuffer.wrap(wm, r * 8, 4).getInt();
                // Now we now right key exists, compare the two
                if (rKey < lKey) {
                    min = r;
                    minKey = rKey;
                }

            }

            // if the smallest child is smaller than the parent, swap them
            if (minKey < iKey) {

                byte[] temp = new byte[8];
                System.arraycopy(wm, i * 8, temp, 0, 8);
                System.arraycopy(wm, min * 8, wm, i * 8, 8);
                System.arraycopy(temp, 0, wm, min * 8, 8);

                i = min;
            }
            // otherwise, we're done
            else
                break;
        }

    }


    // calculate the last non-leaf node, then percDown that and every node
    // before it
    private static void heapify() {
        int start = (size - 2) / 2;
        for (int i = start; i >= 0; i--) {
            percDown(i);
        }
    }


    // fill output buffer with sorted data by swapping the min(root) value of
    // heap into the output buffer, putting the last node a the root, then
    // percDown to sort it.
    public static void heapSort() throws IOException {
        int blocks = size / (BLOCKSIZE / 8);
        ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);

        for (int i = 0; i < blocks; i++) {
            int buffOff = 0;
            for (int j = 0; j < BLOCKSIZE / 8; j++) {
                // set the appropriate spot in output buffer equal to the heap
                // root
                System.arraycopy(wm, 0, wm, OUTBUFFER + buffOff, 8);
                // set the last record in the heap to the root, decrement size
                System.arraycopy(wm, (size - 1) * 8, wm, 0, 8);
                size -= 1;
                // percolate down the record to sort it correctly
                percDown(0);
                buffOff += 8;
            }
            buffer.clear();
            // put everything from the output buffer into a bytebuffer
            buffer.put(wm, OUTBUFFER, BLOCKSIZE);

            // create (or update) the temp.bin file
            try (DataOutputStream file = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream("temp.bin",
                    true)))) {

                buffer.flip();
                file.write(buffer.array());

                file.flush();
                file.close();
            }
        }

    }


    public static void clearTemp() throws IOException {
        new FileOutputStream("temp.bin").close();
    }
}
