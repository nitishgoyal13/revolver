/*
 * Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.dropwizard.revolver.persistence;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.IndexTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.revolver.aeroapike.AerospikeConnectionManager;
import io.dropwizard.revolver.base.core.RevolverCallbackRequest;
import io.dropwizard.revolver.base.core.RevolverCallbackResponse;
import io.dropwizard.revolver.base.core.RevolverRequestState;
import io.dropwizard.revolver.core.config.AerospikeMailBoxConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author phaneesh
 */
@Slf4j
public class AeroSpikePersistenceProvider implements PersistenceProvider {


    private interface BinNames {

        String MAILBOX_ID = "mailbox_id";
        String SERVICE = "service";
        String API = "api";
        String MODE = "mode";
        String METHOD = "method";
        String PATH = "path";
        String QUERY_PARAMS = "query_params";
        String CALLBACK_URI = "callback_uri";
        String REQUEST_HEADERS = "request_headers";
        String REQUEST_BODY = "request_body";
        String REQUEST_TIME = "request_time";
        String RESPONSE_HEADERS = "response_headers";
        String RESPONSE_BODY = "response_body";
        String RESPONSE_TIME = "response_time";
        String RESPONSE_STATUS_CODE = "response_status_code";
        String CREATED = "created";
        String UPDATED = "updated";
        String STATE = "state";

    }

    private final AerospikeMailBoxConfig mailBoxConfig;

    private final ObjectMapper objectMapper;

    private static String MAILBOX_SET_NAME = "mailbox_messages";

    public AeroSpikePersistenceProvider(AerospikeMailBoxConfig mailBoxConfig, final ObjectMapper objectMapper) {
        this.mailBoxConfig = mailBoxConfig;
        this.objectMapper = objectMapper;
        final IndexTask idxMailboxId = AerospikeConnectionManager.getClient().createIndex(null, mailBoxConfig.getNamespace(), MAILBOX_SET_NAME,
                "idx_mailbox_id", BinNames.MAILBOX_ID, IndexType.STRING);
        idxMailboxId.waitTillComplete();
        final IndexTask idxMessageState = AerospikeConnectionManager.getClient().createIndex(null, mailBoxConfig.getNamespace(), MAILBOX_SET_NAME,
                "idx_message_state", BinNames.STATE, IndexType.STRING);
        idxMailboxId.waitTillComplete();
        idxMessageState.waitTillComplete();
    }

    @Override
    public void saveRequest(String requestId, String mailboxId, RevolverCallbackRequest request) {
        final Key key = new Key(mailBoxConfig.getNamespace(), MAILBOX_SET_NAME, requestId);
        try {
            final Bin service = new Bin(BinNames.SERVICE, request.getService());
            final Bin api = new Bin(BinNames.API, request.getApi());
            final Bin mode = new Bin(BinNames.MODE, request.getMode());
            final Bin method = new Bin(BinNames.METHOD, request.getMethod());
            final Bin path = new Bin(BinNames.PATH, request.getPath());
            final Bin mailBoxId = new Bin(BinNames.MAILBOX_ID, mailboxId == null ? "NONE" : mailboxId);
            final Bin queryParams = new Bin(BinNames.QUERY_PARAMS, objectMapper.writeValueAsString(request.getQueryParams()));
            final Bin callbackUri = new Bin(BinNames.CALLBACK_URI, request.getCallbackUri());
            final Bin requestHeaders = new Bin(BinNames.REQUEST_HEADERS, objectMapper.writeValueAsString(request.getHeaders()));
            final Bin requestBody = new Bin(BinNames.REQUEST_BODY, request.getBody());
            final Bin requestTime = new Bin(BinNames.REQUEST_TIME, Instant.now().toEpochMilli());
            final Bin created = new Bin(BinNames.CREATED, Instant.now().toEpochMilli());
            final Bin updated = new Bin(BinNames.UPDATED, Instant.now().toEpochMilli());
            final Bin state = new Bin(BinNames.STATE, RevolverRequestState.RECEIVED.name());
            AerospikeConnectionManager.getClient().put(null, key,
                    service, api, mode, method, path, mailBoxId, queryParams, callbackUri, requestHeaders, requestBody, requestTime,
                    created, updated, state);
        } catch (JsonProcessingException e) {
            log.warn("Error encoding request", e);
        }
    }

    @Override
    public void setRequestState(String requestId, RevolverRequestState state) {
        final Key key = new Key(mailBoxConfig.getNamespace(), MAILBOX_SET_NAME, requestId);
        final Bin binState = new Bin(BinNames.STATE, state.name());
        final Bin updated = new Bin(BinNames.UPDATED, Instant.now().toEpochMilli());
        AerospikeConnectionManager.getClient().operate(null, key,
                Operation.put(binState), Operation.put(updated));
    }

    @Override
    public void saveResponse(String requestId, RevolverCallbackResponse response) {
        final Key key = new Key(mailBoxConfig.getNamespace(), MAILBOX_SET_NAME, requestId);
        final Bin state = new Bin(BinNames.STATE, RevolverRequestState.RESPONDED.name());
        try {
            final Bin responseHeaders = new Bin(BinNames.RESPONSE_HEADERS, objectMapper.writeValueAsString(response.getHeaders()));
            final Bin responseBody = new Bin(BinNames.RESPONSE_BODY, response.getBody());
            final Bin responseStatusCode = new Bin(BinNames.RESPONSE_STATUS_CODE, response.getStatusCode());
            final Bin responseTime = new Bin(BinNames.RESPONSE_TIME, Instant.now().toEpochMilli());
            final Bin updated = new Bin(BinNames.UPDATED, Instant.now().toEpochMilli());
            AerospikeConnectionManager.getClient().operate(null, key,
                    Operation.put(state),
                    Operation.put(responseHeaders),
                    Operation.put(responseBody),
                    Operation.put(responseStatusCode),
                    Operation.put(responseTime),
                    Operation.put(updated));
        } catch (JsonProcessingException e) {
            log.warn("Error encoding response headers", e);
        }
    }

    @Override
    public RevolverRequestState requestState(String requestId) {
        final Key key = new Key(mailBoxConfig.getNamespace(), MAILBOX_SET_NAME, requestId);
        final Record record = AerospikeConnectionManager.getClient().get(null, key, BinNames.STATE);
        if(record == null) {
            return RevolverRequestState.UNKNOWN;
        }
        return RevolverRequestState.valueOf(record.getString("state"));
    }

    @Override
    public RevolverCallbackResponse response(String requestId) {
        final Key key = new Key(mailBoxConfig.getNamespace(), MAILBOX_SET_NAME, requestId);
        final Record record = AerospikeConnectionManager.getClient().get(null, key);
        if(record == null) {
            return null;
        }
        return recordToResponse(record);
    }

    @Override
    public List<RevolverCallbackResponse> responses(String mailboxId) {
        final Statement statement = new Statement();
        statement.setNamespace(mailBoxConfig.getNamespace());
        statement.setSetName(MAILBOX_SET_NAME);
        statement.setIndexName("idx_mailbox_id");
        statement.setFilters(Filter.equal(BinNames.MAILBOX_ID, mailboxId));
        List<RevolverCallbackResponse> responses = new ArrayList<>();
        try (RecordSet records = AerospikeConnectionManager.getClient().query(null, statement)) {
            while (records.next()) {
                responses.add(recordToResponse(records.getRecord()));
            }
        }
        return responses;
    }

    @Override
    public RevolverCallbackRequest request(String requestId) {
        final Key key = new Key(mailBoxConfig.getNamespace(), MAILBOX_SET_NAME, requestId);
        final Record record = AerospikeConnectionManager.getClient().get(null, key);
        if(record == null) {
            return null;
        }
        return recordToRequest(record);
    }

    @Override
    public List<RevolverCallbackRequest> requests(String mailboxId) {
        final Statement statement = new Statement();
        statement.setNamespace(mailBoxConfig.getNamespace());
        statement.setSetName(MAILBOX_SET_NAME);
        statement.setIndexName("idx_mailbox_id");
        statement.setFilters(Filter.equal(BinNames.MAILBOX_ID, mailboxId));
        List<RevolverCallbackRequest> requests = new ArrayList<>();
        try (RecordSet records = AerospikeConnectionManager.getClient().query(null, statement)) {
            while (records.next()) {
                requests.add(recordToRequest(records.getRecord()));
            }
        }
        return requests;
    }

    private RevolverCallbackRequest recordToRequest(Record record) {
        Map<String, List<String>> headers = new HashMap<>();
        Map<String, List<String>> queryParams = new HashMap<>();
        try {
            headers = objectMapper.readValue(record.getString(BinNames.REQUEST_HEADERS), new TypeReference<Map<String, List<String>>>(){});
            queryParams = objectMapper.readValue(record.getString(BinNames.QUERY_PARAMS), new TypeReference<Map<String, List<String>>>(){});
        } catch (IOException e) {
            log.warn("Error decoding response", e);
        }
        return RevolverCallbackRequest.builder()
                .headers(headers)
                .api(record.getString(BinNames.API))
                .callbackUri(record.getString(BinNames.CALLBACK_URI))
                .body(record.getValue(BinNames.REQUEST_BODY) == null ? null : (byte[])record.getValue(BinNames.REQUEST_BODY))
                .method(record.getString(BinNames.METHOD))
                .mode(record.getString(BinNames.MODE))
                .path(record.getString(BinNames.PATH))
                .queryParams(queryParams)
                .service(record.getString(BinNames.SERVICE))
                .build();
    }

    private RevolverCallbackResponse recordToResponse(Record record) {
        Map<String, List<String>> headers = new HashMap<>();
        try {
            headers = objectMapper.readValue(record.getString(BinNames.RESPONSE_HEADERS), new TypeReference<Map<String, List<String>>>(){});
        } catch (IOException e) {
            log.warn("Error decoding response headers", e);
        }
        return RevolverCallbackResponse.builder()
                .body((byte[])record.getValue(BinNames.RESPONSE_BODY))
                .statusCode(record.getInt(BinNames.RESPONSE_STATUS_CODE))
                .headers(headers)
                .build();
    }
}