package net.liamw.genrand.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import net.liamw.genrand.function.Mix32;
import net.liamw.genrand.function.Mix64;

/**
 * Code to manipulate the database where found mixers are held.
 */
@Component
public class Database {
	public static class MixEntry {
		/**
		 * Mix function uses addition with constant.
		 */
		public static final int OP_ADD = 0;
		/**
		 * Mix function uses XOR with constant.
		 */
		public static final int OP_XOR = 1;
		/**
		 * Mix function uses multiplication with constant.
		 */
		public static final int OP_MUL = 2;
		/**
		 * Mix function uses constant left rotation. 
		 */
		public static final int OP_ROL = 3;
		/**
		 * Mix function uses constant right rotation.
		 */
		public static final int OP_ROR = 4;
		/**
		 * Mix function uses left xorshift.
		 */
		public static final int OP_XSL = 5;
		/**
		 * Mix function uses right xorshift.
		 */
		public static final int OP_XSR = 6;
		
		/**
		 * The identifier of this mix function.
		 */
		private final long id;
		/**
		 * Bitfield of operators present in this mix function.
		 */
		private final int operators;
		/**
		 * Number of operators in this mix function.
		 */
		private final int operatorCount;
		/**
		 * Stored avalanche score for this mix function.
		 */
		private final double avalancheScore;
		/**
		 * Stored PractRand score for this mix function. -1 if not present.
		 */
		private final int practRandScore;
		/**
		 * Source code for this mix function.
		 */
		private final String sourceCode;
		/**
		 * ID for the avalanche image associated with this mix function. May be null.
		 */
		private final String avalancheImageRef;
		
		/**
		 * Initialise a MixEntry with all fields.
		 * @param id id field
		 * @param operators operators field
		 * @param operatorCount opcount field
		 * @param avalancheScore av score field
		 * @param practRandScore pr score field
		 * @param sourceCode source code field
		 * @param avalancheImageRef av image ref field
		 */
		public MixEntry(long id, int operators, int operatorCount, double avalancheScore, int practRandScore, String sourceCode, String avalancheImageRef) {
			this.id = id;
			this.operators = operators;
			this.operatorCount = operatorCount;
			this.avalancheScore = avalancheScore;
			this.practRandScore = practRandScore;
			this.sourceCode = sourceCode;
			this.avalancheImageRef = avalancheImageRef;
		}
		
		/**
		 * Map a JDBC ResultSet and row ID to a MixEntry object. Not meant to be directly called.
		 * @param mapper result to extract from
		 * @param rowId the row ID
		 * @return a MixEntry from the database row
		 * @throws SQLException if an SQL error occurs
		 */
		public static MixEntry fromDatabaseRowMapper(ResultSet mapper, int rowId) throws SQLException {
			long id = mapper.getLong("identifier");
			int operators = mapper.getInt("operators");
			int opCount = mapper.getInt("operatorCount");
			double avalancheScore = mapper.getDouble("avalancheScore");
			int practRandScore = mapper.getInt("practRandScore"); // may be null - nullity checked next line
			if (mapper.wasNull()) practRandScore = -1;
			String source = mapper.getString("source");
			String avalancheImageRef = mapper.getString("avalancheImageRef"); // may be null
			return new MixEntry(id, operators, opCount, avalancheScore, practRandScore, source, avalancheImageRef);
		}

		/**
		 * @return the id
		 */
		public final long getId() {
			return id;
		}

		/**
		 * @return the operators
		 */
		public final int getOperators() {
			return operators;
		}

		/**
		 * @return the operatorCount
		 */
		public final int getOperatorCount() {
			return operatorCount;
		}

		/**
		 * @return the avalancheScore
		 */
		public final double getAvalancheScore() {
			return avalancheScore;
		}

		/**
		 * @return the practRandScore
		 */
		public final int getPractRandScore() {
			return practRandScore;
		}

		/**
		 * @return the sourceCode
		 */
		public final String getSourceCode() {
			return sourceCode;
		}

		/**
		 * @return the avalancheImageRef
		 */
		public final String getAvalancheImageRef() {
			return avalancheImageRef;
		}
	}
	
	private static final Path IMAGE_PATH = Paths.get("./images/");
	
	@Autowired
	JdbcTemplate database;
	
	/**
	 * Create the initial tables.
	 */
	public void checkAndInitTables() {
		database.execute("""
				CREATE TABLE IF NOT EXISTS mix32 (
					identifier INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
					operators INTEGER NOT NULL,
					operatorCount INTEGER NOT NULL,
					avalancheScore REAL NOT NULL,
					practRandScore INTEGER,
					source TEXT NOT NULL,
					avalancheImageRef TEXT
				)
				""");
		database.execute("""
				CREATE TABLE IF NOT EXISTS mix64 (
					identifier INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
					operators INTEGER NOT NULL,
					operatorCount INTEGER NOT NULL,
					avalancheScore REAL NOT NULL,
					practRandScore INTEGER,
					source TEXT NOT NULL,
					avalancheImageRef TEXT
				)
				""");
	}
	
	/**
	 * Write a generated mix function into the database.
	 * @param mix mix to write
	 */
	public void submit(Mix32 mix) {
		// determine ops in use
		int operators = 0;
		for (Mix32.MixEntry entry : mix.getOperands()) {
			switch (entry.op()) {
				case ADD: operators |= (1 << MixEntry.OP_ADD); break;
				case XOR: operators |= (1 << MixEntry.OP_XOR); break;
				case MUL: operators |= (1 << MixEntry.OP_MUL); break;
				case ROL: operators |= (1 << MixEntry.OP_ROL); break;
				case ROR: operators |= (1 << MixEntry.OP_ROR); break;
				case XSL: operators |= (1 << MixEntry.OP_XSL); break;
				case XSR: operators |= (1 << MixEntry.OP_XSR); break;
			}
		}
		// get remaining properties
		int operatorCount = mix.oplen();
		String sourceCode = mix.toString();
		// calculate avalanche
		double avalancheScore = Avalanche32.scoreAvalanche(mix);
		// make avalanche image and write it out
		BufferedImage avalancheImage = Avalanche32.createAvalancheGraph(mix);
		long snowflake = putImage(avalancheImage);
		if (snowflake == 0) return; // image write failed
		// make and write the database entry
		final int finalOperators = operators; // for the later lambda expression
		database.update("INSERT INTO mix32 (operators,operatorCount,avalancheScore,source,avalancheImageRef) VALUES (?,?,?,?,?)", pss -> {
			pss.setInt(1,finalOperators);
			pss.setInt(2,operatorCount);
			pss.setDouble(3,avalancheScore);
			pss.setString(4,sourceCode);
			pss.setString(5,String.format("%016X",snowflake));
		});
	}
	
	/**
	 * Write a generated mix function into the database.
	 * @param mix mix to write
	 */
	public void submit(Mix64 mix) {
		// determine ops in use
		int operators = 0;
		for (Mix64.MixEntry entry : mix.getOperands()) {
			switch (entry.op()) {
				case ADD: operators |= (1 << MixEntry.OP_ADD); break;
				case XOR: operators |= (1 << MixEntry.OP_XOR); break;
				case MUL: operators |= (1 << MixEntry.OP_MUL); break;
				case ROL: operators |= (1 << MixEntry.OP_ROL); break;
				case ROR: operators |= (1 << MixEntry.OP_ROR); break;
				case XSL: operators |= (1 << MixEntry.OP_XSL); break;
				case XSR: operators |= (1 << MixEntry.OP_XSR); break;
			}
		}
		// get remaining properties
		int operatorCount = mix.oplen();
		String sourceCode = mix.toString();
		// calculate avalanche
		double avalancheScore = Avalanche64.scoreAvalanche(mix);
		// make avalanche image and write it out
		BufferedImage avalancheImage = Avalanche64.createAvalancheGraph(mix);
		long snowflake = putImage(avalancheImage);
		if (snowflake == 0) return; // image write failed
		// make and write the database entry
		final int finalOperators = operators; // for the later lambda expression
		database.update("INSERT INTO mix64 (operators,operatorCount,avalancheScore,source,avalancheImageRef) VALUES (?,?,?,?,?)", pss -> {
			pss.setInt(1,finalOperators);
			pss.setInt(2,operatorCount);
			pss.setDouble(3,avalancheScore);
			pss.setString(4,sourceCode);
			pss.setString(5,String.format("%016X",snowflake));
		});
	}
	
	private static long putImage(BufferedImage image) {
		long snowflake = Snowflake.generate();
		Path path = IMAGE_PATH.resolve(String.format("%03X",mix12bit(snowflake)));
		try {
			Files.createDirectories(path);
			ImageIO.write(image, "PNG", path.resolve(String.format("%016X.png",snowflake)).toFile());
		} catch (IOException ex) {
			return 0;
		}
		return snowflake;
	}
	
	private static int mix12bit(long v) {
		v ^= v >>> 21;
		v *= 0x2AE264A9B1A36D69L;
		v ^= v >>> 37;
		v *= 0x396747CA3A58E56FL;
		v ^= v >>> 44;
		v *= 0xFB7719182775D593L;
		v ^= v >>> 21;
		return (int)(v & 0xFFFL);
	}
}
