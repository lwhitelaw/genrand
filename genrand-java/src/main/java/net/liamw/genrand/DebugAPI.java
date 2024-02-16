package net.liamw.genrand;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import net.liamw.genrand.util.Database.ARXMixEntry;

@RestController
public class DebugAPI {
	private final ObjectMapper mapper = new ObjectMapper();
	private final Database database;
	
	public DebugAPI(Database database) {
		this.database = database;
	}
	
	@GetMapping(value = "/debug/arx/{type}/definition/{definition}", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String getARXByDefinition(@PathVariable("type") String type, @PathVariable("definition") long definition) {
		List<ARXMixEntry> list = database.getARXByDefinition(type,definition);
		StringBuilder sb = new StringBuilder();
		appendStart(sb);
		if (list.size() == 0) {
			sb.append("not found");
		} else {
			ARXMixEntry e = list.get(0);
			ARXMix<?> mix;
			switch (e.getType()) {
				case "8x2": mix = MixARX8x2.unpack(e.getDefinition()); break;
				case "8x3": mix = MixARX8x3.unpack(e.getDefinition()); break;
				case "8x4": mix = MixARX8x4.unpack(e.getDefinition()); break;
				
				case "16x2": mix = MixARX16x2.unpack(e.getDefinition()); break;
				case "16x3": mix = MixARX16x3.unpack(e.getDefinition()); break;
				case "16x4": mix = MixARX16x4.unpack(e.getDefinition()); break;
				
				case "32x2": mix = MixARX32x2.unpack(e.getDefinition()); break;
				case "32x3": mix = MixARX32x3.unpack(e.getDefinition()); break;
				case "32x4": mix = MixARX32x4.unpack(e.getDefinition()); break;
				
				case "64x2": mix = MixARX64x2.unpack(e.getDefinition()); break;
				case "64x3": mix = MixARX64x3.unpack(e.getDefinition()); break;
				case "64x4": mix = MixARX64x4.unpack(e.getDefinition()); break;
				default: mix = null; break;
			}
			
			sb.append(e.getType()).append("<br>").append(mix.toString());
			sb.append("<br>");
			sb.append("<table>");
			sb.append("<th>defn</th> <th>round1</th> <th>round2</th> <th>round3</th> <th>round4</th>");
			appendMix(sb, e);
			sb.append("</table><br>");
			sb.append("<img src=\"/debug/image/" + toImagePath(e.getAvImage1()) + "\" width=256 height=256 style=\"image-rendering: pixelated;\"><br>");
			sb.append("<img src=\"/debug/image/" + toImagePath(e.getAvImage2()) + "\" width=256 height=256 style=\"image-rendering: pixelated;\"><br>");
			sb.append("<img src=\"/debug/image/" + toImagePath(e.getAvImage3()) + "\" width=256 height=256 style=\"image-rendering: pixelated;\"><br>");
			sb.append("<img src=\"/debug/image/" + toImagePath(e.getAvImage4()) + "\" width=256 height=256 style=\"image-rendering: pixelated;\"><br>");
		}
		appendEnd(sb);
		return sb.toString();
	}
	
	
	@GetMapping(value = "/debug/arx/{type}/list/{page}", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String getARXList(@PathVariable("type") String type, @PathVariable("page") int page) {
		List<ARXMixEntry> list = database.getARXByTypeChronologically(type, 256, page);
		StringBuilder sb = new StringBuilder();
		appendStart(sb);
		sb.append(String.format("<a href=\"/debug/arx/%s/list/%d\">PREV</a> ",type,page-1));
		sb.append(String.format("<a href=\"/debug/arx/%s/list/%d\">NEXT</a> ",type,page+1));
		sb.append("<br>");
		if (list.size() == 0) {
			sb.append("not found");
		} else {
			sb.append("<table>");
			sb.append("<th>defn</th> <th>round1</th> <th>round2</th> <th>round3</th> <th>round4</th>");
			for (ARXMixEntry e : list) {
				appendMix(sb, e);
			}
		}
		appendEnd(sb);
		return sb.toString();
	}
	
	@GetMapping(value = "/debug/image/{path}/{path2}", produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseBody
	public byte[] image(@PathVariable("path") String path, @PathVariable("path2") String path2) {
		System.out.println("Loading " + Database.IMAGE_PATH.resolve(path + "/" + path2 + ".png").toAbsolutePath());
		try {
			return Files.readAllBytes(Database.IMAGE_PATH.resolve(path + "/" + path2 + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}
	
	private static void appendStart(StringBuilder sb) {
		sb.append("<!DOCTYPE html><html><body>");
		
		sb.append("<a href=\"/debug/arx/8x2/list/0\">8x2</a> ");
		sb.append("<a href=\"/debug/arx/8x3/list/0\">8x3</a> ");
		sb.append("<a href=\"/debug/arx/8x4/list/0\">8x4</a> ");
		
		sb.append("<a href=\"/debug/arx/16x2/list/0\">16x2</a> ");
		sb.append("<a href=\"/debug/arx/16x3/list/0\">16x3</a> ");
		sb.append("<a href=\"/debug/arx/16x4/list/0\">16x4</a> ");
		
		sb.append("<a href=\"/debug/arx/32x2/list/0\">32x2</a> ");
		sb.append("<a href=\"/debug/arx/32x3/list/0\">32x3</a> ");
		sb.append("<a href=\"/debug/arx/32x4/list/0\">32x4</a> ");
		
		sb.append("<a href=\"/debug/arx/64x2/list/0\">64x2</a> ");
		sb.append("<a href=\"/debug/arx/64x3/list/0\">64x3</a> ");
		sb.append("<a href=\"/debug/arx/64x4/list/0\">64x4</a> ");
		
		sb.append("<br><br>");
	}
	
	private static void appendEnd(StringBuilder sb) {
		sb.append("</body></html>");
	}
	
	private static void appendMix(StringBuilder sb, ARXMixEntry mix) {
		sb.append("<tr>");
		sb.append(String.format("<td><a href=\"/debug/arx/%s/definition/%d\">%016X</a></td>",mix.getType(),mix.getDefinition(),mix.getDefinition()));
		sb.append(String.format("<td>%f</td>",mix.getAvScore1()));
		sb.append(String.format("<td>%f</td>",mix.getAvScore2()));
		sb.append(String.format("<td>%f</td>",mix.getAvScore3()));
		sb.append(String.format("<td>%f</td>",mix.getAvScore4()));
		sb.append("</tr>");
	}
	
	private static String toImagePath(String snowId) {
		long v = Long.parseLong(snowId, 16);
		return String.format("%03X/%s", Database.mix12bit(v), snowId);
	}
}
