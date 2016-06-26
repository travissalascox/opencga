/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.server.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.opencb.commons.datastore.core.QueryResult;

import org.opencb.opencga.catalog.db.api.CatalogSampleDBAdaptor;
import org.opencb.opencga.catalog.models.Variable;
import org.opencb.opencga.catalog.models.VariableSet;
import org.opencb.opencga.core.exception.VersionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by jacobo on 16/12/14.
 */
@Path("/{version}/variables")
@Produces("application/json")
@Api(value = "Variables", position = 8, description = "Methods for working with 'variables' endpoint")
public class VariableWSServer extends OpenCGAWSServer {


    public VariableWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException, VersionException {
        super(uriInfo, httpServletRequest);
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create variable set", position = 1)
    public Response createSet(@ApiParam(value = "studyId", required = true) @QueryParam("studyId") String studyIdStr,
                              @ApiParam(value = "name", required = true) @QueryParam("name") String name,
                              @ApiParam(value = "unique", required = false) @QueryParam("unique") Boolean unique,
                              @ApiParam(value = "description", required = false) @QueryParam("description") String description,
                              @ApiParam(value = "variables", required = true) List<Variable> variables) {
        try {
            long studyId = catalogManager.getStudyId(studyIdStr);
            QueryResult<VariableSet> queryResult = catalogManager.createVariableSet(studyId, name, unique, description, null, variables,
                    sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{variableSetId}/info")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get VariableSet info", position = 2)
    public Response variableSetInfo(@ApiParam(value = "variableSetId", required = true) @PathParam("variableSetId") long variableSetId) {
        try {
            QueryResult<VariableSet> queryResult = catalogManager.getVariableSet(variableSetId, queryOptions, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get VariableSet info", position = 2)
    public Response search(@ApiParam(value = "studyId", required = true) @QueryParam("studyId") String studyIdStr,
                           @ApiParam(value = "CSV list of variableSetIds", required = false) @QueryParam("id") String id,
                           @ApiParam(value = "name", required = false) @QueryParam("name") String name,
                           @ApiParam(value = "description", required = false) @QueryParam("description") String description,
                           @ApiParam(value = "attributes", required = false) @QueryParam("attributes") String attributes) {
        try {
            long studyId = catalogManager.getStudyId(studyIdStr);
            queryOptions.put(CatalogSampleDBAdaptor.VariableSetFilterOption.studyId.toString(), studyId);
            QueryResult<VariableSet> queryResult = catalogManager.getAllVariableSet(studyId, queryOptions, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{variableSetId}/update")
    @ApiOperation(value = "Update some user variableSet using GET method [PENDING]", position = 3)
    public Response update(@ApiParam(value = "variableSetId", required = true) @PathParam("variableSetId") String variableSetId) throws IOException {
        return createErrorResponse("update - GET", "PENDING");
    }

    @POST
    @Path("/{variableSetId}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update some variableSet attributes using POST method [PENDING]", position = 3)
    public Response updateByPost(@ApiParam(value = "variableSetId", required = true) @PathParam("variableSetId") String variableSetId,
                                 @ApiParam(value = "params", required = true) Map<String, Object> params) {
        return createErrorResponse("update - POST", "PENDING");
    }

    @GET
    @Path("/{variableSetId}/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete an unused variable Set", position = 4)
    public Response delete(@ApiParam(value = "variableSetId", required = true) @PathParam("variableSetId") long variableSetId) {
        try {
            QueryResult<VariableSet> queryResult = catalogManager.deleteVariableSet(variableSetId, queryOptions, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/{variableSetId}/field/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add a new field in a variable set", position = 5)
    public Response addFieldToVariableSet(@ApiParam(value = "variableSetId", required = true) @PathParam("variableSetId")
                                          long variableSetId,
                                          @ApiParam(value = "variable", required = true) Variable variable) {
        try {
            QueryResult<VariableSet> queryResult = catalogManager.addFieldToVariableSet(variableSetId, variable, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{variableSetId}/field/delete")
    @ApiOperation(value = "Delete one field from a variable set", position = 6)
    public Response renameFieldInVariableSet(@ApiParam(value = "variableSetId", required = true) @PathParam("variableSetId")
                                             long variableSetId,
                                             @ApiParam(value = "name", required = true) @QueryParam("name") String name) {
        try {
            QueryResult<VariableSet> queryResult = catalogManager.removeFieldFromVariableSet(variableSetId, name, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{variableSetId}/field/rename")
    @ApiOperation(value = "Rename the field id of a field in a variable set", position = 7)
    public Response renameFieldInVariableSet(@ApiParam(value = "variableSetId", required = true)
                                                 @PathParam("variableSetId") long variableSetId,
                                             @ApiParam(value = "oldName", required = true) @QueryParam("oldName") String oldName,
                                             @ApiParam(value = "newName", required = true) @QueryParam("newName") String newName) {
        try {
            QueryResult<VariableSet> queryResult = catalogManager.renameFieldFromVariableSet(variableSetId, oldName, newName, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
}
