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
package nl.knaw.dans.lib.dataverse.example;

import nl.knaw.dans.lib.dataverse.CompoundFieldBuilder;
import nl.knaw.dans.lib.dataverse.DataverseHttpResponse;
import nl.knaw.dans.lib.dataverse.ExampleBase;
import nl.knaw.dans.lib.dataverse.model.dataset.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataverseImportDataset extends ExampleBase {
    private static final Logger log = LoggerFactory.getLogger(DataverseImportDataset.class);

    public static void main(String[] args) throws Exception {
        var keyMap = new HashMap<String, String>();
        if (args.length > 0) {
            var mdKeyValue = args[0];
            keyMap.put("citation", mdKeyValue);
            System.out.println("Supplied citation metadata key: " + mdKeyValue );
        }

        MetadataBlock citation = new MetadataBlock();
        citation.setName("citation");
        citation.setDisplayName("Citation Metadata");

        MetadataField title = new PrimitiveSingleValueField("title", "Test imported dataset");
        MetadataField description = new CompoundFieldBuilder("dsDescription", true)
                .addSubfield("dsDescriptionValue", "Test description")
                .addSubfield("dsDescriptionDate", "").build();
        MetadataField author = new CompoundFieldBuilder("author", true)
                .addSubfield("authorName", "P E Loki")
                .addSubfield("authorAffiliation", "Walhalla").build();
        MetadataField contact = new CompoundFieldBuilder("datasetContact", true)
                .addSubfield("datasetContactName", "Test Contact")
                .addSubfield("datasetContactEmail", "test@example.com").build();
        MetadataField subjects = new ControlledMultiValueField("subject", Arrays.asList("Arts and Humanities", "Computer and Information Science"));

        citation.setFields(Arrays.asList(title, author, contact, description, subjects));

        DatasetVersion version = new DatasetVersion();
        version.setMetadataBlocks(Collections.singletonMap("citation", citation));
        version.setFiles(Collections.emptyList()); // Otherwise a 400 Bad Request is returned; you are not allowed to change file metadata this way
        // The license field is ignored, for how to set it, see example DatasetUpdateMetadataFromJsonLd
        //        License license = new License();
        //        license.setName("CC BY-NC-SA 4.0");
        //        license.setUri(new URI("http://creativecommons.org/licenses/by-nc-sa/4.0"));
        //        version.setLicense(license);
        version.setTermsOfAccess("Some terms");
        version.setFileAccessRequest(false);

        Dataset dataset = new Dataset();
        dataset.setDatasetVersion(version);

        log.info("--- BEGIN JSON OBJECT ---");
        log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataset));
        log.info("--- END JSON OBJECT ---");

        UUID uuid = UUID.randomUUID();
        Optional<String> optDoi = Optional.of("doi:10.5072/EDDA-RAGNAROK/IMPORTTEST-" + uuid.toString());
        DataverseHttpResponse<DatasetCreationResult> r = client.dataverse("root").importDataset(dataset, 
                optDoi, false, keyMap);
        log.info("Status Line: {}", r.getHttpResponse().getStatusLine());
    }

}
