package uk.co.harieo.FurBridge.redis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {

	public static final String CHANNEL = "minecraft-network";
	private static final String path = "/home/container/deployment-v2/redis.properties";
	private static RedisClient instance;

	private Properties properties;
	private JedisPool pool;

	private static RedisClient newInstance() {
		RedisClient newInstance = new RedisClient();
		System.out.println("Instantiation"); // TODO
		newInstance.pool = newInstance.createPool();

		return newInstance;
	}

	/**
	 * @return the connection resource from the {@link JedisPool} for subscribing
	 */
	public Jedis getResource() {
		return pool.getResource();
	}

	/**
	 * Creates an instance of {@link JedisPool}
	 *
	 * @return the created instance
	 * @throws RuntimeException if an error occurs on connection
	 */
	public JedisPool createPool() throws RuntimeException {
		try {
			checkProperties();
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(128);
			return new JedisPool(config, properties.getProperty("host"),
					Integer.parseInt(properties.getProperty("port").trim()), Integer.parseInt(properties.getProperty("timeout").trim()),
					properties.getProperty("password"), Integer.parseInt(properties.getProperty("database").trim()),
					"redis");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads the properties configuration which contains the necessary connection information for Redis
	 *
	 * @throws RuntimeException if the configuration is invalid
	 * @throws FileNotFoundException if the configuration does not exist
	 */
	private void checkProperties() throws RuntimeException, FileNotFoundException {
		File file = new File(path);
		if (file.exists()) {
			try (FileReader reader = new FileReader(file)) {
				properties = new Properties();
				properties.load(reader);

				if (!checkForProperties(properties, "host", "port", "timeout", "password", "database")) {
					throw new RuntimeException("Missing properties in properties file!");
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to load Redis properties", e);
			}
		} else {
			throw new FileNotFoundException("Failed to locate a redis.properties folder in " + path);
		}
	}

	/**
	 * Checks that all the given keys are available in the specified properties
	 *
	 * @param properties to check the keys of
	 * @param keys to be validated
	 * @return whether all the keys were valid
	 */
	public boolean checkForProperties(Properties properties, String... keys) {
		for (String key : keys) {
			if (!properties.containsKey(key)) {
				return false;
			}
		}

		return true;
	}

	public static RedisClient instance() {
		if (instance == null) {
			instance = newInstance();
			if (instance.pool.isClosed()) {
				System.out.println("Pool is closed"); // TODO
			}
			CompletableFuture.runAsync(() -> {
				try (Jedis jedis = instance.getResource()) {
					if (!jedis.isConnected()) {
						System.out.println("Jedis is not connected"); // TODO
					}
					System.out.println("Attempting the subscription"); // TODO
					jedis.subscribe(RedisReceiver.getInstance(), CHANNEL);
					System.out.println("Subscribed the receiver to the channel"); // TODO
				}
			});
		}

		return instance;
	}

}
