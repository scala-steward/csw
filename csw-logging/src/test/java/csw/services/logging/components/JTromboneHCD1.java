package csw.services.logging.components;

import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.utils.LogUtil;

public class JTromboneHCD1 implements JTromboneHCDLogger {
    private ILogger logger = getLogger();

    public void startLogging() {
        new LogUtil().logInBulk(logger);
    }

    public void setLevel() {
        logger.setLogLevel(LoggingLevels.FATAL$.MODULE$);
    }
}
