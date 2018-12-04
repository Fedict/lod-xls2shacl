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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eclipse.rdf4j.model.BNode;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
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
 * Write model to SHACL file file
 * 
 * @author Bart Hanssens
 */
public class ShaclWriter {
	private final static Logger LOG = LoggerFactory.getLogger(ShaclWriter.class);
	
	private final ValueFactory FAC = SimpleValueFactory.getInstance();
	
	private final String PREFIX = "http://vocab.belgif.be/ns";
	
	/**
	 * Constructor
	 */
	public ShaclWriter() {
	}
	
	private String getOnto(String name) {
		return PREFIX + "/" + name.toLowerCase() + "#";
	}

	/**
	 * Get model for shacl
	 * @param name
	 * @return model
	 */
	private Model getModel(String name) {
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

		Literal version = FAC.createLiteral("Draft " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		IRI onto = FAC.createIRI(getOnto(name));

		m.add(onto, RDF.TYPE, OWL.ONTOLOGY);
		m.add(onto, OWL.VERSIONINFO, version);
		m.setNamespace("shbe-" + name.toLowerCase(), onto.toString());
		
		return m;
	}
	
	/**
	 * Write a model to shacl file (turtle format)
	 * 
	 * @param name file name
	 * @param m RDF model
	 * @throws IOException 
	 */
	public void write(String name, Model m) throws IOException {
		Model shacl = getModel(name);
	
		Set<Resource> subjs = m.filter(null, RDF.TYPE, RDFS.CLASS).subjects();
		for (Resource subj: subjs) {
			Value v = m.filter(subj, SKOS.ALT_LABEL, null).objects().stream().findFirst().orElse(null);
			String label = (v != null) ? v.stringValue() : "";
			
			IRI nodeShape = FAC.createIRI(getOnto(name) + label + "Shape");
			shacl.add(nodeShape, RDF.TYPE, SHACL.NODE_SHAPE);
			shacl.add(nodeShape, SHACL.TARGET_CLASS, subj);
			
			Set<Resource> props = m.filter(null, RDFS.DOMAIN, subj).subjects();
			for (Resource prop: props) {
				BNode blank = FAC.createBNode();
				shacl.add(nodeShape, SHACL.PROPERTY, blank);
				shacl.add(blank, RDF.TYPE, SHACL.PROPERTY_SHAPE);
				shacl.add(blank, SHACL.PATH, prop);
			}
		}
		
		Path p = Paths.get(name.toLowerCase() + ".ttl");
		LOG.info("Writing to " + p);
		try (OutputStream os = Files.newOutputStream(p, StandardOpenOption.CREATE, 
														StandardOpenOption.TRUNCATE_EXISTING,
														StandardOpenOption.WRITE)) {
			RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, os);
			writer.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
			//writer.set(BasicWriterSettings.PRETTY_PRINT, true);
			Rio.write(shacl, writer);
		}
	}
}
