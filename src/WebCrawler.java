
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WebCrawler {
    private final static String userAgent = "Bob";
    private final static int WAIT = 500;
    private static HashSet<String> visitedPages;
    private static Queue<Element> queue;
    private static int id;

    public static boolean isRobotSafe(URL url) {
	String robots = url.getProtocol() + "://" + url.getHost() + "/robots.txt";
	String path = url.getFile();

	try {
	    // Read the robots.txt line by line
	    BufferedReader br = new BufferedReader(new InputStreamReader(new URL(robots).openStream()));
	    String line;

	    while ((line = br.readLine()) != null) {
		String robotAgent = line.replaceAll("[ \\-:]", "").toLowerCase();

		if (robotAgent.startsWith("useragent")) {
		    robotAgent = robotAgent.substring("useragent".length()).toLowerCase().replace("*", ".*");
		}

		if (Pattern.matches(robotAgent, userAgent)) {
		    while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
			String[] temp = line.toLowerCase().trim().split(":");

			if (temp[0].trim().equalsIgnoreCase("allow")) {
			    if (temp.length > 1) {
				// Case sensitive
				// temp[1] = temp[1].trim().replace("*", ".*");

				// Case insensitive
				temp[1] = temp[1].trim().toLowerCase().replace("*", ".*");
				temp[1] = temp[1].replace("?", "\\?");

				if (temp[1].endsWith("$")) {
				    temp[1] = temp[1].substring(0, temp[1].length());
				    Pattern p = Pattern.compile(temp[1]);
				    Matcher m = p.matcher(path);

				    if (m.matches())
					return true;

				} else {
				    temp[1] = temp[1].substring(0, temp[1].length());
				    Pattern p = Pattern.compile(temp[1]);
				    Matcher m = p.matcher(path);

				    if (m.lookingAt())
					return true;
				}

			    } else {
				// Nothing following "Allow:"
				return false;
			    }
			}

			if (temp[0].trim().equalsIgnoreCase("disallow") || temp[0].trim().equalsIgnoreCase("NoIndex")) {
			    if (temp.length > 1) {
				// Case sensitive
				// temp[1] = temp[1].trim().replace("*", ".*");

				// Case insensitive
				temp[1] = temp[1].trim().toLowerCase().replace("*", ".*");
				temp[1] = temp[1].replace("?", "\\?");

				if (temp[1].endsWith("$")) {
				    temp[1] = temp[1].substring(0, temp[1].length());
				    Pattern p = Pattern.compile(temp[1]);
				    Matcher m = p.matcher(path);

				    if (m.matches())
					return false;

				} else {
				    temp[1] = temp[1].substring(0, temp[1].length());
				    Pattern p = Pattern.compile(temp[1]);
				    Matcher m = p.matcher(path);

				    if (m.lookingAt())
					return false;
				}

			    } else {
				// Nothing following "Disallow:"
				return true;
			    }
			}
		    }
		}
	    }
	} catch (IOException | PatternSyntaxException e) {
	    // System.out.println(e);
	    return true;
	}

	return true;
    }

    public static void appendReport(Response resp, String url, String report, String repository, int totalOutboundLinks,
	    int outboundLinks) {
	try {
	    Document doc = resp.parse();
	    int statusCode = resp.statusCode();

	    FileWriter fw = new FileWriter(report, true);
	    try (BufferedWriter out = new BufferedWriter(fw)) {
		// Link to the live URL
		out.write("<p><div><a href = " + url + ">" + doc.title() + "</a> (" + url + ")</div>");

		// Link to the downloaded page in the repository
		String fileLocation = "file:///" + System.getProperty("user.dir") + "/" + repository + "/" + (id - 1)
			+ ".html";
		out.write("<div><a href = " + fileLocation + ">Repository Link</a></div>");

		// Page statistics: HTTP status code, number of out-links, number of images
		out.write("<div>HTTP Status Code: " + statusCode + "</div>");
		out.write("<div>Outbound Links: " + totalOutboundLinks + "</div>");
		out.write("<div>Outbound Links (domain restricted): " + outboundLinks + "</div>");

		Elements images = doc.getElementsByTag("img");
		out.write("<div>Number of Images: " + images.size() + "</div>");

		out.write("</p>");
	    }
	} catch (IOException e) {
	    System.out.println(e);
	}
    }

    public static void crawlPage(String url, int numberLinks, String domain, String repository, String report) {
	try {
	    if (!isRobotSafe(new URL(url))) {
		System.out.println("This page is not allowed to be crawled: " + url);
		return;
	    }

	    // Get Document
	    Response response = Jsoup.connect(url).execute();
	    Document doc = response.parse();

	    // Save into a repository
	    File f = new File(repository + "/" + (id++) + ".html");
	    try (BufferedWriter htmlWriter = new BufferedWriter(
		    new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) {
		htmlWriter.write(doc.toString());
		htmlWriter.close();
	    }

	    // Add current page to set of visited pages
	    visitedPages.add(url);
	    System.out.println(url);

	    // Get all links and add them to the queue
	    Elements questions = doc.select("a[href]");
	    int totalOutboundLinks = questions.size(); // Total number of outbound links
	    int outboundLinks = 0; // Counter of outbound links restricted to the provided domain

	    for (Element link : questions) {
		if (link.attr("abs:href").contains(domain)) {
		    queue.add(link);
		    outboundLinks++;
		}
	    }

	    // Update report
	    appendReport(response, url, report, repository, totalOutboundLinks, outboundLinks);

	    // Visit page in queue
	    while (!queue.isEmpty()) {

		// Limit on how many pages to visit
		if (visitedPages.size() >= numberLinks)
		    return;

		// For politeness, wait a moment before visiting link
		TimeUnit.MILLISECONDS.sleep(WAIT);

		// Visit page (if not yet visited)
		String link = queue.remove().attr("abs:href");
		if (link.contains("#"))
		    link = link.substring(0, link.indexOf("#"));

		if (!visitedPages.contains(link)) {
		    crawlPage(link, numberLinks, domain, repository, report);
		}
	    }

	} catch (org.jsoup.HttpStatusException e) {
	    // Dealing with the HTTP Status Exception (such as 404)
	    System.out.println(e);
	    BufferedWriter out;
	    try {
		// Update report
		out = new BufferedWriter(new FileWriter(report, true));

		out.write("<p><div><a href = " + url + ">" + url + "</a> (" + url + ")</div>");
		out.write("<div>HTTP Status Code: " + e.getStatusCode() + "</div>");
		out.write("</p>");

		out.close();

	    } catch (IOException ex) {
		System.out.println(ex);
	    }

	} catch (MalformedURLException e) {
	    System.out.println(e);
	} catch (IOException | InterruptedException e) {
	    System.out.println(e);
	}
    }

    public static void crawl(String fileName, String repository, String report) {
	// Initialization
	String url = "http://stackexchange.com";
	String domain = "";
	int maxPages = 100;

	visitedPages = new HashSet<>();
	queue = new LinkedList<>();
	id = 0;

	// Read in specifications
	try (Scanner scanner = new Scanner(new File(fileName))) {
	    scanner.useDelimiter(",");

	    if (scanner.hasNext())
		url = scanner.next();
	    if (scanner.hasNext())
		maxPages = Integer.parseInt(scanner.next());
	    if (scanner.hasNext())
		domain = scanner.next();

	} catch (FileNotFoundException e) {
	    System.out.println(e);
	}

	System.out.println("Seed: " + url);
	System.out.println("Max number of pages: " + maxPages);
	System.out.println("Domain Restriction: " + domain + "\n");

	// Repository for storing downloaded pages
	new File(repository).mkdirs();

	// Initialize report
	try (PrintWriter writer = new PrintWriter(report, "UTF-8")) {
	    writer.println("<p>Web Crawler</p>");
	    writer.println("<div>Seed: <a href=" + url + ">" + url + "</a></div>");
	    writer.println("<div>Max number of pages: " + maxPages + "</div>");
	    writer.println("<div>Domain Restriction: " + domain + "</div>");

	} catch (FileNotFoundException | UnsupportedEncodingException e) {
	    System.out.println(e);
	}

	// Start crawling
	crawlPage(url, maxPages, domain, repository, report);
    }

    public static void main(String[] args) {
	try {
	    crawl("specification.csv", "repository", "report.html");
	    // ContentProcessor.processRepository("repository");
	    // ContentProcessor.processFrequency("repository");
	} catch (Exception e) {
	    System.out.println(e);
	}
    }
}