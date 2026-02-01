package org.comroid.api.net.luckperms.model.dto;

public record HealthResult(boolean healthy, Details details) {
    public record Details(boolean storageConnected, short storagePing) {}
}
