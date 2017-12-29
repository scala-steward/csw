package csw.services.alarms.internal

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.services.alarms.AlarmService
import csw.services.alarms.models.AlarmEntity.SeverityLevel
import csw.services.alarms.models.AlarmKey

object RefreshAlarmBehaviorFactory {
  def make(alarmService: AlarmService, initialMap: Map[AlarmKey, SeverityLevel]): Behavior[RefreshAlarmMessages] =
    Actor.withTimers(
      timerScheduler ⇒
        Actor.mutable(
          ctx ⇒ new RefreshAlarmBehavior(alarmService, initialMap, timerScheduler)
      )
    )
}
