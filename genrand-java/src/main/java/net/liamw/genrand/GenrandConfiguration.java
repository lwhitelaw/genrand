package net.liamw.genrand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Handle configuration for the backend.
 */
@Configuration
public class GenrandConfiguration {
	
	/**
	 * Declare the SQLite file to be used.
	 * @return the JDBC DataSource that Spring is to use.
	 */
	@Bean
	public DataSource getDataSource() {
		try {
			Files.createDirectories(Paths.get("./data"));
		} catch (IOException e) {
			throw new RuntimeException("could not create needed directory");
		}
//		DataSource source = new DriverDataSource("jdbc:sqlite:./genrand-test.db", "org.sqlite.JDBC", properties, null, null);
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("org.sqlite.JDBC");
		config.setJdbcUrl("jdbc:sqlite:./data/genrand.db");
		config.setMaximumPoolSize(1);
		config.setConnectionTimeout(Long.MAX_VALUE); // wait forever to get a connection if needed
		DataSource source = new HikariDataSource(config);
		return source;
	}
}
