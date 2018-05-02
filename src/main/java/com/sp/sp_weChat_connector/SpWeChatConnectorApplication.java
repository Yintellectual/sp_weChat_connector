package com.sp.sp_weChat_connector;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

	



	@Data
	@NoArgsConstructor
	static class ExecuteCommandAndReadResultingFile {
		
		private final Logger log = LoggerFactory.getLogger(this.getClass());
		
		private String commandTemplate;
		private Path resultingFile;
		private boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	
		public ExecuteCommandAndReadResultingFile(String commandTemplate, Path resultingFile) {
			this.commandTemplate = commandTemplate;
			this.resultingFile = resultingFile;
		}
		
		public List<String> executeAndReadResultingFile(String ...args) throws IOException, InterruptedException{
			executeAndBlock(args);
			return readResultingFile(resultingFile);
		} 
		private int executeAndBlock(String...args) throws IOException, InterruptedException {
			String cmd = String.format(commandTemplate, args);
			
			String homeDirectory = System.getProperty("user.home");
			Process process;
			if (isWindows) {
				process = Runtime.getRuntime().exec(String.format("cmd.exe /c dir %s", homeDirectory));
			} else {
				process = Runtime.getRuntime().exec(cmd);
			}

			StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), log::info);
			streamGobbler.run();
			
			int exitCode = process.waitFor();
			assert exitCode == 0;
			return exitCode;
		}
		private List<String> readResultingFile(Path resultingFile) throws IOException{
			return Files.readAllLines(resultingFile);
		} 
		
		
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
	}
	
	
	@Bean
	CommandLineRunner commandLineRunner() {
		return new CommandLineRunner() {

			@Override
			public void run(String... args) throws Exception {
				
				String cmdTemplate = "csh /fs/szgenefinding/Glimmer3/scripts/g3-iterated.csh %s tag";
				String fastaFileName = "~/1009-Genome.fas";
				Path resultingFile = Paths.get("fs","szgenefinding","Glimmer3","scripts","tag.predict");
				
				ExecuteCommandAndReadResultingFile glimmerAdapter = new ExecuteCommandAndReadResultingFile(cmdTemplate, resultingFile);
				glimmerAdapter.executeAndReadResultingFile(fastaFileName).forEach(System.out::println);
			}
		};
	}
}
