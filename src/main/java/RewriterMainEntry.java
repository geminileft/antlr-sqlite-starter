import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

public class RewriterMainEntry {

	private final static Logger logger = Logger.getLogger(RewriterMainEntry.class);

	public static class Loader extends SQLiteBaseListener {

		private int exitTableCount = 0;
		private TokenStreamRewriter mRewriter;

		public void setRewriter(TokenStreamRewriter rewriter) {
			mRewriter = rewriter;
		}

		@Override
		public void exitTable_name(SQLiteParser.Table_nameContext ctx) {
			boolean isTableFromQuery = ctx.parent.getRuleIndex() == SQLiteParser.RULE_table_or_subquery;
			if (!isTableFromQuery) {
				logger.info("Not a table from the query");
				return;
			}
			String contextStr = ctx.getText();
			logger.info("We are exiting enterTable_name ctx:" + contextStr);
			mRewriter.insertAfter(ctx.start, "something after");
			exitTableCount++;
		}

		public int getTableCount() {
			return exitTableCount;
		}

	}

	public static void mainWithParameters(InputStream is) throws IOException {

		CharStream cs = CharStreams.fromStream(is);
		SQLiteLexer lexer = new SQLiteLexer(cs);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SQLiteParser sqliteParser = new SQLiteParser(tokens);
		TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

		sqliteParser.setBuildParseTree(true);
		ParseTree tree = sqliteParser.parse();

		ParseTreeWalker walker = new ParseTreeWalker();
		Loader loader = new Loader();
		loader.setRewriter(rewriter);
		walker.walk(loader, tree);
		System.out.println(rewriter.getText());

		
	}
	public static void main(String[] args) throws Exception {
		logger.info("Starting MainEntry");

		Options options = new Options();
		Option input_path = new Option("i", "input_path", true, "input path");
		input_path.setRequired(false);
		Option resource_path = new Option("r", "resource_path", true, "resource path");
		resource_path.setRequired(false);
		options.addOption(input_path);
		options.addOption(resource_path);
		
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		HelpFormatter formatter = new HelpFormatter();
		String inputFile = "";
		InputStream is = null;

		boolean inputPathProvided = true;
		if(cmd.hasOption("input_path")) {
		    System.out.println("We have an input path");
		}
		else if(cmd.hasOption("resource_path")) {
		    System.out.println("We have a resource path");
		    inputPathProvided = false;
		}
		if (inputPathProvided) {
			try {
				cmd = parser.parse(options, args);
				inputFile = cmd.getOptionValue("input_path");
				is = new FileInputStream(inputFile);
			} catch (org.apache.commons.cli.ParseException e) {
				System.out.println(e.getMessage());
				formatter.printHelp("java RewriterMainEntry", options);
				System.exit(1);
			}
		} else {
			inputFile = cmd.getOptionValue("resource_path");
			ClassLoader classLoader = new RewriterMainEntry().getClass().getClassLoader();
			is = classLoader.getResource(inputFile).openStream();
		}

		mainWithParameters(is);
	}

}
