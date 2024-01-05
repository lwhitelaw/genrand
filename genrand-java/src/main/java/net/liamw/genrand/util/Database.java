package net.liamw.genrand.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
		 * The lineage identifier of this mix function. Related mix functions share this identifier.
		 */
		private final long lineageId;
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
		 * Hash for the avalanche image associated with this mix function. May be null.
		 */
		private final String avalancheImageRef;
		
		/**
		 * Initialise a MixEntry with all fields.
		 * @param id id field
		 * @param lineageId lineage id field
		 * @param operators operators field
		 * @param operatorCount opcount field
		 * @param avalancheScore av score field
		 * @param practRandScore pr score field
		 * @param sourceCode source code field
		 * @param avalancheImageRef av image ref field
		 */
		public MixEntry(long id, long lineageId, int operators, int operatorCount, double avalancheScore, int practRandScore, String sourceCode, String avalancheImageRef) {
			this.id = id;
			this.lineageId = lineageId;
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
			long lineage = mapper.getLong("lineage");
			int operators = mapper.getInt("operators");
			int opCount = mapper.getInt("operatorCount");
			double avalancheScore = mapper.getDouble("avalancheScore");
			int practRandScore = mapper.getInt("practRandScore"); // may be null - nullity checked next line
			if (mapper.wasNull()) practRandScore = -1;
			String source = mapper.getString("source");
			String avalancheImageRef = mapper.getString("avalancheImageRef"); // may be null
			return new MixEntry(id, lineage, operators, opCount, avalancheScore, practRandScore, source, avalancheImageRef);
		}

		/**
		 * @return the id
		 */
		public final long getId() {
			return id;
		}

		/**
		 * @return the lineageId
		 */
		public final long getLineageId() {
			return lineageId;
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
	
	@Autowired
	JdbcTemplate database;
	
	/**
	 * Create the initial tables.
	 */
	public void checkAndInitTables() {
		database.execute("""
				CREATE TABLE IF NOT EXISTS mix32 (
					identifier INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
					lineage INTEGER NOT NULL,
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
					lineage INTEGER NOT NULL,
					operators INTEGER NOT NULL,
					operatorCount INTEGER NOT NULL,
					avalancheScore REAL NOT NULL,
					practRandScore INTEGER,
					source TEXT NOT NULL,
					avalancheImageRef TEXT
				)
				""");
	}
}
