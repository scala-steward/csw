package csw.time.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import csw.time.api.models.Cancellable;
import csw.time.api.models.CswInstant.TaiInstant;
import csw.time.api.models.CswInstant.UtcInstant;
import csw.time.api.scaladsl.TimeService;
import csw.time.client.extensions.RichInstant;
import org.junit.AfterClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaTimeServiceTest extends JUnitSuite {

    private static int TaiOffset = 37;
    private TestProperties testProperties = TestProperties$.MODULE$.instance();

    private static ActorSystem system = ActorSystem.create("time-service");
    private static TimeService jTimeService = TimeServiceFactory.make(TaiOffset, system);

    @AfterClass
    public static void teardown() throws Exception {
        Await.result( system.terminate(), FiniteDuration.create(5, TimeUnit.SECONDS));
    }

    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
    @Test
    public void shouldGetUTCTime(){
        UtcInstant utcInstant = jTimeService.utcTime();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals((double)expectedMillis, (double)utcInstant.value().toEpochMilli(), 5.0); // Scala test uses +-5...
    }

    //DEOPSCSW-537: Optimum way for conversion from UTC to TAI
    @Test
    public void shouldConvertUtcToTai(){
        UtcInstant utcInstant = jTimeService.utcTime();
        TaiInstant taiInstant = jTimeService.toTai(utcInstant);

        assertEquals(Duration.between(utcInstant.value(),taiInstant.value()).getSeconds(), TaiOffset);
    }

    //DEOPSCSW-534: PTP accuracy and precision while reading UTC
    @Test
    public void shouldGetMaximumPrecisionSupportedBySystemInUtc(){

        assertFalse(new RichInstant().formatNanos(testProperties.precision(),  jTimeService.utcTime().value()).endsWith("000"));

    }

    //------------------------------TAI-------------------------------

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAITime(){
        TaiInstant taiInstant = jTimeService.taiTime();
        Instant expectedTaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = expectedTaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiInstant.value().toEpochMilli());
    }

    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void shouldGetTAIOffset(){
        assertEquals(TaiOffset, jTimeService.taiOffset());
    }

    //DEOPSCSW-537: Optimum way for conversion from UTC to TAI
    @Test
    public void shouldConvertTaiToUtc(){
        TaiInstant taiInstant = jTimeService.taiTime();
        UtcInstant utcInstant = jTimeService.toUtc(taiInstant);

        assertEquals(Duration.between(utcInstant.value(),taiInstant.value()).getSeconds(), TaiOffset);
    }

    //DEOPSCSW-538: PTP accuracy and precision while reading TAI
    @Test
    public void shouldGetMaximumPrecisionSupportedBySystemInTai(){

        assertFalse(new RichInstant().formatNanos(testProperties.precision(), jTimeService.taiTime().value()).endsWith("000"));

    }

    //------------------------------Scheduling-------------------------------

    //DEOPSCSW-542: Schedule a task to execute in future
    @Test
    public void shouldScheduleTaskAtStartTime(){
        TestProbe testProbe = new TestProbe(system);

        TaiInstant idealScheduleTime = new TaiInstant(jTimeService.taiTime().value().plusSeconds(1));

        Runnable task = () -> testProbe.ref().tell(jTimeService.taiTime(), ActorRef.noSender());

        jTimeService.scheduleOnce(idealScheduleTime, task);

        TaiInstant actualScheduleTime = testProbe.expectMsgClass(TaiInstant.class);

        assertEquals(actualScheduleTime.value().getEpochSecond() - idealScheduleTime.value().getEpochSecond(), 0);
        assertTrue(actualScheduleTime.value().getNano() - idealScheduleTime.value().getNano() < testProperties.allowedJitterInNanos());
    }
}