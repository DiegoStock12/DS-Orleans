### Low-level communication
For each silo we have to notion of a 'master' and a 'slave'. Currently only one master is supported.
Both master and slave run using two threads: 1) control thread to verify slaves/master is still alive and send heartbeats, 2) packet-manager thread which receives packets.

Each silo is configured using a `host` and a `port` for UDP commmunication. Keep in mind that if run on the same computer, different ports per silo need to be used!
A packet has the following form:
```scala
 case class Packet(packetType: String,
                    uuid: String,
                    timestamp: Long,
                    data: List[String] = List())
```
In the table below you can see the packets and its usages:

| Type               | Required data                  | Receiving (master)                                                                                                                                                                                                                                                                                                                                                                                                     | Sending (master)                                                                                                                | Receiving (slave)                                                                                                              | Sending (slave)                                                                                          |
|--------------------|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `HEARTBEAT`        | UUID and timestamp             | Heartbeats are received from slaves and slave info will be updated accordingly.                                                                                                                                                                                                                                                                                                                                        | Heartbeats are send to slaves.                                                                                                  | Heartbeats are received from the master and master info will be updated accordingly                                            | Heartbeats are send to the master.                                                                       |
| `HANDSHAKE`        | UUID and timestamp             | If a handshake is received and the slave is not yet in the cluster it will be added and send a `WELCOME` packet. This means that the master is now aware of this slave and will get heartbeats. It will send other slaves a `SLAVE_CONNECT` packet with details of the newly connected slave. Finally, it will send the new slave a `SLAVE_CONNECT` from all other slaves so that the new slave is aware of the other. | -                                                                                                                               | -                                                                                                                              | When a slave is started it will send the (pre-configured) master a handshake to be added to the cluster. |
| `WELCOME`          | UUID and timestamp             | -                                                                                                                                                                                                                                                                                                                                                                                                                      | Send to a slave when added to the cluster. Afterwards the master will send heartbeats to this slave (and the other way around). | Slave is considered connected to the master and will start sending heartbeats as well as recording heartbeats from the master. | -                                                                                                        |
| `SHUTDOWN`         | UUID and timestamp             | Receives this packets from slaves, so its removed from the slave table. Also other slaves are send a `SLAVE_DISCONNECT` for this slave.                                                                                                                                                                                                                                                                                | If a master goes in shutdown it will send this packet to its slaves so that they shutdown first.                                | This means a master will shutdown and therefore the slave will shutdown (and will then send the same packet to the master).    | Will be send if a slave shuts down.                                                                      |
| `SLAVE_CONNECT`    | UUID, timestamp, host and port | -                                                                                                                                                                                                                                                                                                                                                                                                                      | Will send to a slave when a new slave is added to the cluster (and to make the newly added slave aware of the others).          | This will make the slave aware of another slave.                                                                               | -                                                                                                        |
| `SLAVE_DISCONNECT` | UUID, timestamp                | -                                                                                                                                                                                                                                                                                                                                                                                                                      | Will send to a slave when another slave is disconnecting from the cluster.                                                      | This will make the slave remove the disconnected slave.                                                                        | -                                                                                                        |

**Note:** All communication goes through the master. Although they are aware, slaves won't directly communicate to each other.

## Grain Environment and Functioning

Grains are the unit of computation of the service and reside in the Silos or Slaves. These slaves constantly update the master server who keeps an index of with grains and activations are in each slave. Both master and server hold different services implemented as gRPC servers, which clients and other servers can use in order to obtain information and/or perform operations. These gRPC client and server interfaces communicate via protocol buffers.

#### How a Grain is defined

A grain has two main parts that have to be defined in order to get it to run.

1. **gRPC Service and Message definition**: The first step is defining the message and variable types that will be exchanged between client an server in order to interact with the server. This is done by using protocol buffer and its definition language, whose guide can be found [here](https://developers.google.com/protocol-buffers/docs/proto3). The definition of a service might look like this:

   ```protobuf
   // Grain search
   service GrainSearch {
       rpc SearchGrain (SearchRequest) returns (SearchResult) {
       }
   }
   
   // Message sent by the client asking the master
   // about the server which contains that grain
   message SearchRequest {
       string grainID = 1;
   }
   
   // Message sent by the master indicating
   // which server to ask for.
   // It returns server IP address and port
   message SearchResult {
       string serverAddress = 1;
       int32 serverPort = 2;
   }
   ```

   Here the rpc service is defined as an endpoint receiving a `SearchRequest` and retuning a `SearchResponse`, messages whose composition is defined below.

   These `.proto` files have to be compiled with the tools provided by [ScalaPB](https://scalapb.github.io/grpc.html), which creates the necessary Scala code to implement said service such as client stub and interfaces for the service to override.

2. **Scala service implementation:** The implementations can be found in the `silo/Services/Impl` folder and they includes the operations that the grain should perform when a certain rpc endpoint is called by a client. An example could be in the case of a `Greeter` grain that simply salutes the client. The developer should then provide this method in the grain implementation.

   ```scala
   // Receives a name and greets that user
   override def sayHello(request: HelloRequest): Future[HelloReply] = {
       logger.debug("Received a request by " + request.name)
       val reply = HelloReply(message = "Hello " + request.name)
       // Return successful reply
       Future.successful(reply)
     }
   ```

   These services are binded to a certain IP and port, which can be done through the `ServerBuilder` gRPC object. Each grain is therefore a gRPC server waiting for requests at a certain port.

   ```scala
   // Create a Greeter Grain in port 9000
   ServerBuilder
     .forPort(9000)
     .addService(GreeterGrpc.bindService(new GreeterImpl(),
         ExecutionContext.fromExecutorService(newSingleThreadExecutor())))
     .build
     .start
   ```

### Grains, functionality and properties

Each grain is an independent computing unit and it's the smallest computation unit in the system. A grain is a **single threaded** program that processes calls by clients, executes some predefined operations and answers the clients with a successful or failed Future as a response.

Thus, the grain executes just one request concurrently, which saves the problem of simultaneous access to variables and/or grain state. By default a gRPC server uses an `ExecutionContext` made out of a ThreadPool with multiple threads to serve the requests in parallel, we make sure that this does not happen by supplying a single threaded execution service as in the previous code when creating a new grain 

```scala
// Adding a grain and explicitly declaring a singleThreadedExecutor
.addService(GreeterGrpc.bindService(new GreeterImpl(),
      ExecutionContext.fromExecutorService(newSingleThreadExecutor())))
```

Here we explicitly provide a custom `ExecutionService` with a single thread so multiple requests are processed sequentially

### Client-Server Interaction

After a grain is defined and running on a certain silo, the clients can then retrieve the information of this grain and invoke its methods through gRPC calls. For that, it needs to perform a two step operation:

1. Ask the master about that grain's ID and retrieve its IP address and port
2. With the stub of that grain's offered method, perform a gRPC call that is then executed on the server as if it was local

In order to make this more efficient, the `GrainFactory` class was defined. This way, the client can execute the `getGrain` function and the factory takes care of the searching with the master and returns the grain access interface transparently for the client, so the client can then invoke the methods desired on that grain.

```scala
/**
   * Gets the grain reference
   *
   * It does everything in one go:
   * - Asks the master for a specific grain
   * - The master answers eventually with the address of the grain (server:port)
   * - Builds the stub so the user can just use it
   *
   * @param service service type from the defined ones
   * @param id      id of the service
   */
  def getGrain(service: Service, id: String): Future[ServiceClient] = {
    val f: Future[SearchResult] = searchStub.search(id)
    f.map(serverDetails => {
      val c = ManagedChannelBuilder.forAddress(serverDetails.serverAddress, 		                       serverDetails.serverPort).usePlaintext().build()
      service match {
        //TODO maybe we could have a map of services to clients so it's more automated
        case Service.Hello =>
          val stub = GreeterGrpc.stub(c)
          new GreeterClient(c, stub)
        case Service.ActivateGrain =>
          val stub = ActivateGrainServiceGrpc.stub(channel)
          new ActivateGrainClient(channel, stub)
      }
    })
  }
```

As can be seen, the grain is then casted according to the service specified by the client and the stub is returned as a Future in accordance to the asynchronous nature of the whole system.

**Note:** Some improvements could be made in order to make the stub retrieval process less "hacky" by hard-coding the case of the Service and its corresponding stub but I could not visualize a much better solution yet, this works though.

### Master Server

The master server is in charge of keeping the index of grains and activations, which are kept as a Map.  The map has the following structure:

```scala
// Definition of the grain map. Concurrent so multiple threads
// can edit it at the same time without issues.
// String : ID of the grain
// GrainDescriptor: List of descriptions the the different ACTIVATIONS of that grain
// (can be in multiple slaves at the same time)
val grainMap: ConcurrentHashMap[String, GrainDescriptor] =  
					new ConcurrentHashMap[String, GrainDescriptor]()

// Grain descriptor structure
case class GrainDescriptor(state: GrainState, location : SlaveDetails)

// Slave details structure. 
// Port and Ip of the grain activation in that slave
case class SlaveDetails(address: String, port: Int)
```

Apart from communicating with Slaves, the master offers the clients several services that allow them to interact with the system. The services provided, the invocation and the operations performed by the master are summarized in the following table:

| Service        | Invocation             | Operations                                                   | Result                                                       |
| -------------- | ---------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `GRAIN_SEARCH` | `search(grainID)`      | The master first checks its grainMap and checks which slave has that grainID. It then sends a message to the slave ---possibly the one with the least load--- asking him to return the information of that grain, and returns that information to the client as a Future | Client receives the IP address and port of the specified grain, so it can access it via gRPC |
| `CREATE_GRAIN` | `createGrain(service)` | The master acts as an intermediary between client and slave and simply chooses the most appropriate slave to create a new grain in for a certain service or grain type | Slave creates the grain and returns the information (ip, port) to the master, which relays it to the client |
| `DELETE_GRAIN` | `deleteGrain(grainId)` | The master again acts as a relay service and tells the server to delete a certain grain | Slave deletes the grain and sends a successful Future to the master, who relays it to the user |



As can be seen, all the operations are performed asynchronously and all of them work with futures in order to maximize efficiency and concurrency. These gRPC services (unlike grains), work with a ThreadPool provided by the `ExecutionContext`, which means that multiple calls can be executed in parallel.



### Silos or Slave Servers

The silos or slaves are the servers responsible for holding the grains. Among their responsibilities are:

1. Keep the master updated with the current grains present and its information and state
2. Providing a way or channel for grains to communicate with each other
3. Provide gRPC endpoints for certain operations to be performed

#### Operations available in the Slave

The Slaves have to provide certain gRPC endpoints in order to perform the operations needed by the client and the master.

| Service          | Description                                                  |
| ---------------- | ------------------------------------------------------------ |
| `GRAIN_CREATION` | When the client tells the master that it wants to create a new grain of a certain type, the master is then responsible for selecting a slave to create that grain. The server should then pick a free port from the ones available and create a new gRPC server with a specific ID and return the `PORT` and `grainID` to the master and client so it can be indexed |
| `GRAIN_DELETION` | Invoked by the master on behalf of the client. Simply deletes the grain with that ID |

Apart from those gRPC services, there should be a common ground that all grain in a Slave can access for useful information, as well as for communicating and coordinating between different slaves, we'll call this the **Slave Runtime**.

#### Slave Runtime

The slave runtime is a thread running as a daemon in the slaves that is made accessible (passed by reference) to all the grains running on the slave so they can access it. Among the necessary tasks to be performed by this runtime are:

1. Keeing a `ConcurrentMap` mapping service Id's to port, that is periodically sent to the Master as an update
2. Allow **grains access to this map**, so they can see in which port a certain grain is accessible whenever a grain has to communicate with other grain
3. In case the grains that need to communicate are in different slaves, the runtime should also **provide an index of grains in other servers** or at least guide the grain to find the "foreigner" grain.
4. For that reason, the **runtimes of different slaves should communicate constantly or periodically** in an efficient manner, or maybe having the master relay the info received by other slaves to the rest of them , to increase communication efficiency.
5. It should also provide a way to **turn off grains in case of lack of activity, or initiate the replication process** in case a grain is overloaded with requests.
6. It should also incorporate the rules for assigning free ports in the machine whenever a new grain is created

For that reason, the slave runtime is a very important part including most of the intelligence behind the system such as deactivation, replication and indexing rules. 