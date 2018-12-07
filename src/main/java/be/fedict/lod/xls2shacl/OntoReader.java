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

import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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
	
	private final Map<Integer,IRI> mapping = new HashMap<>();
	private final Map<String,Resource> contexts = new HashMap<>();
	
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
	private final int DEF = 7;
	private final int DEF_NL = 8;
	private final int DEF_FR = 9;
	
	private final int SOURCE = 2;
	private final int DATA_MODEL = 3;
	private final int PREDICATE = 5;
	private final int OBJECT_NAME = 6;
	private final int SUBJECT_ID = 10;
	private final int OBJECT_ID = 11;
	
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
		return FAC.createIRI(GRAPH + name.toLowerCase());
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
	private void processDescRows(Sheet sheet) {	
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
			Cell def_en = row.getCell(DEF);
			Cell def_nl = row.getCell(DEF_NL);
			Cell def_fr = row.getCell(DEF_FR);
			
			if (val != null && type != null && uri != null) {
				Resource context = getContext(val.getStringCellValue());
				try {
					String u = uri.getStringCellValue();
					if (u.startsWith("<")) {
						u = u.substring(1);
					} 
					if (u.endsWith(">")) {
						u = u.substring(0, u.length() - 1);
					}
					IRI s = FAC.createIRI(u);
					IRI o = type.getStringCellValue().toLowerCase().equals("class") ? RDFS.CLASS 
																					: RDF.PROPERTY;
					mapping.put((int) id.getNumericCellValue(), s);
					
					m.add(s, RDF.TYPE, o, context);
					m.add(s, SKOS.ALT_LABEL, makeLiteral(name, "en"), context);
					m.add(s, DCTERMS.TITLE, makeLiteral(name, "en"), context);
					m.add(s, DCTERMS.TITLE, makeLiteral(label_nl, "nl"), context);
					m.add(s, DCTERMS.TITLE, makeLiteral(label_fr, "fr"), context);
					m.add(s, DCTERMS.DESCRIPTION, makeLiteral(def_en, "en"), context);
					m.add(s, DCTERMS.DESCRIPTION, makeLiteral(def_nl, "nl"), context);
					m.add(s, DCTERMS.DESCRIPTION, makeLiteral(def_fr, "fr"), context);
					
				} catch (IllegalArgumentException ioe) {
					LOG.warn("Can't create IRI" + ioe.getMessage());
				}
			}
		}
	}
	
	/**
	 * Get data type for name
	 * 
	 * @param name
	 * @return 
	 */
	private IRI getType(String name) {
		if (name == null) {
			LOG.warn("Cannot get type");
			return null;
		}
		IRI ret = null;
		String n = name.toLowerCase();
		switch(n) {
			case "_boolean":
				ret = XMLSchema.BOOLEAN;
				break;
			case " _string":
				ret = XMLSchema.STRING;
				break;
			case "_langstring":
				ret = RDF.LANGSTRING;
				break;
			case "_concept":
				ret = SKOS.CONCEPT;
			default:
				break;
		}
		return ret;
		
	}
	
	
	/**
	 * Process rows in work sheet
	 * 
	 * @param sheet work sheet to process
	 */
	private void processMapRows(Sheet sheet) {	
		for (Row row: sheet) {
			if (row.getRowNum() == 0) {
				continue; // skip header
			}
			Cell val = row.getCell(DATA_MODEL);
			Cell source = row.getCell(SOURCE);
			
			String src = (source != null) ? source.getStringCellValue().toLowerCase() : "";
			if (! src.equals("fed")) {
				LOG.debug("Skipping source " + src);
				continue; // skip non fed source
			}

			Cell pred = row.getCell(PREDICATE);
			Cell subj = row.getCell(SUBJECT_ID);
			Cell obj = row.getCell(OBJECT_ID);
			Cell name = row.getCell(OBJECT_NAME);

			if (val != null && pred != null) {
				Resource context = getContext(val.getStringCellValue());
				try {
					IRI s = mapping.get((int) subj.getNumericCellValue());
					if (s == null) {
						LOG.debug("Subject not found row " + row.getRowNum());
						continue;
					}
					IRI o = null;
					if (obj != null) {
						o = mapping.get((int) obj.getNumericCellValue());
						if (o == null) {
							LOG.debug("Object not found" + row.getRowNum());
							continue;
						}
					}

					String p = pred.getStringCellValue().toLowerCase();
					switch (p) {
						case "domain":							
								m.add(s, RDFS.DOMAIN ,o, context);
								break;
						case "range": 
								if (o != null) {
									m.add(s, RDFS.RANGE, o, context);
								} else {
									IRI t = getType(name.getStringCellValue());
									if (t != null) {
										m.add(s, OWL.DATATYPEPROPERTY, t);
									}
								}
								break;
						case "subclassof":
								m.add(s, RDFS.SUBCLASSOF, o, context);
						default: 
								break;
					}
				} catch (IllegalArgumentException ioe) {
					LOG.warn("Can't create IRI" + ioe.getMessage());
				}
			}
		}
	}
		
	/**
	 * Read file into RDF models, one context per ontology
	 * 
	 * @param fin input file
	 * @param descSheet name of the sheet with descriptions
	 * @param mapSheet name of the sheet with mappings
	 * @return model
	 */
	public Model read(File fin, String descSheet, String mapSheet) {
		try (InputStream is = new FileInputStream(fin)) {
			Workbook wb = WorkbookFactory.create(is);
			
			Sheet sheet = wb.getSheet(descSheet);
			if (sheet != null) {
				processDescRows(sheet);
			} else {
				LOG.error("Worksheet not found: " + descSheet);
			}
			
			sheet = wb.getSheet(mapSheet);
			if (sheet != null) {
				processMapRows(sheet);
			} else {
				LOG.error("Worksheet not found: " + mapSheet);
			}
		} catch (IOException ex) {
			LOG.error("Could not parse file " + fin.getName());
		}
		
		if (m.isEmpty()) {
			LOG.warn("Empty models");
		}
		return m;
	}
}
