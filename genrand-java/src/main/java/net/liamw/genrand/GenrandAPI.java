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
	/**
	 * Spring needs this to exist to convert objects to JSON, even though it is otherwise not used.
	 */
	private final ObjectMapper mapper = new ObjectMapper();
	/**
	 * Handle to the database interface.
	 */
	private final Database database;
	
	public GenrandAPI(Database database) {
		this.database = database;
	}
	
	/**
	 * Get an ARX mix by its type and definition. Returns a one-item list with the mix, or an empty list otherwise.
	 * @param type the type to query
	 * @param definition the packed definition to query
	 * @return a one element list containing the mix, or an empty list
	 */
	@GetMapping("/arx/{type}/definition/{definition}")
	public List<ARXMixEntry> getARXByDefinition(@PathVariable("type") String type, @PathVariable("definition") long definition) {
		List<ARXMixEntry> list = database.getARXByDefinition(type,definition);
		return list;
	}
	
	/**
	 * Get the number of ARX mixes with the given type.
	 * @param type the type to query
	 * @return the number of ARX mixes
	 */
	@GetMapping("/arx/{type}/count")
	public long getARXCount(@PathVariable("type") String type) {
		return database.getARXCount(type);
	}
	
	/**
	 * Maximum size a list should return as the page size the frontend will see.
	 */
	private static final int PAGE_SIZE = 256;
	
	// The functions below are duplicates due to limitations with prepared statements not being able to change the column affected
	// by sorting in the query.
	
	/**
	 * Get a page of ARX mixes in chronological order, 256 elements at a time. The list will be empty if there is no such page.
	 * Page numbers are zero-indexed!
	 * @param type the type to query
	 * @param page page to query
	 * @return a list of mixes
	 */
	@GetMapping("/arx/{type}/list/{page}")
	public List<ARXMixEntry> getARXByTypeChronologically(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeChronologically(type,PAGE_SIZE,page);
		return list;
	}
	
	@GetMapping("/arx/{type}/topScoring/round1/{page}")
	public List<ARXMixEntry> getARXByTypeSortedByAv1(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeSortByRound1(type,PAGE_SIZE,page);
		return list;
	}
	
	@GetMapping("/arx/{type}/topScoring/round2/{page}")
	public List<ARXMixEntry> getARXByTypeSortedByAv2(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeSortByRound2(type,PAGE_SIZE,page);
		return list;
	}
	
	@GetMapping("/arx/{type}/topScoring/round3/{page}")
	public List<ARXMixEntry> getARXByTypeSortedByAv3(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeSortByRound3(type,PAGE_SIZE,page);
		return list;
	}
	
	@GetMapping("/arx/{type}/topScoring/round4/{page}")
	public List<ARXMixEntry> getARXByTypeSortedByAv4(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeSortByRound4(type,PAGE_SIZE,page);
		return list;
	}
}
