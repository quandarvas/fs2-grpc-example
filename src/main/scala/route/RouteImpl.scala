package route

import org.slf4j.Logger
import io.grpc.{StatusException, Status}
import cats.effect.kernel.Ref
import cats.effect.IO
import io.grpc.Metadata
import protos.route.*
import scala.math._

class RouteImpl(features: Seq[Feature], routeNotesRef: Ref[IO, Map[Point, List[RouteNote]]], logger: Logger) extends RouteGuideFs2Grpc[IO, Metadata] {

  override def getFeature(request: Point, ctx: Metadata): IO[Feature] =
    IO.fromOption(findFeature(request))(StatusException(Status.NOT_FOUND, ctx))

  override def listFeatures(request: Rectangle, ctx: Metadata): fs2.Stream[IO, Feature] = {
    val left = request.getLo.longitude min request.getHi.longitude
    val right = request.getLo.longitude max request.getHi.longitude
    val top = request.getLo.latitude max request.getHi.latitude
    val bottom = request.getLo.latitude min request.getHi.latitude

    fs2.Stream.emits(
      features.filter { feature =>
        val lat = feature.getLocation.latitude
        val lon = feature.getLocation.longitude
        lon >= left && lon <= right && lat >= bottom && lat <= top
      }
    )
  }

  override def recordRoute(request: fs2.Stream[IO, Point], ctx: Metadata): IO[RouteSummary] =
    IO.realTime.flatMap { start =>
      request
        .zipWithPrevious
        .fold(RouteSummary()) {
          case (summary, (maybePrevPoint, currentPoint)) =>
            // Compute the next status based on the current status.
            summary.copy(
              pointCount = summary.pointCount + 1,
              featureCount =
                summary.featureCount + (if (findFeature(currentPoint).isDefined) 1
                else 0),
              distance = summary.distance + maybePrevPoint
                .map(calcDistance(_, currentPoint))
                .getOrElse(0)
            )
        }
        .compile
        .lastOrError
        .flatMap { summary =>
          IO.realTime.map { end =>
            summary.copy(elapsedTime = (end - start).toMillis)
          }
        }
    }

  override def routeChat(request: fs2.Stream[IO, RouteNote], ctx: Metadata): fs2.Stream[IO, RouteNote] =
    request.flatMap { note =>
      val updateMapEffect: IO[List[RouteNote]] =
        routeNotesRef.modify { routeNotes =>
          val messages = routeNotes.getOrElse(note.getLocation, Nil)
          (routeNotes.updated(note.getLocation, note :: messages), note :: messages)
        }
      fs2.Stream.evalSeq {
        IO.delay(logger.info(s"time=${ctx.get(Metadata.Key.of("time", Metadata.ASCII_STRING_MARSHALLER))}")) *>
          updateMapEffect
      }
    }

  private def findFeature(point: Point): Option[Feature] =
    features.find(f => f.getLocation == point && f.name.nonEmpty)

  private def calcDistance(start: Point, end: Point): Int = {
    val r = 6371000
    val CoordFactor: Double = 1e7
    val lat1 = toRadians(start.latitude) / CoordFactor
    val lat2 = toRadians(end.latitude) / CoordFactor
    val lon1 = toRadians(start.longitude) / CoordFactor
    val lon2 = toRadians(end.longitude) / CoordFactor
    val deltaLat = lat2 - lat1
    val deltaLon = lon2 - lon1

    val a = sin(deltaLat / 2) * sin(deltaLat / 2)
    +cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    (r * c).toInt
  }
}
