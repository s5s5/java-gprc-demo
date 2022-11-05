package a.b.c;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class AddService extends AddServiceGrpc.AddServiceImplBase {
  public static void main(String[] args) throws IOException, InterruptedException {
    // 创建一个服务，监听端口9999
    Server server = ServerBuilder.forPort(9999)
        .addService(new AddService()) // 注册服务
        .build(); // 构建服务

    server.start(); // 启动服务，端口可能被占用，启动失败时抛出异常 throws IOException
    System.out.println("server started");

    // 阻塞主线程，避免服务退出，可能会抛出 InterruptedException
    server.awaitTermination();
  }

  public void add(AddRequest request, io.grpc.stub.StreamObserver<AddResponse> responseObserver) {
    // 从request中获取参数
    int result = myAdd(request.getA(), request.getB());
    // 将结果封装到response中
    // .onNext()方法用来发送消息，可以调用多次
    // newBuilder()方法是由protobuf生成的，用来创建一个AddResponse对象
    // .build()方法是由protobuf生成的，用来将AddResponse对象序列化为字节数组
    responseObserver.onNext(AddResponse.newBuilder().setResult(result).build());
    // .onCompleted()方法用来通知客户端，消息发送完毕
    responseObserver.onCompleted();
  }

  public int myAdd(int a, int b) {
    return a + b;
  }
}
