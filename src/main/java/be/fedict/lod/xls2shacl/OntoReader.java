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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read properties and classes from XLSX into a RDF model
 * 
 * @author Bart Hanssens
 */
public class OntoReader {
	private final static Logger LOG = LoggerFactory.getLogger(OntoReader.class);
	
	private final static Model m = new LinkedHashModel();
	static {
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
	}

	private final HashMap<String,Resource> contexts = new HashMap<>();
	private final ValueFactory FAC = SimpleValueFactory.getInstance();
	
	private final String PREFIX = "http://vocab.belgif.be";
	private final String GRAPH = "http://fedict.be/graph/";
	
	private final int ID = 0;
	private final int ONTO = 1;
	private final int TYPE = 2;
	private final int URI = 3;
	private final int NAME = 4;
	private final int LABEL_NL = 5;
	private final int LABEL_FR = 6;
	
	/**
	 * Get existing (ontology) context or create a new one
	 * 
	 * @param name name of the ontology
	 * @return model
	 */
	private Resource getContext(String name) {
		Resource context = contexts.get(name);
		if (context != null) {
			return context;
		}
		context = FAC.createIRI(GRAPH + name.toLowerCase());

		Literal version = FAC.createLiteral("Draft " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		IRI onto = FAC.createIRI(PREFIX + "/" + name.toLowerCase() + "#");

		m.add(onto, RDF.TYPE, OWL.ONTOLOGY, context);
		m.add(onto, OWL.VERSIONINFO, version, context);
		m.setNamespace("shbe-" + name.toLowerCase(), onto.toString());
	
		return context;
	}
	
	
	/**
	 * Create a literal from string value of a cell
	 * 
	 * @param cell spreadsheet cell
	 * @param lang langauge code
	 * @return literal
	 */
	private Literal makeLiteral(Cell cell, String lang) {
		if (cell.getCellType() == CellType.NUMERIC) {
			return FAC.createLiteral(String.valueOf(cell.getNumericCellValue()), lang);
		} 
		return FAC.createLiteral(cell.getStringCellValue(), lang);
	}

	//Ontology	Type	URI	Name	LabelNL	LabelFR	Definition	DefinitionNL	DefinitionFR	Comment	CommentNL	CommentFR

	/**
	 * Process rows in work sheet
	 * 
	 * @param sheet work sheet to process
	 */
	private void processRows(Sheet sheet) {	
		for (Row row: sheet) {
			if (row.getRowNum() == 0) {
				continue; // skip header
			}
			Cell id = row.getCell(ID);
			Cell val = row.getCell(ONTO);
			Cell type = row.getCell(TYPE);
			Cell uri = row.getCell(URI);
			Cell name = row.getCell(NAME);
			Cell label_nl = row.getCell(LABEL_NL);
			Cell label_fr = row.getCell(LABEL_FR);
			
			if (val != null && type != null && uri != null) {
				Resource context = getContext(val.getStringCellValue());
				try {
					IRI s = FAC.createIRI(uri.getStringCellValue());
					IRI o = type.getStringCellValue().toLowerCase().equals("class") ? RDFS.CLASS 
																					: RDF.PROPERTY;
					m.add(s, RDF.TYPE, o, context);
					m.add(s, SKOS.NOTATION, makeLiteral(id, "en"), context);
					m.add(s, DCTERMS.IDENTIFIER, makeLiteral(name, "en"), context);
					m.add(s, DCTERMS.TITLE, makeLiteral(label_nl, "nl"), context);
					m.add(s, DCTERMS.TITLE, makeLiteral(label_fr, "fr"), context);
				} catch (IllegalArgumentException ioe) {
					LOG.warn("Can't create IRI" + ioe.getMessage());
				}
			}
		}
	}
	
	/**
	 * Get map of names and contexts
	 * 
	 * @return 
	 */
	public Map<String,Resource> getContexts() {
		return contexts;
	}
		
	/**
	 * Read file into RDF models, one context per ontology
	 * 
	 * @param fin input file
	 * @param name name of the worksheet
	 * @return model
	 */
	public Model read(File fin, String name, String mappings) {
		try (InputStream is = new FileInputStream(fin)) {
			Workbook wb = WorkbookFactory.create(is);
			
			Sheet sheet = wb.getSheet(name);
			if (sheet != null) {
				processRows(sheet);
			} else {
				LOG.error("Worksheet not found: " + name);
			}
		} catch (IOException ex) {
			LOG.error("Could not parse file " + fin.getName());
		}
		
		if (m.isEmpty()) {
			LOG.warn("Empty models");
		}
		
		m.contexts().forEach(ctx -> { 
			System.err.println("MODEL " + ctx.toString());
			m.filter(null, null, null, ctx).forEach(s -> System.err.println(s)); });
		
		return m;
	}
}
