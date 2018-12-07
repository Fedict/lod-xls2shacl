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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

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
public class OwlWriter extends Writer {
	private final static Logger LOG = LoggerFactory.getLogger(ShaclWriter.class);
	
	private final ValueFactory FAC = SimpleValueFactory.getInstance();
	private final String PREFIX = "http://vocab.belgif.be/ns";
	
	@Override
	protected String getOnto(String name) {
		return PREFIX + "/" + name.toLowerCase() + "#";
	}
		
	@Override
	public Model createTriples(String name, Model m) {
		Model owl = getModel(name);

		Literal version = FAC.createLiteral("Draft " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		IRI onto = FAC.createIRI(getOnto(name) + "#");
		owl.setNamespace("be-" + name.toLowerCase(), onto.toString());
		
		owl.add(onto, RDF.TYPE, OWL.ONTOLOGY);
		owl.add(onto, OWL.VERSIONINFO, version);
		for (String lang: LANGS) {
			owl.add(onto, RDFS.LABEL, FAC.createLiteral(name, lang));
		}
		
		Set<Resource> cls = m.filter(null, RDF.TYPE, RDFS.CLASS).subjects();
		for (Resource cl: cls) {
			owl.add(cl, RDFS.ISDEFINEDBY, onto);
			owl.add(cl, RDF.TYPE, RDFS.CLASS);
			owl.add(cl, RDF.TYPE, OWL.CLASS);
		}
		
		Set<Resource> props = m.filter(null, RDF.TYPE, RDF.PROPERTY).subjects();
		for (Resource prop: props) {
			owl.add(prop, RDFS.ISDEFINEDBY, onto);
			owl.add(prop, RDF.TYPE, RDF.PROPERTY);
			owl.add(prop, RDF.TYPE, OWL.DATATYPEPROPERTY);
			
		}
		
		return owl;
	}
}
