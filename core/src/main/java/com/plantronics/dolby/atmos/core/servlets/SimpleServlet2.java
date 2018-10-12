/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.plantronics.dolby.atmos.core.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.rmi.ServerException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=Simple Demo Servlet",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/test.txt",
		"sling.servlet.extensions=" + "txt" })

public class SimpleServlet2 extends SlingSafeMethodsServlet {

	private static final long serialVersionUid = 1L;
	@Reference
	private ResourceResolverFactory resolverFactory;
	private Session session;

	/** Default log. */
	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {

		try {
			// final boolean isMultipart =
			// org.apache.commons.fileupload.servlet.ServletFileUpload
			// .isMultipartContent(request);
			PrintWriter out = null;

			log.info("GET THE STREAM");

			out = response.getWriter();
			StringBuffer sb = new StringBuffer();

			final java.util.Map<String, org.apache.sling.api.request.RequestParameter[]> params = request
					.getRequestParameterMap();
			for (final java.util.Map.Entry<String, org.apache.sling.api.request.RequestParameter[]> pairs : params
					.entrySet()) {
				final String k = pairs.getKey();
				final org.apache.sling.api.request.RequestParameter[] pArr = pairs.getValue();
				final org.apache.sling.api.request.RequestParameter param = pArr[0];

				sb.append(param + "=" + pArr + ":");
			}

			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			String path = "/content/dam/plantronics/dupcodes.csv";
			Resource dataResource = resourceResolver.getResource(path + "/jcr:content");
			InputStream is = dataResource.adaptTo(InputStream.class);

			// FileInputStream is = dataResource.adaptTo(FileInputStream.class);
			log.info("GET THE STREAM22");
			String code = request.getParameter("code");
			// Save the uploaded file into the Adobe CQ DAM
			int excelValue = injectSpreadSheet(is, code);
			if (excelValue == 0)
				out.println("Customer name " + code + " not found in excel");
			else
				out.println("Customer name " + code + " exists in excel");

		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Get data from the excel spreadsheet
	public int injectSpreadSheet(InputStream is, String code) {

		BufferedReader br = null;

		try {

			br = new BufferedReader(new InputStreamReader(is));

			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.equalsIgnoreCase(code)) {
					return 1;
				}

			}

			log.info("Line entered : " + line);
			return 0;
		} catch (Exception e) {
			log.error(" error occured {}", e);
		}
		return -1;
	}

	public  void replaceStringInFile(File dir, String fileName, String match, String replacingString){
		//Open file in read-write mode
		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile("contents.txt","rw");
		

		String bookCode ="200";
		String bookName = "Mastering JSP ";
		String bookMas;

		String line=raf.readLine();

		while( line != null){

		long filePos=raf.getFilePointer(); //change here

		String s=line.substring(0,3); 

		if (s.equals(bookCode)){ 
		bookMas=bookCode+" "+bookName;
		raf.seek(filePos);
		raf.writeBytes(bookMas);
		}
		line=raf.readLine();
		}
		raf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}	
	


	/*
	 * Determines if the content/customer node exists This method returns these
	 * values: -1 - if customer does not exist 0 - if content/customer node exists;
	 * however, contains no children number - the number of children that the
	 * content/customer node contains
	 */
	private int doesCustExist(Node content) {
		try {
			int index = 0;
			int childRecs = 0;

			java.lang.Iterable<Node> custNode = JcrUtils.getChildNodes(content, "customerexcel");
			Iterator it = custNode.iterator();

			// only going to be 1 content/customer node if it exists
			if (it.hasNext()) {
				// Count the number of child nodes in content/customer
				Node customerRoot = content.getNode("customerexcel");
				Iterable itCust = JcrUtils.getChildNodes(customerRoot);
				Iterator childNodeIt = itCust.iterator();

				// Count the number of customer child nodes
				while (childNodeIt.hasNext()) {
					childRecs++;
					childNodeIt.next();
				}
				return childRecs;
			} else
				return -1; // content/customer does not exist
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
