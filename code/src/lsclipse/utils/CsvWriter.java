package lsclipse.utils;

import java.io.IOException;
import java.io.Writer;

public class CsvWriter {
	private static final char DEFAULT_SEPARATOR = ',';
	
	public static void writeLine(Writer w, CharSequence... columns) throws IOException {
		writeLine(w, DEFAULT_SEPARATOR, ' ', columns);
	}
	
	public static void writeLine(Writer w, char separator, CharSequence... columns) throws IOException {
		writeLine(w, separator, ' ', columns);
	}
	
	private static String format(CharSequence value) {
		String result = (String)value;
		if (result.contains("\"")) {
			result = result.replace("\"", "\"\"");
		}
		return result;
	}
	
	public static void writeLine(Writer w, char separator, char customQuotation, CharSequence... columns) throws IOException {
		String line = buildLine(separator, customQuotation, columns);
		w.append(line);
	}
	
	public static String buildLine(CharSequence... columns) {
		return buildLine(DEFAULT_SEPARATOR, ' ', columns);
	}
	
	public static String buildLine(char separator, CharSequence... columns) {
		return buildLine(separator, ' ', columns);
	}
	
	public static String buildLine(char separator, char customQuotation, CharSequence... columns) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		
		if (separator == ' ') {
			separator = DEFAULT_SEPARATOR;
		}
		
		for (CharSequence column : columns) {
			if (!first)
				sb.append(separator);
			if (customQuotation == ' ')
				sb.append(format(column));
			else
				sb.append(customQuotation).append(format(column)).append(customQuotation);
			first = false;
		}
		sb.append("\n");
		return sb.toString();
	}
}
