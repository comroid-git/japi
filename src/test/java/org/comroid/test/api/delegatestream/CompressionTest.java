package org.comroid.test.api.delegatestream;

import lombok.SneakyThrows;
import org.comroid.api.DelegateStream;
import org.comroid.util.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({"DataFlowIssue", "FieldCanBeLocal"})
public class CompressionTest {
    private DelegateStream.IO base;
    private DelegateStream.IO compressor;
    private DelegateStream.IO intermediate;

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
        compressedFile = File.createTempFile("decompressed", ".txt");


        // prepare validation data
        validInput = TestUtils.fillWithText(inputFile, 512);

        var buf = new StringWriter();
        new DelegateStream.Input(new StringReader(validInput))
                .transferTo(new GZIPOutputStream(new DelegateStream.Output(buf)));
        validCompressed = buf.toString();

        buf = new StringWriter();
        new GZIPInputStream(new DelegateStream.Input(new StringReader(validCompressed)))
                .transferTo(new DelegateStream.Output(buf));
        validDecompressed = buf.toString();


        // prepare streams
        base = DelegateStream.IO.builder()
                .input(new FileInputStream(inputFile))
                .output(new FileOutputStream(decompressedFile))
                .build();
        compressor = base.useCompression();
        intermediate = DelegateStream.IO.builder()
                .input(new FileInputStream(compressedFile))
                .output(new FileOutputStream(compressedFile))
                .build();
    }

    @Test
    public void test() throws Throwable {
        // input
        Assert.assertEquals("Input data mismatch", validInput, DelegateStream.readAll(new FileInputStream(inputFile)));

        // compress
        compressor.getInput().transferTo(intermediate.getOutput());
        Assert.assertEquals("Compressed data mismatch", validCompressed, DelegateStream.readAll(new FileInputStream(compressedFile)));

        // decompress
        intermediate.getInput().transferTo(compressor.getOutput());
        Assert.assertEquals("Decompressed data mismatch", validDecompressed, DelegateStream.readAll(new FileInputStream(decompressedFile)));
    }
}
