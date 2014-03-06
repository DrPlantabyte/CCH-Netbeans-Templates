/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cch.templates.java;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


@TemplateRegistration(folder = "Project/Samples/Standard", displayName = "#PortableJar_displayName", description = "PortableJarDescription.html", iconBase = "cch/templates/java/PortableJar.png", content = "PortableJarProject.zip")
@Messages("PortableJar_displayName=Portable Jar App")
public class PortableJarWizardIterator implements WizardDescriptor./*Progress*/InstantiatingIterator {

	private final String appNameSymbol = "__PROJECTNAME__";
	private final String appMainSymbol = "//__PROJECTMAINMETHOD__";
	private final String propertyNameSymbol = "PortableJarNetbeansTemplate";
	
	private int index;
	private WizardDescriptor.Panel[] panels;
	private WizardDescriptor wiz;

	public PortableJarWizardIterator() {
	}

	public static PortableJarWizardIterator createIterator() {
		return new PortableJarWizardIterator();
	}

	private WizardDescriptor.Panel[] createPanels() {
		return new WizardDescriptor.Panel[]{
			new PortableJarWizardPanel(),};
	}

	private String[] createSteps() {
		return new String[]{
			NbBundle.getMessage(PortableJarWizardIterator.class, "LBL_CreateProjectStep")
		};
	}
	/** creates the project files */
	public Set/*<FileObject>*/ instantiate(/*ProgressHandle handle*/) throws IOException {
		Set<FileObject> resultSet = new LinkedHashSet<FileObject>();
		File dirF = FileUtil.normalizeFile((File) wiz.getProperty("projdir"));
		dirF.mkdirs();

		FileObject template = Templates.getTemplate(wiz);
		FileObject dir = FileUtil.toFileObject(dirF);
		unZipFile(template.getInputStream(), dir);

		// Always open top dir as a project:
		resultSet.add(dir);
		// Look for nested projects to open as well:
		Enumeration<? extends FileObject> e = dir.getFolders(true);
		while (e.hasMoreElements()) {
			FileObject subfolder = e.nextElement();
			if (ProjectManager.getDefault().isProject(subfolder)) {
				resultSet.add(subfolder);
			}
		}

		File parent = dirF.getParentFile();
		if (parent != null && parent.exists()) {
			ProjectChooser.setProjectsFolder(parent);
		}

		// modify the files and generate code
		// code generation
		String projName = wiz.getProperty("name").toString();
		String className = projName.replaceAll("\\W", "");
		if(Character.isLetter(className.charAt(0)) == false){className = "J"+className;}
		projName = projName.toLowerCase(Locale.US).replaceAll("\\W", "");
		if(Character.isLetter(projName.charAt(0)) == false){projName = "j"+projName;}
		File projectMainFile = new File(dirF.getPath() + File.separator + "src" + File.separator + projName + File.separator + className+".java");
		try{
			projectMainFile.getParentFile().mkdirs();
			BufferedWriter w = new BufferedWriter(new FileWriter(projectMainFile));
			w.write("package " + projName + ";\n\n"
					+ "public class "+className+" {\n\n"
					+ "\tpublic static void main(String[] args){\n"
					+ "\t\t//TODO: implementation\n"
					+ "\t}\n\n"
					+ "}\n");
			w.flush();
			w.close();
			// template substitution
			List<String> allLines = Files.readAllLines((new File(dirF.getPath() + File.separator + "src" 
					+ File.separator + "main" + File.separator + "Main.java")).toPath(), Charset.forName("UTF-8"));
			for(int i = 0; i < allLines.size(); i++){
				allLines.set(i, allLines.get(i).replace(appNameSymbol, wiz.getProperty("name").toString()).replace(appMainSymbol, projName+"."+className+".main(args);"));
			}
			Files.write((new File(dirF.getPath() + File.separator + "src" 
					+ File.separator + "main" + File.separator + "Main.java")).toPath(),allLines, Charset.forName("UTF-8"));
			File propertiesFile = (new File(dirF.getPath() + File.separator + "nbproject" 
					+ File.separator + "project.properties"));
			List<String> allLines2 = Files.readAllLines(propertiesFile.toPath(), Charset.forName("UTF-8"));
			for(int i = 0; i < allLines2.size(); i++){
				allLines2.set(i, allLines2.get(i).replace(propertyNameSymbol, className));
			}
			Files.write(propertiesFile.toPath(),allLines2, Charset.forName("UTF-8"));
			
			
		}catch(IOException ex){
			ex.printStackTrace(System.err);
		}
		
		return resultSet;
	}

	public void initialize(WizardDescriptor wiz) {
		this.wiz = wiz;
		index = 0;
		panels = createPanels();
		// Make sure list of steps is accurate.
		String[] steps = createSteps();
		for (int i = 0; i < panels.length; i++) {
			Component c = panels[i].getComponent();
			if (steps[i] == null) {
                // Default step name to component name of panel.
				// Mainly useful for getting the name of the target
				// chooser to appear in the list of steps.
				steps[i] = c.getName();
			}
			if (c instanceof JComponent) { // assume Swing components
				JComponent jc = (JComponent) c;
                // Step #.
				// TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
				jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
				// Step name (actually the whole list for reference).
				jc.putClientProperty("WizardPanel_contentData", steps);
			}
		}
	}

	public void uninitialize(WizardDescriptor wiz) {
		this.wiz.putProperty("projdir", null);
		this.wiz.putProperty("name", null);
		this.wiz = null;
		panels = null;
	}

	public String name() {
		return MessageFormat.format("{0} of {1}",
				new Object[]{new Integer(index + 1), new Integer(panels.length)});
	}

	public boolean hasNext() {
		return index < panels.length - 1;
	}

	public boolean hasPrevious() {
		return index > 0;
	}

	public void nextPanel() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		index++;
	}

	public void previousPanel() {
		if (!hasPrevious()) {
			throw new NoSuchElementException();
		}
		index--;
	}

	public WizardDescriptor.Panel current() {
		return panels[index];
	}

	// If nothing unusual changes in the middle of the wizard, simply:
	public final void addChangeListener(ChangeListener l) {
	}

	public final void removeChangeListener(ChangeListener l) {
	}

	private static void unZipFile(InputStream source, FileObject projectRoot) throws IOException {
		try {
			ZipInputStream str = new ZipInputStream(source);
			ZipEntry entry;
			while ((entry = str.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					FileUtil.createFolder(projectRoot, entry.getName());
				} else {
					FileObject fo = FileUtil.createData(projectRoot, entry.getName());
					if ("nbproject/project.xml".equals(entry.getName())) {
						// Special handling for setting name of Ant-based projects; customize as needed:
						filterProjectXML(fo, str, projectRoot.getName());
					} else {
						writeFile(str, fo);
					}
				}
			}
		} finally {
			source.close();
		}
	}

	private static void writeFile(ZipInputStream str, FileObject fo) throws IOException {
		OutputStream out = fo.getOutputStream();
		try {
			FileUtil.copy(str, out);
		} finally {
			out.close();
		}
	}

	private static void filterProjectXML(FileObject fo, ZipInputStream str, String name) throws IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FileUtil.copy(str, baos);
			Document doc = XMLUtil.parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())), false, false, null, null);
			NodeList nl = doc.getDocumentElement().getElementsByTagName("name");
			if (nl != null) {
				for (int i = 0; i < nl.getLength(); i++) {
					Element el = (Element) nl.item(i);
					if (el.getParentNode() != null && "data".equals(el.getParentNode().getNodeName())) {
						NodeList nl2 = el.getChildNodes();
						if (nl2.getLength() > 0) {
							nl2.item(0).setNodeValue(name);
						}
						break;
					}
				}
			}
			OutputStream out = fo.getOutputStream();
			try {
				XMLUtil.write(doc, out, "UTF-8");
			} finally {
				out.close();
			}
		} catch (Exception ex) {
			Exceptions.printStackTrace(ex);
			writeFile(str, fo);
		}

	}

}
