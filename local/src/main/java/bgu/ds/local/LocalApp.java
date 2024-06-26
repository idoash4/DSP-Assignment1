package bgu.ds.local;

import bgu.ds.common.awssdk.Ec2Operations;
import bgu.ds.common.awssdk.S3ObjectOperations;
import bgu.ds.common.awssdk.SqsOperations;
import bgu.ds.common.sqs.SqsMessageConsumer;
import bgu.ds.common.sqs.protocol.AddInputMessage;
import bgu.ds.common.sqs.protocol.SetWorkersCountMessage;
import bgu.ds.common.sqs.protocol.SqsMessageType;
import bgu.ds.common.sqs.protocol.TerminateManagerMessage;
import bgu.ds.local.config.AWSConfigProvider;
import bgu.ds.local.config.LocalAWSConfig;
import bgu.ds.local.processors.SqsOutputMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalApp {
    private static final Logger logger = LoggerFactory.getLogger(LocalApp.class);
    final static S3ObjectOperations s3 = S3ObjectOperations.getInstance();
    final static Ec2Operations ec2 = Ec2Operations.getInstance();
    final static SqsOperations sqs = SqsOperations.getInstance();
    final static LocalAWSConfig config = AWSConfigProvider.getConfig();

    private SqsMessageConsumer consumer;
    private final Map<UUID, String> inputUUIDToOutputPath = new ConcurrentHashMap<>();
    private AtomicInteger inputFilesCount;
    private final String[] inFilesPath;
    private final String[] outFilesPath;
    private final int tasksPerWorker;
    private final boolean terminateManager;

    public LocalApp(String[] inFilesPath, String[] outFilesPath, int tasksPerWorker, boolean terminateManager) {
        this.inFilesPath = inFilesPath;
        this.outFilesPath = outFilesPath;
        this.tasksPerWorker = tasksPerWorker;
        this.terminateManager = terminateManager;
    }

    private void setup() {
        if (!ec2.isInstanceRunning(config.ec2Name())) {
            ec2.createInstance(config.ec2Name(), config.instanceType(), config.ami(), 1, 1,
                    config.instanceProfileName(), config.securityGroupName(), config.userDataCommands());
        } else {
            logger.info("Instance {} is already running", config.ec2Name());
        }

        s3.createBucketIfNotExists(config.bucketName());
        sqs.createQueueIfNotExists(config.sqsInputQueueName());
    }

    public void addOutputFile(UUID inputId, String bucketName, String objectKey) {
        String filePath = inputUUIDToOutputPath.remove(inputId);
        if (filePath == null) {
            logger.warn("Input id {} is not found", inputId);
        }

        try {
            logger.info("Writing object {} from bucket {} to path {}", objectKey, bucketName, filePath);
            s3.getObject(objectKey, bucketName, new File(filePath));
        } catch (IOException e) {
            logger.error("Failed to write object {} from bucket {} to path {}", objectKey, bucketName, filePath, e);
        }

        if (inputFilesCount.decrementAndGet() == 0) {
            consumer.shutdown();
        }
    }

    public void start() {
        setup();

        // Send a message to the Manager to set the number of workers
        String inputQueueUrl = sqs.getQueueUrl(config.sqsInputQueueName());
        sqs.sendMessage(inputQueueUrl, new SetWorkersCountMessage(tasksPerWorker));

        // Set the input files count
        inputFilesCount = new AtomicInteger(inFilesPath.length);

        // Create a queue for the output
        String outputQueueName = config.sqsOutputQueuePrefix() + "-" + UUID.randomUUID();
        sqs.createQueueIfNotExists(outputQueueName);

        // Start the consumer
        this.consumer = new SqsMessageConsumer(sqs.getQueueUrl(outputQueueName), config.consumerThreads(),
                config.consumerVisibilityTimeout(), config.consumerMaxVisibilityExtensionTime(),
                config.consumerVisibilityThreadSleepTime());
        consumer.registerProcessor(SqsMessageType.SEND_OUTPUT, new SqsOutputMessageProcessor(this));
        consumer.start();

        // Send the input files to the Manager
        for (int i=0; i < inFilesPath.length; i++) {
            String objectKey = s3.putObject(inFilesPath[i], config.bucketName());
            UUID uuid = UUID.randomUUID();
            inputUUIDToOutputPath.put(uuid, outFilesPath[i]);
            sqs.sendMessage(inputQueueUrl, new AddInputMessage(uuid, config.bucketName(), objectKey, outputQueueName));
        }

        // Wait for all the input files to be processed
        try {
            consumer.join();
        } catch (InterruptedException e) {
            logger.info("LocalApp was interrupted while waiting for consumer thread", e);
        }

        sqs.deleteQueueIfExists(outputQueueName);

        if (terminateManager) {
            logger.info("Terminating Manager instance");
            sqs.sendMessage(inputQueueUrl, new TerminateManagerMessage());
        }
    }
}
