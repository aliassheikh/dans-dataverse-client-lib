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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseResponse;
import nl.knaw.dans.lib.dataverse.ExampleBase;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DatasetUpdateMetadataFromJsonLd extends ExampleBase {

    public static void main(String[] args) throws Exception {
        String persistentId = args[0];
        var fieldName = args[1];
        var fieldValue = args[2];
        // Maybe default fieldName to http://schema.org/license ?
        var fieldObject = Map.of(fieldName, fieldValue);
        var jsonLd = toPrettyJson(fieldObject);

        log.info(jsonLd);

        var keyMap = new HashMap<String, String>();
        if (args.length > 4) {
            var mdBlockName = args[3];
            var mdKeyValue = args[4];
            keyMap.put(mdBlockName, mdKeyValue);
            log.info("Supplied metadata key (name, value): ({}< {})", mdBlockName, mdKeyValue);
        }

        DataverseResponse<Object> r = client.dataset(persistentId)
            .updateMetadataFromJsonLd(jsonLd, true, keyMap);
        log.info("Response message: {}", r.getEnvelopeAsJson().toPrettyString());
        log.info("Updated dataset metadata from JSON-LD {}", r.getData());
    }
}
