package a.b.c;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AddClient {
  AddServiceGrpc.AddServiceBlockingStub stub;
  ManagedChannel channel;

  public static void main(String[] args) {
    int a = 99;
    int b = 100;
    AddClient client = new AddClient();

    AddResponse addResponse = client.stub.add(AddRequest.newBuilder().setA(a).setB(b).build());
    System.out.println("result: " + addResponse.getResult());
  }

  public AddClient() {
    // 创建一个通道，连接到服务端
    channel = ManagedChannelBuilder.forAddress("127.0.0.1", 9999)
        .usePlaintext() // 使用明文传输
        .build();
    // BlockingStub 的意思是阻塞式的Stub，用于同步调用
    // Stub 是一个代理，所有的方法调用都由它来转发到服务端
    stub = AddServiceGrpc.newBlockingStub(channel);
  }
}
