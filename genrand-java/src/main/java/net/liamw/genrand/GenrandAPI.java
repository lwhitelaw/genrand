package net.liamw.genrand;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.liamw.genrand.util.Database;
import net.liamw.genrand.util.Database.ARXMixEntry;

/**
 * API methods to be exposed by Spring.
 */
@RestController
public class GenrandAPI {
	private final ObjectMapper mapper = new ObjectMapper();
	private final Database database;
	
	public GenrandAPI(Database database) {
		this.database = database;
	}
	
	@GetMapping("/arx/32x2/definition/{definition}")
	public List<ARXMixEntry> getARX32x2ByDefinition(@PathVariable("definition") long definition) {
		List<ARXMixEntry> list = database.getARX32x2ByDefinition(definition);
		return list;
	}
}
