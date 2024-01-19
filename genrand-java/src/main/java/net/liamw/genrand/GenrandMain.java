package net.liamw.genrand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import net.liamw.genrand.function.Mix32.Operand;
import net.liamw.genrand.function.Mix64;
import net.liamw.genrand.util.Database;

/**
 * Class where main logic happens.
 */
@Component
public class GenrandMain {
	@Autowired
	private Database database;
	
//	/**
//	 * Test JDBC connectivity
//	 */
//	public void test() {
//		Gen32Bit.run(64, 0.1, Operand.ADD, Operand.XSL, Operand.XSR);
//		Gen64Bit.run(64, 0.2, Mix64.Operand.ADD, Mix64.Operand.MUL, Mix64.Operand.XSR);
//		System.out.println(jdbcAccessor);
//		// Create a table
//		jdbcAccessor.execute("""
//				CREATE TABLE IF NOT EXISTS hello (
//					id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
//					text TEXT NOT NULL
//				)
//				""");
//		// Insert into the table
//		jdbcAccessor.update("INSERT INTO hello (text) VALUES ('world')");
//		// Query rows
//		for (var row : jdbcAccessor.queryForList("SELECT * FROM hello")) {
//			System.out.println(row);
//		}
//	}

	public void runMix32() {
		database.checkAndInitTables();
		Gen32BitAddXorshift.run(database,64,0.1);
	}
}
