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
	
	@GetMapping("/arx/{type}/definition/{definition}")
	public List<ARXMixEntry> getARXByDefinition(@PathVariable("type") String type, @PathVariable("definition") long definition) {
		List<ARXMixEntry> list = database.getARXByDefinition(type,definition);
		return list;
	}
	
	private static final int PAGE_SIZE = 256;
	
	@GetMapping("/arx/{type}/list/{page}")
	public List<ARXMixEntry> getARXByTypeChronologically(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeChronologically(type,PAGE_SIZE,page);
		return list;
	}
}
