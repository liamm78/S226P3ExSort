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
 * @author {Tanner and Liam}
 * @version Spring 2026
 */
public class ExternalSort {

    /**
     * The working memory available to the program: 50,000 bytes
     */
    private static final int MEMBYTES = 50000;
    private static final int BLOCKSIZE = 4096;
    private static final int OUTBUFFER = 45056; // Heap size
    private static final int INBUF = 20480; // 5 blocks
    private static final int OUTBUF_START = 40960;
    private static final int OUTBUF_SIZE = 8192; // 2 blocks
    private static int fileSize;

    // Allocate 50,000 bytes of working memory
    private static byte[] wm;

    // tracks the amount of data in working memory
    private static int size;

    //
    private static int numRuns;

    /**
     * Create a new ExternalSort object.
     * 
     * @param theFileName
     *            The name of the file to be sorted
     *
     * @throws IOException
     */
    public static void sort(String theFileName) throws IOException {
        clearTemp();
        File tempfile = new File(theFileName);
        fileSize = (int)tempfile.length();
        System.out.println("Input file size: " + tempfile.length());

        numRuns = (int)Math.ceil((double)fileSize / OUTBUFFER);

        wm = new byte[MEMBYTES];

        // create a randomAccessFile, file channel, and byteBuffer
        RandomAccessFile raf = new RandomAccessFile(theFileName, "rw");
        FileChannel channel = raf.getChannel();

        for (int r = 0; r < numRuns; r++) {
            size = loadBlocks(channel, 0, 11) / 8; // Size read for this run.
                                                   // Number of records
            // Sorting file
            heapify();
            heapSort();
        }
        merge(theFileName);

        // merge(theFileName); // Sorts runs into temp.bin. -> theFileName.bin

        raf.close();
    }


    /**
     * Loads blocks from disk into working memory array.
     * 
     * @param channel
     *            FileChannel to read from
     * @param wmOffset
     *            Byte Position to start the block at in the WM.
     * @param maxBlocks
     *            the maximum amount of blocks to load
     * @return Total bytes read
     * @throws IOException
     * 
     */
    public static int loadBlocks(
        FileChannel channel,
        int wmOffset,
        int maxBlocks)
        throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);

        int totalRead = 0;
        for (int i = 0; i < maxBlocks; i++) { // load up to 11 blocks
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1)
                break;
            buffer.flip();
            buffer.get(wm, wmOffset + totalRead, bytesRead);
            buffer.clear();
            totalRead += bytesRead;
        }

        return totalRead;
    }


    /**
     * Loads up to maxBlocks blocks, but never reads past bytesRemaining.
     *
     * @param channel
     *            FileChannel to read from
     * @param wmOffset
     *            byte offset in working memory
     * @param maxBlocks
     *            maximum number of blocks to read
     * @param bytesRemaining
     *            bytes left in this run
     * @return number of bytes read into working memory
     * @throws IOException
     */
    public static int loadBlocksLimited(
        FileChannel channel,
        int wmOffset,
        int maxBlocks,
        long bytesRemaining)
        throws IOException {
        if (bytesRemaining <= 0) {
            return 0;
        }

        int totalRead = 0;
        long remaining = bytesRemaining;

        for (int i = 0; i < maxBlocks && remaining > 0; i++) {
            int chunk = (int)Math.min(BLOCKSIZE, remaining);
            ByteBuffer buffer = ByteBuffer.allocate(chunk);
            int bytesRead = channel.read(buffer);
            if (bytesRead <= 0) {
                break;
            }
            buffer.flip();
            buffer.get(wm, wmOffset + totalRead, bytesRead);
            totalRead += bytesRead;
            remaining -= bytesRead;
            if (bytesRead < chunk) {
                break;
            }
        }

        return totalRead;
    }


    /**
     * 
     * takes the ith record and sorts it in the heap
     * 
     * @param i
     *            the record of the heap to be percolated down
     */
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


    /**
     * calculate the last non-leaf node, then percDown that and every node
     */
    private static void heapify() {
        int start = (size - 2) / 2;
        for (int i = start; i >= 0; i--) {
            percDown(i);
        }
    }


    /**
     * 
     * fill output buffer with sorted data by swapping the min(root) value of
     * heap into the output buffer, putting the last node a the root, then
     * percDown to sort it.
     * whenever output buffer is full, we flush that data into temp.bin
     * 
     * @throws IOException
     */
    public static void heapSort() throws IOException {
        int blocks = size / (BLOCKSIZE / 8);
        ByteBuffer buffer = ByteBuffer.allocate(BLOCKSIZE);

        for (int i = 0; i < blocks; i++) {
            int buffOff = 0;
            for (int j = 0; j < BLOCKSIZE / 8; j++) { // iterate over 512
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

                buffer.flip(); // r -> w
                file.write(buffer.array());

                file.flush();
                file.close();
            }
        }

    }


    /**
     * 
     * this will be the multi-way merge function. It assumes the temp.bin file
     * is currently full of individually sorted runs
     * either of 45056 bytes, or the last, smaller run of some multiple of 4096.
     * 
     * it creates three buffers, two 5 block long buffers for each compared run,
     * and one 2 block long for the new outbuffer
     * 
     * it will load in the data from each run, then compare them record by
     * record until one of the two buffers runs out, and it will go get more
     * data.
     * 
     * whenever the outbuffer fills up, it flushes to the original input file.
     * once both runs have been sorted, it will perform the same operation on
     * the next pair of runs.
     * 
     * this continues until it has less then 2 runs left, in which case it
     * either copies the remaining run to the input file, or ends this iteration
     * it will then continue the process by merging the new, bigger runs back
     * into the temp file.
     * 
     * this bounces back and forth until we have one, completely sorted run, and
     * then it will return.
     * 
     * @param originFile
     *            the original input file
     * 
     */
    public static void merge(String originFile)
        throws FileNotFoundException,
        IOException {
        int currentRuns = numRuns;
        long runSize = OUTBUFFER; // How long each run chunk is at a given pass.

        String srcFile = "temp.bin";
        String destFile = "temp2.bin";

        // Keep halving currentRuns until 1
        while (currentRuns > 1) {
            System.out.println("On pass: " + currentRuns + " runSize: "
                + runSize);

            RandomAccessFile inFile = new RandomAccessFile(srcFile, "rw");
            FileChannel inChannel = inFile.getChannel();

            RandomAccessFile outFile = new RandomAccessFile(destFile, "rw");
            outFile.setLength(0); // Clear dest file

            for (int i = 0; i < currentRuns - 1; i += 2) {
                System.out.println("On run " + i);

                // Compute actual sizes of each run
                long fileSize = inFile.length();
                long run1Start = (long)i * runSize;
                long run2Start = (long)(i + 1) * runSize;
                long run1Total = Math.min(runSize, fileSize - run1Start);
                long run2Total = Math.min(runSize, Math.max(0, fileSize
                    - run2Start));

                int outCount = 0;
                int ptr1 = 0;
                int ptr2 = INBUF;
                long run1Read = 0;
                long run2Read = 0;

                inChannel.position(run1Start);
                int buf1Size = loadBlocksLimited(inChannel, 0, 5, run1Total
                    - run1Read);
                run1Read += buf1Size;

                inChannel.position(run2Start);
                int buf2Size = loadBlocksLimited(inChannel, INBUF, 5, run2Total
                    - run2Read);
                run2Read += buf2Size;

                // Merge while both runs have data (in-memory or on disk)
                while ((ptr1 < buf1Size || run1Read < run1Total)
                    && (ptr2 < INBUF + buf2Size || run2Read < run2Total)) {

                    // Reload buf1 if we've consumed what's in memory
                    if (ptr1 >= buf1Size) {
                        inChannel.position(run1Start + run1Read);
                        buf1Size = loadBlocksLimited(inChannel, 0, 5, run1Total
                            - run1Read);
                        if (buf1Size == 0)
                            break;
                        run1Read += buf1Size;
                        ptr1 = 0;
                    }

                    // Reload buf2 if we've consumed what's in memory
                    if (ptr2 >= INBUF + buf2Size) {
                        inChannel.position(run2Start + run2Read);
                        buf2Size = loadBlocksLimited(inChannel, INBUF, 5,
                            run2Total - run2Read);
                        if (buf2Size == 0)
                            break;
                        run2Read += buf2Size;
                        ptr2 = INBUF;
                    }

                    // Flush output buffer if full
                    if (outCount == OUTBUF_SIZE / 8) {
                        outFile.write(wm, OUTBUF_START, OUTBUF_SIZE);
                        outCount = 0;
                    }

                    int key1 = ByteBuffer.wrap(wm, ptr1, 4).getInt();
                    int key2 = ByteBuffer.wrap(wm, ptr2, 4).getInt();

                    if (key1 <= key2) {
                        System.arraycopy(wm, ptr1, wm, OUTBUF_START + (outCount
                            * 8), 8);
                        ptr1 += 8;
                    }
                    else {
                        System.arraycopy(wm, ptr2, wm, OUTBUF_START + (outCount
                            * 8), 8);
                        ptr2 += 8;
                    }
                    outCount++;
                }

                // Drain leftover in-memory bytes from buf1
                while (ptr1 < buf1Size) {
                    if (outCount == OUTBUF_SIZE / 8) {
                        outFile.write(wm, OUTBUF_START, OUTBUF_SIZE);
                        outCount = 0;
                    }
                    System.arraycopy(wm, ptr1, wm, OUTBUF_START + (outCount
                        * 8), 8);
                    ptr1 += 8;
                    outCount++;
                }
                // Then drain remaining run1 bytes from disk
                while (run1Read < run1Total) {
                    if (ptr1 >= buf1Size) {
                        inChannel.position(run1Start + run1Read);
                        buf1Size = loadBlocksLimited(inChannel, 0, 5, run1Total
                            - run1Read);
                        if (buf1Size == 0)
                            break;
                        run1Read += buf1Size;
                        ptr1 = 0;
                    }
                    if (outCount == OUTBUF_SIZE / 8) {
                        outFile.write(wm, OUTBUF_START, OUTBUF_SIZE);
                        outCount = 0;
                    }
                    System.arraycopy(wm, ptr1, wm, OUTBUF_START + (outCount
                        * 8), 8);
                    ptr1 += 8;
                    outCount++;
                }

                // Drain leftover in-memory bytes from buf2
                while (ptr2 < INBUF + buf2Size) {
                    if (outCount == OUTBUF_SIZE / 8) {
                        outFile.write(wm, OUTBUF_START, OUTBUF_SIZE);
                        outCount = 0;
                    }
                    System.arraycopy(wm, ptr2, wm, OUTBUF_START + (outCount
                        * 8), 8);
                    ptr2 += 8;
                    outCount++;
                }
                // Then drain remaining run2 bytes from disk
                while (run2Read < run2Total) {
                    if (ptr2 >= INBUF + buf2Size) {
                        inChannel.position(run2Start + run2Read);
                        buf2Size = loadBlocksLimited(inChannel, INBUF, 5,
                            run2Total - run2Read);
                        if (buf2Size == 0)
                            break;
                        run2Read += buf2Size;
                        ptr2 = INBUF;
                    }
                    if (outCount == OUTBUF_SIZE / 8) {
                        outFile.write(wm, OUTBUF_START, OUTBUF_SIZE);
                        outCount = 0;
                    }
                    System.arraycopy(wm, ptr2, wm, OUTBUF_START + (outCount
                        * 8), 8);
                    ptr2 += 8;
                    outCount++;
                }

                // Flush any remaining output
                if (outCount > 0) {
                    outFile.write(wm, OUTBUF_START, outCount * 8);
                    outCount = 0;
                }
            }

            // If odd number of runs, copy the last unpaired run straight to
            // dest
            if (currentRuns % 2 != 0) {
                long lastRunStart = (long)(currentRuns - 1) * runSize;
                inChannel.position(lastRunStart);
                ByteBuffer block = ByteBuffer.allocate(BLOCKSIZE);
                while (inChannel.read(block) > 0) {
                    block.flip();
                    byte[] arr = new byte[block.limit()];
                    block.get(arr);
                    outFile.write(arr);
                    block.clear();
                }
            }

            inFile.close();
            outFile.close();

            // Swap src and dest for next pass
            String temp = srcFile;
            srcFile = destFile;
            destFile = temp;

            currentRuns = (int)Math.ceil(currentRuns / 2.0);
            runSize *= 2;
        }

        // Write the final sorted result back to the original input file
        SortUtils.copyFile(srcFile, originFile);
    }


    /**
     * 
     * clears temp file so we can retest without just appending to a the file of
     * a previous test
     * 
     * @throws IOException
     */
    public static void clearTemp() throws IOException {
        new FileOutputStream("temp.bin").close();
    }
}