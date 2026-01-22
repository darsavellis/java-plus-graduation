package ru.practicum.ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.collector.service.UserActionHandler;
import ru.practicum.ewm.stats.grpc.UserActionControllerGrpc;
import ru.practicum.ewm.stats.grpc.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    final UserActionHandler userActionHandler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.debug("gRPC collectUserAction called: {}", request);
        try {
            userActionHandler.handle(request);
            log.info("User action successfully handled: {}", request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error("Error handling user action: {}", request, exception);
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(exception)));
        }
    }
}
