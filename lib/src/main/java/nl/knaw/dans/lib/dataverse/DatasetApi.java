/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.dataverse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.model.DataMessage;
import nl.knaw.dans.lib.dataverse.model.Lock;
import nl.knaw.dans.lib.dataverse.model.RoleAssignment;
import nl.knaw.dans.lib.dataverse.model.RoleAssignmentReadOnly;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetLatestVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetPublicationResult;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.Embargo;
import nl.knaw.dans.lib.dataverse.model.dataset.FieldList;
import nl.knaw.dans.lib.dataverse.model.dataset.FileList;
import nl.knaw.dans.lib.dataverse.model.dataset.RetentionPeriod;
import nl.knaw.dans.lib.dataverse.model.dataset.SubmitForReviewResult;
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;
import nl.knaw.dans.lib.dataverse.model.file.FileMetaUpdate;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * API end-points dealing with a single dataset.
 *
 * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#datasets" target="_blank">Dataverse documentation</a>
 */
@Slf4j
public class DatasetApi extends AbstractTargetedApi {
    @Override
    public String toString() {
        return format("DatasetApi(id=''{0}, isPersistentId={1})", id, isPersistentId);
    }

    DatasetApi(HttpClientWrapper httpClientWrapper, String id, boolean isPersistentId, String invocationId) {
        super(httpClientWrapper, id, isPersistentId, invocationId, Paths.get("api/datasets/"));
    }

    /**
     * Returns metadata of the latest version of the dataset <b>that you can see with your permissions</b>. If there is a draft for a new version this is returned in the field
     * <code>latestVersionPublishingState</code>. The use of this method is not recommended, as the {@link #getVersion(String)} method is more explicit and you can retrieve the latest version by
     * passing either {@link Version#LATEST}, {@link Version#DRAFT} or {@link Version#LATEST_PUBLISHED} as the version parameter.
     *
     * @return a JSON object that starts at the dataset level, most fields are replicated at the dataset version level.
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset" target="_blank">Dataverse documentation</a>
     * @deprecated use {@link #getVersion()} instead
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public DataverseHttpResponse<DatasetLatestVersion> getLatestVersion() throws IOException, DataverseException {
        return getUnversionedFromTarget("", DatasetLatestVersion.class);
    }

    /**
     * Retrieves that latest version of a dataset <b>that you can see with your permissions</b>. The difference with {@link #getLatestVersion()} is that the latter returns a different type of object.
     * The use of {@link #getLatestVersion()} is not recommended, as {@link #getVersion(String)} gives you more control over the version you want to retrieve.
     *
     * @return object containing the dataset version metadata
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#get-version-of-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> getVersion() throws IOException, DataverseException {
        // Not specifying a version results in getting all versions.
        return getVersion(Version.LATEST.toString(), false);
    }

    /**
     * Retrieves a specific version of a dataset if you can see it with your permissions, otherwise returns a 401.
     *
     * @param version version to retrieve
     * @return object containing the dataset version metadata
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#get-version-of-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> getVersion(String version) throws IOException, DataverseException {
        return getVersion(version, false);
    }

    /**
     * @param version      version to retrieve
     * @param excludeFiles whether to exclude file metadata from the response
     * @return object containing the dataset version metadata
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public DataverseHttpResponse<DatasetVersion> getVersion(@NonNull String version, boolean excludeFiles) throws IOException, DataverseException {
        if (StringUtils.isBlank(version))
            throw new IllegalArgumentException("Argument 'version' may not be empty");
        return getVersionedFromTarget("", version, Map.of("excludeFiles", Collections.singletonList(Boolean.toString(excludeFiles))), DatasetVersion.class);
    }

    /**
     * @return list of objects containing dataset version metadata
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#list-versions-of-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<List<DatasetVersion>> getAllVersions() throws IOException, DataverseException {
        // Not specifying a version results in getting all versions.
        return getVersionedFromTarget("", "", List.class, DatasetVersion.class);
    }

    /**
     * @param version version to get file metadata from
     * @return a list of file metas
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#list-files-in-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<List<FileMeta>> getFiles(String version) throws IOException, DataverseException {
        return getVersionedFromTarget("files", version, List.class, FileMeta.class);
    }

    /**
     * @return dataset publication result
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#publish-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetPublicationResult> publish() throws IOException, DataverseException {
        Path path = buildPath(targetBase, persistendId, publish);
        HashMap<String, List<String>> parameters = new HashMap<>();
        parameters.put("persistentId", singletonList(id));
        parameters.put("type", singletonList("major"));
        return httpClientWrapper.postJsonString(path, "", parameters, emptyMap(), DatasetPublicationResult.class);
    }

    /**
     * @param updateType      major or minor version update
     * @param assureIsIndexed To make sure that indexing has already happened the `assureIsIndexed` is set to `true`, it will then cause Dataverse to fail fast if indexing is still pending. In this
     *                        case the publish request will be retried a number of times.
     * @return dataset publication result
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#publish-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DataMessage> publish(UpdateType updateType, boolean assureIsIndexed) throws IOException, DataverseException {
        if (assureIsIndexed) {
            return publishWithRetriesForAwaitIndexing(updateType,
                httpClientWrapper.getConfig().getAwaitIndexingMaxNumberOfRetries(),
                httpClientWrapper.getConfig().getAwaitIndexingMillisecondsBetweenRetries());
        }
        else
            return publishWithoutRetries(updateType, false);
    }

    private DataverseHttpResponse<DataMessage> publishWithoutRetries(UpdateType updateType, boolean assureIsIndexed) throws IOException, DataverseException {
        HashMap<String, List<String>> parameters = new HashMap<>();
        parameters.put("assureIsIndexed", singletonList(String.valueOf(assureIsIndexed)));
        parameters.put("type", singletonList(updateType.toString()));
        return httpClientWrapper.postJsonString(subPath(publish), "", params(parameters), new HashMap<>(), DataMessage.class);
    }

    private DataverseHttpResponse<DataMessage> publishWithRetriesForAwaitIndexing(UpdateType updateType,
        int awaitIndexingMaxNumberOfRetries, int awaitIndexingMillisecondsBetweenRetries) throws IOException, DataverseException {

        int retryCount = 0;
        while (retryCount < awaitIndexingMaxNumberOfRetries) {
            try {
                return publishWithoutRetries(updateType, true);
            }
            catch (DataverseException e) {
                if (e.getStatus() != HttpStatus.SC_CONFLICT) {
                    log.error("Not an awaiting indexing status {}, rethrowing exception", e.getStatus());
                    throw e;
                }
                log.debug("Attempt to publish dataset failed because Dataset is awaiting indexing");
                retryCount++;
                log.debug("Retry count: {}", retryCount);
                if (retryCount == awaitIndexingMaxNumberOfRetries) {
                    log.error("Max retries ({}) reached, stop trying to publish dataset", awaitIndexingMaxNumberOfRetries);
                    throw e;
                }
                try {
                    log.debug("Sleeping for {} milliseconds before trying again", awaitIndexingMillisecondsBetweenRetries);
                    Thread.sleep(awaitIndexingMillisecondsBetweenRetries);
                }
                catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        throw new RuntimeException("This should never happen, either should have returned or thrown another error");
    }

    public DataverseHttpResponse<DatasetPublicationResult> releaseMigrated(String publicationDateJsonLd, boolean assureIsIndexed) throws IOException, DataverseException {
        Map<String, List<String>> parameters = singletonMap("assureIsIndexed", singletonList(String.valueOf(assureIsIndexed)));
        return httpClientWrapper.postJsonLdString(subPath("actions/:releasemigrated"), publicationDateJsonLd, params(parameters), emptyMap(), DatasetPublicationResult.class);
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified fields must be either currently empty or allow multiple values.
     *
     * @param s JSON document containing the edits to perform
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(String s) throws IOException, DataverseException {
        return editMetadata(s, true, emptyMap());
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified fields must be either currently empty or allow multiple values.
     *
     * @param s            JSON document containing the edits to perform
     * @param metadataKeys the HashMap maps the names of the metadata blocks to their 'secret' key values.
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(String s, Map<String, String> metadataKeys) throws IOException, DataverseException {
        return editMetadata(s, true, metadataKeys);
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified fields must be either currently empty or allow multiple values.
     *
     * @param s       JSON document containing the edits to perform
     * @param replace whether to replace existing values
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(String s, Boolean replace) throws IOException, DataverseException {
        return editMetadata(s, replace, emptyMap());
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified fields must be either currently empty or allow multiple values.
     * Whenever there are metadata field from a block that is protected by a 'key', the corresponding keys must be provided.
     *
     * @param s            JSON document containing the edits to perform
     * @param replace      whether to replace existing values
     * @param metadataKeys the HashMap maps the names of the metadata blocks to their 'secret' key values.
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(String s, Boolean replace, Map<String, String> metadataKeys) throws IOException, DataverseException {
        Map<String, List<String>> queryParams = getQueryParamsFromMetadataKeys(metadataKeys);
        if (replace)
            /*
             * Sic! any value for "replace" is interpreted by Dataverse as "true", even "replace=false"
             * It is by the *absence* of the parameter that replace is set to false.
             */
            queryParams.put("replace", singletonList("true"));
        return putToTarget("editMetadata", s, queryParams, DatasetVersion.class);
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified fields must be either currently empty or allow multiple values.
     *
     * @param fields  list of fields to edit
     * @param replace whether to replace existing values
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(FieldList fields, Boolean replace) throws IOException, DataverseException {
        return editMetadata(fields, replace, emptyMap());
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified fields must be either currently empty or allow multiple values.
     *
     * @param fields       list of fields to edit
     * @param replace      whether to replace existing values
     * @param metadataKeys the HashMap maps the names of the metadata blocks to their 'secret' key values.
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(FieldList fields, Boolean replace, Map<String, String> metadataKeys) throws IOException, DataverseException {
        return editMetadata(httpClientWrapper.writeValueAsString(fields), replace, metadataKeys);
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet.
     *
     * @param fields list of fields to edit
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(FieldList fields) throws IOException, DataverseException {
        return editMetadata(fields, true, emptyMap());
    }

    /**
     * Edits the current draft's metadata, adding the fields that do not exist yet.
     *
     * @param fields       list of fields to edit
     * @param metadataKeys the HashMap maps the names of the metadata blocks to their 'secret' key values.
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> editMetadata(FieldList fields, Map<String, String> metadataKeys) throws IOException, DataverseException {
        return editMetadata(fields, true, metadataKeys);
    }

    public DataverseHttpResponse<DatasetVersion> deleteMetadata(FieldList fields, Map<String, String> metadataKeys) throws IOException, DataverseException {
        return deleteMetadata(httpClientWrapper.writeValueAsString(fields), metadataKeys);
    }

    public DataverseHttpResponse<DatasetVersion> deleteMetadata(String s, Map<String, String> metadataKeys) throws IOException, DataverseException {
        Map<String, List<String>> queryParams = getQueryParamsFromMetadataKeys(metadataKeys);
        return httpClientWrapper.putJsonString(subPath("deleteMetadata"), s, params(queryParams), emptyMap(), DatasetVersion.class);
    }

    // TODO https://guides.dataverse.org/en/latest/api/native-api.html#export-metadata-of-a-dataset-in-various-formats
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#schema-org-json-ld
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#view-dataset-files-and-folders-as-a-directory-index
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#list-all-metadata-blocks-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#list-single-metadata-block-for-a-dataset

    /**
     * @param metadata JSON document describing the metadata
     * @param replace  replace existing metadata
     * @return a generic DataverseHttpResponse
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/developers/dataset-semantic-metadata-api.html#add-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<Object> updateMetadataFromJsonLd(String metadata, boolean replace) throws IOException, DataverseException {
        return updateMetadataFromJsonLd(metadata, replace, emptyMap());
    }

    /**
     * @param metadata     JSON document describing the metadata
     * @param replace      replace existing metadata
     * @param metadataKeys maps the names of the metadata blocks to their 'secret' key values
     * @return a generic DataverseResponse
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/developers/dataset-semantic-metadata-api.html#add-dataset-metadata" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<Object> updateMetadataFromJsonLd(String metadata, boolean replace, Map<String, String> metadataKeys) throws IOException, DataverseException {
        Map<String, List<String>> queryParams = getQueryParamsFromMetadataKeys(metadataKeys);
        queryParams.put("replace", singletonList((String.valueOf(replace))));
        return httpClientWrapper.putJsonLdString(subPath("metadata"), metadata, params(queryParams), extraHeaders, RoleAssignmentReadOnly.class);
    }

    /**
     * @param s JSON document containing the new metadata
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#update-metadata-for-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> updateMetadata(String s) throws IOException, DataverseException {
        return updateMetadata(s, emptyMap());
    }

    /**
     * @param s            JSON document containing the new metadata
     * @param metadataKeys maps the names of the metadata blocks to their 'secret' key values
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#update-metadata-for-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> updateMetadata(String s, Map<String, String> metadataKeys) throws IOException, DataverseException {
        Map<String, List<String>> queryParams = getQueryParamsFromMetadataKeys(metadataKeys);
        // Cheating with endPoint here, because the only version that can be updated is :draft anyway
        return putToTarget("versions/:draft", s, queryParams, DatasetVersion.class);
    }

    /**
     * Note that not all the attributes of the DatasetVersion object are writable. Dataverse may ignore some (e.g., license) or return an error if some are filled in (e.g., files). However, for a few
     * attributes, such as fileAccessRequest and termsOfAccess, it is necessary to pass the whole version object instead of only the metadata blocks (as is done in the example of the API
     * documentation).
     *
     * @param version a version object containing the new metadata
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#update-metadata-for-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> updateMetadata(DatasetVersion version) throws IOException, DataverseException {
        return updateMetadata(httpClientWrapper.writeValueAsString(version), emptyMap());
    }

    /**
     * @param version      a version object containing the new metadata
     * @param metadataKeys maps the names of the metadata blocks to their 'secret' key values
     * @return DatasetVersion
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#update-metadata-for-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DatasetVersion> updateMetadata(DatasetVersion version, Map<String, String> metadataKeys) throws IOException, DataverseException {
        return updateMetadata(httpClientWrapper.writeValueAsString(version), metadataKeys);
    }

    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#delete-dataset-metadata

    /**
     * @return a data message
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#delete-dataset-draft" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DataMessage> deleteDraft() throws IOException, DataverseException {
        return httpClientWrapper.delete(subPath("versions/:draft"), params(emptyMap()), DataMessage.class);
    }

    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#set-citation-date-field-type-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#revert-citation-date-field-type-to-default-for-dataset

    /**
     * @return role assignment information
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#list-role-assignments-on-a-dataverse-api" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<List<RoleAssignmentReadOnly>> listRoleAssignments() throws IOException, DataverseException {
        return getUnversionedFromTarget("assignments", List.class, RoleAssignmentReadOnly.class);
    }

    /**
     * @param roleAssignment JSON document describing the assignment
     * @return role assignment information
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#assign-a-new-role-on-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<RoleAssignmentReadOnly> assignRole(String roleAssignment) throws IOException, DataverseException {
        return httpClientWrapper.postJsonString(subPath("assignments"), roleAssignment, params(emptyMap()), extraHeaders, RoleAssignmentReadOnly.class);
    }

    public DataverseHttpResponse<RoleAssignmentReadOnly> assignRole(RoleAssignment roleAssignment) throws IOException, DataverseException {
        return assignRole(httpClientWrapper.writeValueAsString(roleAssignment));
    }

    /**
     * @param roleAssignmentId Delete the assignment with this id
     * @return A data information message
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#delete-role-assignment-from-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<DataMessage> deleteRoleAssignment(int roleAssignmentId) throws IOException, DataverseException {
        return httpClientWrapper.delete(subPath("assignments/" + roleAssignmentId), params(emptyMap()), DataMessage.class);
    }

    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#create-a-private-url-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#get-the-private-url-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#delete-the-private-url-from-a-dataset

    /**
     * @param file     the file to add
     * @param metadata json document with the file metadata
     * @return a file list
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#add-a-file-to-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<FileList> addFile(Path file, String metadata) throws IOException, DataverseException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        Optional.ofNullable(file).ifPresent(f -> builder.addPart("file", new FileBody(f.toFile(), ContentType.APPLICATION_OCTET_STREAM, f.getFileName().toString())));
        Optional.ofNullable(metadata).ifPresent(m -> builder.addPart("jsonData", new StringBody(m, ContentType.APPLICATION_JSON)));
        return httpClientWrapper.post(subPath("add"), builder.build(), params(emptyMap()), extraHeaders, FileList.class);
    }

    /**
     * @param file     the file to add
     * @param fileMeta json document with the file metadata
     * @return a file list
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#add-a-file-to-a-dataset" target="_blank">Dataverse documentation</a>
     */
    public DataverseHttpResponse<FileList> addFile(Path file, FileMeta fileMeta) throws IOException, DataverseException {
        return addFile(file, httpClientWrapper.writeValueAsString(fileMeta));
    }

    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#report-the-data-file-size-of-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#get-the-size-of-downloading-all-the-files-of-a-dataset-version

    /**
     * @return todo
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#submit-a-dataset-for-review">Dataverse documentation</a>
     */
    public DataverseHttpResponse<SubmitForReviewResult> submitForReview() throws IOException, DataverseException {
        return httpClientWrapper.post(subPath("submitForReview"), new StringEntity(""), params(emptyMap()), extraHeaders, SubmitForReviewResult.class);
    }

    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#return-a-dataset-to-author
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#link-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#dataset-locks
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#retrieving-total-views-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#retrieving-unique-views-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#retrieving-total-downloads-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#retrieving-unique-downloads-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#retrieving-citations-for-a-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#delete-unpublished-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#delete-published-dataset
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#configure-a-dataset-to-use-a-specific-file-store

    // TODO: FIRST
    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#view-the-timestamps-on-a-dataset
    /**
     * Publishes the current draft of an imported dataset as a new version with the original publication date.
     *
     * If publish is called shortly after a modification and there is a pre-publication workflow installed, there is a risk of the workflow failing to
     * start because of an OptimisticLockException. This is caused by Dataverse indexing the dataset on a separate thread. This will appear to the client
     * as Dataverse silently failing (i.e. returning success but not publishing the dataset). To make sure that indexing has already happened the `assureIsIndexed`
     * parameter is set to `true`. It will cause Dataverse to fail fast if indexing is still pending.
     *
     * @param publicationDateJsonLd original publication date
     * @param assureIsIndexed       make Dataverse return 409 Conflict if an index action is pending
     * @return
     */

    /**
     * @param embargo the embargo data
     * @return a hash map
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#set-an-embargo-on-files-in-a-dataset" target="_blank">Dataverse documentation</a>
     */
    // TODO: can the hash map be converted to a more specific object?
    public DataverseHttpResponse<HashMap> setEmbargo(Embargo embargo) throws IOException, DataverseException {
        return setEmbargo(httpClientWrapper.writeValueAsString(embargo));
    }

    public DataverseHttpResponse<HashMap> setEmbargo(String json) throws IOException, DataverseException {
        return httpClientWrapper.postJsonString(subPath("files/actions/:set-embargo"), json, params(emptyMap()), extraHeaders, HashMap.class);
    }

    // TODO: https://guides.dataverse.org/en/latest/api/native-api.html#remove-an-embargo-on-files-in-a-dataset

    /**
     * @param retentionPeriod the retention period data
     * @return a hash map
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public DataverseHttpResponse<HashMap> setRetentionPeriod(RetentionPeriod retentionPeriod) throws IOException, DataverseException {
        return setRetentionPeriod(httpClientWrapper.writeValueAsString(retentionPeriod));
    }

    public DataverseHttpResponse<HashMap> setRetentionPeriod(String json) throws IOException, DataverseException {
        return httpClientWrapper.postJsonString(subPath("files/actions/:set-retention"), json, params(emptyMap()), extraHeaders, HashMap.class);
    }

    public DataverseHttpResponse<String> deleteFiles(List<Integer> fileIds) throws IOException, DataverseException {
        return httpClientWrapper.putJsonString(subPath("deleteFiles"), httpClientWrapper.writeValueAsString(fileIds), params(emptyMap()), extraHeaders, String.class);
    }

    public DataverseHttpResponse<String> updateFileMetadatas(List<FileMetaUpdate> fileMetas) throws IOException, DataverseException {
        return httpClientWrapper.postJsonString(subPath("files/metadata"), httpClientWrapper.writeValueAsString(fileMetas), params(emptyMap()), extraHeaders, String.class);
    }

    /*
     * Helper methods
     */
    private <D> DataverseHttpResponse<D> getVersionedFromTarget(String endPoint, String version, Class<?>... outputClass) throws IOException, DataverseException {
        return httpClientWrapper.get(versionedSubPath(endPoint, version), params(emptyMap()), extraHeaders, outputClass);
    }

    private <D> DataverseHttpResponse<D> getVersionedFromTarget(String endPoint, String version, Map<String, List<String>> queryParams, Class<?>... outputClass)
        throws IOException, DataverseException {
        return httpClientWrapper.get(versionedSubPath(endPoint, version), params(queryParams), extraHeaders, outputClass);
    }

    private <D> DataverseHttpResponse<D> getUnversionedFromTarget(String endPoint, Map<String, List<String>> queryParams, Class<?>... outputClass)
        throws IOException, DataverseException {
        return httpClientWrapper.get(subPath(endPoint), params(queryParams), extraHeaders, outputClass);
    }

    private <D> DataverseHttpResponse<D> getUnversionedFromTarget(String endPoint, Class<?>... outputClass) throws IOException, DataverseException {
        return getUnversionedFromTarget(endPoint, emptyMap(), outputClass);
    }

    private <D> DataverseHttpResponse<D> putToTarget(String endPoint, String body, Map<String, List<String>> queryParams, Class<?>... outputClass)
        throws IOException, DataverseException {
        return httpClientWrapper.putJsonString(subPath(endPoint), body, params(queryParams), extraHeaders, outputClass);
    }

    /**
     * @return a list of locks
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     * @see <a href="https://guides.dataverse.org/en/latest/api/native-api.html#dataset-locks" target="_blank">...</a>
     * @see <a href="https://github.com/DANS-KNAW/dans-dataverse-client-lib/blob/master/examples/src/main/java/nl/knaw/dans/lib/dataverse/example/DatasetGetLocks.java">Code example</a>
     */
    public DataverseHttpResponse<List<Lock>> getLocks() throws IOException, DataverseException {
        return getUnversionedFromTarget("locks", List.class, Lock.class);
    }

    /**
     * Utility function that lets you wait until all locks are cleared before proceeding. Unlike most other functions in this library, this does not correspond directly with an API call. Rather the
     * {@link #getLocks()} call is done repeatedly to check if the locks have been cleared. Note that in scenarios where concurrent processes might access the same dataset it is not guaranteed that
     * the locks, once cleared, stay that way.
     *
     * @param maxNumberOfRetries     the maximum number the check for unlock is made, defaults to [[awaitLockStateMaxNumberOfRetries]]
     * @param waitTimeInMilliseconds the time between tries, defaults to [[awaitLockStateMillisecondsBetweenRetries]]
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public void awaitUnlock(int maxNumberOfRetries, int waitTimeInMilliseconds) throws IOException, DataverseException {
        awaitLockState(this::notLocked, "Wait for unlock expired", maxNumberOfRetries, waitTimeInMilliseconds, "");
    }

    /**
     * The same
     *
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public void awaitUnlock() throws IOException, DataverseException {
        awaitUnlock(httpClientWrapper.getConfig().getAwaitLockStateMaxNumberOfRetries(), httpClientWrapper.getConfig().getAwaitLockStateMillisecondsBetweenRetries());
    }

    /**
     * The same for a specific set of locks.
     *
     * @param lockTypes              the lock types to wait for
     * @param maxNumberOfRetries     the maximum number the check for unlock is made, defaults to #awawaitLockStateMaxNumberOfRetries
     * @param waitTimeInMilliseconds the time between tries, defaults to [[awaitLockStateMillisecondsBetweenRetries]]
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public void awaitUnlock(List<String> lockTypes, int maxNumberOfRetries, int waitTimeInMilliseconds) throws IOException, DataverseException {
        awaitLockState(this::notLocked, String.format("Wait for unlock of types %s on %s expired", lockTypes, this.id), maxNumberOfRetries, waitTimeInMilliseconds, lockTypes.toArray(new String[0]));
    }

    /**
     * Utility function that lets you wait until a specified lock type is set. Unlike most other functions in this library, this does not correspond directly with an API call. Rather the
     * {@link #getLocks()} call is done repeatedly to check if the locks has been set. A use case is when an http/sr workflow wants to make sure that a dataset has been locked on its behalf, so that
     * it can be sure to have exclusive access via its invocation ID.
     *
     * @param lockType               the lock type to wait for
     * @param maxNumberOfRetries     the maximum number the check for unlock is made, defaults to #awawaitLockStateMaxNumberOfRetries
     * @param waitTimeInMilliseconds the time between tries, defaults to [[awaitLockStateMillisecondsBetweenRetries]]
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public void awaitLock(String lockType, int maxNumberOfRetries, int waitTimeInMilliseconds) throws IOException, DataverseException {
        awaitLockState(this::isLocked, String.format("Wait for lock of type %s expired", lockType), maxNumberOfRetries, waitTimeInMilliseconds, lockType);
    }

    /**
     * The same as {@link #awaitLock(String, int, int)} but with defaults for number of tries and time between tries.
     *
     * @param lockType the lock type to wait for
     * @throws IOException        when I/O problems occur during the interaction with Dataverse
     * @throws DataverseException when Dataverse fails to perform the request
     */
    public void awaitLock(String lockType) throws IOException, DataverseException {
        awaitLock(lockType, httpClientWrapper.getConfig().getAwaitLockStateMaxNumberOfRetries(), httpClientWrapper.getConfig().getAwaitLockStateMillisecondsBetweenRetries());
    }

    /**
     * Private functional interface to get the locking status, and methods implementing it.
     */
    @FunctionalInterface
    private interface Locked {
        boolean get(List<Lock> locks, String lockType);
    }

    private boolean isLocked(List<Lock> locks, String lockType) {
        for (Lock lock : locks) {
            if (lock.getLockType().equals(lockType))
                return true;
        }
        return false;
    }

    private boolean notLocked(List<Lock> locks, String lockType) {
        return locks.isEmpty();
    }

    /**
     * Helper function that waits until the specified lockState function returns `true`, or throws a LockException if this never occurs within `maxNumberOrRetries` with `waitTimeInMilliseconds`
     * pauses.
     *
     * @param desiredState           the function that returns whether the required state has been reached
     * @param errorMessage           error to report in LockException if it occurs
     * @param maxNumberOfRetries     the maximum number of tries
     * @param waitTimeInMilliseconds the time to wait between tries
     * @param lockTypesToCheck              type or types of locking
     */
    private void awaitLockState(Locked desiredState, String errorMessage, int maxNumberOfRetries, int waitTimeInMilliseconds, String... lockTypesToCheck)
        throws IOException, DataverseException {
        int numberOfTimesTried = 0;

        class CurrentLocks {
            private List<Lock> getCurrentLocks() throws IOException, DataverseException {
                List<Lock> locks = getLocks().getData();
                log.debug(String.format("Current locks: %s", locks.toString()));
                return locks;
            }

            private boolean slept() {
                log.debug(String.format("Sleeping %d ms before next try..", waitTimeInMilliseconds));
                try {
                    Thread.sleep(waitTimeInMilliseconds);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }

        CurrentLocks currentLocks = new CurrentLocks();
        List<Lock> locks;
        do {
            locks = currentLocks.getCurrentLocks();
            numberOfTimesTried += 1;
        }
        while (notAllLocksHaveDesiredState(desiredState, locks, lockTypesToCheck) && numberOfTimesTried != maxNumberOfRetries && currentLocks.slept());

        if (notAllLocksHaveDesiredState(desiredState, locks, lockTypesToCheck)) {
            var remainingLocks = locks.stream().map(Lock::getLockType).toList();
            throw new RuntimeException(String.format("%s. Number of tries = %d, wait time between tries = %d ms. Remaning lock(s): %s.",
                errorMessage, maxNumberOfRetries, waitTimeInMilliseconds, String.join(",", remainingLocks)
            ));
        }
    }

    private boolean notAllLocksHaveDesiredState(Locked desiredState, List<Lock> locks, String[] lockTypes) {
        for (String lockType : lockTypes) {
            if (!desiredState.get(locks, lockType)) {
                log.debug(String.format("Lock %s does not have desired state", lockType));
                return true;
            }
        }
        return false;
    }

    /**
     * Waits for the dataset to reach the expected state. This is useful when you want to wait for a dataset to be published, for example.
     *
     * @param expectedState   the state to wait for (e.g. "RELEASED")
     * @param pollingInterval the time to wait between checks
     * @param timeout         the maximum time to wait
     * @throws IOException        if there is a problem with the HTTP request
     * @throws DataverseException if Dataverse returns an error
     */
    public void awaitState(String expectedState, long timeout, long pollingInterval) throws IOException, DataverseException {
        var state = "";
        var startTime = System.currentTimeMillis();
        log.debug("Starting at {} to wait for dataset to become {}", startTime, expectedState);
        try {
            state = getState();
            log.debug("Initial state for dataset {} is {}", id, state);
            while (!expectedState.equals(state) && (System.currentTimeMillis() - startTime) < timeout) {
                log.debug("Sleeping for {} milliseconds before checking again", pollingInterval);
                Thread.sleep(pollingInterval);

                state = getState();
                log.debug("Current state for dataset {} is {}, remaining time: {} ms", id, state, timeout - (System.currentTimeMillis() - startTime));
            }

            if (!expectedState.equals(state)) {
                throw new IllegalStateException(String.format(
                    "Dataset did not become %s within the wait period (%d seconds); current state is %s",
                    expectedState, timeout / 1000, state
                ));
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Dataset state check was interrupted; last know state is " + state);
        }
    }

    /**
     * Returns the state of the latest version of the dataset that is visible to the user.
     *
     * @return the state of the dataset
     * @throws IOException        if there is a problem with the HTTP request
     * @throws DataverseException if Dataverse returns an error
     */
    public String getState() throws IOException, DataverseException {
        return getVersion(Version.LATEST.toString(), true).getData().getVersionState();
    }
}
