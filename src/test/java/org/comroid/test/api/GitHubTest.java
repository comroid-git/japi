package org.comroid.test.api;

import org.comroid.api.net.GitHub;
import org.comroid.api.net.MD5;
import org.comroid.api.net.REST;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GitHubTest {
    @Test(timeout = 5000)
    public void testDownload() throws IOException, ExecutionException, InterruptedException {
        var releases = GitHub.Default.fetchReleases("comroid-git", "clmath").join();
        var file = File.createTempFile("japi-test-github-dl-clmath", ".zip");
        file.deleteOnExit();

        var tasks = releases.stream()
                .flatMap(release -> release.getAssets().stream())
                .findAny()
                .map(asset -> new CompletableFuture[]{
                        REST.get("https://api.comroid.org/md5?url=" + asset.getBrowserDownloadUrl())
                                .thenApply(response -> response.getBody().asString()),
                        asset.download(file).execute()
                })
                .orElseThrow();

        CompletableFuture.allOf(tasks).join();

        var original = tasks[0].get().toString();
        var output = MD5.calculate(new FileInputStream(file));

        Assert.assertEquals("invalid md5 of downloaded file", original, output);
    }
}
