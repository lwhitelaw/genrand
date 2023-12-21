package net.liamw.genrand;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.util.DriverDataSource;

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
		Properties properties = new Properties();
		DataSource source = new DriverDataSource("jdbc:sqlite:./genrand.db", "org.sqlite.JDBC", properties, null, null);
		return source;
	}
}
