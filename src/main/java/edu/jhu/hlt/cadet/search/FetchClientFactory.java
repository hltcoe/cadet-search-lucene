/*
 * Copyright 2012-2017 Johns Hopkins University HLTCOE. All rights reserved.
 * This software is released under the 2-clause BSD license.
 * See LICENSE in the project root directory.
 */

package edu.jhu.hlt.cadet.search;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import edu.jhu.hlt.concrete.access.FetchCommunicationService;

public class FetchClientFactory {
    protected TTransport transport;
    protected TCompactProtocol protocol;

    public FetchCommunicationService.Client createClient(String host, int port) throws TTransportException {
        transport = new TFramedTransport(new TSocket(host, port), Integer.MAX_VALUE);
        protocol = new TCompactProtocol(transport);
        transport.open();
        return new FetchCommunicationService.Client(protocol);
    }

    public void freeClient() {
        if (transport.isOpen()) {
            transport.close();
        }
    }
}
