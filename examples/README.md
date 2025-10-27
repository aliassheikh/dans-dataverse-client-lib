Test Plan
=========

New releases of dataverse might break `response.getData()`.
Run all tests to verify that everything is working as expected.

Arguments:
* All Dataset classes require a \<doi> as first argument e.g.: `doi:10.5072/DAR/I529LE` 
* AdminValidateDatasetFiles expects either a \<doi> or a dataset ID. AdminSmokeTest only tests a doi.
* DatasetAwaitUnlock and DatasetAwaitLock have comment headers suggesting test scenario's.
  DatasetAwaitUnlock shows usage when no arguments are specified.
  The optional second argument for DatasetAwaitLock is the lock-type which defaults to "_InReview_"
* More scenario's or recommend orders of running the tests (WIP):
  * DataverseCreate - DataversePublish test
  * DatasetCreate - DatasetPublish

Some more argument specs / examples:
* BasicFileAccessGetFile  \<file-id> \<destination-file> \[ \<range-start> \<range-end> ] 
* DatasetAddFile   \<doi> \<existing-file-path>  
* DatasetAssignRole   \<doi> \<user-name> fileDownloader  
* DatasetDeleteFiles   \<doi> \<file-id> ...
* DatasetDeleteMetadata   \<doi> "\<description-value>" \[ \<citation-md-key-value> ] 
* DatasetDeleteRoleAssignment   \<doi> \<role-assignment-id>  
* DatasetEditMetadata   \<doi> "\<title-value>" \[ \<citation-md-key-value> ]  
* DatasetGetFiles   \<doi> \<version> 
* DatasetGetVersion   \<doi> \<version> 
* DatasetUpdateMetadataFromJsonLd  \<doi> \<field-name> \<field-value>   \[ \<md-block-name> \<md-key-value> ]
* FileReplace   \<doi> \<file-id>  
* FileUpdateMetadata  \<file-id> \<new-name> \<new-dir>  

You can find actual ID's by exporting the metadata of a dataset. And

    select role_id, o.dtype, o.identifier from roleassignment join dvobject o on o.id = definitionpoint_id;
