/*
 * Copyright (c) 2018, FPS BOSA DG DT
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.fedict.lod.xls2shacl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert a specific XLSX containing ontologies to OWL and SHACL 
 * 
 * @author Bart Hanssens
 */
public class Main {
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	private final static Options OPTS = 
			new Options().addRequiredOption("i", "input", true, "input XLS")
						.addOption("o", "outdir", true, "output directory");
			
	/**
	 * Parse command line arguments
	 * 
	 * @param args arguments
	 * @return commandline object 
	 */
	private static CommandLine parseArgs(String[] args) {
		try {
			return new DefaultParser().parse(OPTS, args);
		} catch (ParseException ex) {
			LOG.error(ex.getMessage());
			HelpFormatter help = new HelpFormatter();
			help.printHelp("xls2shacl.jar", OPTS);
			return null;
		}
	}
	
	/**
	 * Log message and exit with exit code
	 * 
	 * @param code system exit code
	 * @param msg message to be logged
	 */
	private static void exit(int code, String msg) {
		LOG.error(msg);
		System.exit(code);
	}
	
	/**
	 * Main
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		CommandLine cli = parseArgs(args);
		if (cli == null) {
			exit(-1, "Couldn't parse command line");
		}
	}
	
}
