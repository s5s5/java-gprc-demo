# 如何使用 gRPC

视频教程：https://www.bilibili.com/video/BV1Np4y1x7JB

## 理论

像调用本地程序一样调用远程程序

![Concept Diagram](https://grpc.io/img/landing-2.svg)

## 安装

- Maven 依赖

```
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-netty-shaded</artifactId>
  <version>1.50.2</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-protobuf</artifactId>
  <version>1.50.2</version>
</dependency>
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-stub</artifactId>
  <version>1.50.2</version>
</dependency>
<dependency> <!-- necessary for Java 9+ -->
  <groupId>org.apache.tomcat</groupId>
  <artifactId>annotations-api</artifactId>
  <version>6.0.53</version>
  <scope>provided</scope>
</dependency>
```

- Maven 插件

```xml

<build>
  <extensions>
    <extension>
      <groupId>kr.motd.maven</groupId>
      <artifactId>os-maven-plugin</artifactId>
      <version>1.6.2</version>
    </extension>
  </extensions>
  <plugins>
    <plugin>
      <groupId>org.xolstice.maven.plugins</groupId>
      <artifactId>protobuf-maven-plugin</artifactId>
      <version>0.6.1</version>
      <configuration>
        <protocArtifact>com.google.protobuf:protoc:3.21.7:exe:${os.detected.classifier}
        </protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.50.2:exe:${os.detected.classifier}
        </pluginArtifact>
        <protoSourceRoot>src/resources/proto</protoSourceRoot>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>compile</goal>
            <goal>compile-custom</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

注意这里，按实际 proto 目录更改 `<protoSourceRoot>src/resources/proto</protoSourceRoot>`

## 生成代码

定义 proto 文件，在 `src/resources/proto` 目录下新建 `add.proto` 文件

- 这里是一个简单的加法运算

```proto
syntax = "proto3";

package grpc; // 没什么用，只是为了生成的代码包名
option java_package = "a.b.c"; // 生成的代码包名
option java_outer_classname = "AddServiceProto"; // 生成的代码类名
option java_multiple_files = true; // 生成多个文件

service AddService {
    rpc add(AddRequest) returns (AddResponse) {}
}

message AddRequest {
    int32 a = 1; // 1表示字段的序号
    int32 b = 2;
}

message AddResponse {
    int32 result = 1;
}
```

- 运行 maven 插件 lifecycle 中的 install
- 生成代码中会有 `AddServiceImplBase` 抽象类，我们要去继承该类，实现 add 方法
    - `target/generated-sources/protobuf/grpc-java/a.b.c/AddServiceGrpc.java`

```java
public static abstract class AddServiceImplBase implements io.grpc.BindableService {
  public void add(useGoogleRPC.AddRequest request,
      io.grpc.stub.StreamObserver<useGoogleRPC.AddResponse> responseObserver) {
    io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddMethod(), responseObserver);
  }
}
```

## 服务端

```java
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

```

## 客户端

```java
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
```