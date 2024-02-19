package bgu.ds.manager.config;

import java.util.List;

public interface ManagerAWSConfig {
    String managerName();
    String bucketName();
    String sqsTasksInputQueueName();
    String sqsWorkersInputQueueName();
    String sqsWorkersOutputQueueName();
    int minWorkersCount();
    int maxWorkersCount();
    int workersHandlerThreadSleepTime();
    String ec2Name();
    String instanceType();
    String ami();
    String instanceProfileName();
    String securityGroupId();
    List<String> userDataCommands();
}