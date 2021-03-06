/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *      Mathieu Debove (https://github.com/debovema) - update existing files
 *******************************************************************************/
package org.springsource.ide.p2.composite.site.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A simple mojo that generates a composite update site from a list of urls of
 * other update sites.
 */
@Mojo(name = "update", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
public class P2CompositeSiteUpdaterMojo extends P2CompositeSiteCreatorMojo {

	@Parameter(required = true)
	String site;

	@Parameter(required = true)
	String childToAdd;

	private File compositeArtifacts;

	private File remoteFiles;

	public void execute() throws MojoExecutionException {
		try {
			init();

			getRemoteFiles();

			sites = getExistingSites();
			if (!sites.contains(childToAdd)) {
				sites.add(childToAdd);
			}

			generate();
		} catch (Exception e) {
			throw new MojoExecutionException("Problem executing: " + this, e);
		}
	}

	private List<String> getExistingSites() throws ParserConfigurationException, SAXException, IOException {
		List<String> result = new ArrayList<String>();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(compositeArtifacts);

		NodeList children = document.getDocumentElement().getElementsByTagName("children");
		NodeList childrenList = children.item(0).getChildNodes();

		for (int i = 0; i < childrenList.getLength(); i++) {
			Node child = childrenList.item(i);
			if ("child".equals(child.getNodeName())) {
				result.add(child.getAttributes().getNamedItem("location").getNodeValue());
			}
		}
		return result;
	}

	private File getRemoteFile(String fileName) throws IOException {
		URL remoteCompositeArtifactsURL = new URL(site + "/" + fileName);
		ReadableByteChannel rbc = Channels.newChannel(remoteCompositeArtifactsURL.openStream());
		File result = new File(remoteFiles, fileName);
		try (FileOutputStream fos = new FileOutputStream(result)) {
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		}
		return result;
	}

	private void getRemoteFiles() throws IOException {
		remoteFiles = new File(target, "remotes");
		remoteFiles.mkdirs();

		compositeArtifacts = getRemoteFile(compositeArtifactsXml);
		File compositeContent = getRemoteFile(compositeContentXml);

		getLog().info("Remote site is '" + site + "'");
		getLog().info("");
		getLog().info("Retrieved " + compositeArtifactsXml + " to '" + compositeArtifacts + "'");
		getLog().info("Retrieved " + compositeContentXml + " to '" + compositeContent + "'");
	}

}