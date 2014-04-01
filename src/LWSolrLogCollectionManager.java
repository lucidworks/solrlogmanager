/*
 * Licensed to LucidWorks under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. LucidWorks licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * LWSolrLogCollectionManager
 * @author  MH
 * @version 1.1 
 * @since 2013 
*/

public class LWSolrLogCollectionManager extends CollectionManager{
	private String fieldsPath = "";
	private String addNewDocPath = "";
	
	private enum SOLR_RSP {
		NOCONTENT(204), NOTFOUND(404), DOCUMENTADDED(200), FIELDCREATED(200), COLLECTIONCREATED(201);

		private final int id;
		SOLR_RSP(int id) { this.id = id; }
		public int getValue() { return id; }
	}

	public void init(String host, int pt, String collection, boolean forceCommit) {
		String port = String.valueOf(pt);
		String update = "/update";
		
		if (forceCommit)
			update = "/update/?commit=true";
		
		// Note the Solr documentation examples do not include the collection.  But if
		// if collection is left out then new records are not added to the index because 
		// the fields are not found even though they do somehow appear in the managed-schema file.
		fieldsPath = "http://" + host + ":" + port + "/solr/" + collection + "/schema/fields";
		addNewDocPath = "http://" + host + ":" + port + "/solr/" + collection + update;
	}

	/**
	 * Add new field to Solr schema if field does not already exist.
	 * @param key the field name
	 * @param val the field creation string.
	 * @throws Exception
	 */
	public void createSchemaField(String key, String val) throws Exception {
		HttpURLConnection conn = null;
		try {
			// Solr will throw an exception if you try to explicitly create a field that matches a configured dynamic field pattern.
			// By using 'includeDynamic=true' fields whose name matches a configured dynamic field pattern will be reported by the 
			// server as existent even if they do not yet exist literally.  Then we skip creation here and let the field be created 
			// and typed dynamically when the document is saved in another method. 
			URL url = new URL(fieldsPath + "/" + key + "?includeDynamic=true");
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			// Create required field if it does not already exist
			int respCode = conn.getResponseCode();
			if (respCode == SOLR_RSP.NOTFOUND.getValue()) {
				//LOG.info("Attempting to create schema field - " + url.getPath() + ",  CreateString = " + val);
				
				conn.disconnect();
				url = new URL(fieldsPath);
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				OutputStream os = conn.getOutputStream();
				
				os.write(val.getBytes());
				os.flush();
				respCode = conn.getResponseCode();
				if (respCode != SOLR_RSP.FIELDCREATED.getValue()) {
					throw new RuntimeException("Failed to create field <" + key + "> - \n\n" + conn.getResponseMessage());
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != conn)
				conn.disconnect();
		}
	}

	public void flushDocs(String documents) throws Exception {
		HttpURLConnection conn = null;

		try {
			URL url = new URL(addNewDocPath);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/xml");
			OutputStream os = conn.getOutputStream();
			
			// Wrap collection of documents 
			String s = "<add>" + documents + "</add>";
			os.write(s.getBytes());
			os.flush();
			int respCode = conn.getResponseCode();
			if (respCode != SOLR_RSP.DOCUMENTADDED.getValue()) {
				throw new RuntimeException("Failed to add documents (possibly need to escape/transform/remove illegal character[s] in data prior to submission) - " + s + " - \n\n" + conn.getResponseMessage());
			}
//			LOG.info("Flushed docs = " + s.toString());
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != conn)
				conn.disconnect();
		}
	}
	
	public String createSolrDocument(HashMap <String, String> event) throws Exception {
		String id = "";
		StringBuffer s = new StringBuffer();
		s.append("<doc>");

		// Check for proper encoding and create field's for tags.
		for (Map.Entry<String, String> entry : event.entrySet()) {
			String key = entry.getKey().toLowerCase().trim();
			
			if ("id".equals(key)) {
				id = checkURLEncode((String)entry.getValue());
			} else if ("tags".equals(key)) {
				String[] tags = ((String)entry.getValue()).split(",");
				for (int i = 0; i < tags.length; i++)
					s.append("<field name=\"tags\">" + checkURLEncode(tags[i]) + "</field>");
			} else {
				s.append("<field name=\"" + checkURLEncode(key) + "\">" + checkURLEncode((String)entry.getValue()) + "</field>");
			}
		}
		id = id.isEmpty() ? genId() : id;
		s.append("<field name=\"id\">" + id + "</field></doc>");
		
//		LOG.info("Added doc = " + s.toString());
		return s.toString();
	}
}

