/*
 * Copyright 2015-2017 OpenCB
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

package org.opencb.opencga.storage.core.manager;

import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryParam;
import org.opencb.opencga.catalog.db.api.ProjectDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.models.Project;
import org.opencb.opencga.core.models.Study;
import org.opencb.opencga.storage.core.manager.variant.VariantCatalogQueryUtils;
import org.opencb.opencga.storage.core.variant.adaptors.VariantField;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryUtils.isValidParam;

/**
 * Created by pfurio on 02/12/16.
 */
public class CatalogUtils {

    protected final CatalogManager catalogManager;

    public CatalogUtils(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    /**
     * @see org.opencb.opencga.catalog.db.mongodb.MongoDBUtils#ANNOTATION_PATTERN
     */
    public static final Pattern ANNOTATION_PATTERN = Pattern.compile("^([a-zA-Z\\\\.]+)([\\^=<>~!$]+.*)$");

    /**
     * Parse a generic string with comma separated key=values and obtain a query understandable by catalog.
     *
     * @param value String of the kind age>20;ontologies=hpo:123,hpo:456;name=smith
     * @param getParam Get param function that will return null if the key string is not one of the accepted keys in catalog. For those
     *                 cases, they will be treated as annotations.
     * @return A query object.
     */
    protected static Query parseSampleAnnotationQuery(String value, Function<String, QueryParam> getParam) {

        Query query = new Query();

        List<String> annotationList = new ArrayList<>();
        List<String> params = Arrays.asList(value.replaceAll("\\s+", "").split(";"));
        for (String param : params) {
            Matcher matcher = ANNOTATION_PATTERN.matcher(param);
            String key;
            if (matcher.find()) {
                key = matcher.group(1);
                if (getParam.apply(key) != null && !key.startsWith("annotation")) {
                    query.put(key, matcher.group(2));
                } else {
                    // Annotation
                    String myKey = key;
                    if (!key.startsWith("annotation.")) {
                        myKey = "annotation." + key;
                    }
                    annotationList.add(myKey + matcher.group(2));
                }
            }
        }

        query.put("annotation", annotationList);

        return query;
    }

    /**
     * Get the list of studies. Discards negated studies (starting with '!').
     *
     * @see org.opencb.opencga.storage.core.metadata.StudyConfigurationManager#getStudyIds(List, QueryOptions)
     * @param query     Query with the values
     * @param sessionId User's sessionId
     * @return          List of positive studies.
     * @throws CatalogException if there is an error with catalog
     */
    public List<Long> getStudies(Query query, String sessionId) throws CatalogException {
        List<String> studies = VariantQueryUtils.getIncludeStudiesList(query, Collections.singleton(VariantField.STUDIES));
        if (studies == null) {
            if (isValidParam(query, VariantCatalogQueryUtils.PROJECT)) {
                String project = query.getString(VariantCatalogQueryUtils.PROJECT.key());
                QueryOptions queryOptions = new QueryOptions(QueryOptions.INCLUDE,
                        Arrays.asList(ProjectDBAdaptor.QueryParams.STUDY_UID.key(), ProjectDBAdaptor.QueryParams.UID.key()));
                return catalogManager.getProjectManager()
                        .get(project, queryOptions, sessionId)
                        .first()
                        .getStudies()
                        .stream()
                        .map(Study::getUid)
                        .collect(Collectors.toList());
            } else {
                String userId = catalogManager.getUserManager().getUserId(sessionId);
                return catalogManager.getStudyManager().getIds(userId, Collections.emptyList());
            }
        } else {
            return catalogManager.getStudyManager().getIds(catalogManager.getUserManager().getUserId(sessionId), studies);
        }
    }

    public Project getProjectFromQuery(Query query, String sessionId, QueryOptions options) throws CatalogException {
        if (isValidParam(query, VariantCatalogQueryUtils.PROJECT)) {
            String project = query.getString(VariantCatalogQueryUtils.PROJECT.key());
            return catalogManager.getProjectManager().get(project, options, sessionId).first();
        } else {
            long studyId = getAnyStudyId(query, sessionId);
            Long projectId = catalogManager.getStudyManager().getProjectId(studyId);
            return catalogManager.getProjectManager().get(new Query(ProjectDBAdaptor.QueryParams.UID.key(), projectId), options, sessionId)
                    .first();
        }
    }

    /**
     * Gets any studyId referred in the Query. If none, tries to get the default study. If more than one, thrown an exception.
     * @param query     Variants query
     * @param sessionId User's sessionId
     * @return  Any study id
     * @throws CatalogException if there is a catalog error or the study is missing
     */
    public long getAnyStudyId(Query query, String sessionId) throws CatalogException {
        List<Long> studies = getStudies(query, sessionId);
        if (studies.isEmpty()) {
            throw new CatalogException("Missing StudyId. Unable to get any variant!");
        } else {
            return studies.get(0);
        }
    }

}
