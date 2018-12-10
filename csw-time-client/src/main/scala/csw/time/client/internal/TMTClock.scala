package csw.time.client.internal

import java.time.{Instant, ZoneId, ZonedDateTime}

import com.sun.jna.NativeLong
import csw.time.api.models.TMTTime.{TAITime, UTCTime}
import csw.time.client.internal.native_models.ClockId.{ClockRealtime, ClockTAI}
import csw.time.client.internal.native_models.{NTPTimeVal, TimeSpec, Timex}

import scala.util.Try
import scala.util.control.NonFatal

trait TMTClock {
  def utcTime(zoneId: ZoneId): UTCTime
  def taiTime(zoneId: ZoneId): TAITime
  def offset: Int
  def setOffset(offset: Int): Unit
}

object TMTClock {

  def instance(offset: Int = 0): TMTClock = OSType.value match {
    case OSType.Linux => new LinuxClock()
    case OSType.Other => new NonLinuxClock(offset)
  }

  class LinuxClock extends TMTClock {

    override def utcTime(zoneId: ZoneId): UTCTime = UTCTime(timeFor(ClockRealtime, zoneId))
    override def taiTime(zoneId: ZoneId): TAITime = TAITime(timeFor(ClockTAI, zoneId))

    override def offset: Int = {
      val timeVal = new NTPTimeVal()
      TimeLibrary.ntp_gettimex(timeVal)
      timeVal.tai
    }

    private def timeFor(clockId: Int, zoneId: ZoneId): ZonedDateTime = {
      val timeSpec = new TimeSpec()
      TimeLibrary.clock_gettime(clockId, timeSpec)
      Instant.ofEpochSecond(timeSpec.seconds.longValue(), timeSpec.nanoseconds.longValue()).atZone(zoneId)
    }

    // todo: without sudo or somehow handle it internally?
    // sets the tai offset on kernel (needed when ptp is not setup)
    override def setOffset(offset: Int): Unit = {
      val timex = new Timex()
      timex.modes = 128
      timex.constant = new NativeLong(offset)
      Try(TimeLibrary.ntp_adjtime(timex)).recover {
        case NonFatal(e) =>
          throw new RuntimeException("Failed to set offset, make sure you have sudo access to perform this action.", e.getCause)
      }
    }
  }

  class NonLinuxClock(val offset: Int) extends TMTClock {
    override def utcTime(zoneId: ZoneId): UTCTime = UTCTime(ZonedDateTime.now(zoneId))
    override def taiTime(zoneId: ZoneId): TAITime = TAITime(ZonedDateTime.now(zoneId).plusSeconds(offset))

    override def setOffset(offset: Int): Unit = {}
  }

}
