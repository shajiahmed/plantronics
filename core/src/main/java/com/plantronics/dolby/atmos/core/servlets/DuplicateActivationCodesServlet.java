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
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
//import com.plantronics.dolby.core.configuration.DuplicateCodeConfigService;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(service = Servlet.class, immediate=true, property = { Constants.SERVICE_DESCRIPTION + "=Simple Demo Servlet",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=/bin/dupcodes.txt",
		"sling.servlet.extensions=" + "txt" })

public class DuplicateActivationCodesServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUid = 1L;
	@Reference
	private ResourceResolverFactory resolverFactory;
	private Session session;
	public static final String SERVICE_USER_NAME = "datawrite";

	ResourceResolver resourceResolver;
	//@Reference
	//private DuplicateCodeConfigService config;
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
			
		    resourceResolver = getServiceUser();
			//resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			log.info("GET THE STREAM");

			out = response.getWriter();
			StringBuffer sb = new StringBuffer();

		

		

			// FileInputStream is = dataResource.adaptTo(FileInputStream.class);
			log.info("GET THE STREAM22");
			String code = request.getParameter("code");
			String email = request.getParameter("email");

			JsonObject crxcode = getNewCodeforOldCode(code);
			String newCode = "";
			String date = "";
			if (crxcode != null && !crxcode.entrySet().isEmpty()) {

				if (crxcode.has("newCode")) {
					newCode = crxcode.get("newCode").getAsString();
					date = crxcode.get("date").getAsString();
					out.println("You have already created your new code " + newCode + " on " + date);
				}
			} else {
				// Save the uploaded file into the Adobe CQ DAM
				int excelValue = findDuplicateCode( code);
				if (excelValue == 0)
					out.println("This code " + code + " is not a duplicated one");
				else {

					String newcode = persistCRXData(code, email);
					out.println(
							"Your new code for  " + code + " create, use this code to enter on xbox site " + newcode);
				}
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Get data from the excel spreadsheet
	public int findDuplicateCode( String code) {

		BufferedReader br = null;

		try {
			//String path = config.getNewcodesFile();
			String path = "/content/dam/plantronics/dupcodes.csv";
			Resource dataResource = resourceResolver.getResource(path + "/jcr:content");
			InputStream is = dataResource.adaptTo(InputStream.class);

			br = new BufferedReader(new InputStreamReader(is));

			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.equalsIgnoreCase(code)) {
					is.close();
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

			//String path2 = config.getNewcodesFile();
			String path2 = "/content/dam/plantronics/newcodes.csv";
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
					is.close();
					return newCode;
				}

			}
			is.close();
			log.info("Line entered : " + line);
			return newCode;
		} catch (Exception e) {
			log.error(" error occured {}", e);
		}
		return newCode;
	}

	// Stores customer data in the Adobe CQ JCR
	public String persistCRXData(String oldCode, String email) {
		int num = 0;
		String newCode = "";
		try {

			// Invoke the adaptTo method to create a Session used to create a QueryManager

			session = resourceResolver.adaptTo(Session.class);

			Map<String, Object> props = new HashMap<>();
			props.put(JcrConstants.JCR_PRIMARYTYPE, "sling:OrderedFolder");
			String basepath = "/content/dolbyatmos/" ;//config.getBasepath();
			String activationpath = basepath + "activationcodes"; //config.getBasepath() + config.getActivationcodesNode();
			Resource folder = ResourceUtil.getOrCreateResource(resourceResolver, basepath, props, null,
					true);
			Resource activationRsc = ResourceUtil.getOrCreateResource(resourceResolver,
					activationpath, JcrConstants.NT_UNSTRUCTURED,
					JcrConstants.NT_UNSTRUCTURED, false);
			ModifiableValueMap activationProperties = activationRsc.adaptTo(ModifiableValueMap.class);
			// JsonArray codesArray;
			String[] codesArray;
			List<String> list = new ArrayList<String>();
			String lastnewcode = activationProperties.get("lastnewcode", String.class);
			lastnewcode = (lastnewcode == null) ? "" : lastnewcode;
			String firstLetter = String.valueOf(String.valueOf(oldCode.charAt(0)));
			if (activationProperties.containsKey(firstLetter)) {
				codesArray = activationProperties.get(firstLetter, String[].class);
				list = Arrays.asList(codesArray);
				JsonObject crxcode = getNewCodeforOldCode(oldCode);
				String lastcode = "";
				if (crxcode != null) {

					if (crxcode.has("newCode")) {
						newCode = crxcode.get("newCode").getAsString();
						return newCode;
					} else {
						newCode = getNewCode(lastnewcode);
					}
				}
			} else {
				list = new ArrayList<String>();
				// codesArray = new JsonArray();
				newCode = getNewCode(lastnewcode);
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

			activationProperties.remove(firstLetter);
			activationProperties.put(firstLetter, l.toArray());
			activationProperties.put("lastnewcode", newCode);
			resourceResolver.commit();
			return newCode;

		} catch (Exception e) {
			log.error("RepositoryException: {} ", e);
			return "";
		}

	}

	public JsonObject getNewCodeforOldCode(String codeRequested) {
		JsonObject jsobj=null;
		List<String> crxCodes;
		try {
			String basepath = "/content/dolbyatmos/" ;//config.getBasepath();
			String activationpath = basepath + "activationcodes"; //config.getBasepath() + config.getActivationcodesNode();
		
			Resource activationRsc = resourceResolver
					.getResource(activationpath);
			String firstLetter = String.valueOf(String.valueOf(codeRequested.charAt(0)));
			ValueMap activationProperties = activationRsc.adaptTo(ValueMap.class);
			String[] codesArray;

			codesArray = activationProperties.get(firstLetter, String[].class);
			crxCodes = Arrays.asList(codesArray);
			String lastnewcode = activationProperties.get("lastnewcode", String.class);
			lastnewcode = (lastnewcode == null) ? "" : lastnewcode;
			firstLetter = String.valueOf(String.valueOf(codeRequested.charAt(0)));
			for (String code : crxCodes) {
				JsonParser parser = new JsonParser();
				jsobj = parser.parse(code).getAsJsonObject();
				String oldcode = jsobj.get("oldCode").getAsString();
				String newcode = jsobj.get("newCode").getAsString();
				if (oldcode.equals(codeRequested)) {
					return jsobj;
				}
				
			}
			

			return jsobj;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private ResourceResolver getServiceUser() throws LoginException {
		return resolverFactory.getServiceResourceResolver(new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;
			{
				put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER_NAME);
			}
		});
	}


}
