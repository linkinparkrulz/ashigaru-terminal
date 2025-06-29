package com.sparrowwallet.sparrow.net;

/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import com.sparrowwallet.sparrow.AppServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matches a request based on IP Address or subnet mask matching against the remote
 * address.
 * <p>
 * Both IPv6 and IPv4 addresses are supported, but a matcher which is configured with an
 * IPv4 address will never match a request which returns an IPv6 address, and vice-versa.
 *
 * @author Luke Taylor
 * @since 3.0.2
 *
 * Slightly modified by omidzk to have zero dependency to any frameworks other than the JRE.
 */
public final class IpAddressMatcher {
    private static final Logger log = LoggerFactory.getLogger(IpAddressMatcher.class);
    private static final IpAddressMatcher LOCAL_RANGE_1 = new IpAddressMatcher("10.0.0.0/8");
    private static final IpAddressMatcher LOCAL_RANGE_2 = new IpAddressMatcher("172.16.0.0/12");
    private static final IpAddressMatcher LOCAL_RANGE_3 = new IpAddressMatcher("192.168.0.0/16");
    private static final IpAddressMatcher LOCAL_RANGE_4 = new IpAddressMatcher("100.64.0.0/10");

    private final int nMaskBits;
    private final InetAddress requiredAddress;

    /**
     * Takes a specific IP address or a range specified using the IP/Netmask (e.g.
     * 192.168.1.0/24 or 202.24.0.0/14).
     *
     * @param ipAddress the address or range of addresses from which the request must
     * come.
     */
    public IpAddressMatcher(String ipAddress) {

        if (ipAddress.indexOf('/') > 0) {
            String[] addressAndMask = ipAddress.split("/");
            ipAddress = addressAndMask[0];
            nMaskBits = Integer.parseInt(addressAndMask[1]);
        }
        else {
            nMaskBits = -1;
        }
        requiredAddress = parseAddress(ipAddress);
        assert  (requiredAddress.getAddress().length * 8 >= nMaskBits) :
                String.format("IP address %s is too short for bitmask of length %d",
                        ipAddress, nMaskBits);
    }

    public boolean matches(String address) {
        InetAddress remoteAddress = parseAddress(address);

        if (!requiredAddress.getClass().equals(remoteAddress.getClass())) {
            return false;
        }

        if (nMaskBits < 0) {
            return remoteAddress.equals(requiredAddress);
        }

        byte[] remAddr = remoteAddress.getAddress();
        byte[] reqAddr = requiredAddress.getAddress();

        int nMaskFullBytes = nMaskBits / 8;
        byte finalByte = (byte) (0xFF00 >> (nMaskBits & 0x07));

        for (int i = 0; i < nMaskFullBytes; i++) {
            if (remAddr[i] != reqAddr[i]) {
                return false;
            }
        }

        if (finalByte != 0) {
            return (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte);
        }

        return true;
    }

    private InetAddress parseAddress(String address) {
        try {
            return InetAddress.getByName(address);
        } catch(UnknownHostException e) {
            throw new IllegalArgumentException("Failed to resolve address: " + address, e);
        }
    }

    public static boolean isLocalNetworkAddress(String address) {
        try {
            return "localhost".equals(address) || "127.0.0.1".equals(address) || LOCAL_RANGE_1.matches(address) || LOCAL_RANGE_2.matches(address) || LOCAL_RANGE_3.matches(address) || LOCAL_RANGE_4.matches(address);
        } catch(IllegalArgumentException e) {
            if(AppServices.isUsingProxy()) {
                log.info(e.getMessage() + ", assuming it is a non-local address to be resolved by the configured proxy");
                return false;
            }
            throw e;
        }
    }
}
