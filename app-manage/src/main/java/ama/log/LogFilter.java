package ama.log;

import ama.log.LoggerDisruptorQueue;
import ama.log.LoggerMessage;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.DateFormat;

@Service
public class LogFilter extends Filter<ILoggingEvent>{
    @Override
    public FilterReply decide(ILoggingEvent event) {
        LoggerMessage loggerMessage = new LoggerMessage(
                event.getMessage()
                , DateFormat.getDateTimeInstance().format(new Date(event.getTimeStamp())),
                event.getThreadName(),
                event.getLoggerName(),
                event.getLevel().levelStr
        );
        LoggerDisruptorQueue.publishEvent(loggerMessage);
        return FilterReply.ACCEPT;
    }
}
