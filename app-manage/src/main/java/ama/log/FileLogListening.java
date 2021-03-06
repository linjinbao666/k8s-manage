package ama.log;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class FileLogListening implements ApplicationContextAware{

    private long lastTimeFileSize = 0;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String logPath = "imageBuild.log";

        try {
            File logFile = ResourceUtils.getFile(logPath);
            final RandomAccessFile randomFile = new RandomAccessFile(logFile, "rw");
            //指定文件可读可写
            ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
            exec.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    try {
                        randomFile.seek(lastTimeFileSize);
                        String tmp = "";
                        while ((tmp = randomFile.readLine()) != null) {
                            String log=new String(tmp.getBytes("ISO8859-1"));
                            LoggerDisruptorQueue.publishEvent(log);
                        }
                        lastTimeFileSize = randomFile.length();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * 监听日志文件
     *
     * @throws IOException
     */
    @PostConstruct
    public void start() throws IOException {

    }
}