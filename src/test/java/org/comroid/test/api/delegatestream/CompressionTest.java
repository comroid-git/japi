package org.comroid.test.api.delegatestream;

import org.comroid.api.func.util.DelegateStream;
import org.comroid.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({ "DataFlowIssue", "FieldCanBeLocal" })
public class CompressionTest {
    private DelegateStream.IO compressor;

    private File inputFile;
    private File compressedFile;
    private File decompressedFile;

    private String validInput;
    private String validCompressed;
    private String validDecompressed;

    @BeforeEach
    public void setup() throws Throwable {
        // prepare files
        inputFile      = File.createTempFile("input", ".txt");
        compressedFile = File.createTempFile("compressed", ".gzip");
        decompressedFile = File.createTempFile("decompressed", ".txt");

        // prepare validation data
        validInput = TestUtil.fillWithText(inputFile, 512);

        var buf = new StringWriter();
        DelegateStream.tryTransfer(
                new DelegateStream.Input(new StringReader(validInput)),
                new GZIPOutputStream(new DelegateStream.Output(buf)));
        validCompressed = buf.toString();

        buf = new StringWriter();
        DelegateStream.tryTransfer(
                new GZIPInputStream(new DelegateStream.Input(new StringReader(validCompressed))),
                new DelegateStream.Output(buf));
        validDecompressed = buf.toString();

        // prepare IO
        compressor = new DelegateStream.IO().useCompression();
    }

    //todo fixme @Test
    public void test() throws Throwable {
        // input == decompressed
        Assertions.assertEquals(validInput, validDecompressed, "Input != Decompressed data mismatch");

        // compress
        try (
                var fos = new FileOutputStream(compressedFile);
                var fis = new FileInputStream(inputFile)
        ) {
            compressor.redirect(fos);
            DelegateStream.tryTransfer(fis, compressor.getOutput());

            Assertions.assertEquals(validInput, DelegateStream.readAll(new FileInputStream(inputFile)), "Input data mismatch");
            Assertions.assertEquals(validCompressed, DelegateStream.readAll(new FileInputStream(compressedFile)), "Compressed data mismatch");
        }

        // decompress
        try (
                var fis = new FileInputStream(compressedFile);
                var fos = new FileOutputStream(decompressedFile)
        ) {
            DelegateStream.tryTransfer(compressor.redirect(fis).getInput(), fos);

            Assertions.assertEquals(validInput, DelegateStream.readAll(new FileInputStream(decompressedFile)), "Decompressed data mismatch");
            Assertions.assertEquals(validDecompressed, DelegateStream.readAll(new FileInputStream(decompressedFile)), "Decompressed data mismatch");
        }
    }
}
