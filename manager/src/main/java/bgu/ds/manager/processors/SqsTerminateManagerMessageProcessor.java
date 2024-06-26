package bgu.ds.manager.processors;

import bgu.ds.common.sqs.SqsMessageProcessor;
import bgu.ds.common.sqs.protocol.SqsMessage;
import bgu.ds.common.sqs.protocol.SqsMessageType;
import bgu.ds.manager.Manager;

public class SqsTerminateManagerMessageProcessor implements SqsMessageProcessor {
    private final Manager manager;

    public SqsTerminateManagerMessageProcessor(Manager manager) {
        this.manager = manager;
    }

    public void process(SqsMessage message) {
        if (message.getMessageType() != SqsMessageType.TERMINATE_MANAGER) {
            throw new IllegalArgumentException("Invalid message type: " + message.getMessageType());
        }
        manager.shutdown();
    }
}
