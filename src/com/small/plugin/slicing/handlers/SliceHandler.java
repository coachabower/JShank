/**
 * Author: Ryan Small
 * 
 * Developed as an assignment for Software Testing 2
 * @Florida Institute of Technology - Dr. Keith Gallagher
 */
package com.small.plugin.slicing.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * SliceHandler extends AbstractHandler, an IHandler base class.
 * 
 * This Eclipse extension will create a program slice based on
 * a specific, user chosen, variable.
 * 
 * This file is designed for stand-alone .java files, variable scope
 * will not be traced to other files.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SliceHandler extends AbstractHandler {
	private Vector<String> statements = new Vector<String>(0);
	private Vector<ASTNode> locStats;
	private Vector<String> typeStats;
	private Vector<String> varMethods = new Vector<String>(0);
	private Vector<String> varNames = new Vector<String>(0);
	private Vector<String> methodNames = new Vector<String>(0);
	private HashMap<String, HashMap<String, ASTNode>> nodeMap = new HashMap<String, HashMap<String, ASTNode>>();
	private Vector<String[]> varsInSlice;

	private boolean done;

	private Vector<String> fieldsToFind;
	private HashMap<String, Vector<String>> varsByMethod = new HashMap<String, Vector<String>>();

	private VariableDeclarationFragment frag;
	private Vector<List<CatchClause>> ccs = new Vector<List<CatchClause>>();

	private String innerString = "";

	/**
	 * Main method when plugin is invoked
	 * 
	 * This method performs the basic setup, then starts the
	 * slicing process if the chosen file is a .java file.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		ISelection sel = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection) sel;

		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof ICompilationUnit) {
			//Start slicing here
			ICompilationUnit cu = (ICompilationUnit) firstElement;
			write(shell, null, cu);
		} else {
			//something went wrong
			MessageDialog.openInformation(shell, "Info",
					"Please select a Java source file");
		}
		return null;
	}

	/**
	 * This method calls the slicing methods to prepare the data, then
	 * handles writing the slice to console.
	 * 
	 * @param shell - Required to open the variable chooser dialog box
	 * @param dir - passed as null
	 * @param cu - ICompilationUnit containing the source code of the file under slice
	 */
	private void write(Shell shell, String dir, ICompilationUnit cu) {
		try {
			/* 
			 * Parse the ICompiliationUnit to get a list
			 * of variables available for slicing
			 */
			parseCU(cu);
			ElementListSelectionDialog dialog = 
					new ElementListSelectionDialog(shell, new LabelProvider());
			dialog.setElements(typeStats.toArray());
			dialog.setTitle("Which variable should I slice?");

			if (dialog.open() != Window.OK) {
				return;
			}
			String result = (String)dialog.getResult()[0]; 
			/*
			 * Variable to slice has now been chosen
			 */

			IImportContainer imports = cu.getImportContainer();
			IJavaElement[] children = cu.getChildren();
			IType type = null;
			
			for (IJavaElement element : children) {
				int eleType = element.getElementType();
				if (eleType == IJavaElement.TYPE) {
					type = (IType)element;
				}
			}

			//Output Begins - basic imports and class name
			System.out.println(imports.getSource() + "\n");

			String classString = type.getSource();
			classString = classString.split("\\{")[0] + "{";
			System.out.println(classString + "\n");

			String vn = result.substring(0, result.indexOf("("));
			String mt = result.substring(result.indexOf("(")+1, result.indexOf(")"));
			
			/*
			 * Variable Slicing Begins
			 */
			varsInSlice = new Vector<String[]>();
			fieldsToFind = new Vector<String>();
			if (!mt.equals("Field")) {
				varsInSlice.add(new String[]{vn.trim(), mt.trim()});
			} else {
				fieldsToFind.add(vn.trim());
			}
			/*
			 * If parseMethod(cu) finds a new variable to include in the slice,
			 * i.e. if the variable under slice relies on another variable, the
			 * method returns false.  This while loop will run until all 
			 * variables have been found and sliced.
			 */
			while (!parseMethod(cu)) {}

			System.out.println(innerString + "\n}\n");
			
			imports = null;
			statements.clear();
			methodNames.clear();

		} catch (JavaModelException e) {
			System.out.print("java model exception");
		} 

	}

	/**
	 * Parses the ICompilationUnit to get the list of
	 * variable names presented to the user (variables
	 * available for slicing)
	 * 
	 * @param cu
	 * @return
	 */
	private Vector<ASTNode> parseCU (ICompilationUnit cu) {
		locStats = new Vector<ASTNode>(0);
		typeStats = new Vector<String>(0);
		varNames = new Vector<String>(0);
		varMethods = new Vector<String>(0);

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setStatementsRecovery(true);
		parser.setSource(cu);
		parser.setKind(ASTParser.K_STATEMENTS);
		Block block = (Block) parser.createAST(null);

		System.out.println("");

		block.accept(new ASTVisitor() {

			public boolean visit(VariableDeclarationFragment node) {

				frag = node;


				locStats.add(frag.getName());
				typeStats.add(frag.getName().toString() + "(Field)");

				nodeMap.put(frag.getName().toString(), new HashMap<String, ASTNode>());


				varNames.add(frag.getName().toString().trim());
				varMethods.add("field");

				return true;
			}

			public boolean visit(MethodDeclaration node) {

				final MethodDeclaration method = node;

				node.accept(new ASTVisitor() {
					public boolean visit(VariableDeclarationFragment node) {

						locStats.add(node.getName());

						String vName = node.getName().toString() + " (" 
								+ method.getName().toString() + ")";
						if (varNames.contains(node.getName().toString())
								&& varMethods.get(varNames.indexOf(node.getName().toString())).equals(method.getName().toString())) {
							int pos = typeStats.indexOf(vName);
							typeStats.set(pos, "** " + vName + " ** - Scope Issue");
							vName = "** " + vName + " ** - Scope Issue";
						} else {
							varNames.add(node.getName().toString().trim());
							varMethods.add(method.getName().toString().trim());
						}

						typeStats.add(vName);
						return true;
					}

					public boolean visit(SingleVariableDeclaration node) {

						locStats.add(node.getName());

						String vName = node.getName().toString() + " (" 
								+ method.getName().toString() + ")";
						if (varNames.contains(node.getName().toString())
								&& varMethods.get(varNames.indexOf(node.getName().toString())).equals(method.getName().toString())) {
							int pos = typeStats.indexOf(vName);
							typeStats.set(pos, "** " + vName + " ** - Scope Issue");
							vName = "** " + vName + " ** - Scope Issue";
						} else {
							varNames.add(node.getName().toString().trim());
							varMethods.add(method.getName().toString().trim());
						}

						typeStats.add(vName);
						return true;
					}
				});
				return false;
			}

		});


		return locStats;
	}

	/**
	 * This method handles the slicing once the variable has been chosen.
	 * 
	 * Converts the ICompilationUnit into an ASTParser block, then visits
	 * relevant nodes.  Declaration and Assignment nodes are sought out, 
	 * then any nodes they were nested inside of are included.
	 * 
	 * New variables are only added if they directly effect the value
	 * of a currently tracked variable.
	 * 
	 * Usage of the tracked variables will not be included unless the value
	 * of some tracked variable was assigned.
	 * 
	 * @param icu
	 * @return
	 */
	private boolean parseMethod (ICompilationUnit icu) {

		varsByMethod = getMap();
		done = true;
		ccs.clear();
		innerString = "";

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setStatementsRecovery(true);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

		parser.setSource(icu);
		parser.setKind(ASTParser.K_STATEMENTS);

		Block block = (Block) parser.createAST(null);

		block.accept(new ASTVisitor() {

			/*
			 * Handles FIELD declaration nodes (global variables)
			 * (non-Javadoc)
			 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
			 */
			public boolean visit(FieldDeclaration node) {
				if (!done) return false;
				if (!varsByMethod.containsKey("field")) return false;
				
				@SuppressWarnings("unchecked")
				List<VariableDeclarationFragment> ls = (List<VariableDeclarationFragment>)(node.fragments());
				boolean hasVar = false;
				for (int i = 0; i < ls.size(); i++) {
					if (varsByMethod.get("field").contains(ls.get(i).getName().toString())) hasVar = true;
				}

				if (hasVar) {
					String ndString = node.toString();

					// Check if other vars exist in the declaration
					for (int j = 0; j < varNames.size(); j++) {
						if(ndString.contains(varNames.get(j))) {
							if (varMethods.get(j).equals("field")
									&& !fieldsToFind.contains(varNames.get(j))) {
								fieldsToFind.add(varNames.get(j));
								done = false;
							}
						}
					}


					innerString += ndString + "\n";
				}

				return false;
			}

			/*
			 * Handles Parsing Methods
			 * (non-Javadoc)
			 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
			 */
			public boolean visit(MethodDeclaration node) {
				if (!done) return false;

				final String mName = node.getName().toString();

				if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

				String meth = node.toString();
				String mString;
				mString = meth.substring(0, meth.indexOf('{')+1) + "\n";

				//SliceVisitor handles the advanced slicing
				SliceVisitor visitor = new SliceVisitor(varsByMethod, varsInSlice, fieldsToFind, varMethods, varNames, mName, node);

				String iString = "";
				node.accept(visitor);
				done = visitor.isDone();
				if (done) iString += visitor.getInnerString();

				if (iString.trim().length() > 0) innerString += mString + iString + "\n}\n\n";
				
				return false;
			}

		});


		return done;
	}

	/**
	 * Returns a HashMap containing the variables that have 
	 * been found so far, and their method association (scope)
	 * 
	 * @return
	 */
	private HashMap<String, Vector<String>> getMap () {
		HashMap<String, Vector<String>> map = new HashMap<String, Vector<String>>();

		for (int i = 0; i < varsInSlice.size(); i++) {
			if (map.containsKey(varsInSlice.get(i)[1])) {
				map.get(varsInSlice.get(i)[1]).add(varsInSlice.get(i)[0]);
			} else {
				map.put(varsInSlice.get(i)[1], new Vector<String>());
				map.get(varsInSlice.get(i)[1]).add(varsInSlice.get(i)[0]);
			}
		}

		for (int i = 0; i < fieldsToFind.size(); i++) {
			if (map.containsKey("field")) {
				map.get("field").add(fieldsToFind.get(i));
			} else {
				map.put("field", new Vector<String>());
				map.get("field").add(fieldsToFind.get(i));
			}
		}

		return map;
	}

}
