package uk.co.harieo.FurBridge.redis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {

	private static final String path = "/home/container/deployment/redis.properties";
	private static Properties properties;
	private static JedisPool pool;

	/**
	 * @return the connection resource from the {@link JedisPool}
	 */
	public static Jedis getResource() {
		if (pool == null) {
			pool = createPool();
		}

		return pool.getResource();
	}

	/**
	 * Creates an instance of {@link JedisPool}
	 *
	 * @return the created instance
	 * @throws RuntimeException if an error occurs on connection
	 */
	public static JedisPool createPool() throws RuntimeException {
		try {
			checkProperties();
			return new JedisPool(new JedisPoolConfig(), properties.getProperty("host"),
					Integer.parseInt(properties.getProperty("port")), Integer.parseInt(properties.getProperty("timeout")),
					properties.getProperty("password"), Integer.parseInt(properties.getProperty("database")),
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
	private static void checkProperties() throws RuntimeException, FileNotFoundException {
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
	public static boolean checkForProperties(Properties properties, String... keys) {
		for (String key : keys) {
			if (!properties.contains(key)) {
				return false;
			}
		}

		return true;
	}

}
