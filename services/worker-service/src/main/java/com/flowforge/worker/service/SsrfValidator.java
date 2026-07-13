package com.flowforge.worker.service;

import com.flowforge.worker.config.SsrfProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Service
public class SsrfValidator {

    private static final Logger logger = LoggerFactory.getLogger(SsrfValidator.class);
    private final SsrfProperties properties;

    public SsrfValidator(SsrfProperties properties) {
        this.properties = properties;
    }

    public void validateUri(URI uri) throws UnknownHostException, SsrfValidationException {
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            logger.warn("SSRF Blocked: scheme {} is not allowed", scheme);
            throw new SsrfValidationException("Scheme not allowed: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            logger.warn("SSRF Blocked: host is empty or null in URI: {}", uri);
            throw new SsrfValidationException("Host is null or empty");
        }

        InetAddress[] addresses = resolveAddresses(host);
        for (InetAddress address : addresses) {
            if (!isSafeAddress(address)) {
                logger.warn("SSRF Blocked: address {} is unsafe", address.getHostAddress());
                throw new SsrfValidationException("Unsafe IP address: " + address.getHostAddress());
            }
        }
    }

    protected InetAddress[] resolveAddresses(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    private boolean isSafeAddress(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return false;
        }
        if (address.isLinkLocalAddress()) {
            return false;
        }
        if (address.isMulticastAddress()) {
            return false;
        }
        if (properties.isBlockPrivateRanges()) {
            if (address.isSiteLocalAddress()) {
                return false;
            }
            // Explicit byte-level validation for extra safety
            byte[] bytes = address.getAddress();
            if (bytes.length == 4) {
                int octet1 = bytes[0] & 0xFF;
                int octet2 = bytes[1] & 0xFF;
                // 10.0.0.0/8
                if (octet1 == 10) return false;
                // 172.16.0.0/12
                if (octet1 == 172 && (octet2 >= 16 && octet2 <= 31)) return false;
                // 192.168.0.0/16
                if (octet1 == 192 && octet2 == 168) return false;
                // 127.0.0.0/8
                if (octet1 == 127) return false;
                // 169.254.0.0/16
                if (octet1 == 169 && octet2 == 254) return false;
                // 0.0.0.0/8
                if (octet1 == 0) return false;
            }
        }
        return true;
    }

    public static class SsrfValidationException extends Exception {
        public SsrfValidationException(String message) {
            super(message);
        }
    }
}
