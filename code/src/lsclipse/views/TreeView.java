/* 
*    Ref-Finder
*    Copyright (C) <2015>  <PLSE_UCLA>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package lsclipse.views;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import changetypes.CodeLineRetriever;
import changetypes.CodeSegment;
import changetypes.CodeSegment.LineSegment;
import lsclipse.LSDiffRunner;
import lsclipse.TopologicalSort;
import lsclipse.dialogs.ConfirmProjectPathDialog;
import lsclipse.dialogs.ProgressBarDialog;
import lsclipse.dialogs.SelectProjectDialog;
import lsclipse.linegetter.LineGetterFactory;
import lsclipse.utils.CsvWriter;
import metapackage.MetaInfo;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class TreeView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "lsclipse.views.TreeView";
	
	public static final int NUM_THREADS = 4;
	public static final long TIMEOUT = 60; // wait 60 minutes for line retriever

	private List viewer;
	private List list;
	private Action doubleClickTreeAction;
	private Action doubleClickListAction;
	private Action selectAction;
	private Action countAction;
	private Action selectUccAction;
	private String uccFilePath;
	private Composite parent;
	private Vector<Node> nodeList;
	private Map<String, Node> allNodes;
	private HashMap<String, Node> hashNode;
	HashMap<String, Node> strNodeRelation;
	GridData layoutData1;
	ArrayList<EditorInput> listDiffs = new ArrayList<EditorInput>();
	IProject baseproj = null;
	IProject newproj = null;
	CodeLineRetriever lineRetriever;

	/**
	 * The constructor.
	 */
	public TreeView() {
		nodeList = new Vector<Node>();
		hashNode = new HashMap<String, Node>();
		allNodes = new HashMap<String, Node>();
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);

		layoutData1 = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		layoutData1.grabExcessHorizontalSpace = true;
		layoutData1.grabExcessVerticalSpace = true;
		layoutData1.horizontalAlignment = GridData.FILL;
		layoutData1.verticalAlignment = GridData.FILL;
		layoutData1.exclude = false;

		this.parent = parent;
		viewer = new List(this.parent, SWT.SINGLE | SWT.V_SCROLL);
		list = new List(this.parent, SWT.SINGLE | SWT.V_SCROLL);
		viewer.setLayoutData(layoutData1);
		list.setLayoutData(layoutData1);

		parent.layout();

		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();

		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		mgr.add(selectAction);
		mgr.add(selectUccAction);
		mgr.add(countAction);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// manager.add(dummyNodeAction);
		// manager.add(new Separator());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
	}

	class EditorInput extends CompareEditorInput {
		public EditorInput(CompareConfiguration configuration) {
			super(configuration);
		}

		String base_;
		String mod_;

		public void setBase(InputStream inputStream) {
			try {
				base_ = convertToString(inputStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void setBase(String s) {
			base_ = s;
		}

		private String convertToString(InputStream is) throws IOException {
			final char[] buffer = new char[0x10000];
			StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(is, "UTF-8");
			int read;
			while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
				if (read > 0) {
					out.append(buffer, 0, read);
				}
			}
			return out.toString();
		}

		public void setMod(InputStream inputStream) {
			try {
				mod_ = convertToString(inputStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void setMod(String s) {
			mod_ = s;
		}

		@Override
		public Object prepareInput(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			Differencer d = new Differencer();
			Object diff = d.findDifferences(false, new NullProgressMonitor(),
					null, null, new Input(base_), new Input(mod_));
			return diff;
		}

		class Input implements ITypedElement, IStreamContentAccessor {
			String fContent;

			public Input(String s) {
				fContent = s;
			}

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "name";
			}

			@Override
			public String getType() {
				// TODO Auto-generated method stub
				return "java";
			}

			@Override
			public InputStream getContents() throws CoreException {
				// TODO Auto-generated method stub
				return new ByteArrayInputStream(fContent.getBytes());
			}

			@Override
			public Image getImage() {
				// TODO Auto-generated method stub
				return null;
			}
		}
	}

	private void makeActions() {
		// Double click a Node to open the associated file.
		doubleClickListAction = new Action() {
			public void run() {
				int selind = list.getSelectionIndex();
				if (selind >= listDiffs.size())
					return;
				EditorInput file = listDiffs.get(selind);
				if (file == null)
					return;

				CompareUI.openCompareEditor(file);
			}
		};

		// Double click a refactoring to display the associated information.
		doubleClickTreeAction = new Action() {
			public void run() {

				// clear list
				list.removeAll();
				listDiffs.clear();

				// Print out the list of code elements associated with this
				// refactoring
				int index = viewer.getSelectionIndex();
				if (index < 0 || index >= nodeList.size())
					return;

				Node node = nodeList.get(index);

				list.add(node.getName());
				listDiffs.add(null);
				list.add(node.params);
				listDiffs.add(null);
				// Seperator
				list.add(" ");
				listDiffs.add(null);

				int numtabs = 0;

				CodeLineRetriever lineRetriever = new CodeLineRetriever(LSDiffRunner.getOldEntityLineMap(), LSDiffRunner.getNewEntityLineMap());
				for (String statement : node.getDependents()) {
					StringBuilder output = new StringBuilder();
					if (statement.equals(")"))
						--numtabs;

					for (int i = 0; i < numtabs; ++i) {
						output.append("\t");
					}
					if (statement.equals("(")) {
						output.append("(");
						++numtabs;
					} else {
						output.append(lineRetriever.retrieve(statement));
						output.append(" ");
						output.append(statement);
					}
					list.add(output.toString());
					listDiffs.add(null);
				}

				if (!node.oldFacts.isEmpty()) {
					list.add("");
					listDiffs.add(null);
					list.add("Compare:");
					listDiffs.add(null);
					for (String display : node.oldFacts.keySet()) {
						IJavaElement filenameBase = node.oldFacts.get(display);
						IJavaElement filenameMod = node.newFacts.get(display);
						list.add(display);
						EditorInput ei = new EditorInput(
								new CompareConfiguration());
						try {
							ei.setBase(((IFile) filenameBase
									.getCorrespondingResource()).getContents());
						} catch (Exception ex) {
							ei.setBase("");
						}
						try {
							ei.setMod(((IFile) filenameMod
									.getCorrespondingResource()).getContents());
						} catch (Exception ex) {
							ei.setMod("");
						}
						listDiffs.add(ei);
					}
				}

			}
		};
		// Select Action
		selectAction = new Action("Select version...") {
			public void run() {
				// collect information from seldiag
				final SelectProjectDialog seldiag = new SelectProjectDialog(
						parent.getShell());
				final int returncode = seldiag.open();
				if (returncode > 0)
					return;

				long start = System.currentTimeMillis();

				// remember base project (and new project)
				baseproj = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(seldiag.getProj1());
				newproj = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(seldiag.getProj2());

				// open new log box
				final ProgressBarDialog pbdiag = new ProgressBarDialog(
						parent.getShell());
				pbdiag.open();
				pbdiag.setStep(0);

				// do lsdiff (ish)
				if ((new LSDiffRunner()).doFactExtractionForRefFinder(
						seldiag.getProj1(), seldiag.getProj2(), pbdiag)) {
					refreshTree();
				} else {
					System.out
							.println("Something went wrong - fact extraction failed");
				}
				pbdiag.setStep(5);
				pbdiag.setMessage("Cleaning up... ");
				pbdiag.appendLog("OK\n");

				pbdiag.dispose();
				long end = System.currentTimeMillis();
				System.out.println("Total time: " + (end - start));
			}
		};
		selectAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
				
		// Select UCC exe Action
		selectUccAction = new Action("Browse UCC.exe") {
			public void run() {
				final FileDialog dialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
				dialog.setText("Select UCC executable file");
				dialog.setFilterExtensions(new String[] {"*.exe","*.*"});
				String uccPath = dialog.open();
				uccFilePath = uccPath;
				this.setImageDescriptor(PlatformUI.getWorkbench()
					.getSharedImages()
					.getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
			}
		};
		selectUccAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_ADD));

		// Count Action
		countAction = new Action("Count refactoring SLOC") {
			public void run() {
				// Refactoring detection should have been run
				if (baseproj == null || newproj == null)
					selectAction.run();
				
				// Browse UCC executable file if needed
				if (uccFilePath == null || uccFilePath.isEmpty())
					selectUccAction.run();

				long start = System.currentTimeMillis();

				// open new log box
				final ProgressBarDialog pbdiag = new ProgressBarDialog(
						parent.getShell());
				pbdiag.open();
				pbdiag.setText("UCC counting...");
				
				// Retrieving code lines and write to CSV
				pbdiag.setMessage("Retrieving code lines...\n");
				CodeLineRetriever lineRetriever = new CodeLineRetriever(LSDiffRunner.getOldEntityLineMap(), LSDiffRunner.getNewEntityLineMap());
				Vector<String> lines = new Vector<String>();
				ExecutorService execService = Executors.newFixedThreadPool(NUM_THREADS);
				java.util.List<Future<Vector<String>>> futures = new java.util.LinkedList<Future<Vector<String>>>();
				for (Node node : nodeList) {
					NodeLineGetter nlg = new NodeLineGetter(node, lineRetriever);
					futures.add(execService.submit(nlg));
				}
				execService.shutdown();
				try {
					execService.awaitTermination(TIMEOUT, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				for (Future<Vector<String>> f : futures) {
					try {
						lines.addAll(f.get());
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (java.util.concurrent.ExecutionException e) {
						e.printStackTrace();
					}
				}
				pbdiag.appendLog("Retrievation OK! Got " + lines.size() + " line segments\n");

				// Write refactoring lines to CSV
				pbdiag.setMessage("Writing code lines to CSV...\n");
				try {
					FileWriter fw = new FileWriter(MetaInfo.exportLineFile);
					for (String line : lines) {
						fw.append(line);
					}
					fw.flush();
					fw.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				pbdiag.appendLog("Writing done! File: " + MetaInfo.exportLineFile + "\n");

				// Get projects paths
				ConfirmProjectPathDialog dialogPath = new ConfirmProjectPathDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());

				try {
					String path = "";
					IJavaProject javaProject = JavaCore.create(baseproj);
					for (IPackageFragmentRoot packFrag : javaProject.getAllPackageFragmentRoots()) {
						if (packFrag.getKind() == IPackageFragmentRoot.K_SOURCE)
							path = packFrag.getResource().getLocation().toOSString();
					}
					if (!path.isEmpty())
						dialogPath.addBasePath(path);
					path = "";
					javaProject = JavaCore.create(newproj);
					for (IPackageFragmentRoot packFrag : javaProject.getAllPackageFragmentRoots()) {
						if (packFrag.getKind() == IPackageFragmentRoot.K_SOURCE)
							path = packFrag.getResource().getLocation().toOSString();
					}
					if (!path.isEmpty())
						dialogPath.addChangePath(path);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				dialogPath.addBasePath(baseproj.getLocation().toOSString());
				dialogPath.addBasePath(newproj.getLocation().toOSString());

				final int returncode = dialogPath.open();
				if (returncode > 0)
					return;

				String basepath, newpath;
				basepath = dialogPath.getBasePath();
				newpath = dialogPath.getChangePath();

				// Call UCC to count refactoring lines
				pbdiag.setMessage("UCC counting...\n");
				pbdiag.appendLog("+ UCC exec path: " + uccFilePath + "\n");
				pbdiag.appendLog("+ Base project path: " + basepath + "\n");
				pbdiag.appendLog("+ Changed project path: " + newpath + "\n");
				try {
					Process ucc = Runtime.getRuntime().exec(new String[] {
							uccFilePath, "-d", "-dir", basepath, newpath, "*.java",
							"-reffile", MetaInfo.exportLineFile }, null, MetaInfo.uccDir);

					InputStream instream = ucc.getInputStream();
					int size = 0;
					byte[] buffer = new byte[1024];
					while ((size = instream.read(buffer)) != -1) {
						pbdiag.appendLog(new String(buffer, 0, size));
					}
					ucc.waitFor();
					pbdiag.appendLog("Counting done! Results written to file " + MetaInfo.uccCountFile + "\n");
					
					Scanner scanner = new Scanner(new java.io.File(MetaInfo.uccCountFile));
					String sResult = scanner.useDelimiter("\\Z").next();
					scanner.close();
					java.awt.datatransfer.StringSelection stringSelection = new StringSelection(sResult);
					java.awt.datatransfer.Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
					clpbrd.setContents(stringSelection, null);
					MessageBox msg = new MessageBox(parent.getShell(), SWT.ICON_INFORMATION | SWT.OK);
					msg.setMessage("Count results have been copied to clipboard!");
					msg.open();
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}

				pbdiag.dispose();
				long end = System.currentTimeMillis();
				System.out.println("Total time: " + (end - start));				
			}
		};
		countAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_DEF_PERSPECTIVE));
	}

	public void refreshTree() {
		list.removeAll();
		nodeList.clear();
		viewer.removeAll();
		System.out.println(baseproj.toString());
		System.out.println(newproj.toString());

		TopologicalSort tSort = new TopologicalSort();

		long beforetime = System.currentTimeMillis();
		tSort.sort(MetaInfo.refsOnlyFile);
		long aftertime = System.currentTimeMillis();

		Map<String, java.util.List<String>> dependentMap = tSort.dependents;

		Set<String> allchildren = new HashSet<String>();

		for (java.util.List<String> s : dependentMap.values()) {
			allchildren.addAll(s);
		}

		strNodeRelation = new HashMap<String, Node>();
		Set<String> parents = new HashSet<String>();

		for (Entry<String, java.util.List<String>> queryEntry : dependentMap
				.entrySet()) {
			String filledQuery = queryEntry.getKey();
			Node temp = makeNode(filledQuery, queryEntry.getValue(), baseproj,
					newproj);
			hashNode.put("[" + temp.getName() + "]", temp);
			allNodes.put(filledQuery, temp);
			System.out.println(filledQuery);
			strNodeRelation.put(filledQuery, temp);

			nodeList.add(temp);
			viewer.add(temp.getName());
			parents.add(filledQuery);

		}

		// Print Summary Data
		System.out.println("\nTotal time for inference(ms): "
				+ (aftertime - beforetime));
		Set<lsclipse.Node> nodes = tSort.getGraph();
		int totalCount = 0;
		for (lsclipse.Node node : nodes) {
			if (node.numFound() > 0) {
				totalCount += node.numFound();
				System.out.print(node.toString() + ", ");
			}
		}
		System.out.println("\nFor a total of " + totalCount
				+ " refactorings found.");
	}

	private String getName(String filledQuery) {
		int parenthIndex = filledQuery.indexOf('(');
		return filledQuery.substring(0, parenthIndex);
	}

	private Node makeNode(String filledQuery, java.util.List<String> children,
			IProject baseProject, IProject newProject) {
		String name = getName(filledQuery);
		String nicename = name.replace('_', ' ');
		nicename = nicename.substring(0, 1).toUpperCase()
				+ nicename.substring(1);
		int nameIndex = filledQuery.indexOf(name);

		Node temp = new Node(nicename, null);
		temp.setDependents(children);
		temp.setFile("test.java.txt");// ?
		temp.setProjectName("LSclipse");// ?
		temp.params = filledQuery.substring(nameIndex + name.length());
		if (temp.params.length() > 4) {
			String[] params = temp.params
					.substring(2, temp.params.length() - 2).split("\",\"");
			ArrayList<String> fields = new ArrayList<String>();
			ArrayList<String> methods = new ArrayList<String>();
			ArrayList<String> classes = new ArrayList<String>();
			// guess what kind of parameter this is
			for (String s : params) {
				if (s.contains("{")
						|| s.contains("}")
						|| s.contains(";")
						|| s.contains("=")
						|| !s.contains("%")
						|| (s.contains("(") && (s.indexOf("(") < s.indexOf("%")))) { // looks
																						// like
																						// abody
					// body! ignore
				} else if (s.contains("(") && s.contains(")")) { // looks like
																	// method
					methods.add(s);
				} else if (s.contains("#")) { // looks like field
					fields.add(s);
				} else if (s.contains("%.")) { // looks like class
					classes.add(s);
				} else { // may be package or body
					// ignore
				}
			}
			for (String s : methods) {
				int indhex = s.indexOf("#");
				String qualifiedClassName = s.substring(0, indhex);
				temp.oldFacts.put(qualifiedClassName, LSDiffRunner
						.getOldTypeToFileMap().get(qualifiedClassName));
				temp.newFacts.put(qualifiedClassName, LSDiffRunner
						.getNewTypeToFileMap().get(qualifiedClassName));
			}
			for (String s : fields) {
				int indhex = s.indexOf("#");
				String qualifiedClassName = s.substring(0, indhex);
				temp.oldFacts.put(qualifiedClassName, LSDiffRunner
						.getOldTypeToFileMap().get(qualifiedClassName));
				temp.newFacts.put(qualifiedClassName, LSDiffRunner
						.getNewTypeToFileMap().get(qualifiedClassName));
			}
			for (String s : classes) {
				temp.oldFacts.put(s, LSDiffRunner.getOldTypeToFileMap().get(s));
				temp.newFacts.put(s, LSDiffRunner.getNewTypeToFileMap().get(s));
			}
		}
		return temp;
	}

	private void hookDoubleClickAction() {
		viewer.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent arg0) {
				doubleClickTreeAction.run();
			}

			public void mouseDown(MouseEvent arg0) {
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});

		list.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent arg0) {
				doubleClickListAction.run();
			}

			public void mouseDown(MouseEvent arg0) {
			}

			public void mouseUp(MouseEvent arg0) {
			}
		});
	}

	@Override
	public void setFocus() {
	}

	static class NodeLineGetter implements Callable<Vector<String>> {
		Node node;
		CodeLineRetriever retriever;
		
		public NodeLineGetter(Node node, CodeLineRetriever retriever) {
			super();
			this.node = node;
			this.retriever = retriever;
		}
		
		@Override
		public Vector<String> call() throws Exception {
			Vector<String> lines = new Vector<String>();
			String refName = node.getName();
			java.util.List<CodeSegment> segments = LineGetterFactory.returnLineGetterByName(refName)
					.retrieveCode(retriever, node.getDependents());
			for (CodeSegment segment : segments) {
				if (segment == null)
					continue;
				for (LineSegment line : segment.getLines()) {
					lines.add(CsvWriter.buildLine(refName,
							segment.getFile().getResource().getLocation().toOSString(),
							Integer.toString(line.getBeginning()), Integer.toString(line.getEnd())));
				}
			}
			return lines;
		}
	}
}