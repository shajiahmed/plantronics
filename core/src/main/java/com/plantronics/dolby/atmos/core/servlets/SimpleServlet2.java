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


import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.rmi.ServerException;
import java.util.Iterator;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(service=Servlet.class,
           property={
                   Constants.SERVICE_DESCRIPTION + "=Simple Demo Servlet",
                   "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                   "sling.servlet.paths=/bin/test.txt",
                   "sling.servlet.extensions=" + "txt"
           })

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
//			final boolean isMultipart = org.apache.commons.fileupload.servlet.ServletFileUpload
//					.isMultipartContent(request);
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
				String path = "/content/dam/plantronics/Plantronics.xls";
				Resource dataResource = resourceResolver.getResource(path + "/jcr:content");
				InputStream is = dataResource.adaptTo(InputStream.class);

				log.info("GET THE STREAM22");
				String code = request.getParameter("code");
				// Save the uploaded file into the Adobe CQ DAM
				int excelValue = injectSpreadSheet(is, code );
				if (excelValue == 0)
					out.println(
							"Customer name " + code + " not found in excel" 
									);
				else
					out.println("Customer name " + code + " exists in excel");
			
		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Get data from the excel spreadsheet
	public int injectSpreadSheet(InputStream is, String code) {
		try {

			log.info("GET THE STREAM33");
			// Get the spreadsheet
			Workbook workbook = Workbook.getWorkbook(is);

			log.info("GET THE STREAMWorkbook");
			Sheet sheet = workbook.getSheet(0);
			
			//Sheet sh = workbook.getSheet("Duplicates");

			log.info("GET THE STREAMWorkbook");
			String firstName = "";
			String lastName = "";
			String address = "";
			String desc = "";

			log.info("GET THE STREAM44");
			for (int index = 0; index < 4; index++) {
				Cell a3 = sheet.getCell(0, index + 2);
				Cell b3 = sheet.getCell(1, index + 2);
				Cell c3 = sheet.getCell(2, index + 2);
				Cell d3 = sheet.getCell(3, index + 2);

				firstName = a3.getContents();
				lastName = b3.getContents();
				address = c3.getContents();
				desc = d3.getContents();
				if(firstName.equals(code)) {
					return 1;
				}
				log.debug("Continuing to find cust data ..." + firstName);

				// Store the excel data into the Adobe AEM JCR
				//injestCustData(firstName, lastName, address, desc);

			}

			return 0;

		}

		catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	// Stores customer data in the Adobe CQ JCR
	public int injestCustData(String firstName, String lastName, String address, String desc) {
		int num = 0;
		try {

			// Invoke the adaptTo method to create a Session used to create a QueryManager
			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			session = resourceResolver.adaptTo(Session.class);

			// Create a node that represents the root node
			Node root = session.getRootNode();

			// Get the content node in the JCR
			Node content = root.getNode("content");

			// Determine if the content/customer node exists
			Node customerRoot = null;
			int custRec = doesCustExist(content);

			log.info("*** Value of  custRec is ..." + custRec);
			// -1 means that content/customer does not exist
			if (custRec == -1) {
				// content/customer does not exist -- create it
				customerRoot = content.addNode("customerexcel");
			} else {
				// content/customer does exist -- retrieve it
				customerRoot = content.getNode("customerexcel");
			}

			int custId = custRec + 1; // assign a new id to the customer node

			// Store content from the client JSP in the JCR
			Node custNode = customerRoot.addNode("customer" + firstName + lastName + custId, "nt:unstructured");

			// make sure name of node is unique
			custNode.setProperty("id", custId);
			custNode.setProperty("firstName", firstName);
			custNode.setProperty("lastName", lastName);
			custNode.setProperty("address", address);
			custNode.setProperty("desc", desc);

			// Save the session changes and log out
			session.save();
			session.logout();
			return custId;
		}

		catch (Exception e) {
			log.error("RepositoryException: " + e);
		}
		return 0;
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
