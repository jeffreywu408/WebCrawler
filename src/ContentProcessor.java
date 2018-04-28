import java.util.Collections;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class ContentProcessor {
    public static HashMap<String, Integer> wordCount;
    public static Map<String, Integer> rank;

    public static void removeComments(Node node) {
	for (int i = 0; i < node.childNodes().size();) {
	    Node child = node.childNode(i);

	    if (child.nodeName().equals("#comment")) {
		child.remove();
	    } else {
		removeComments(child);
		i++;
	    }
	}
    }

    public static String processFile(Document doc) {
	String content = "";

	// Parsing based on some HTML heuristics
	Elements paragraph = doc.select("p, pre, code, h1, h2, h3, h4, h5, h6");
	for (Element elem : paragraph) {
	    String text = elem.text().replace("\n", "").replace("\r", "").trim();

	    int numWords = text.trim().split("\\s+").length;
	    if (numWords > 1) {
		// System.out.println(text);
		content = content + " " + text;
	    }

	    elem.remove();
	}

	// Looking at the rest of the document
	Elements tags = doc.body().select("*");
	for (Element tag : tags) {
	    String text = tag.text().replace("\n", "").replace("\r", "").trim();

	    int numWords = text.trim().split("\\s+").length;
	    if (numWords > 10) {
		// System.out.println(text);
		content = content + " " + text;
	    }

	    tag.remove();
	}

	return content;
    }

    public static void processRepository(String repository) {
	File dir = new File(repository);
	File[] list = dir.listFiles();

	for (File f : list) {
	    if (f.isFile()) {
		try {
		    Document doc = Jsoup.parse(f, "UTF-8");

		    // Some basic processing: Get the title, remove the comments
		    // Get rid of the header, style, meta, javascript
		    String mainContent = doc.title();
		    removeComments(doc);
		    doc.select("style, meta, script, nav, link, head").remove();

		    // Now process the document and get the main content
		    mainContent += processFile(doc).trim();

		    // Remove all punctuation
		    Pattern punctuation = Pattern.compile("[\\p{Punct}]", UNICODE_CHARACTER_CLASS);
		    Matcher m = punctuation.matcher(mainContent);
		    mainContent = m.replaceAll("");

		    // Overwrite the file we're processing with the main content that has been
		    // parsed out
		    try (BufferedWriter writer = new BufferedWriter(
			    new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) {
			writer.write(mainContent);
		    }

		} catch (IOException e) {
		    System.out.println(e);
		}

	    }
	}
    }

    public static void getWordFreqeuncy(File file) {
	try {
	    Document doc = Jsoup.parse(file, "UTF-8");
	    String[] words = doc.text().split("\\s+");

	    for (String word : words) {
		if (word == null)
		    continue;
		word = word.toLowerCase().trim();

		// If the HashMap has the word, count = value + 1, else count = 1
		int count = wordCount.containsKey(word) ? wordCount.get(word) + 1 : 1;

		// Update HashMap
		wordCount.put(word, ++count);
	    }

	} catch (IOException e) {
	    System.out.println(e);
	}
    }

    public static Map<String, Integer> sortByValue() {
	List<Map.Entry<String, Integer>> list = new LinkedList<>(wordCount.entrySet());

	// Sort the Word Count Hash Map by decreasing frequency
	Collections.sort(list, (Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) -> (o2.getValue())
		.compareTo(o1.getValue()));

	Map<String, Integer> result = new LinkedHashMap<>();
	list.stream().forEach((entry) -> {
	    result.put(entry.getKey(), entry.getValue());
	});

	return result;
    }

    public static void processFrequency(String repository) {
	wordCount = new HashMap<>();

	File dir = new File(repository);
	File[] list = dir.listFiles();
	for (File f : list) {
	    if (f.isFile()) {
		getWordFreqeuncy(f);
	    }
	}

	// Sort and save to file
	rank = sortByValue();
	try (PrintWriter writer = new PrintWriter(repository + "/WordFrequency.txt", "UTF-8")) {
	    // Format: rank frequency word (space delimited)
	    int wordRank = 1;
	    System.out.println("\nWord Frequency");

	    for (String key : rank.keySet()) {
		if (wordRank >= 300) {
		    System.out.println((wordRank++) + " " + rank.get(key) + " " + key);
		    writer.print(wordRank + " " + rank.get(key) + " " + key);
		    break;

		} else {
		    System.out.println((wordRank++) + " " + rank.get(key) + " " + key);
		    writer.println((wordRank++) + " " + rank.get(key) + " " + key);
		}
	    }

	} catch (FileNotFoundException | UnsupportedEncodingException e) {
	    System.out.println(e);
	}
    }
}