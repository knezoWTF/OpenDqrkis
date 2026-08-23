package xyz.dqrkis.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DiscordWebhook {
	private final String url;
	private final List<Path> attachments = new ArrayList<>();
	private String title;
	private String description;

	public DiscordWebhook(String url) {
		this.url = url;
	}

	public DiscordWebhook title(String title) {
		this.title = title;
		return this;
	}

	public DiscordWebhook description(String description) {
		this.description = description;
		return this;
	}

	public DiscordWebhook attach(Path file) {
		attachments.add(file);
		return this;
	}

	private String buildPayloadJson() {
		StringBuilder sb = new StringBuilder("{\"embeds\":[{");
		if (title != null)
			sb.append("\"title\":").append(Json.quote(title));
		if (description != null) {
			if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '{')
				sb.append(',');
			sb.append("\"description\":").append(Json.quote(description));
		}
		sb.append("}]}");
		return sb.toString();
	}

	public void sendAsync() {
		Thread.ofVirtual().start(this::sendBlocking);
	}

	public void sendBlocking() {
		try {
			String boundary = "----DqrkisBoundary" + System.nanoTime();
			var byteArrays = new ArrayList<byte[]>();

			byteArrays.add(multipartField(boundary, "payload_json", buildPayloadJson()));

			for (Path file : attachments) {
				if (!Files.exists(file))
					continue;
				byteArrays.add(multipartFileHeader(boundary, file.getFileName().toString()));
				byteArrays.add(Files.readAllBytes(file));
				byteArrays.add(("\r\n").getBytes());
			}

			byteArrays.add(("--" + boundary + "--\r\n").getBytes());

			var requestBody = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Content-Type", "multipart/form-data; boundary=" + boundary)
					.POST(requestBody)
					.build();

			HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
		} catch (IOException | InterruptedException ignored) {}
	}

	private static byte[] multipartField(String boundary, String name, String value) {
		return ("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
				+ value + "\r\n").getBytes();
	}

	private static byte[] multipartFileHeader(String boundary, String fileName) {
		return ("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"files[]\"; filename=\"" + fileName + "\"\r\n"
				+ "Content-Type: image/png\r\n\r\n").getBytes();
	}

	private static final class Json {
		static String quote(String s) {
			return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		}
	}
}
