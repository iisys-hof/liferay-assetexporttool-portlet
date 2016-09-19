package de.iisys.liferay.portlet.wikiexport;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ParsingSyntax {
	public static final String CREOLE_SYNTAX = "creole";
	public static final String HTML_SYNTAX = "html";
	
	private String currentSyntax = "";
	
	public ParsingSyntax(String syntax) {
		currentSyntax = syntax;
	}
	
	public String getSyntax() {
		return currentSyntax;
	}
	
	public String startHeading1() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "=";
			case HTML_SYNTAX:	return "<h1>";
		}
		return "";
	}
	public String endHeading1() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "=";
			case HTML_SYNTAX:	return "</h1>";
		}
		return "";
	}
	
	public String startHeading2() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "==";
			case HTML_SYNTAX:	return "<h2>";
		}
		return "";
	}
	public String endHeading2() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "==";
			case HTML_SYNTAX:	return "</h2>";
		}
		return "";
	}
	
	public String startHeading3() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "===";
			case HTML_SYNTAX:	return "<h3>";
		}
		return "";
	}
	public String endHeading3() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "===";
			case HTML_SYNTAX:	return "</h3>";
		}
		return "";
	}
	
	public String startHeading4() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "====";
			case HTML_SYNTAX:	return "<h4>";
		}
		return "";
	}
	public String endHeading4() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "====";
			case HTML_SYNTAX:	return "</h4>";
		}
		return "";
	}
	
	public Pattern boldPattern() {
		Pattern pattern = Pattern.compile("");
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	pattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
//			case HTML_SYNTAX:	pattern = Pattern.compile("\\<strong(.*?)<\\/strong\\>");
			case HTML_SYNTAX:	pattern = Pattern.compile("(?i)<strong\\b[^>]*>(.*?)<\\/strong>");
			// (	start group
			// ?i	case insensitive checking
			// )	end group
			// .*?	match anything
		}
		return pattern;
	}
	
	public String startBold() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "**";
			case HTML_SYNTAX:	return "<strong>";
		}
		return "";
	}
	public String endBold() {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	return "**";
			case HTML_SYNTAX:	return "</strong>";
		}
		return "";
	}
	
	public Set<String> removeFormatting() {
		Set<String> formattings = new HashSet<String>();
		switch(currentSyntax) {
			case CREOLE_SYNTAX:
				formattings.add("{{{");
				formattings.add("}}}");
//			case HTML_SYNTAX:
//				formattings.add("<p>");
//				formattings.add("</p>");
		}
		return formattings;
	}
	
	public String doSpecificFormatting(String text) {
		switch(currentSyntax) {
			case CREOLE_SYNTAX:	;
			case HTML_SYNTAX:	text = text.replaceAll("\\<.*?>", "");
		}
		
		return text;
	}
}
