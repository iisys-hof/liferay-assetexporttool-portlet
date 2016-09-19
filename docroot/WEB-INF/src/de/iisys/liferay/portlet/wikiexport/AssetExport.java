package de.iisys.liferay.portlet.wikiexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.liferay.portlet.wiki.model.WikiPage;
import com.liferay.portlet.wiki.service.WikiPageLocalServiceUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.liferay.util.portlet.PortletProps;

public class AssetExport extends MVCPortlet {
	
	private final String FILENAME_MAX_LENGTH = "filename.max.length";
//	private final int FILENAME_MAX_LENGTH = 150;
	
	private ThemeDisplay themeDisplay;

	public AssetExport() { }
	
	public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		System.out.println("AssetExport Render()");
		themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);
		try {
			themeDisplay.setScopeGroupId(themeDisplay.getLayout().getScopeGroup().getPrimaryKey());
		} catch (PortalException | SystemException e) {
			e.printStackTrace();
		}
		
		
		super.render(request, response);
	}
	
	public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
		String targetFormat = request.getParameter("exportTo");
		String target = request.getParameter("exportTarget");
		String dir = request.getParameter("chosenDir");
		
		String assetSearchPKs = request.getParameter("assetLinksSearchContainerPrimaryKeys");
		if(!assetSearchPKs.equals("") && assetSearchPKs!=null) {
			List<String> wikiPagesIds = new ArrayList<String>( Arrays.asList(assetSearchPKs.split(",")) );
			
			List<WikiPage> pages = new ArrayList<WikiPage>();
			List<JournalArticle> journals = new ArrayList<JournalArticle>();
			List<BlogsEntry> blogs = new ArrayList<BlogsEntry>();
			for(String sWP : wikiPagesIds) {
				
				// asset tests
				try {
					AssetEntry ae = AssetEntryLocalServiceUtil.getAssetEntry(Long.parseLong(sWP));
					
					System.out.println("Title: "+ae.getTitleCurrentValue()+" - PK:"+ae.getClassPK());
					
					// Wiki Page:
					if(ae.getClassName().equals(WikiPage.class.getName())) {
						try {
							WikiPage theWP = WikiPageLocalServiceUtil.getPage(ae.getClassPK());
							pages.add(theWP);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (PortalException e) {
							e.printStackTrace();
						}
					}
					// Web Content Article:
					else if(ae.getClassName().equals(JournalArticle.class.getName())) {
						JournalArticle jour = JournalArticleLocalServiceUtil.getArticlesByResourcePrimKey(ae.getClassPK()).get(0);
						journals.add(jour);
					}
					// Blogs Entry:
					else if(ae.getClassName().equals(BlogsEntry.class.getName())) {
						BlogsEntry blog = BlogsEntryLocalServiceUtil.getBlogsEntry(ae.getClassPK());
						blogs.add(blog);
					}
					
				} catch (NumberFormatException e1) { e1.printStackTrace();
				} catch (PortalException e1) { e1.printStackTrace();
				} catch (SystemException e1) { e1.printStackTrace();
				}
				// END asset tests
			} 
			
			File docFile = null;
			
			String fileName = "";
			List<String> contents = new ArrayList<String>();
			List<String> syntax = new ArrayList<String>();
			List<String> authors = new ArrayList<String>();
			
			int i=0;
			for(WikiPage wp : pages) {
				String content = "";
				if(i>0) {
					fileName += "_";
					content += WikiParser.NEW_PAGE;
				}
				fileName += wp.getTitle();
//				content += "="+wp.getTitle()+"=" + "\n" + wp.getContent();
				contents.add(content+"="+wp.getTitle()+"=" + "\n" + wp.getContent());
				syntax.add(WikiParser.SYNTAX_CREOLE);
				authors.add(wp.getUserName());
				i++;
			}
			for(JournalArticle ja : journals) {
				String content = "";
				if(i>0) {
					fileName += "_";
					content += WikiParser.NEW_PAGE;
				}
//				fileName += ja.getTitle().replaceAll("\\<.*?>", ""); // title without html tags
				fileName += ja.getTitleCurrentValue();
//				content += JournalContentUtil.getContent(themeDisplay.getScopeGroupId(), ja.getArticleId(), null, themeDisplay.getLanguageId(), themeDisplay);
				contents.add(content+JournalContentUtil.getContent(themeDisplay.getScopeGroupId(), ja.getArticleId(), null, themeDisplay.getLanguageId(), themeDisplay));
				syntax.add(WikiParser.SYNTAX_HTML);
				authors.add(ja.getUserName());
				i++;
			}
			for(BlogsEntry blog : blogs) {
				String content = "";
				if(i>0) {
					fileName += "_";
					content += WikiParser.NEW_PAGE;
				}
				fileName += blog.getTitle();
				contents.add(content+blog.getContent());
				syntax.add(WikiParser.SYNTAX_HTML);
				authors.add(blog.getUserName());
			}
			
			int fileNameMaxLength = Integer.valueOf(PortletProps.get(FILENAME_MAX_LENGTH));
			if(fileName.length() >= fileNameMaxLength)
				fileName = fileName.substring(0, fileNameMaxLength);
			
//			if(content!="") {
			if(contents.size()>0) {
				if(targetFormat.equals("pdf")) {
					docFile = WikiParser.createPDF(fileName, contents, syntax, authors, themeDisplay.getUser().getFullName());
//					docFile = ExportParser.createPDFfromHtml(fileName, content, authors, themeDisplay.getUser().getFullName());
//					docFile = WikiParser.createPDF(fileName, content, authors, themeDisplay.getUser().getFullName());
				}
				if(targetFormat.equals("docx")) {
					docFile = WikiParser.createDOCX(fileName, contents, syntax, authors, themeDisplay.getUser().getFullName(), getPortletContext().getRealPath("/")+"templates/");
//					docFile = WikiParser.createDOCX(fileName, content, authors, themeDisplay.getUser().getFullName(), getPortletContext().getRealPath("/")+"templates/");
				}
				
	//			String SRC_TYPE = "html"; 
	//			String TARGET_TYPE = "pdf";
	//			docFile = this.openOfficeConvert(SRC_TYPE, TARGET_TYPE, String.valueOf( pages.get(0).getPageId() ), content);
				
				if(docFile != null) {
					if(target.equals("download")) { // download file:
						InputStream fileIs = new FileInputStream(docFile);
						try {
							ServletResponseUtil.sendFile(
									PortalUtil.getHttpServletRequest(request), 
									PortalUtil.getHttpServletResponse(response), 
									fileName+"."+targetFormat, 
									fileIs, 
									MimeTypesUtil.getContentType(docFile));
							System.out.println("Successfully created File '"+fileName+"."+targetFormat+"' and sent to download.");
						} catch (IOException ioE) {
							ioE.printStackTrace();
						}
					} else if(target.equals("dms")) { // send file to nuxeo						
						String userId = themeDisplay.getUser().getScreenName();
						String password = (String)PortalUtil.getHttpServletRequest(request).getSession().getAttribute(WebKeys.USER_PASSWORD);						
						NuxeoConnector nuxeoCon;
						if(password!=null)
							nuxeoCon = new NuxeoConnector(userId, password);
						else
							nuxeoCon = new NuxeoConnector();
						nuxeoCon.createDocument(
								fileName+"."+targetFormat,
								docFile, 
								authors.get(0), 
								dir
						);
						System.out.println("Successfully created File '"+fileName+"."+targetFormat+"' and sent to Nuxeo.");
					}
				}
			}
		}

	}
	
	public void serveResource(ResourceRequest request, ResourceResponse response)
		throws IOException, PortletException {
		
		String userId = themeDisplay.getUser().getScreenName();
		String password = (String)PortalUtil.getHttpServletRequest(request).getSession().getAttribute(WebKeys.USER_PASSWORD);
		
		NuxeoConnector nc;
		if(password!=null)
			nc = new NuxeoConnector(userId, password);
		else
			nc = new NuxeoConnector();

		List<String> paths = nc.getFolders();
		
		JSONObject json = JSONFactoryUtil.createJSONObject();
		JSONArray personalWorkspace = JSONFactoryUtil.createJSONArray();
		json.put("personalDir", personalWorkspace);
		JSONArray publicWorkspaces = JSONFactoryUtil.createJSONArray();
		json.put("publicDir", publicWorkspaces);
		
		for(String path : paths) {
			if(path.startsWith(NuxeoConnector.NUXEO_PATH_WORKSPACES))
				publicWorkspaces.put(path.substring(NuxeoConnector.NUXEO_PATH_WORKSPACES.length()+1));
			else if(path.startsWith(NuxeoConnector.NUXEO_PATH_USERWORKSPACES))
				personalWorkspace.put(path.substring(NuxeoConnector.NUXEO_PATH_USERWORKSPACES.length()+1+userId.length()+1));
		}
		
		writeJSON(request, response, json);
	}

	/*
	private File openOfficeConvert(String sourceExtension, String targetExtension, String id, String content) {
		File file = null;
		
		try {
			InputStream is = new UnsyncByteArrayInputStream(content.getBytes(StringPool.UTF8));
			
			file = DocumentConversionUtil.convert(id, is, sourceExtension, targetExtension);
		} catch (UnsupportedEncodingException uEE) {
			uEE.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return file;
	} */
}
