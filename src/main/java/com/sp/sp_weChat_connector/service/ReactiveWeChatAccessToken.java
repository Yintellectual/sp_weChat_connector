package com.sp.sp_weChat_connector.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import ch.qos.logback.core.net.server.Client;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReactiveWeChatAccessToken {
	@Autowired
	@Qualifier("weChatWebClient")
	private WebClient client;
	@Value("${wechat.appid}")
	private String appid;
	@Value("${wechat.secret}")
	private String secret;

	private static final Long EXPIRE_TIME = 3600l;
	
	private Mono<String> asscessTokenMono = null;
	private ConnectableFlux<String> access_token = null;
	
	/*
	 * blackboard pattern
	 * 
	 * You will have a buffer of value. The value will be updated regularly. 
	 * Anyone who are interested will read the value from the buffer.
	 * This is like the blackboard that stores use it to announce some price. The price change once a day by the store stuff, 
	 * and read many times by shoppers. So, I name this pattern "blackboard pattern" 
	 * 
	 * */
	@PostConstruct
	public void init() {
		asscessTokenMono = client.get()
				.uri(builder -> builder.path("/cgi-bin/token").queryParam("grant_type", "client_credential")
						.queryParam("appid", appid).queryParam("secret", secret).build())
				.accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Map.class)
				.map(m -> m.get("access_token").toString()).retry();
		access_token = Flux.interval(Duration.ZERO, Duration.ofSeconds(EXPIRE_TIME))
				.map(l -> asscessTokenMono.block()).replay(1);
		access_token.connect();
	}

	public String get() {
		return access_token.elementAt(0).block();
	}
}
