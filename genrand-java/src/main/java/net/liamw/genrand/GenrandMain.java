package net.liamw.genrand;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.liamw.genrand.function.arx.ARXMix;
import net.liamw.genrand.function.arx.ARXMixInfo;
import net.liamw.genrand.function.arx.MixARX32x2;
import net.liamw.genrand.util.AvalancheVector;
import net.liamw.genrand.util.AvalancheVector.DiffuserVector;
import net.liamw.genrand.util.BitVector;
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
	public void runMixers() {
		database.checkAndInitTables();
		ARXMix.generateInNewThread(database,MixARX32x2.INFO);
	}

	public void runMix32() {
		database.checkAndInitTables();
		Gen32BitAddXorshift.run(database,64,0.1);
	}
	
	public void runMix64() {
		database.checkAndInitTables();
		Gen64BitAddXorshift.run(database,64,0.2);
	}
	
	public void runMix64C() {
		database.checkAndInitTables();
		Gen64BitC.run(database,16,0.2);
	}
	
	public void vectorTest() {
		BitVector a = new BitVector(64);
		a.setBitAt(15,1);
		System.out.println(a);
		a.setBitAt(30,1);
		System.out.println(a);
		a.setBitsAt(0,8,0xFF);
		System.out.println(a);
		a.setBitsAt(25,16,0xFFFF);
		a.setBitsAt(48,16,0xFFFF);
		System.out.println(a);
		System.out.printf("%04X\n",a.getBitsAt(0, 16));
		System.out.printf("%08X\n",a.getBitsAt(8, 32));
		System.out.printf("%08X\n",a.getBitsAt(32, 32));
		
		DiffuserVector d = new DiffuserVector() {
			@Override
			public BitVector diffuse(BitVector input) {
				long a = ((input.get32BitsAlignedAt(3) & 0xFFFFFFFFL) << 32) | (input.get32BitsAlignedAt(2) & 0xFFFFFFFFL);
				long c = ((input.get32BitsAlignedAt(1) & 0xFFFFFFFFL) << 32) | (input.get32BitsAlignedAt(0) & 0xFFFFFFFFL);
				
				a ^= Long.rotateLeft(c,3); c += Long.rotateLeft(a,7);
				a ^= Long.rotateLeft(c,4); c ^= Long.rotateLeft(a,13);
				
				long v = a + c;
				BitVector out = new BitVector(64);
//				out.set32BitsAlignedAt(3, (int)(a >>> 32));
//				out.set32BitsAlignedAt(2, (int)(a & 0xFFFFFFFFL));
				out.set32BitsAlignedAt(1, (int)(v >>> 32));
				out.set32BitsAlignedAt(0, (int)(v & 0xFFFFFFFFL));
				return out;
			}
			
			@Override
			public int inputSize() {
				return 128;
			}
			
			@Override
			public int outputSize() {
				return 64;
			}
		};
		BufferedImage bimg = AvalancheVector.createAvalancheGraph(d);
		try {
			ImageIO.write(bimg,"PNG",new File("E:/ProjectTesting/Random/avv.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void unpackTest() {
		MixARX32x2 mix = ARXMix.unpack(MixARX32x2.class, 1000);
		System.out.println("It worked: " + mix);
		mix = ARXMix.unpack(MixARX32x2.class, 1000);
		System.out.println("It worked: " + mix);
	}
}
