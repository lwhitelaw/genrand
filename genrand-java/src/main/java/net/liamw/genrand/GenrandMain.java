package net.liamw.genrand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.liamw.genrand.function.arx.ARXMix;
import net.liamw.genrand.function.arx.MixARX16x2;
import net.liamw.genrand.function.arx.MixARX16x3;
import net.liamw.genrand.function.arx.MixARX16x4;
import net.liamw.genrand.function.arx.MixARX32x2;
import net.liamw.genrand.function.arx.MixARX32x3;
import net.liamw.genrand.function.arx.MixARX32x4;
import net.liamw.genrand.function.arx.MixARX64x2;
import net.liamw.genrand.function.arx.MixARX64x3;
import net.liamw.genrand.function.arx.MixARX64x4;
import net.liamw.genrand.function.arx.MixARX8x2;
import net.liamw.genrand.function.arx.MixARX8x3;
import net.liamw.genrand.function.arx.MixARX8x4;
import net.liamw.genrand.util.Database;

/**
 * Class where main logic happens.
 */
@Component
public class GenrandMain {
	@Autowired
	private Database database;
	
	public void runMixers() {
		database.checkAndInitTables();
		
		ARXMix.generateInNewThread(database,MixARX8x2.INFO);
		ARXMix.generateInNewThread(database,MixARX8x3.INFO);
		ARXMix.generateInNewThread(database,MixARX8x4.INFO);
		
		ARXMix.generateInNewThread(database,MixARX16x2.INFO);
		ARXMix.generateInNewThread(database,MixARX16x3.INFO);
		ARXMix.generateInNewThread(database,MixARX16x4.INFO);
		
		ARXMix.generateInNewThread(database,MixARX32x2.INFO);
		ARXMix.generateInNewThread(database,MixARX32x3.INFO);
		ARXMix.generateInNewThread(database,MixARX32x4.INFO);
		
		ARXMix.generateInNewThread(database,MixARX64x2.INFO);
		ARXMix.generateInNewThread(database,MixARX64x3.INFO);
		ARXMix.generateInNewThread(database,MixARX64x4.INFO);
	}

	public void runMix32() {
		database.checkAndInitTables();
		Gen32BitAddXorshift.run(database,64,0.1);
	}
	
	public void runMix64() {
		database.checkAndInitTables();
		Gen64BitAddXorshift.run(database,64,0.2);
	}
}
