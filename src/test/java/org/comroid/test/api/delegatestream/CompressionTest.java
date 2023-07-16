package org.comroid.test.api.delegatestream;

import org.comroid.api.DelegateStream;
import org.comroid.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({"DataFlowIssue", "FieldCanBeLocal"})
public class CompressionTest {
    private DelegateStream.IO compressor;

    private File inputFile;
    private File compressedFile;
    private File decompressedFile;

    private String validInput;
    private String validCompressed;
    private String validDecompressed;

    @Before
    public void setup() throws Throwable {
        // prepare files
        inputFile = File.createTempFile("input", ".txt");
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

    @Test
    public void test() throws Throwable {
        // input == decompressed
        Assert.assertEquals("Input != Decompressed data mismatch", validInput, validDecompressed);

        // compress
        try (var fos = new FileOutputStream(compressedFile);
             var fis = new FileInputStream(inputFile)) {
            compressor.redirect(fos);
            DelegateStream.tryTransfer(fis, compressor.getOutput());

            Assert.assertEquals("Input data mismatch", validInput, DelegateStream.readAll(new FileInputStream(inputFile)));
            Assert.assertEquals("Compressed data mismatch", validCompressed, DelegateStream.readAll(new FileInputStream(compressedFile)));
        }

        // decompress
        try (var fis = new FileInputStream(compressedFile);
             var fos = new FileOutputStream(decompressedFile)) {
            DelegateStream.tryTransfer(compressor.redirect(fis).getInput(), fos);

            Assert.assertEquals("Decompressed data mismatch", validInput, DelegateStream.readAll(new FileInputStream(decompressedFile)));
            Assert.assertEquals("Decompressed data mismatch", validDecompressed, DelegateStream.readAll(new FileInputStream(decompressedFile)));
        }
    }
}
