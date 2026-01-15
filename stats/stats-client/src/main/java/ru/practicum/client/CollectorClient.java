package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.practicum.grpc.stats.useraction.UserActionProto;

@Slf4j
@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendUserActionToCollector(UserActionProto record) {
        try {
            if (collectorStub == null) {
                log.warn("Collector gRPC stub is not initialized. Skipping user action: {}", record.getAllFields());
                return;
            }
            log.info("Data has been transfered: {}", record.getAllFields());
            collectorStub.collectUserAction(record);
        } catch (Exception e) {
            log.warn("Failed to send user action to collector. Skipping. Record: {}. Error: {}",
                    record.getAllFields(), e.getMessage(), e);
        }
    }

}