/*
 * Copyright © 2016 - 2017 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an “AS IS” BASIS, without warranties or conditions of any kind,
 * EITHER EXPRESS OR IMPLIED. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.vrg.rapid;

import com.vrg.rapid.pb.AlertMessage;
import com.vrg.rapid.pb.EdgeStatus;
import com.vrg.rapid.pb.Endpoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for multi node cut detection
 */
public class CutDetectionTest {
    private static final int K = 10;
    private static final int H = 8;
    private static final int L = 2;
    private static final long CONFIGURATION_ID = -1;  // Should not affect the following tests
    private static final String HOST = "127.0.0.1";
    private final MembershipView dummyView = new MembershipView(K);

    /**
     * A series of updates with the right ring indexes
     */
    @Test
    public void cutDetectionTest() {
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final Endpoint dst = Utils.hostFromParts("127.0.0.2", 2);
        Set<Endpoint> ret;

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        final AlertMessage decidingMessage = createAlertMessage(Utils.hostFromParts(HOST, H), dst, H - 1);
        ret = wb.aggregateForProposal(decidingMessage, dummyView);
        assertEquals(1, ret.size());
        assertEquals(1, wb.getNumProposals());
    }

    @Test
    public void cutDetectionTestBlockingOneBlocker() {
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final Endpoint dst1 = Utils.hostFromParts("127.0.0.2", 2);
        final Endpoint dst2 = Utils.hostFromParts("127.0.0.3", 2);
        Set<Endpoint> ret;

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst1, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst2, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }
        final AlertMessage beforeBlocker = createAlertMessage(Utils.hostFromParts(HOST, H), dst1, H - 1);
        ret = wb.aggregateForProposal(beforeBlocker, dummyView);
        assertEquals(0, ret.size());
        assertEquals(0, wb.getNumProposals());

        final AlertMessage decidingMessage = createAlertMessage(Utils.hostFromParts(HOST, H), dst2,H - 1);
        ret = wb.aggregateForProposal(decidingMessage, dummyView);
        assertEquals(2, ret.size());
        assertEquals(1, wb.getNumProposals());
    }


    @Test
    public void cutDetectionTestBlockingThreeBlockers() {
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final Endpoint dst1 = Utils.hostFromParts("127.0.0.2", 2);
        final Endpoint dst2 = Utils.hostFromParts("127.0.0.3", 2);
        final Endpoint dst3 = Utils.hostFromParts("127.0.0.4", 2);
        Set<Endpoint> ret;

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst1, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst2, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst3, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        final AlertMessage dst1Blocker = createAlertMessage(Utils.hostFromParts(HOST, H), dst1, H - 1);
        ret = wb.aggregateForProposal(dst1Blocker, dummyView);
        assertEquals(0, ret.size());
        assertEquals(0, wb.getNumProposals());

        final AlertMessage dst3Blocker = createAlertMessage(Utils.hostFromParts(HOST, H), dst3, H - 1);
        ret = wb.aggregateForProposal(dst3Blocker, dummyView);
        assertEquals(0, ret.size());
        assertEquals(0, wb.getNumProposals());

        final AlertMessage dst2Blocker = createAlertMessage(Utils.hostFromParts(HOST, H), dst2, H - 1);
        ret = wb.aggregateForProposal(dst2Blocker, dummyView);
        assertEquals(3, ret.size());
        assertEquals(1, wb.getNumProposals());
    }

    @Test
    public void cutDetectionTestBlockingMultipleBlockersPastH() {
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final Endpoint dst1 = Utils.hostFromParts("127.0.0.2", 2);
        final Endpoint dst2 = Utils.hostFromParts("127.0.0.3", 2);
        final Endpoint dst3 = Utils.hostFromParts("127.0.0.4", 2);
        Set<Endpoint> ret;

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst1, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst2, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst3, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        // Unlike the previous test, add more reports for
        // dst1 and dst3 past the H boundary.
        final AlertMessage dst1Msg1 = createAlertMessage(Utils.hostFromParts(HOST, H), dst1, H - 1);
        final AlertMessage dst1Msg2 = createAlertMessage(Utils.hostFromParts(HOST, H + 1), dst1, H - 1);
        wb.aggregateForProposal(dst1Msg1, dummyView);
        ret = wb.aggregateForProposal(dst1Msg2, dummyView);
        assertEquals(0, ret.size());
        assertEquals(0, wb.getNumProposals());

        final AlertMessage dst3Msg1 = createAlertMessage(Utils.hostFromParts(HOST, H), dst3, H - 1);
        final AlertMessage dst3Msg2 = createAlertMessage(Utils.hostFromParts(HOST, H + 1), dst3, H - 1);
        wb.aggregateForProposal(dst3Msg1, dummyView);
        ret = wb.aggregateForProposal(dst3Msg2, dummyView);
        assertEquals(0, ret.size());
        assertEquals(0, wb.getNumProposals());

        final AlertMessage dst3Msg = createAlertMessage(Utils.hostFromParts(HOST, H), dst2, H - 1);
        ret = wb.aggregateForProposal(dst3Msg, dummyView);
        assertEquals(3, ret.size());
        assertEquals(1, wb.getNumProposals());
    }

    @Test
    public void cutDetectionTestBelowL() {
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final Endpoint dst1 = Utils.hostFromParts("127.0.0.2", 2);
        final Endpoint dst2 = Utils.hostFromParts("127.0.0.3", 2);
        final Endpoint dst3 = Utils.hostFromParts("127.0.0.4", 2);
        Set<Endpoint> ret;

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst1, i);
            ret = wb.aggregateForProposal(msg,
                    dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        // Unlike the previous test, dst2 has < L updates
        for (int i = 0; i < L - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst2, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, i + 1), dst3, i);
            ret = wb.aggregateForProposal(msg, dummyView);
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, H), dst1, H - 1);
        ret = wb.aggregateForProposal(msg, dummyView);
        assertEquals(0, ret.size());
        assertEquals(0, wb.getNumProposals());

        final AlertMessage dst3Msg = createAlertMessage(Utils.hostFromParts(HOST, H), dst3, H - 1);
        ret = wb.aggregateForProposal(dst3Msg, dummyView);
        assertEquals(2, ret.size());
        assertEquals(1, wb.getNumProposals());
    }


    @Test
    public void cutDetectionTestBatch() {
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final int numNodes = 3;
        final List<Endpoint> endpoints = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            endpoints.add(Utils.hostFromParts("127.0.0.2", 2 + i));
        }

        final List<Endpoint> proposal = new ArrayList<>();
        for (final Endpoint endpoint : endpoints) {
            for (int ringNumber = 0; ringNumber < K; ringNumber++) {
                final AlertMessage msg = createAlertMessage(Utils.hostFromParts(HOST, 1), endpoint, ringNumber);
                proposal.addAll(wb.aggregateForProposal(msg, dummyView));
            }
        }

        assertEquals(proposal.size(), numNodes);
    }

    @Test
    public void cutDetectionTestLinkInvalidation() {
        final MembershipView mView = new MembershipView(K);
        final MultiNodeCutDetector wb = new MultiNodeCutDetector(K, H, L);
        final int numNodes = 30;
        final List<Endpoint> endpoints = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            final Endpoint node = Utils.hostFromParts("127.0.0.2", 2 + i);
            endpoints.add(node);
            mView.ringAdd(node, Utils.nodeIdFromUUID(UUID.randomUUID()));
        }

        final Endpoint dst = endpoints.get(0);
        final List<Endpoint> observers = mView.getObserversOf(dst);
        assertEquals(K, observers.size());

        final Set<Endpoint> ret = new HashSet<>();

        // This adds alerts from the observers[0, H - 1) of node dst.
        for (int i = 0; i < H - 1; i++) {
            final AlertMessage msg = createAlertMessage(observers.get(i), dst, EdgeStatus.DOWN, CONFIGURATION_ID, i);
            ret.addAll(wb.aggregateForProposal(msg, mView));
            assertEquals(0, ret.size());
            assertEquals(0, wb.getNumProposals());
        }

        // Next, we add alerts *about* observers[H, K) of node dst.
        final Set<Endpoint> failedObservers = new HashSet<>(K - H - 1);
        for (int i = H - 1; i < K; i++) {
            final List<Endpoint> observersOfObserver = mView.getObserversOf(observers.get(i));
            failedObservers.add(observers.get(i));
            for (int j = 0; j < K; j++) {
                final AlertMessage msg = createAlertMessage(observersOfObserver.get(j), observers.get(i),
                                                            EdgeStatus.DOWN, CONFIGURATION_ID, j);
                ret.addAll(wb.aggregateForProposal(msg, mView));
            }
        }

        // At this point, (K - H - 1) observers of dst will be past H, and dst will be in H - 1. Link invalidation
        // should bring the failed observers and dst to the stable region.
        assertEquals(4, ret.size());
        assertEquals(3, wb.getNumProposals());
        for (final Endpoint node: ret) {
            assertTrue(failedObservers.contains(node) || node.equals(dst));
        }
    }

    private AlertMessage createAlertMessage(final Endpoint src, final Endpoint dst, final int ringNumber) {
        return AlertMessage.newBuilder()
                .setEdgeSrc(src)
                .setEdgeDst(dst)
                .setEdgeStatus(EdgeStatus.UP)
                .addRingNumber(ringNumber)
                .setConfigurationId(CONFIGURATION_ID).build();
    }

    private AlertMessage createAlertMessage(final Endpoint src, final Endpoint dst, final EdgeStatus status,
                                            final long configuration, final int ringNumber) {
        return AlertMessage.newBuilder()
                .setEdgeSrc(src)
                .setEdgeDst(dst)
                .setEdgeStatus(status)
                .addRingNumber(ringNumber)
                .setConfigurationId(configuration).build();
    }
}