package lu.uni.trux.jucify.utils;

public class CustomPrints {
	
	private static final String end = "\u001B[0m";
	private static final String red = "\u001B[31m";
	private static final String green = "\u001B[32m";
	private static final String blue = "\u001B[36m";
	private static final String yellow = "\u001B[33m";
	
	private static void pprint(String prefix, String message, String color) {
		System.out.println(String.format("%s[%s] %s%s", color, prefix, message, end));
	}
	
	public static void perror(String message) {
		pprint("!", message, red);
	}
	
	public static void psuccess(String message) {
		pprint("✓", message, green);
	}
	
	public static void pinfo(String message) {
		pprint("*", message, blue);
	}
	
	public static void pwarning(String message) {
		pprint("*", message, yellow);
	}
}
