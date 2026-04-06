import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.*;
import student.TestCase;

/**
 * This class was designed to test the External Sort class.
 * Each tests generates random ascii and binary files of the specified size,
 * then sorts both and then checking each one with the file checker.
 *
 * @author CS3114/5040 Staff
 * @version Spring 2026
 */
public class ExternalSortTest extends TestCase {
    private CheckFile fileChecker;

    /**
     * This method sets up the tests that follow.
     */
    public void setUp() {
        fileChecker = new CheckFile();
    }


    // ----------------------------------------------------------
    /**
     * Helper method for the tests: Run a test suite for a given size.
     * Creates two files (one "ascii" and one "binary") of the specified size,
     * then for each one, runs the sort and runs the checker.
     * 
     * @param fileSize
     *            Number of (4096 byte) blocks to test for
     * @throws Exception
     */
    public void sortHelper(int fileSize) throws Exception {
        // clearing our tempfile each time
        ExternalSort.clearTemp();
        FileGenerator it = new FileGenerator();
        String namea = "input" + fileSize + "asave.bin";
        String nameb = "input" + fileSize + "bsave.bin";
        it.generateFile(namea, fileSize, "a");
        it.generateFile(nameb, fileSize, "b");
        String[] args = new String[1];

        String testFilea = "testa" + fileSize + ".bin";
        args[0] = testFilea;
        SortUtils.copyFile(namea, testFilea);
        System.out.println("Sorting " + testFilea);
        ExternalSortProj.main(args);
        assertTrue(fileChecker.checkFileA(testFilea, fileSize));

        String testFileb = "testb" + fileSize + ".bin";
        args[0] = testFileb;
        SortUtils.copyFile(nameb, testFileb);
        System.out.println("Sorting " + testFileb);
        ExternalSortProj.main(args);
        assertTrue(fileChecker.checkFile(testFileb, fileSize));
    }


    // ----------------------------------------------------------
    /**
     * Test a file with 1 block
     * 
     * @throws Exception
     */
    public void test1() throws Exception {
        sortHelper(130);
    }


    /**
     * Helper method for the tests: Run a test suite for a given size.
     * Test a file with multiple blocks and print out the integers in the
     * input-file after successful
     * External Sort
     * 
     * @throws IOException
     * 
     */
    public void sortHelper2(int numBlocks) throws IOException {
        int SIZE = 4096 * numBlocks;
        int records = SIZE / 8;
        System.out.println("# records: " + records);

        ExternalSort.clearTemp();
        FileGenerator it = new FileGenerator();
        String nameb = "input" + numBlocks + "test.bin";
        it.generateFile(nameb, numBlocks, "b"); // 1 block is size 4096

        ExternalSort.sort(nameb);

        RandomAccessFile raf = new RandomAccessFile(nameb, "r");
        byte[] testBytes = new byte[SIZE];
        raf.read(testBytes);
        IntBuffer view = ByteBuffer.wrap(testBytes).asIntBuffer();

        // Iterate over the input file after merge (first pass)
        for (int i = 0; i < records; i++) {
            int raw = view.get(i * 2);
            long key = Integer.toUnsignedLong(raw);
            System.out.println("Record " + i + " key: " + key);

            // *2 because each record = 1 int key + 1 int value
        }

        raf.close();
    }

}
