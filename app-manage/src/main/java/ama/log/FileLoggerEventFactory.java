package ama.log;

import com.lmax.disruptor.EventFactory;

public class FileLoggerEventFactory  implements EventFactory<FileLoggerEvent> {
    @Override
    public FileLoggerEvent newInstance() {
        return new FileLoggerEvent();
    }
}
