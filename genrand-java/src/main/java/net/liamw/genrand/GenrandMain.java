package net.liamw.genrand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Class where main logic happens.
 */
@Component
public class GenrandMain {
	@Autowired
	private JdbcTemplate jdbcAccessor;
	
	/**
	 * Test JDBC connectivity
	 */
	public void test() {
		System.out.println(jdbcAccessor);
		// Create a table
		jdbcAccessor.execute("""
				CREATE TABLE IF NOT EXISTS hello (
					id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
					text TEXT NOT NULL
				)
				""");
		// Insert into the table
		jdbcAccessor.update("INSERT INTO hello (text) VALUES ('world')");
		// Query rows
		for (var row : jdbcAccessor.queryForList("SELECT * FROM hello")) {
			System.out.println(row);
		}
	}
}
