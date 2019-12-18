package org.opencb.opencga.core.rest;

import org.opencb.commons.datastore.core.Event;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.opencga.core.results.OpenCGAResult;

import java.util.ArrayList;
import java.util.List;

public class RestResponse<T> {
    private String apiVersion;
    private int time;

    private List<Event> events;

    private ObjectMap params;
    private List<OpenCGAResult<T>> responses;

    public RestResponse() {
        this("", -1, new ArrayList<>(), new ObjectMap(), new ArrayList<>());
    }

    public RestResponse(ObjectMap params, List<OpenCGAResult<T>> responses) {
        this("", -1, new ArrayList<>(), params, responses);
    }

    public RestResponse(String apiVersion, int time, List<Event> events, ObjectMap params, List<OpenCGAResult<T>> responses) {
        this.apiVersion = apiVersion;
        this.time = time;
        this.events = events;
        this.params = params;
        this.responses = responses;
    }

    /**
     * Fetch the m-result of the first response.
     * @param m Position of the result from the array of results.
     * @return the m-result of the first response.
     */
    public T result(int m) {
        return result(m, 0);
    }

    /**
     * Fetch the m-result of the n-response.
     * @param m Position of the result from the array of results.
     * @param n Position of the response from the array of responses.
     * @return the m-result of the n-response.
     */
    public T result(int m, int n) {
        return this.responses.get(n).getResults().get(m);
    }

    /**
     * Fetch the list of results of the m-response.
     * @param m Position of the response from the array of responses.
     * @return the list of results of the m-response.
     */
    public List<T> results(int m) {
        return this.responses.get(m).getResults();
    }

    /**
     * Fetch the list of responses.
     * @return the list of responses.
     */
    public List<OpenCGAResult<T>> responses() {
        return this.responses;
    }

    /**
     * Fetch the OpenCGAResult of the m-response.
     * @param m Position of the response from the array of responses.
     * @return the OpenCGAResult of the m-response.
     */
    public OpenCGAResult<T> response(int m) {
        return this.responses.get(m);
    }

    /**
     * This method just returns the first OpenCGAResult of response, or null if response is null or empty.
     * @return the first OpenCGAResult in the response
     */
    public OpenCGAResult<T> first() {
        if (responses != null && responses.size() > 0) {
            return responses.get(0);
        }
        return null;
    }

    /**
     * This method returns the first result from the first OpenCGAResult of response, equivalent to response.get(0).getResult.get(0).
     * @return T value if exists, null otherwise
     */
    public T firstResult() {
        if (responses != null && responses.size() > 0) {
            return responses.get(0).first();
        }
        return null;
    }

    public int allResultsSize() {
        int totalSize = 0;
        if (responses != null && responses.size() > 0) {
            for (OpenCGAResult<T> dataResult : responses) {
                totalSize += dataResult.getResults().size();
            }
        }
        return totalSize;
    }

    /**
     * This method flats the two levels (RestResponse and OpenCGAResult) into a single list of T.
     * @return a single list with all the results, or null if no response exists
     */
    public List<T> allResults() {
        List<T> results = null;
        if (responses != null && responses.size() > 0) {
            // We first calculate the total size needed
            int totalSize = allResultsSize();

            // We init the list and copy data
            results = new ArrayList<>(totalSize);
            for (OpenCGAResult<T> dataResult : responses) {
                results.addAll(dataResult.getResults());
            }
        }
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RestResponse{");
        sb.append("apiVersion='").append(apiVersion).append('\'');
        sb.append(", time=").append(time);
        sb.append(", events=").append(events);
        sb.append(", params=").append(params);
        sb.append(", responses=").append(responses);
        sb.append('}');
        return sb.toString();
    }


    public String getApiVersion() {
        return apiVersion;
    }

    public RestResponse<T> setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public int getTime() {
        return time;
    }

    public RestResponse<T> setTime(int time) {
        this.time = time;
        return this;
    }

    public List<Event> getEvents() {
        return events;
    }

    public RestResponse<T> setEvents(List<Event> events) {
        this.events = events;
        return this;
    }

    public ObjectMap getParams() {
        return params;
    }

    public RestResponse<T> setParams(ObjectMap params) {
        this.params = params;
        return this;
    }

    public List<OpenCGAResult<T>> getResponses() {
        return responses;
    }

    public RestResponse<T> setResponses(List<OpenCGAResult<T>> responses) {
        this.responses = responses;
        return this;
    }

}
