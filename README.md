# liferay-assetexporttool-portlet
Exports Liferay assets (wiki, web content) to PDF, DOCX and Nuxeo (incl. NuxeoConnector).

Liferay 6.2 version

Configuration: docroot/WEB-INF/src/portlet.properties

*Installation*

Build .war:

1. Project has to be placed in a folder called "AssetExportTool-portlet" in the *portlets* folder of a Liferay Plugins SDK.
1. Import in Liferay IDE using "Liferay project from existing source"
1. Right click on project and execute Liferay - SDK - war

Deploy:

4. Put generated war in Liferay's "deploy" folder
5. Restart Liferay