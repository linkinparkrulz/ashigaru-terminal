package com.sparrowwallet.nightjar.stomp;

import com.samourai.http.client.HttpUsage;
import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.IStompClientService;
import com.sparrowwallet.nightjar.http.JavaHttpClient;
import com.sparrowwallet.nightjar.http.JavaHttpClientService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class JavaStompClientService implements IStompClientService {
    private final JavaHttpClientService httpClientService;
    private final ThreadPoolTaskScheduler taskScheduler;

    public JavaStompClientService(JavaHttpClientService httpClientService) {
        this.httpClientService = httpClientService;

        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("stomp-heartbeat");
        taskScheduler.setDaemon(true);
        taskScheduler.initialize();
    }

    @Override
    public IStompClient newStompClient() {
        JavaHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.COORDINATOR_WEBSOCKET);
        return new JavaStompClient(httpClient, taskScheduler);
    }
}
