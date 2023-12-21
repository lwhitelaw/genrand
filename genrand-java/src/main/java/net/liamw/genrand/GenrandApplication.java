package net.liamw.genrand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entrypoint for the backend.
 */
@SpringBootApplication
public class GenrandApplication implements CommandLineRunner {
	/**
	 * Main entrypoint.
	 * @param args Any arguments passed on the command line.
	 */
	public static void main(String[] args) {
		// Called to boot Spring.
		SpringApplication.run(GenrandApplication.class, args);
	}
	
	@Autowired
	GenrandMain genmain;
	
	@Override
	public void run(String... args) throws Exception {
		genmain.test();
	}
}
