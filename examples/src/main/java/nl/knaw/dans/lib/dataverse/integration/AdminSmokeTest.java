package nl.knaw.dans.lib.dataverse.integration;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.example.AdminGetDatabaseSetting;
import nl.knaw.dans.lib.dataverse.example.AdminListSingleUser;
import nl.knaw.dans.lib.dataverse.example.AdminPutDatabaseSetting;
import nl.knaw.dans.lib.dataverse.example.AdminValidateDatasetFiles;

import java.util.List;

@Slf4j
public class AdminSmokeTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            log.error("Expecting a doi argument (e.g: doi:10.5072/DAR/I529LE) of an existing dataset with files");
            System.exit(1);
        }
        AdminGetDatabaseSetting.main(List.of("AllowCustomTermsOfUse").toArray(new String[0]));
        AdminPutDatabaseSetting.main(List.of("AllowCustomTermsOfUse","true").toArray(new String[0]));
        AdminListSingleUser.main(List.of("user001").toArray(new String[0]));
        AdminListSingleUser.main(List.of("dataverseAdmin").toArray(new String[0]));
        AdminValidateDatasetFiles.main(args);
        AdminValidateDatasetFiles.main(List.of("2").toArray(new String[0]));
    }
}
