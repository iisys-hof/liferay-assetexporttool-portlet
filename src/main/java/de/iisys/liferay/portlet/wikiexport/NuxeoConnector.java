package de.iisys.liferay.portlet.wikiexport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.FileBlob;

import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.util.portlet.PortletProps;


public class NuxeoConnector {
	
	private final String NUXEO_URL = "nuxeo.url";
	private final static String NUXEO_USER_NAME_PROP = "current.user.name";
	private final static String NUXEO_USER_PW_PROP = "current.user.pw";
	
	public final static String NUXEO_PATH_DEFAULT = "/default-domain";
	public final static String NUXEO_PATH_USERWORKSPACES = NUXEO_PATH_DEFAULT+"/UserWorkspaces";
	public final static String NUXEO_PATH_WORKSPACES = NUXEO_PATH_DEFAULT+"/workspaces";
	
/*	private static String NUXEOuserName = "LiferayWikiExport";
	private static String NUXEOuserPW = "iisys"; */
	
	private HttpAutomationClient client;
	private Session session;
	private String userId;
	
	public NuxeoConnector() throws Exception {
		this(PortletProps.get(NuxeoConnector.NUXEO_USER_NAME_PROP), PortletProps.get(NuxeoConnector.NUXEO_USER_PW_PROP));
	}
	
	public NuxeoConnector(String username, String userPw) throws Exception {
		client = new HttpAutomationClient(PortletProps.get(NUXEO_URL)+"/site/automation");
		session = client.getSession(username, userPw);
		this.userId = username;
	}
	
	/**
	 * TODO: For future folder chose. Not working right now!
	 */
	public List<String> getFolders() {
		List<String> paths = new ArrayList<String>();
		try {			
			Documents docs = (Documents) session.newRequest("Document.Query")
					.setHeader(Constants.HEADER_NX_SCHEMAS, "*")
					.set("query", "SELECT * FROM Document WHERE "+
								"(ecm:path STARTSWITH '"+NUXEO_PATH_WORKSPACES+"'"+
									" OR ecm:path STARTSWITH '"+NUXEO_PATH_USERWORKSPACES+"/"+this.userId+"')"+
								" AND ecm:mixinType = 'Folderish'")
					.execute();

			for(int i=0; i<docs.size(); i++) {
//				System.out.println(docs.get(i).getPath());
				paths.add(docs.get(i).getPath());
			}
			
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			session.close();
			session = null;
			client.shutdown();
			client = null;
		}
		return paths;
	}
	
	public void createDocument(String fileName, File file, String author) {
		createDocument(fileName,file,author,"");
	}
	
	public void createDocument(String fileName, File file, String author, String targetFolder) {
//		HttpAutomationClient client = new HttpAutomationClient(PortletProps.get(NUXEO_URL)+"/site/automation");		
//		Session session = client.getSession(PortletProps.get(NUXEO_USER_NAME_PROP), PortletProps.get(NUXEO_USER_PW_PROP));
		
/*		// Or use SSO authentication scheme:
		client.setRequestInterceptor(new PortalSSOAuthInterceptor("myNuxeoSecretKey", NUXEOuserName));
		Session session = client.getSession(); */
		
		try {
			Document root = null;
			if(targetFolder.equals("") || targetFolder.equals("personal_none") || targetFolder.equals("public_none")
					|| targetFolder.equals("personal")) {	
				// Fetch the root of Nuxeo repository
				root = (Document) session.newRequest("Document.Fetch")
						.set("value", NUXEO_PATH_USERWORKSPACES+"/"+this.userId)
						.execute();
			} else {
				String path = "";
				if(targetFolder.startsWith("public_")) {
					path = NUXEO_PATH_WORKSPACES + "/" + targetFolder.substring("public_".length());
				} else if(targetFolder.startsWith("personal_")) {
					path = NUXEO_PATH_USERWORKSPACES+"/"+this.userId+"/"+targetFolder.substring("personal_".length());
				} else {
					return;
				}

				// Fetch the root of Nuxeo repository
				root = (Document) session.newRequest("Document.Fetch")
						.set("value", path+"/")
						.execute();
			}
			// Instantiate a new Document with the simple constructor
			Document document = new Document(fileName, "File");
			document.set("dc:title", fileName);
			document.set("dc:description", "by Liferay Asset Export Tool");
			document.set("dc:author", this.userId);

			 
			// Create a document of File type by setting the parameter 'properties' with String metadata values delimited by comma ','
			document = (Document) session.newRequest("Document.Create")
					.setHeader(Constants.HEADER_NX_SCHEMAS, "*")
					.setInput(root)
					.set("type", document.getType())
					.set("name", fileName)
					.set("properties", document)
				.execute();
			
			FileBlob fb = new FileBlob(file);
			fb.setMimeType(MimeTypesUtil.getContentType(file));
			// uploading a file will return null since we used HEADER_NX_VOIDOP
			session.newRequest("Blob.Attach")
						.setHeader(Constants.HEADER_NX_VOIDOP, "true")
						.setInput(fb)
						.set("document", document.getId())
					.execute();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
			session = null;
			client.shutdown();
			client = null;
		}
	}
	
	/*
	public static void postToRestApi(String url, String fileName, File file) {
		String crlf = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";
		
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setUseCaches(false);
			con.setDoOutput(true);
			
			con.setRequestMethod("POST");
			Timestamp stamp = new Timestamp(System.currentTimeMillis());
//			con.setRequestProperty( "X-Batch-Id", String.valueOf(stamp.getTime()) );
			con.setRequestProperty( "X-Batch-Id", "123" );
			con.setRequestProperty("X-File-Idx", "0");
			con.setRequestProperty("X-File-Name", fileName);
			
			DataOutputStream request = new DataOutputStream(con.getOutputStream());
			request.writeBytes(twoHyphens + boundary + crlf);
			request.writeBytes("Content-Disposition: form-data; name=\"" + fileName + "\";filename=\"" + fileName + "\"" + crlf);
			request.writeBytes(crlf);
			
			FileInputStream fileStream = new FileInputStream(file);
			byte[] b = new byte[(int)file.length()];
			fileStream.read(b, 0, b.length);
			
			request.write(b);
			request.writeBytes(crlf);
			request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
			
			fileStream.close();
			con.disconnect();
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} */
}
