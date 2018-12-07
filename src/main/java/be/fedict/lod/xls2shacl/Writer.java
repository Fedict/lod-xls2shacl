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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.ROV;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write model to RDF file
 * 
 * @author Bart Hanssens
 */
public abstract class Writer {
	private final static Logger LOG = LoggerFactory.getLogger(Writer.class);
	private final ValueFactory FAC = SimpleValueFactory.getInstance();
	
	protected final static String[] LANGS = { "nl", "fr", "de", "en" }; 
	
	/**
	 * Constructor
	 */
	public Writer() {
	}
	
	/**
	 * Construct URI for ontology
	 * 
	 * @param name name
	 * @return URI as string
	 */
	protected abstract String getOnto(String name);

	
	/**
	 * Get empty RDF model with a set of predefined namespaces
	 * 
	 * @param name
	 * @return model
	 */
	protected Model getModel(String name) {
		Model m = new LinkedHashModel();
		m.setNamespace("adms", "http://www.w3.org/ns/adms#");
		m.setNamespace(DCAT.NS);
		m.setNamespace(DCTERMS.NS);
		m.setNamespace(FOAF.NS);
		m.setNamespace("locn", "http://www.w3.org/ns/locn#");
		m.setNamespace(ORG.NS);
		m.setNamespace(OWL.NS);
		m.setNamespace(RDF.NS);
		m.setNamespace(RDFS.NS);
		m.setNamespace(ROV.NS);
		m.setNamespace("schema", "http://schema.org/");
		m.setNamespace(SHACL.NS);
		m.setNamespace(SKOS.NS);
		m.setNamespace(XMLSchema.NS);
		
		return m;
	}
	
	/**
	 * Creates the triples for a SHACL/OWL/... file
	 * 
	 * @param name name file/ontology name
	 * @param m input RDF model
	 */
	public abstract Model createTriples(String name, Model m);
	
	/**
	 * Write a file
	 * 
	 * @param dir (sub)directory
	 * @param name name of the file
	 * @param m model to write
	 * @throws IOException 
	 */
	public void writeFile(Path dir, String name, Model m) throws IOException {
		Model triples = createTriples(name, m);
		
		if (triples.isEmpty()) {
			LOG.info("Nothing to write for " + name);
			return;
		}
		Path p = Paths.get(dir.toFile().toString(), name.toLowerCase() + ".ttl");
		LOG.info("Writing to " + p);
		
		File subdir = dir.toFile();
		if (! subdir.exists()) {
			LOG.info("Creating subdir");
			subdir.mkdirs();
		}
		
		try (OutputStream os = Files.newOutputStream(p, StandardOpenOption.CREATE, 
														StandardOpenOption.TRUNCATE_EXISTING,
														StandardOpenOption.WRITE)) {
			RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, os);
			writer.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
			//writer.set(BasicWriterSettings.PRETTY_PRINT, true);
			Rio.write(triples, writer);
		}
	}
}
