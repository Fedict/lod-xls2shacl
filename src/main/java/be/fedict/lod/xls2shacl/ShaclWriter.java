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
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write model to SHACL validation file
 * 
 * @author Bart Hanssens
 */
public class ShaclWriter extends Writer {
	private final static Logger LOG = LoggerFactory.getLogger(ShaclWriter.class);
	
	private final ValueFactory FAC = SimpleValueFactory.getInstance();
	private final String PREFIX = "http://vocab.belgif.be/shacl";
	
	@Override
	protected String getOnto(String name) {
		return PREFIX + "/" + name.toLowerCase() + "#";
	}
		
	@Override
	public Model createTriples(String name, Model m) {
		Model shacl = getModel(name);
		
		Literal version = FAC.createLiteral("Draft " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		IRI onto = FAC.createIRI(getOnto(name));

		shacl.setNamespace("shbe-" + name.toLowerCase(), onto.toString());
		shacl.add(onto, RDF.TYPE, OWL.ONTOLOGY);
		shacl.add(onto, OWL.VERSIONINFO, version);
		for (String lang: LANGS) {	
			shacl.add(onto, RDFS.LABEL, FAC.createLiteral(name + " model", lang));
		}
		
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
				
				Value range = m.filter(prop, RDFS.RANGE, null).objects().stream()
																		.findFirst()
																		.orElse(null);
				if (range != null) {
					shacl.add(blank, SHACL.CLASS, range);
				} else {
					Value dt = m.filter(prop, OWL.DATATYPEPROPERTY, null).objects().stream()
																				.findFirst()
																				.orElse(null);
					if (dt != null) {
						shacl.add(blank, SHACL.DATATYPE, dt);
					}
				}
			}
		}
		
		return shacl;
	}
}
