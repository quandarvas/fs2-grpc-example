package route

import cats.effect.{IO, IOApp}
import cats.effect.kernel.{Resource, Ref}
import io.grpc.ServerServiceDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.slf4j.LoggerFactory
import protos.route.*
import fs2.grpc.syntax.all.*

object RouteServer extends IOApp.Simple {
  val logger = LoggerFactory.getLogger("route-server")

  val features: Seq[Feature] = Seq(Feature(name = "Feature 1"))

  val routeService: Resource[IO, ServerServiceDefinition] =
    Resource.eval(Ref.of[IO, Map[Point, List[RouteNote]]](Map.empty)).flatMap { ref =>
      RouteGuideFs2Grpc.bindServiceResource[IO](RouteImpl(features, ref, logger))
    }


  def startServer(service: ServerServiceDefinition): IO[Nothing] =
    NettyServerBuilder
      .forPort(9999)
      .addService(service)
      .resource[IO]
      .evalMap { server =>
        IO.delay(logger.info("Server started")) *>
          IO(server.start())
      }
      .useForever

  val run: IO[Unit] = routeService.use(startServer)
}
