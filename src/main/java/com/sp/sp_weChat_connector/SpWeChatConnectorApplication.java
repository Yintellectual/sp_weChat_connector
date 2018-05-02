package com.sp.sp_weChat_connector;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.springframework.http.MediaType;

import org.springframework.util.LinkedMultiValueMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sp.sp_weChat_connector.service.ReactiveWeChatAccessToken;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
public class SpWeChatConnectorApplication {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Bean("weChatWebClient")
	public WebClient client() {
		return WebClient.builder().baseUrl("https://api.weixin.qq.com")
				// .builder().baseUrl("https://api.github.com").defaultHeader("User-Agent",
				// "Spring-boot WebClient").filter(ExchangeFilterFunctions
				// .basicAuthentication("Yintellectual",
				// "1e7dd5c5ba9e23df9a423cd26de72b74a64fbd17"))
				.filter(eff).build();
	}

	@Autowired
	private ReactiveWeChatAccessToken token;

	@Autowired
	@Qualifier("weChatWebClient")
	private WebClient client;

	public static void main(String[] args) {
		SpringApplication.run(SpWeChatConnectorApplication.class, args);
	}

	ExchangeFilterFunction eff = new ExchangeFilterFunction() {
		@Override
		public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
			logger.info("\n\n" + request.method().toString().toUpperCase() + ":\n\nURL:" + request.url().toString()
					+ ":\n\nHeaders:" + request.headers().toString() + "\n\nAttributes:" + request.attributes()
					+ "\n\n");

			return next.exchange(request);
		}
	};

	// @GetMapping(value = "/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	// public Flux<Long> test() {
	// return Flux.interval(Duration.ofSeconds(1));
	// }

	@PostMapping("/test")
	public String test(@RequestBody String data) {
		System.out.println(data);
		return data;
	}

	@GetMapping(value = "/QRCode/{text}", produces = MediaType.IMAGE_PNG_VALUE)
	public byte[] qrCode(@PathVariable String text) throws WriterException, IOException {
		return getQRCodeImage(text, 350, 350);
	}

	// @Bean
	// public RouterFunction<ServerResponse> helloWorld() {
	// return
	// RouterFunctions.route(path("/hello-world").and(method(HttpMethod.POST)),
	// req -> ServerResponse.ok()
	// .contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromObject(token.get())));
	// }

	// client.post()
	// .uri(builder ->
	// builder.path("/cgi-bin/qrcode/create").queryParam("access_token",
	// token.get()).build())
	// .contentType(MediaType.APPLICATION_JSON)
	// .body(BodyInserters.fromObject("{\"expire_seconds\": 604800,
	// \"action_name\": \"QR_SCENE\", \"action_info\": {\"scene\":
	// {\"scene_id\": 123}}}"))
	// .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Mp.class).block(Duration.ofSeconds(5))

	private static final String QR_CODE_IMAGE_PATH = "./MyQRCode.png";

	private static void generateQRCodeImage(String text, int width, int height, String filePath)
			throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

		Path path = FileSystems.getDefault().getPath(filePath);
		MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
	}

	private byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

		ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
		byte[] pngData = pngOutputStream.toByteArray();
		return pngData;
	}

	private boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

	private static class StreamGobbler implements Runnable {
		private InputStream inputStream;
		private Consumer<String> consumer;

		public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
			this.inputStream = inputStream;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
		}
	}

	@Bean
	CommandLineRunner commandLineRunner() {
		return new CommandLineRunner() {

			@SuppressWarnings("serial")
			@Override
			public void run(String... args) throws Exception {

				// generateQRCodeImage("hello world", 350, 350, QR_CODE_IMAGE_PATH);

				// logger.info("\n\n\n"+BodyInserters.fromFormData(new
				// LinkedMultiValueMap<String, String>(){{
				// add("name", "ttst");
				// }})+"\n\n\n");
				// System.out.println(
				// client.post().uri(builder->builder.path("/user/repos").build())
				// .contentType(MediaType.APPLICATION_JSON)
				// .body(BodyInserters.fromObject(new LinkedMultiValueMap<String, String>(){{
				// add("name", "tttst");
				// }}))
				// .retrieve()
				// .bodyToMono(String.class)
				// .block(Duration.ofSeconds(3)));

				/*
				 * https://open.weixin.qq.com/connect/oauth2/authorize?
				 * appid=wx520c15f417810387&
				 * redirect_uri=https%3A%2F%2Fchong.qq.com%2Fphp%2Findex.php%3Fd%3D%26c%
				 * 3DwxAdapter%26m%3DmobileDeal%26showwxpaytitle%3D1%26vb2ctag%
				 * 3D4_2030_5_1194_60& response_type=code& scope=snsapi_base&
				 * state=123#wechat_redirect
				 * 
				 * 
				 * https://open.weixin.qq.com/connect/oauth2/authorize?
				 * appid=wxf0e81c3bee622d60&
				 * redirect_uri=http%3A%2F%2Fnba.bluewebgame.com%2Foauth_response.php&
				 * response_type=code& scope=snsapi_userinfo& state=STATE#wechat_redirect
				 */

				// logger.info("\n\n\n\n"+ client
				// .get()
				// .uri(builder->builder.path("/cgi-bin/user/info")
				// .queryParam("access_token", token.get())
				// .queryParam("openid", "oPEtq1QWobper9qCFtPWwihkjNEE")
				// .queryParam("lang", "zh_CN")
				// .build())
				// .retrieve()
				// .bodyToMono(String.class).block()
				// );

				// logger.info("\n\n\n\n"+ client
				// .get()
				// .uri("https://open.weixin.qq.com/connect/oauth2/authorize?"
				// +"appid=wxe0ef1b4366d54654&"
				// +"redirect_uri=http%3A%2F%2Fwww.hanchen.site&"
				// +"response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect")
				//
				// .retrieve()
				// .bodyToMono(String.class).block()
				// );

				String homeDirectory = System.getProperty("user.home");
				Process process;
				if (isWindows) {
					process = Runtime.getRuntime().exec(String.format("cmd.exe /c dir %s", homeDirectory));
				} else {
					process = Runtime.getRuntime().exec(String.format("sh -c ls %s", homeDirectory));
				}
				StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
				Executors.newSingleThreadExecutor().submit(streamGobbler);
				int exitCode = process.waitFor();
				assert exitCode == 0;
			}
		};
	}
}
