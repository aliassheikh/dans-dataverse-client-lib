package nl.knaw.dans.lib.dataverse.integration;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.example.AdminGetDatabaseSetting;
import nl.knaw.dans.lib.dataverse.example.AdminListSingleUser;
import nl.knaw.dans.lib.dataverse.example.AdminPutDatabaseSetting;
import nl.knaw.dans.lib.dataverse.example.AdminValidateDatasetFiles;
import nl.knaw.dans.lib.dataverse.example.DataverseCreate;
import nl.knaw.dans.lib.dataverse.example.DataverseCreateDataset;
import nl.knaw.dans.lib.dataverse.example.DataverseDelete;
import nl.knaw.dans.lib.dataverse.example.DataverseGetContents;
import nl.knaw.dans.lib.dataverse.example.DataverseGetStorageSize;
import nl.knaw.dans.lib.dataverse.example.DataverseImportDataset;
import nl.knaw.dans.lib.dataverse.example.DataverseIsMetadataBlocksRoot;
import nl.knaw.dans.lib.dataverse.example.DataverseListRoleAssignments;
import nl.knaw.dans.lib.dataverse.example.DataverseListRoles;
import nl.knaw.dans.lib.dataverse.example.DataversePublish;
import nl.knaw.dans.lib.dataverse.example.DataverseSetMetadataBlocksRoot;
import nl.knaw.dans.lib.dataverse.example.DataverseView;

import java.util.List;

@Slf4j
public class DataverseSmokeTest {
    public static void main(String[] args) throws Exception {
        DataverseGetContents.main(new String[0]);
        DataverseGetStorageSize.main(new String[0]);
        DataverseIsMetadataBlocksRoot.main(List.of("root").toArray(new String[0]));
        DataverseListRoleAssignments.main(new String[0]);
        DataverseListRoles.main(new String[0]);
        DataverseView.main(List.of("root").toArray(new String[0]));
        DataverseCreate.main(new String[0]);
        DataversePublish.main(List.of("test2").toArray(new String[0]));
        DataverseIsMetadataBlocksRoot.main(List.of("test2").toArray(new String[0]));
        DataverseSetMetadataBlocksRoot.main(List.of("test2").toArray(new String[0]));
        DataverseSetMetadataBlocksRoot.main(List.of("root").toArray(new String[0]));
        DataverseDelete.main(List.of("test2").toArray(new String[0]));
        DataverseCreateDataset.main(new String[0]); // TODO citation-md-key-value
        DataverseImportDataset.main(new String[0]); // TODO citation-md-key-value
    }
}
