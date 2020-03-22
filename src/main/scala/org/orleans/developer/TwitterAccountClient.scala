package org.orleans.developer
import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannel
import org.orleans.silo.Services.Client.ServiceClient
import org.orleans.silo.twitterAcount.TwitterGrpc.TwitterStub

class TwitterAccountClient(channel: ManagedChannel)
    extends ServiceClient(channel, new TwitterStub(channel))
    with LazyLogging {}
