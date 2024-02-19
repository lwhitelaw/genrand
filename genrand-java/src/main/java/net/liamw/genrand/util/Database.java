package net.liamw.genrand.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.liamw.genrand.function.Mix32;
import net.liamw.genrand.function.Mix64;
import net.liamw.genrand.function.arx.ARXMix;
import net.liamw.genrand.function.arx.MixARX32x2;

/**
 * Code to manipulate the database where found mixers are held.
 */
@Component
public class Database {
	/**
	 * Representation of a generic mix in the database.
	 */
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
	
	/**
	 * Representation of an ARX-based mix in the database.
	 */
	public static class ARXMixEntry {
		private final String type;
		private final long definition;
		private final double avScore1;
		private final double avScore2;
		private final double avScore3;
		private final double avScore4;
		private final String avImage1;
		private final String avImage2;
		private final String avImage3;
		private final String avImage4;
		
		/**
		 * Construct a mix entry, setting all values.
		 * @param type mix type
		 * @param definition mix packed definition
		 * @param avScore1 one round avalanche score
		 * @param avScore2 two round avalanche score
		 * @param avScore3 three round avalanche score
		 * @param avScore4 four round avalanche score
		 * @param avImage1 one round avalanche graph
		 * @param avImage2 two round avalanche graph
		 * @param avImage3 three round avalanche graph
		 * @param avImage4 four round avalanche graph
		 */
		public ARXMixEntry(String type, long definition, double avScore1, double avScore2, double avScore3,
				double avScore4, String avImage1, String avImage2, String avImage3, String avImage4) {
			this.type = type;
			this.definition = definition;
			this.avScore1 = avScore1;
			this.avScore2 = avScore2;
			this.avScore3 = avScore3;
			this.avScore4 = avScore4;
			this.avImage1 = avImage1;
			this.avImage2 = avImage2;
			this.avImage3 = avImage3;
			this.avImage4 = avImage4;
		}
		
		/**
		 * Map a JDBC ResultSet and row ID to an ARXMixEntry object. Not meant to be directly called.
		 * @param mapper result to extract from
		 * @param rowId the row ID
		 * @return a MixEntry from the database row
		 * @throws SQLException if an SQL error occurs
		 */
		public static ARXMixEntry fromDatabaseRowMapper(ResultSet mapper, int rowId) throws SQLException {
			String id = mapper.getString("type");
			long definition = mapper.getLong("definition");
			double avScore1 = mapper.getDouble("avScore1");
			double avScore2 = mapper.getDouble("avScore2");
			double avScore3 = mapper.getDouble("avScore3");
			double avScore4 = mapper.getDouble("avScore4");
			String avImage1 = mapper.getString("avImage1");
			String avImage2 = mapper.getString("avImage2");
			String avImage3 = mapper.getString("avImage3");
			String avImage4 = mapper.getString("avImage4");
			return new ARXMixEntry(id, definition, avScore1, avScore2, avScore3, avScore4, avImage1, avImage2, avImage3, avImage4);
		}
		
		/**
		 * @return the type
		 */
		public final String getType() {
			return type;
		}
		/**
		 * @return the definition
		 */
		public final long getDefinition() {
			return definition;
		}
		/**
		 * @return the avScore1
		 */
		public final double getAvScore1() {
			return avScore1;
		}
		/**
		 * @return the avScore2
		 */
		public final double getAvScore2() {
			return avScore2;
		}
		/**
		 * @return the avScore3
		 */
		public final double getAvScore3() {
			return avScore3;
		}
		/**
		 * @return the avScore4
		 */
		public final double getAvScore4() {
			return avScore4;
		}
		/**
		 * @return the avImage1
		 */
		public final String getAvImage1() {
			return avImage1;
		}
		/**
		 * @return the avImage2
		 */
		public final String getAvImage2() {
			return avImage2;
		}
		/**
		 * @return the avImage3
		 */
		public final String getAvImage3() {
			return avImage3;
		}
		/**
		 * @return the avImage4
		 */
		public final String getAvImage4() {
			return avImage4;
		}
	}
	
	/**
	 * Return a zero/one-element list containing an ARX mix with the given type and definition.
	 * @param type type to query
	 * @param def definition to query
	 */
	public List<ARXMixEntry> getARXByDefinition(String type, long def) {
		return database.query("SELECT * FROM mixarx WHERE type = ? AND definition = ?", pss -> {
			pss.setString(1, type);
			pss.setLong(2, def);
		}, ARXMixEntry::fromDatabaseRowMapper);
	}
	
	/**
	 * Return the number of ARX mixes with the given type.
	 * @param type type to query
	 * @return number of ARX mixes
	 */
	public long getARXCount(String type) {
		return database.query("SELECT count(*) FROM mixarx WHERE type = ?", pss -> {
			pss.setString(1, type);
		},rs -> {
			return rs.getLong(1);
		});
	}
	
	// The functions here are duplicated due to limits with prepared statements for sort queries.
	
	/**
	 * Get ARX mixes by type in chronological order, returning pages with the given size.
	 * An empty list may be returned if there are no mixes at the requested page.
	 * @param type type to query
	 * @param limit maximum size of a page
	 * @param page page to get
	 * @return a list of mixes
	 */
	public List<ARXMixEntry> getARXByTypeChronologically(String type, int limit, int page) {
		return database.query("SELECT * FROM mixarx WHERE type = ? ORDER BY rowid DESC LIMIT ?,?", pss -> {
			pss.setString(1, type);
			pss.setLong(2,(long)page * (long)limit); // page order from zero
			pss.setLong(3,(long)limit); // limit per page
		}, ARXMixEntry::fromDatabaseRowMapper);
	}
	
	/**
	 * Get ARX mixes by type sorted by round 1 score, returning pages with the given size.
	 * An empty list may be returned if there are no mixes at the requested page.
	 * @param type type to query
	 * @param limit maximum size of a page
	 * @param page page to get
	 * @return a list of mixes
	 */
	public List<ARXMixEntry> getARXByTypeSortByRound1(String type, int limit, int page) {
		return database.query("SELECT * FROM mixarx WHERE type = ? ORDER BY avScore1 ASC LIMIT ?,?", pss -> {
			pss.setString(1, type);
			pss.setLong(2,(long)page * (long)limit); // page order from zero
			pss.setLong(3,(long)limit); // limit per page
		}, ARXMixEntry::fromDatabaseRowMapper);
	}
	
	/**
	 * Get ARX mixes by type sorted by round 2 score, returning pages with the given size.
	 * An empty list may be returned if there are no mixes at the requested page.
	 * @param type type to query
	 * @param limit maximum size of a page
	 * @param page page to get
	 * @return a list of mixes
	 */
	public List<ARXMixEntry> getARXByTypeSortByRound2(String type, int limit, int page) {
		return database.query("SELECT * FROM mixarx WHERE type = ? ORDER BY avScore2 ASC LIMIT ?,?", pss -> {
			pss.setString(1, type);
			pss.setLong(2,(long)page * (long)limit); // page order from zero
			pss.setLong(3,(long)limit); // limit per page
		}, ARXMixEntry::fromDatabaseRowMapper);
	}
	
	/**
	 * Get ARX mixes by type sorted by round 3 score, returning pages with the given size.
	 * An empty list may be returned if there are no mixes at the requested page.
	 * @param type type to query
	 * @param limit maximum size of a page
	 * @param page page to get
	 * @return a list of mixes
	 */
	public List<ARXMixEntry> getARXByTypeSortByRound3(String type, int limit, int page) {
		return database.query("SELECT * FROM mixarx WHERE type = ? ORDER BY avScore3 ASC LIMIT ?,?", pss -> {
			pss.setString(1, type);
			pss.setLong(2,(long)page * (long)limit); // page order from zero
			pss.setLong(3,(long)limit); // limit per page
		}, ARXMixEntry::fromDatabaseRowMapper);
	}
	
	/**
	 * Get ARX mixes by type sorted by round 4 score, returning pages with the given size.
	 * An empty list may be returned if there are no mixes at the requested page.
	 * @param type type to query
	 * @param limit maximum size of a page
	 * @param page page to get
	 * @return a list of mixes
	 */
	public List<ARXMixEntry> getARXByTypeSortByRound4(String type, int limit, int page) {
		return database.query("SELECT * FROM mixarx WHERE type = ? ORDER BY avScore4 ASC LIMIT ?,?", pss -> {
			pss.setString(1, type);
			pss.setLong(2,(long)page * (long)limit); // page order from zero
			pss.setLong(3,(long)limit); // limit per page
		}, ARXMixEntry::fromDatabaseRowMapper);
	}
	
	/**
	 * The root path of the image store.
	 */
	public static final Path IMAGE_PATH = Paths.get("./images/");
	
	/**
	 * Spring's reference to the database.
	 */
	@Autowired
	private JdbcTemplate database;
	/**
	 * Spring's reference to database operations that are transactional.
	 */
	@Autowired
	private TransactionTemplate dbTransaction;
	
	/**
	 * Create the initial tables.
	 */
	public void checkAndInitTables() {
		// Normal tables
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
		// ARX tables
		database.execute("""
				CREATE TABLE IF NOT EXISTS mixarx (
					type TEXT NOT NULL,
					definition INTEGER NOT NULL,
					avScore1 REAL NOT NULL,
					avScore2 REAL NOT NULL,
					avScore3 REAL NOT NULL,
					avScore4 REAL NOT NULL,
					avImage1 TEXT,
					avImage2 TEXT,
					avImage3 TEXT,
					avImage4 TEXT,
					PRIMARY KEY (type,definition)
				)
				""");
		// ARX search status
		database.execute("""
				CREATE TABLE IF NOT EXISTS arxsearch (
					type TEXT NOT NULL PRIMARY KEY,
					checkpoint INTEGER NOT NULL
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
		double avalancheScore = Avalanche32.scoreAvalanche(mix,32);
		// make avalanche image and write it out
		BufferedImage avalancheImage = Avalanche32.createAvalancheGraph(mix,32);
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
		double avalancheScore = Avalanche64.scoreAvalanche(mix,64);
		// make avalanche image and write it out
		BufferedImage avalancheImage = Avalanche64.createAvalancheGraph(mix,64);
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
	
	/**
	 * Write a generated mix function into the database and execute postAction in a transaction. postAction may be null if there is no need to
	 * do anything in a transaction with the writing of the mix function.
	 * @param mix mix to write
	 * @param postAction action to execute
	 */
	public void submit(ARXMix<?> mix, Consumer<Database> postAction) {
		// pack into long value
		long definition = mix.pack();
		// score avalanche functions for 1 to 4 rounds
		double av1 = mix.score(1);
//		System.out.printf("1 round... %f\n",av1);
		double av2 = mix.score(2);
//		System.out.printf("2 round... %f\n",av2);
		double av3 = mix.score(3);
//		System.out.printf("3 round... %f\n",av3);
		double av4 = mix.score(4);
//		System.out.printf("4 round... %f\n",av4);
		// make avalanche graphs for the same - if any fail, they'll be zero. This is fine. It'll be made null later.
		long avImageSnowflake1 = putImage(mix.graph(1));
		long avImageSnowflake2 = putImage(mix.graph(2));
		long avImageSnowflake3 = putImage(mix.graph(3));
		long avImageSnowflake4 = putImage(mix.graph(4));
//		System.out.printf("Images done...\n");
		// Write out into database
		try {
			dbTransaction.executeWithoutResult(status -> {
				database.update("INSERT INTO mixarx (type,definition,avScore1,avScore2,avScore3,avScore4,avImage1,avImage2,avImage3,avImage4) VALUES (?,?,?,?,?,?,?,?,?,?)", pss -> {
					pss.setString(1,mix.getInfo().getDatabaseTag());
					pss.setLong(2,definition);
					
					pss.setDouble(3,av1);
					pss.setDouble(4,av2);
					pss.setDouble(5,av3);
					pss.setDouble(6,av4);
					
					if (avImageSnowflake1 == 0) {
						pss.setNull(7,Types.VARCHAR);
					} else {
						pss.setString(7,String.format("%016X",avImageSnowflake1));
					}
					if (avImageSnowflake2 == 0) {
						pss.setNull(8,Types.VARCHAR);
					} else {
						pss.setString(8,String.format("%016X",avImageSnowflake2));
					}
					if (avImageSnowflake3 == 0) {
						pss.setNull(9,Types.VARCHAR);
					} else {
						pss.setString(9,String.format("%016X",avImageSnowflake3));
					}
					if (avImageSnowflake4 == 0) {
						pss.setNull(10,Types.VARCHAR);
					} else {
						pss.setString(10,String.format("%016X",avImageSnowflake4));
					}
				});
				if (postAction != null) postAction.accept(this);
			});
		} catch (DataAccessException ex) {
			System.out.println("Insertion into database failed for type " + definition);
			ex.printStackTrace(System.out);
		}
	}
	
	/**
	 * Put an image into the image store and return a snowflake ID. If
	 * writing fails, zero is returned.
	 * @param image the image to put
	 * @return the snowflake ID to reference the image with
	 */
	private static long putImage(BufferedImage image) {
		long snowflake = Snowflake.generate();
		try {
			Path path = snowflakeToPath(snowflake);
			ImageIO.write(image, "PNG", path.toFile());
		} catch (IOException ex) {
			return 0;
		}
		return snowflake;
	}
	
	/**
	 * Convert a snowflake to an image path.
	 * @param snowflake value to convert
	 * @return the path to the image
	 * @throws IOException if directories could not be created in the process
	 */
	private static Path snowflakeToPath(long snowflake) throws IOException {
		Path dirPath = IMAGE_PATH.resolve(String.format("%03X",mix12bit(snowflake)));
		Files.createDirectories(dirPath);
		return dirPath.resolve(String.format("%016X.png",snowflake));
	}
	
	/**
	 * Mix a 64 bit value to a deterministically random 12 bit value.
	 * @param v value to convert
	 * @return a 12 bit value
	 */
	public static int mix12bit(long v) {
		v ^= v >>> 21;
		v *= 0x2AE264A9B1A36D69L;
		v ^= v >>> 37;
		v *= 0x396747CA3A58E56FL;
		v ^= v >>> 44;
		v *= 0xFB7719182775D593L;
		v ^= v >>> 21;
		return (int)(v & 0xFFFL);
	}
	
	/**
	 * Return the checkpoint value for the mix generator of this type to start from.
	 * @param ident mix type
	 * @return the checkpoint value to start at
	 */
	public long getCheckpoint(String ident) {
		return database.query("SELECT checkpoint FROM arxsearch WHERE type = ?", pss -> pss.setString(1,ident), rse -> {
			boolean hasRow = rse.next();
			if (!hasRow) return 0L;
			long v = rse.getLong(1);
			if (rse.wasNull()) return 0L;
			return v;
		});
	}
	
	/**
	 * Set the checkpoint value for the mix generator of this type to start from.
	 * @param ident mix type
	 * @param value the checkpoint value to start at
	 */
	public void setCheckpoint(String ident, long value) {
		// Don't allow values < 1
		if (value < 1) return;
		
		dbTransaction.executeWithoutResult(status -> {
			// Check to see if checkpoint is set
			long originalValue = getCheckpoint(ident);
			if (originalValue == 0) {
				// Value not set. Needs to insert value.
				database.update("INSERT INTO arxsearch (type,checkpoint) VALUES (?,?)", pss -> {
					pss.setString(1, ident);
					pss.setLong(2, value);
				});
//				System.out.println("Inserted value");
			} else {
				// Value is set. Update instead.
				database.update("UPDATE arxsearch SET checkpoint = ? WHERE type = ?", pss -> {
					pss.setLong(1, value);
					pss.setString(2, ident);
				});
//				System.out.println("Set value");
			}
		});
	}
	
	/**
	 * Clear all mixes and images with the given ARX mix type.
	 * @param type type to clear
	 */
	public void clearARXTable(String type) {
		List<ARXMixEntry> list = database.query("SELECT * FROM mixarx WHERE type = ?", pss -> {
			pss.setString(1, type);
		}, ARXMixEntry::fromDatabaseRowMapper);
		for (ARXMixEntry mix : list) {
			try {
				Path p1 = snowflakeToPath(Long.parseLong(mix.getAvImage1(),16));
				System.out.println("Deleting " + p1);
				Files.delete(p1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				Path p2 = snowflakeToPath(Long.parseLong(mix.getAvImage2(),16));
				System.out.println("Deleting " + p2);
				Files.delete(p2);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				Path p3 = snowflakeToPath(Long.parseLong(mix.getAvImage3(),16));
				System.out.println("Deleting " + p3);
				Files.delete(p3);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				Path p4 = snowflakeToPath(Long.parseLong(mix.getAvImage4(),16));
				System.out.println("Deleting " + p4);
				Files.delete(p4);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Clearing " + type);
		// Clear database
		database.update("DELETE FROM mixarx WHERE type = ?", pss -> {
			pss.setString(1, type);
		});
		System.out.println("Resetting checkpoint");
		database.update("DELETE FROM arxsearch WHERE type = ?", pss -> {
			pss.setString(1, type);
		});
	}
	
	/**
	 * Run the given code in a database transaction.
	 * @param caller code to run
	 */
	public void runTransactionally(Consumer<Database> caller) {
		dbTransaction.execute((TransactionCallback<Void>)(status -> {
			caller.accept(this);
			return null;
		}));
	}
}
