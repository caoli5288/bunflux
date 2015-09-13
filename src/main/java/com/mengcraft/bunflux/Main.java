package com.mengcraft.bunflux;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.plugin.Plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengcraft.influxdb.InfluxHandler;

public class Main extends Plugin implements Runnable {

	private InfluxHandler influx;
	private String server;

	@Override
	public void onLoad() {
		File file = new File(getDataFolder(), "config.json");
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try (InputStream in = getResourceAsStream("config.json")) {
				Files.copy(in, file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try (FileInputStream in = new FileInputStream(file)) {
			InputStreamReader reader = new InputStreamReader(in);
			JsonObject root = new JsonParser().
					parse(reader).
					getAsJsonObject();

			reader.close();

			setServer(root.get("server").getAsString());
			JsonObject node = root.get("influx").getAsJsonObject();
			setInflux(new InfluxHandler(node.get("url").getAsString(),
					node.get("userName").getAsString(),
					node.get("password").getAsString(),
					node.get("database").getAsString()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		getProxy().getScheduler().runAsync(this, () -> {
			getInflux().createDatabase();
		});
	}

	@Override
	public void onEnable() {
		getProxy().getScheduler().
				schedule(this, this, 10, 10,
						TimeUnit.SECONDS);
	}

	@Override
	public void onDisable() {
		influx.shutdown();
	}

	@Override
	public void run() {
		getProxy().getScheduler().runAsync(this, () -> {
			influx.write("player_value")
				  .where("server", "play.915mc.com")
				  .value("value", getProxy().getOnlineCount())
				  .flush();
		});
	}

	private InfluxHandler getInflux() {
		return influx;
	}

	private void setInflux(InfluxHandler influx) {
		this.influx = influx;
	}

	private String getServer() {
		return server;
	}

	private void setServer(String server) {
		this.server = server;
	}

}
