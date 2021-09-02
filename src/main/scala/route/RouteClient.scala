package route

import cats.effect.kernel.Resource
import cats.effect.{IO, IOApp}
import fs2.grpc.syntax.all.*
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.{ManagedChannel, Metadata}
import org.slf4j.LoggerFactory
import protos.route.{Point, Rectangle, RouteGuideFs2Grpc, RouteNote}

object RouteClient extends IOApp.Simple {
  val logger = LoggerFactory.getLogger("route-client")

  def managedChannelResource: Resource[IO, ManagedChannel] =
    NettyChannelBuilder
      .forAddress("localhost", 9999)
      .usePlaintext()
      .resource[IO]

//  def request = Point()
//
//  def sendRequest(client: RouteGuideFs2Grpc[IO, Metadata]): IO[Unit] =
//    client.getFeature(request, Metadata()).flatMap { reply =>
//      IO.delay(logger.info(reply.toProtoString))
//    }.handleErrorWith(th => IO.delay(logger.error(th.getLocalizedMessage)))

//  def request = Rectangle()
//
//  def sendRequest(client: RouteGuideFs2Grpc[IO, Metadata]): IO[Unit] =
//    client
//      .listFeatures(request, Metadata())
//      .evalTap(reply => IO.delay(logger.info(reply.toProtoString)))
//      .handleErrorWith(th => fs2.Stream.eval(IO.delay(logger.error(th.getLocalizedMessage))))
//      .compile
//      .drain

//  def request = fs2.Stream[IO, Point](Point())
//
//  def sendRequest(client: RouteGuideFs2Grpc[IO, Metadata]): IO[Unit] =
//    client.recordRoute(request, Metadata()).flatMap { reply =>
//      IO.delay(logger.info(reply.toProtoString))
//    }.handleErrorWith(th => IO.delay(logger.error(th.getLocalizedMessage)))

  def request = fs2.Stream[IO, RouteNote](RouteNote(message = "Hello! My name is Bob."))

  def metadata = {
    val metadata = new Metadata()
    metadata.put(Metadata.Key.of("time", Metadata.ASCII_STRING_MARSHALLER), "12:45")
    metadata
  }

  def sendRequest(client: RouteGuideFs2Grpc[IO, Metadata]): IO[Unit] =
    client.routeChat(request, metadata)
      .evalTap(reply => IO.delay(logger.info(reply.toProtoString)))
      .handleErrorWith(th => fs2.Stream.eval(IO.delay(logger.error(th.getLocalizedMessage))))
      .compile
      .drain


  val run: IO[Unit] =
    managedChannelResource
      .flatMap(managedChannel => RouteGuideFs2Grpc.clientResource[IO, Metadata](managedChannel, identity))
      .use(sendRequest)
}
