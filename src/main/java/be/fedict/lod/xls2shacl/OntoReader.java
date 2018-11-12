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
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read properties and classes from XLSX into a RDF model
 * 
 * @author Bart Hanssens
 */
public class OntoReader {
	private final static Logger LOG = LoggerFactory.getLogger(OntoReader.class);
	
	private final Map<String,Model> modelMap = new HashMap<>();
	private final ValueFactory FAC = SimpleValueFactory.getInstance();
	
	private final int ID = 0;
	private final int ONTO = 1;
	private final int TYPE = 2;
	private final int URI = 3;
	
	/**
	 * Get RDF4J model
	 * 
	 * @param name name of the ontology
	 * @return model
	 */
	private Model getModel(String name) {
		Model m = modelMap.getOrDefault(name.trim(), null);
		if (m == null) {
			m = new LinkedHashModel();
			modelMap.put(name, m);
		}
		return m;
	}
	//prOntology	Type	URI	Name	LabelNL	LabelFR	Definition	DefinitionNL	DefinitionFR	Comment	CommentNL	CommentFR

	/**
	 * Process rows in work sheet
	 * 
	 * @param sheet work sheet to process
	 */
	private void processRows(Sheet sheet) {
		sheet.getHeader();
		
		for (Row row: sheet) {
			Cell val = row.getCell(ONTO);
			Cell type = row.getCell(TYPE);
			Cell uri = row.getCell(URI);
			
			if (val != null && type != null && uri != null) {
				Model m = getModel(val.getStringCellValue());
				try {
					IRI s = FAC.createIRI(uri.getStringCellValue());
					IRI o = uri.getStringCellValue().toLowerCase().equals("class") ? RDFS.CLASS : RDF.PROPERTY;
					m.add(s, RDF.TYPE, o);
				} catch (IllegalArgumentException ioe) {
					LOG.warn("Can't create IRI");
				}
			}
		}
	}
	/**
	 * Read file
	 * 
	 * @param fin input file
	 * @param name name of the worksheet
	 */
	public void read(File fin, String name) {
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
		
		if (modelMap.isEmpty()) {
			LOG.warn("Empty models");
		}
		
		modelMap.forEach((k,v) -> { v.forEach(s -> System.err.println(s)); });
	}
}
