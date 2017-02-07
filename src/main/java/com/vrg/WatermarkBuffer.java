package com.vrg;

import com.google.common.net.HostAndPort;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * A basic watermark buffer that delivers messages about a node if and only if:
 * - there are H messages about the node.
 * - there is no other node with more than L but less than H messages about it.
 */
@DefaultQualifier(value = NonNull.class, locations = {TypeUseLocation.ALL})
class WatermarkBuffer {
    private static final int K_MIN = 3;
    private final int K;
    private final int H;
    private final int L;
    private final AtomicInteger deliverCounter = new AtomicInteger(0);
    private final AtomicInteger updatesInProgress = new AtomicInteger(0);
    private final Map<HostAndPort, AtomicInteger> updateCounters;
    private final ArrayList<Node> readyList = new ArrayList<>();
    private final Object lock = new Object();
    private static final List<Node> EMPTY_LIST =
            Collections.unmodifiableList(new ArrayList<Node>());

    WatermarkBuffer(final int K, final int H, final int L) {
        if (H > K || L > H || K < K_MIN) {
            throw new IllegalArgumentException("Arguments do not satisfy K > H >= L >= 0:" +
                                               " (K: " + K + ", H: " + H + ", L: " + L);
        }
        this.K = K;
        this.H = H;
        this.L = L;
        this.updateCounters = new HashMap<>();
    }

    int getNumDelivers() {
        return deliverCounter.get();
    }

    List<Node> receiveLinkUpdateMessage(final LinkUpdateMessage msg) {
        synchronized (lock) {

            final AtomicInteger counter = updateCounters.computeIfAbsent(msg.getDst(),
                                             (k) -> new AtomicInteger(0));
            final int value = counter.incrementAndGet();

            if (value == L) {
                updatesInProgress.incrementAndGet();
            }

            if (value == H) {
                 // This message has received enough copies that it is safe to deliver, provided
                 // there are no outstanding updates in progress.
                readyList.add(new Node(msg.getDst()));
                final int updatesInProgressVal = updatesInProgress.decrementAndGet();

                if (updatesInProgressVal == 0) {
                    // No outstanding updates, so deliver all messages that have crossed the H threshold of copies.
                    this.deliverCounter.incrementAndGet();
                    for (final Node n: readyList) {
                        // The counter below should never be null.
                        @Nullable final AtomicInteger updateCounter = updateCounters.get(n.address);
                        if (updateCounter == null) {
                            throw new RuntimeException("Node to be delivered not in UpdateCounters map: "
                                                        + n.address);
                        }
                        updateCounter.set(0);
                    }
                    final List<Node> ret = Collections.unmodifiableList(new ArrayList<>(readyList));
                    readyList.clear();
                    return ret;
                }
            }

            return EMPTY_LIST;
        }
    }
}