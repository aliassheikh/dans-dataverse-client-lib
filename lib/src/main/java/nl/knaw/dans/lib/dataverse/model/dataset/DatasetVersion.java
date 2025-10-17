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
package nl.knaw.dans.lib.dataverse.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import nl.knaw.dans.lib.dataverse.model.file.FileMeta;

import java.util.List;
import java.util.Map;

// Ignoring isPartOf, has to specified as option in the API call, which we don't do yet
@Data
@JsonIgnoreProperties({"isPartOf"})
public class DatasetVersion {

    private Integer id;
    private Integer datasetId;
    private String datasetPersistentId;
    private DatasetType datasetType;
    private String storageIdentifier;
    private Integer versionNumber;
    private Integer internalVersionNumber;
    private Integer versionMinorNumber;
    private String versionState;
    private String latestVersionPublishingState;
    private String versionNote;
    private String deaccessionNote;
    private String deaccessionLink;
    @JsonProperty("UNF")
    private String unf;
    private String lastUpdateTime;
    private String releaseTime;
    private String createTime;
    private String distributionDate;
    private String productionDate;
    private Boolean fileAccessRequest;
    private String termsOfUse;
    private String confidentialityDeclaration;
    private String specialPermissions;
    private String restrictions;
    private String citationRequirements;
    private String depositorRequirements;
    private String conditions;
    private String disclaimer;
    private String termsOfAccess;
    private String dataAccessPlace;
    private String originalArchive;
    private String availabilityStatus;
    private String contactForAccess;
    private String sizeOfCollection;
    private String studyCompletion;
    private License license;
    private String protocol;
    private String authority;
    private String identifier;
    private Map<String, MetadataBlock> metadataBlocks;
    private List<FileMeta> files;
    private String citation;
    private String publicationDate;
    private String citationDate;
    private String alternativePersistentId;
    // isPartOf ignore!
}
