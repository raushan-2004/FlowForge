package com.flowforge.api.shared.identity;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class PublicIdGenerator {

    private Supplier<UUID> uuidSupplier = UUID::randomUUID;

    public UUID generate() {
        return uuidSupplier.get();
    }

    // Allows tests to inject a mock/deterministic supplier
    public void setUuidSupplier(Supplier<UUID> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null");
        }
        this.uuidSupplier = supplier;
    }

    public void reset() {
        this.uuidSupplier = UUID::randomUUID;
    }
}
