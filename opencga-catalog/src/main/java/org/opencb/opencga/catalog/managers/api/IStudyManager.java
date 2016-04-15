package org.opencb.opencga.catalog.managers.api;

import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.db.api.CatalogStudyDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.models.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public interface IStudyManager extends ResourceManager<Long, Study> {

    String getUserId(long studyId) throws CatalogException;

    Long getProjectId(long studyId) throws CatalogException;

    Long getStudyId(String studyId) throws CatalogException;

    /**
     * Creates a new Study in catalog.
     *
     * @param projectId    Parent project id
     * @param name         Study Name
     * @param alias        Study Alias. Must be unique in the project's studies
     * @param type         Study type: CONTROL_CASE, CONTROL_SET, ... (see org.opencb.opencga.catalog.models.Study.Type)
     * @param creatorId    Creator user id. If null, user by sessionId
     * @param creationDate Creation date. If null, now
     * @param description  Study description. If null, empty string
     * @param status       Unused
     * @param cipher       Unused
     * @param uriScheme    UriScheme to select the CatalogIOManager. Default: CatalogIOManagerFactory.DEFAULT_CATALOG_SCHEME
     * @param uri          URI for the folder where to place the study. Scheme must match with the uriScheme. Folder must exist.
     * @param datastores   DataStores information
     * @param stats        Optional stats
     * @param attributes   Optional attributes
     * @param options      QueryOptions
     * @param sessionId    User's sessionId
     * @return Generated study
     * @throws CatalogException CatalogException
     */
    QueryResult<Study> create(long projectId, String name, String alias, Study.Type type, String creatorId, String creationDate,
                              String description, Status status, String cipher, String uriScheme, URI uri,
                              Map<File.Bioformat, DataStore> datastores, Map<String, Object> stats, Map<String, Object> attributes,
                              QueryOptions options, String sessionId) throws CatalogException;

    QueryResult<Study> share(long studyId, AclEntry acl) throws CatalogException;


    /*---------------------*/
    /* VariableSet METHODS */
    /*---------------------*/

    QueryResult<VariableSet> createVariableSet(long studyId, String name, Boolean unique, String description,
                                               Map<String, Object> attributes, List<Variable> variables, String sessionId)
            throws CatalogException;

    QueryResult<VariableSet> createVariableSet(long studyId, String name, Boolean unique, String description,
                                               Map<String, Object> attributes, Set<Variable> variables, String sessionId)
            throws CatalogException;

    QueryResult<VariableSet> readVariableSet(long variableSet, QueryOptions options, String sessionId) throws CatalogException;

    QueryResult<VariableSet> readAllVariableSets(long studyId, QueryOptions options, String sessionId) throws CatalogException;

    QueryResult<VariableSet> deleteVariableSet(long variableSetId, QueryOptions queryOptions, String sessionId) throws CatalogException;

    QueryResult<VariableSet> addFieldToVariableSet(long variableSetId, Variable variable, String sessionId) throws CatalogException;

    QueryResult<VariableSet> removeFieldFromVariableSet(long variableSetId, String name, String sessionId) throws CatalogException;

    QueryResult<VariableSet> renameFieldFromVariableSet(long variableSetId, String oldName, String newName, String sessionId)
            throws CatalogException;

    /**
     * Ranks the elements queried, groups them by the field(s) given and return it sorted.
     *
     * @param projectId  Project id.
     * @param query      Query object containing the query that will be executed.
     * @param field      A field or a comma separated list of fields by which the results will be grouped in.
     * @param numResults Maximum number of results to be reported.
     * @param asc        Order in which the results will be reported.
     * @param sessionId  sessionId.
     * @return           A QueryResult object containing each of the fields in field and the count of them matching the query.
     * @throws CatalogException CatalogException
     */
    QueryResult rank(long projectId, Query query, String field, int numResults, boolean asc, String sessionId) throws CatalogException;

    default QueryResult rank(Query query, String field, int numResults, boolean asc, String sessionId) throws CatalogException {
        long projectId = query.getLong(CatalogStudyDBAdaptor.QueryParams.PROJECT_ID.key());
        if (projectId == 0L) {
            throw new CatalogException("Study[rank]: Study id not found in the query");
        }
        return rank(projectId, query, field, numResults, asc, sessionId);
    }

    /**
     * Groups the elements queried by the field(s) given.
     *
     * @param projectId  Project id.
     * @param query   Query object containing the query that will be executed.
     * @param field   Field by which the results will be grouped in.
     * @param options QueryOptions object.
     * @param sessionId  sessionId.
     * @return        A QueryResult object containing the results of the query grouped by the field.
     * @throws CatalogException CatalogException
     */
    QueryResult groupBy(long projectId, Query query, String field, QueryOptions options, String sessionId) throws CatalogException;

    default QueryResult groupBy(Query query, String field, QueryOptions options, String sessionId) throws CatalogException {
        long projectId = query.getLong(CatalogStudyDBAdaptor.QueryParams.PROJECT_ID.key());
        if (projectId == 0L) {
            throw new CatalogException("Study[groupBy]: Study id not found in the query");
        }
        return groupBy(projectId, query, field, options, sessionId);
    }

    /**
     * Groups the elements queried by the field(s) given.
     *
     * @param projectId  Project id.
     * @param query   Query object containing the query that will be executed.
     * @param fields  List of fields by which the results will be grouped in.
     * @param options QueryOptions object.
     * @param sessionId  sessionId.
     * @return        A QueryResult object containing the results of the query grouped by the fields.
     * @throws CatalogException CatalogException
     */
    QueryResult groupBy(long projectId, Query query, List<String> fields, QueryOptions options, String sessionId) throws CatalogException;

    default QueryResult groupBy(Query query, List<String> field, QueryOptions options, String sessionId) throws CatalogException {
        long projectId = query.getLong(CatalogStudyDBAdaptor.QueryParams.PROJECT_ID.key());
        if (projectId == 0L) {
            throw new CatalogException("Study[groupBy]: Study id not found in the query");
        }
        return groupBy(projectId, query, field, options, sessionId);
    }
}
