package com.small.plugin.slicing.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.eclipse.jdt.core.ICompilationUnit;
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
public class SliceParser {
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
	 * Parses the ICompilationUnit to get the list of
	 * variable names presented to the user (variables
	 * available for slicing)
	 * 
	 * @param cu
	 * @return
	 */
	protected Vector<ASTNode> parseCU (ICompilationUnit cu) {
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
	
	protected String parseMethodDriver (ICompilationUnit cu, String chosenVariable) {
		String vn = chosenVariable.substring(0, chosenVariable.indexOf("("));
		String mt = chosenVariable.substring(chosenVariable.indexOf("(")+1, chosenVariable.indexOf(")"));
		
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

		return innerString + "\n}\n";
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
	
	protected Vector<String> getTypeStats () {
		return this.typeStats;
	}
	
	protected Vector<String> getStatements () {
		return this.statements;
	}
	
	protected Vector<String> getMethodNames () {
		return this.methodNames;
	}

}
