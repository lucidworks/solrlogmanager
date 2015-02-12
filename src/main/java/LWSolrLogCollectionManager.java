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

import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * LWSolrLogCollectionManager
 *
 * @version 1.1
 * @since 2013
 */

public class LWSolrLogCollectionManager {
  private String fieldsPath = "";
  private String addNewDocPath = "";

  protected SolrServer solr;
  protected boolean forceCommit = false;
  protected String idField;
  protected String collection;
  protected Collection<SolrInputDocument> buffer;
  protected boolean commitOnClose;
  protected int maxRetries = 3;
  protected int sleep = 5000;
  protected int bufferSize;
  protected ModifiableSolrParams params = null;

  public void init(String zkHost, String idField, String collection, boolean forceCommit, SolrParams defaultParams, int queueSize) {
    solr = new CloudSolrServer(zkHost);
    ((CloudSolrServer) solr).setDefaultCollection(collection);
    sharedInit(idField, collection, forceCommit, defaultParams, queueSize);
  }

  private void sharedInit(String idField, String collection, boolean forcecommit, SolrParams defaultParams, int queueSize) {
    this.params = new ModifiableSolrParams(defaultParams);
    this.idField = idField != null ? idField : "id";
    this.collection = collection;
    this.forceCommit = forcecommit;
    bufferSize = queueSize;
    if (bufferSize > 0) {
      buffer = new ArrayList<SolrInputDocument>(bufferSize);
    } else {
      buffer = new ArrayList<SolrInputDocument>();
    }
  }

  public void init(String solrURL, String idField, String collection, int queueSize, int threadCount, boolean forceCommit,
                   SolrParams defaultParams) {

    //If the solr server has not already been set, then set it from configs.
    sharedInit(idField, collection, forceCommit, defaultParams, queueSize);

    if (!solrURL.endsWith("/")) {
      solrURL += "/";
    }
    solrURL += collection;

    solr = new ConcurrentUpdateSolrServer(solrURL, queueSize, threadCount);

    // Note the Solr documentation examples do not include the collection.  But if
    // if collection is left out then new records are not added to the index because
    // the fields are not found even though they do somehow appear in the managed-schema file.
    //fieldsPath = "http://" + host + ":" + port + "/solr/" + collection + "/schema/fields";
    //addNewDocPath = "http://" + host + ":" + port + "/solr/" + collection + update;
  }

  /**
   * Add new field to Solr schema if field does not already exist.
   *
   * @param key the field name
   * @param val the field creation string.
   * @throws Exception
   */
  public void createSchemaField(String key, String val) throws Exception {

    SolrParams params = new ModifiableSolrParams();
    //TODO: add the fields
    SolrRequest request = new QueryRequest(params);
    //request.setMethod(SolrRequest.METHOD.POST);
    request.setPath(fieldsPath);

    solr.request(request);
  }


  public void addSolrDocument(HashMap<String, String> event) throws Exception {
    SolrInputDocument sid = new SolrInputDocument();
    String id = "";

    // Check for proper encoding and create field's for tags.
    for (Map.Entry<String, String> entry : event.entrySet()) {
      String key = entry.getKey().toLowerCase().trim();
      if ("tags".equals(key)) {
        String[] tags = entry.getValue().split(",");
        for (String tag : tags) {
          sid.addField("tags", tag);
        }
      } else {
        sid.addField(key, entry.getValue());
      }
    }
    id = id.isEmpty() ? genId() : id;
    sid.setField(idField, id);

    try {
      buffer.add(sid);
      if (buffer.size() >= bufferSize) {
        sendBuffer();
      }
    } catch (SolrServerException e) {
      maybeRetry(e);
    }
  }

  protected void maybeRetry(SolrServerException e) throws IOException {
    Throwable rootCause = e.getRootCause();
    if (rootCause instanceof ConnectException || rootCause instanceof ConnectTimeoutException ||
            rootCause instanceof NoHttpResponseException ||
            rootCause instanceof SocketException
            ) {
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e1) {
      }
      boolean sent = false;
      for (int i = 0; i < maxRetries; i++) {
        try {
          sendBuffer();
          //success
          sent = true;
          break;

        } catch (SolrServerException e1) {
          try {
            Thread.sleep(i * sleep);
          } catch (InterruptedException e2) {
          }
        }
      }
      if (sent == false) {
        throw makeIOException(e);
      }
    } else {
      throw makeIOException(e);
    }
  }

  public void commit() throws IOException, SolrServerException {
    solr.commit(false, false);
  }

  protected void sendBuffer() throws SolrServerException, IOException {
    //log.info("Sending {} documents", buffer.size());
    //flush the buffer
    if (params == null) {
      solr.add(buffer);
    } else {
      UpdateRequest req = new UpdateRequest();
      req.setParams(params);
      req.add(buffer);
      solr.request(req);
    }
    buffer.clear(); //this shouldn't get hit if there are exceptions, so it should be fine here as part of our retry logic
  }

  public static IOException makeIOException(SolrServerException e) {
    final IOException ioe = new IOException();
    ioe.initCause(e);
    return ioe;
  }

  public void close() throws IOException {
    try {
      if (buffer.isEmpty() == false) {
        try {
          sendBuffer();
          if (solr instanceof ConcurrentUpdateSolrServer){
            ((ConcurrentUpdateSolrServer)solr).blockUntilFinished();
          }
        } catch (SolrServerException e) {
          maybeRetry(e);
        }
      }
      if (commitOnClose == true) {
        solr.commit(false, false);
      }
      solr.shutdown();
    } catch (final SolrServerException e) {
      throw makeIOException(e);
    }
  }

  protected String genId() {
    return UUID.randomUUID().toString();
  }
}

