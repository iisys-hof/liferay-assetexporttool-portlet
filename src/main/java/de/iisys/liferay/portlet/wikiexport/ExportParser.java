package de.iisys.liferay.portlet.wikiexport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

public class ExportParser {
	
	public static final String NEW_PAGE = "\new_page";
	private static final String FORMAT_PDF = "format_pdf";
	private static final String FORMAT_DOCX = "format_docx";
	
	private static XWPFParagraph tempParagraph;
	
	/**
	 * Creates a PDF File from content with wiki syntax (creole), using the iText core library.
	 * iText is licensed as AGPL software.
	 * @param title: document title and future filename
	 * @param content: content in wiki syntax
	 * @param authors: author(s) of the content
	 * @param creator: creator of the future file
	 * @return: returns the created .pdf file
	 */
	public static File createPDF(String title, String content, List<String> authors, String creator) {
		Document doc = new Document();
		File file = new File(title);
		try {
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
			
			for(String author : authors) {
				doc.addAuthor(author);
			}
			doc.addCreator(creator);
			doc.addTitle(title);
			
			doc.open();
			
			parseContentToDocument(FORMAT_PDF, content, doc);
			doc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return file;
	}
	
	public static File createPDFfromHtml(String title, String content, List<String> authors, String creator) {
		Document doc = new Document();
		File file = new File(title);
		try {
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
			
			for(String author : authors) {
				doc.addAuthor(author);
			}
			doc.addCreator(creator);
			doc.addTitle(title);
			
			doc.open();
			
			InputStream is = new ByteArrayInputStream(content.getBytes());
			try {
				XMLWorkerHelper.getInstance().parseXHtml(writer, doc, is);
			} catch (IOException e) {
				e.printStackTrace();
			}
			doc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return file;
	}
	
	/**
	 * Creates a DOCX File from content with wiki syntax (creole), using the Apache POI XWPF library.
	 * Apche POI is licensed under the Apache License, Version 2.0.
	 * @param title: document title and future filename
	 * @param content: content in wiki syntax
	 * @param authors: not supported
	 * @param creator: creator of the future file
	 * @param templatePath: path to the template.docx file
	 * @return: returns the created .docx file
	 */
	public static File createDOCX(String title, String content, List<String> authors, String creator, String templatePath) {
		XWPFDocument doc;
		try {
			doc = new XWPFDocument(OPCPackage.open(new FileInputStream(templatePath+"template.docx")));
			// delete present content in template:
			List<XWPFParagraph> pars = doc.getParagraphs();
			for (int i=(pars.size()-1); i>=0; i--) {
				XWPFParagraph par = pars.get(i);
				doc.removeBodyElement(doc.getPosOfParagraph(par));
			}
		} catch (FileNotFoundException e1) {
			System.out.println("Could not find wikiExport_template.docx");
			doc = new XWPFDocument();
		} catch (IOException e1) {
			System.out.println("Failed to use wikiExport_template.docx");
			doc = new XWPFDocument();
		} catch (InvalidFormatException e) {
			e.printStackTrace();
			return null;
		}
		File file = new File(title);

		POIXMLProperties xmlProps = doc.getProperties();
		POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties();
		coreProps.setCreator(creator);
		coreProps.setTitle(title);
		
		try {
			parseContentToDocument(FORMAT_DOCX, content, doc);
			doc.write(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return file;
	}
	
	/**
	 * Parses the wiki content (creole) and calls the functions to put the right parts
	 * in the document, specified by the targetFormat (e.g. for pdf or docx).
	 * @param targetFormat
	 * @param content
	 * @param doc
	 * @throws DocumentException
	 */
	private static void parseContentToDocument(String targetFormat, String content, Object doc) throws DocumentException {
		ParsingSyntax ps = new ParsingSyntax(ParsingSyntax.CREOLE_SYNTAX);
		String pages[] = content.split(NEW_PAGE);
		
		for(int p=0; p<pages.length; p++) { String page = pages[p];
			
			String lines[] = page.split("\n");
			Object someList = getList(targetFormat);
			
			for(int i=0; i<lines.length; i++) { String line = lines[i];
				
				if(line.equals(""))
					continue;
			
				// Are we in a list?
				if(line.startsWith("*")) {
					line = line.substring(1);
					addToList(targetFormat, line, someList);
					if(i==lines.length-1)
						addList(targetFormat, someList, doc);
					else
						continue;
				} else {
					if(listHasElements(targetFormat, someList)) {
						addList(targetFormat, someList, doc);
						someList = getList(targetFormat);
					}
				}
				
				// Is this line a caption?
				if(line.startsWith(ps.startHeading3())) {
					line = line.substring(3);
					if(line.endsWith(ps.endHeading3()))
						line = line.substring(0, line.length()-3);
					addCaption3(targetFormat, line, doc);
					continue;
				} else if(line.startsWith(ps.startHeading2())) {
					line = line.substring(2);
					if(line.endsWith(ps.endHeading2()))
						line = line.substring(0, line.length()-2);
					addCaption2(targetFormat, line, doc);
					continue;
				} else if(line.startsWith(ps.startHeading1())) {
					if(line.endsWith(ps.endHeading1())) {
						line = line.substring(1, line.length()-1);
						addCaption1(targetFormat, line, doc);
						continue;
					}
				} else if(line.startsWith("{{{")) {
					if(line.endsWith("}}}")) {
						line = line.substring(3, line.length()-3);
					}
				}
				
				// Has this line bold parts?
				String[] lineParts = line.split("\\*\\*");
				boolean lineStartsBold = line.startsWith(ps.startBold());
				if(lineParts.length>2 || (lineParts.length==2 && line.endsWith(ps.endBold())) ) {
					for(int j=0; j<lineParts.length; j++) {
						// if line starts bold, all uneven numbers are the normal ones:
						if( (lineStartsBold && (j%2 == 0)) || (!lineStartsBold && (j%2 != 0)) ) {
							addBoldPhrase(targetFormat, lineParts[j], doc);
						} else {
							// Has this part of the line italic parts?
							String[] lineItalicParts = lineParts[j].split("//");
							boolean lineStartsItalic = lineParts[j].startsWith("//");
							if(lineItalicParts.length>2 || (lineItalicParts.length==2 && lineParts[j].endsWith("//")) ) {
								for(int k=0; k<lineItalicParts.length; k++) {
									if( (lineStartsItalic && (k%2 == 0)) || (!lineStartsItalic && (k%2 != 0)) ) {
										addItalicPhrase(targetFormat, lineItalicParts[k], doc);
									} else {
										if(j==0 && k==0)
											addNormalParagraph(targetFormat, lineItalicParts[k], doc);
										else
											addNormalPhrase(targetFormat, lineItalicParts[k], doc);
									}
								}
							} else {
								if(j==0)
									addNormalParagraph(targetFormat, lineParts[j], doc);
								else
									addNormalPhrase(targetFormat, lineParts[j], doc);
							}
						}
					}
				} else {
					// No bold parts. But has this line italic parts?
					String[] lineItalicParts = line.split("//");
					boolean lineStartsItalic = line.startsWith("//");
					if(lineItalicParts.length>2  || (lineItalicParts.length==2 && line.endsWith("//")) ) {
						for(int k=0; k<lineItalicParts.length; k++) {
							if( (lineStartsItalic && (k%2 == 0)) || (!lineStartsItalic && (k%2 != 0)) ) {
								addItalicPhrase(targetFormat, lineItalicParts[k], doc);
							} else {
								if(k==0)
									addNormalParagraph(targetFormat, lineItalicParts[k], doc);
								else
									addNormalPhrase(targetFormat, lineItalicParts[k], doc);
							}
						}
					} else {
						// neither bold nor italic parts
						addNormalParagraph(targetFormat, line, doc);
					}
				}
			}
			
			if(p<pages.length-1)
				newPage(targetFormat, doc);
		}
	}
	
	private static void newPage(String targetFormat, Object target) {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.newPage();
		} else if(targetFormat == FORMAT_DOCX) {
			XWPFDocument doc = (XWPFDocument)target;
			XWPFParagraph par = doc.createParagraph();
			par.setPageBreak(true);
		}
	}
	
	private static void addCaption1(String targetFormat, String caption, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Paragraph(caption, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
		} else if(targetFormat == FORMAT_DOCX) {
			XWPFDocument doc = (XWPFDocument)target;
			XWPFParagraph par = doc.createParagraph();
			par.setStyle("Heading1");
			par.createRun().setText(caption);
		}
	}
	
	private static void addCaption2(String targetFormat, String caption, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Paragraph(caption, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
		} else if(targetFormat == FORMAT_DOCX) {
			XWPFDocument doc = (XWPFDocument)target;
			XWPFParagraph par = doc.createParagraph();
			par.setStyle("Heading2");
			par.createRun().setText(caption);
		}
	}
	
	private static void addCaption3(String targetFormat, String caption, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Paragraph(caption, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
		} else if(targetFormat == FORMAT_DOCX) {
			XWPFDocument doc = (XWPFDocument)target;
			XWPFParagraph par = doc.createParagraph();
			par.setStyle("Heading3");
			par.createRun().setText(caption);
		}
	}
	
	private static void addList(String targetFormat, Object list, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			com.itextpdf.text.List theList = (com.itextpdf.text.List)list;
			doc.add(theList);
		} else if(targetFormat == FORMAT_DOCX) {
			System.out.println("addList() FORMAT_DOCX");
			XWPFDocument doc = (XWPFDocument)target;
			
			List bulletList = (List)list;
			for(Object o : bulletList) {
				String listText = (String)o;
				
				XWPFNumbering numbering = doc.createNumbering();
				// if 0 does not work as abstractNumId, look into your word template's file numbering.xml for it.
				// 0 = bullet list, 1 = numbered list
				BigInteger numId = numbering.addNum(BigInteger.valueOf(0));
				XWPFParagraph par = doc.createParagraph();
				par.setNumID(numId);
				par.setStyle("ListParagraph");
				par.getCTP().getPPr().getNumPr().addNewIlvl().setVal(BigInteger.valueOf(0));
				
				par.createRun().setText(listText);
			}
		}
	}
	
	private static void addToList(String targetFormat, String content, Object list) {
		if(targetFormat == FORMAT_PDF) {
			com.itextpdf.text.List theList = (com.itextpdf.text.List)list;
			theList.add(content);
		} else if(targetFormat == FORMAT_DOCX) {
			System.out.println("addToList() FORMAT_DOCX");
			List bulletList = (List)list;
			bulletList.add(content);
		}
	}
	
	private static boolean listHasElements(String targetFormat, Object list) {
		if(targetFormat == FORMAT_PDF) {
			com.itextpdf.text.List theList = (com.itextpdf.text.List)list;
			return (!theList.isEmpty());
		} else if(targetFormat == FORMAT_DOCX) {
			ArrayList<String> bulletList = (ArrayList<String>)list;
			return (!bulletList.isEmpty());
		}
		return false;
	}
	
	private static void addNormalParagraph(String targetFormat, String text, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, 12)));
		} else if(targetFormat == FORMAT_DOCX) {
			XWPFDocument doc = (XWPFDocument)target;
			tempParagraph = doc.createParagraph();
			XWPFRun run = tempParagraph.createRun();
			run.setText(text);
		}
	}
	
	private static void addNormalPhrase(String targetFormat, String text, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 12)));
		}  else if(targetFormat == FORMAT_DOCX) {
			if(tempParagraph==null) {
				XWPFDocument doc = (XWPFDocument)target;
				tempParagraph = doc.createParagraph();
			}
			XWPFRun run = tempParagraph.createRun();	
			run.setText(text);
		}
	}
	
	private static void addBoldPhrase(String targetFormat, String text, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
		}  else if(targetFormat == FORMAT_DOCX) {
			if(tempParagraph==null) {
				XWPFDocument doc = (XWPFDocument)target;
				tempParagraph = doc.createParagraph();
			}
			XWPFRun run = tempParagraph.createRun();	
			run.setText(text);
			run.setBold(true);
		}
	}
	
	private static void addItalicPhrase(String targetFormat, String text, Object target) throws DocumentException {
		if(targetFormat == FORMAT_PDF) {
			Document doc = (Document)target;
			doc.add(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12)));
		}  else if(targetFormat == FORMAT_DOCX) {
			if(tempParagraph==null) {
				XWPFDocument doc = (XWPFDocument)target;
				tempParagraph = doc.createParagraph();
			}
			XWPFRun run = tempParagraph.createRun();	
			run.setText(text);
			run.setItalic(true);
		}
	}

	private static Object getList(String targetFormat) {
		if(targetFormat == FORMAT_PDF) {
			return getiTextList();
		}  else if(targetFormat == FORMAT_DOCX) {
			return getXWPFList();
		}
		return null;
	}
	
	// iText specific parts:
	
	private static com.itextpdf.text.List getiTextList() {
		com.itextpdf.text.List list = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED, 10);
		list.setListSymbol("\u2022");
		list.setIndentationLeft(10);
		return list;
	}
	
	// Apache POI XWPF specific parts:
	
	private static ArrayList<String> getXWPFList() {
		return new ArrayList<String>();
	}
}
