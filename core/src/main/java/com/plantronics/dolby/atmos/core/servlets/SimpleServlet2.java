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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.Servlet;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;

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
	private static final String BASE_PATH = "/content/dolbyatmos/";
	private static final String ACTIVATION_PATH = BASE_PATH + "activation";

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
			String email = request.getParameter("email");
			// Save the uploaded file into the Adobe CQ DAM
			int excelValue = injectSpreadSheet(is, code);
			if (excelValue == 0)
				out.println("Customer name " + code + " not found in excel");
			else {
				
				String newcode = injestCustData(code, email);
				out.println("Customer name " + code + " exists in excel, new code is " + newcode);
			}
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

	// Get data from the excel spreadsheet
	public String getNewCode(String lastcodeUsed) {

		BufferedReader br = null;
		String newCode = "";

		try {
			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);

			String path2 = "/content/dam/plantronics/newcodesonly.csv";
			Resource newcodeRsc = resourceResolver.getResource(path2 + "/jcr:content");
			InputStream is = newcodeRsc.adaptTo(InputStream.class);

			br = new BufferedReader(new InputStreamReader(is));

			String line = null;

			while ((line = br.readLine()) != null) {
				if (lastcodeUsed.equals("")) {
					return line;
				}
				if (line.equalsIgnoreCase(lastcodeUsed)) {
					newCode = br.readLine();
					return newCode;
				}

			}

			log.info("Line entered : " + line);
			return newCode;
		} catch (Exception e) {
			log.error(" error occured {}", e);
		}
		return newCode;
	}

	// Stores customer data in the Adobe CQ JCR
	public String injestCustData(String oldCode, String email) {
		int num = 0;
		String newCode = "";
		try {

			// Invoke the adaptTo method to create a Session used to create a QueryManager
			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			session = resourceResolver.adaptTo(Session.class);

			Map<String, Object> props = new HashMap<>();
			props.put(JcrConstants.JCR_PRIMARYTYPE, "sling:OrderedFolder");
			Resource folder = ResourceUtil.getOrCreateResource(resourceResolver, BASE_PATH, props, null, true);
			Resource activationRsc = ResourceUtil.getOrCreateResource(resourceResolver, ACTIVATION_PATH,
					JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, false);
			ModifiableValueMap activationProperties = activationRsc.adaptTo(ModifiableValueMap.class);
			// JsonArray codesArray;
			String[] codesArray;
			List<String> list = new ArrayList<String>();
			if (activationProperties.containsKey("codes")) {
				codesArray = activationProperties.get("codes", String[].class);
				list = Arrays.asList(codesArray);
				JsonObject crxcode = getNewCodeforOldCode(list, oldCode);
				String lastcode = "";
				if (crxcode != null) {
					if(crxcode.has("lastcodeused"))
						lastcode = crxcode.get("lastcodeused").getAsString();
					lastcode = lastcode == null ? "" : lastcode;
					if (lastcode.equals("") && crxcode.has("newCode")) {
						newCode = crxcode.get("newCode").getAsString();
						return newCode;
					} else {
						newCode = getNewCode(lastcode);
					}
				}
			} else {
				list = new ArrayList<String>();
				// codesArray = new JsonArray();
				newCode = getNewCode("");
			}
			JsonObject code = new JsonObject();
			code.addProperty("oldCode", oldCode);
			code.addProperty("newCode", newCode);
			code.addProperty("email", email);
			Date date = new Date();
			code.addProperty("date", date.toString());
			String codeStr = code.toString();
			// codesArray.add(code);
			List<String> l = new ArrayList<String>(list);
			l.add(codeStr);
			activationProperties.remove("codes");
			activationProperties.put("codes", l.toArray());

			resourceResolver.commit();
			return newCode;
		}

		catch (Exception e) {
			log.error("RepositoryException: {} ", e);
			return "";
		}

	}

	public JsonObject getNewCodeforOldCode(List<String> crxCodes, String codeRequested) {
		JsonObject jsobj;
		try {
			for (String code : crxCodes) {
				JsonParser parser = new JsonParser();
				jsobj = parser.parse(code).getAsJsonObject();
				String oldcode = jsobj.get("oldCode").getAsString();
				String newcode = jsobj.get("newCode").getAsString();
				if (oldcode.equals(codeRequested)) {
					return jsobj;
				}
				jsobj = new JsonObject();
				jsobj.addProperty("lastcodeused", newcode);
				return jsobj;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
