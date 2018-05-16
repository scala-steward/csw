abstract class Severity(level: Int, name: String)

case object Disconnected  extends Severity(-2, "Disconnected")
case object Indeterminate extends Severity(-1, "Indeterminate")
case object Okay          extends Severity(0, "Okay")
case object Warning       extends Severity(1, "Warning")
case object Major         extends Severity(2, "Major")
case object Critical      extends Severity(3, "Critical")
