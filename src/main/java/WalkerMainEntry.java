import java.io.FileInputStream;
import java.io.InputStream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

public class WalkerMainEntry {

	private final static Logger logger = Logger.getLogger(WalkerMainEntry.class);
	
	public static class Loader extends SQLiteBaseListener {
		
		private int exitTableCount = 0;
		
		@Override public void exitTable_name(SQLiteParser.Table_nameContext ctx) {
			boolean isTableFromQuery =
					ctx.parent.getRuleIndex() == SQLiteParser.RULE_table_or_subquery;
			if (!isTableFromQuery) {
				logger.info("Not a table from the query");
				return;
			}
			String contextStr = ctx.getText();
			logger.info("We are exiting enterTable_name ctx:" + contextStr);
			exitTableCount++;
		}
		
		public int getTableCount() {
			return exitTableCount;
		}

	}
	
	public static void main(String[] args) throws Exception {
		logger.info("Starting MainEntry");

		Options options = new Options();
		Option input_path = new Option("i", "input_path", true, "input path");
		input_path.setRequired(true);
		options.addOption(input_path);
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		String inputFile = "";
		
		try {
			cmd = parser.parse(options,  args);
			inputFile = cmd.getOptionValue("input_path");
		} catch (org.apache.commons.cli.ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("java WalkerMainEntry", options);
			System.exit(1);
		}
		
		InputStream is = new FileInputStream(inputFile);
		CharStream cs = CharStreams.fromStream(is);
		SQLiteLexer lexer = new SQLiteLexer(cs);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SQLiteParser sqliteParser = new SQLiteParser(tokens);
		sqliteParser.setBuildParseTree(true);
		ParseTree tree = sqliteParser.parse();
		
		ParseTreeWalker walker = new ParseTreeWalker();
		Loader loader = new Loader();
		walker.walk(loader, tree);
		System.out.println(String.format("Total count: %d", loader.getTableCount()));
	}

}
