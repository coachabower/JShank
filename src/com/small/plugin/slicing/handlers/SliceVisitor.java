/**
 * Author: Ryan Small
 * 
 * Developed as an assignment for Software Testing 2
 * @Florida Institute of Technology - Dr. Keith Gallagher
 */
package com.small.plugin.slicing.handlers;

import java.util.HashMap;
import java.util.Vector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * This is where the Slicing magic happens.
 * 
 * An AST Method node is passed in, then
 * all node types are visited and assessed
 * for tracked variable activity.  
 *
 */
public class SliceVisitor extends ASTVisitor {

	private String visited = "";
	private boolean done = true;
	private boolean doIf = false;

	private HashMap<String, Vector<String>> varsByMethod;
	private Vector<String[]> varsInSlice;
	private Vector<String> fieldsToFind;
	private Vector<String> varMethods;
	private Vector<String> varNames;
	private String mName;
	private ASTNode startNode;

	public SliceVisitor (HashMap<String, Vector<String>> varsByMethod,
			Vector<String[]> varsInSlice, 
			Vector<String> fieldsToFind, Vector<String> varMethods, 
			Vector<String> varNames, String mName, ASTNode startNode) {
		this.varsByMethod = varsByMethod;
		this.varsInSlice = varsInSlice;
		this.fieldsToFind = fieldsToFind;
		this.varMethods = varMethods;
		this.varNames = varNames;
		this.mName = mName;
		this.startNode = startNode;
	}

	public boolean isDone () {
		return done;
	}

	public String getInnerString () {
		return visited;
	}
	
	public void setDoIf (boolean doIf) {
		this.doIf = doIf;
	}

	public boolean visit(TryStatement node) {
		if (node.equals(startNode)) return true;
		if (!done) return false;
		if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

		String ndString = node.toString();

		String oString = "", iString = "";

		if (checkContents(ndString)) {
			oString += ndString.substring(0, ndString.indexOf(')')+1) + " {\n";
			SliceVisitor inside = new SliceVisitor(varsByMethod, varsInSlice, fieldsToFind, varMethods, varNames, mName, node);
			node.accept(inside);
			done = inside.isDone();
			if (done) iString += inside.getInnerString();
			if (iString.trim().length() > 0) visited += oString + iString + "}\n";
		}

		return false;
	}

	public boolean visit(ForStatement node) {
		if (node.equals(startNode)) return true;
		if (!done) return false;
		if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

		String ndString = node.toString();

		String oString = "", iString = "";
		
		if (checkContents(ndString)) {
			oString += ndString.substring(0, ndString.indexOf(')')+1) + " {\n";
			SliceVisitor inside = new SliceVisitor(varsByMethod, varsInSlice, fieldsToFind, varMethods, varNames, mName, node);
			node.accept(inside);
			done = inside.isDone();
			if (done) iString += inside.getInnerString();
			if (iString.trim().length() > 0) visited += oString + iString + "}\n";
		}

		return false;
	}

	public boolean visit(WhileStatement node) {
		if (node.equals(startNode)) return true;
		if (!done) return false;
		if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

		String ndString = node.toString();

		String oString = "", iString = "";

		if (checkContents(ndString)) {
			oString += ndString.substring(0, ndString.indexOf(')')+1) + " {\n";
			SliceVisitor inside = new SliceVisitor(varsByMethod, varsInSlice, fieldsToFind, varMethods, varNames, mName, node);
			node.accept(inside);
			done = inside.isDone();
			if (done) iString += inside.getInnerString();
			if (iString.trim().length() > 0) visited += oString + iString + "}\n";
		}

		return false;
	}

	public boolean visit(IfStatement node) {
		if (node.equals(startNode) && !doIf) return true;
		if (!done) return false;
		if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

		String ndString = node.toString();
		if (!checkContents(ndString)) return false;
		
		ASTNode then = node.getThenStatement();
		String thenString = then.toString();
		String oString = "", iString = "", iiString = "";

		oString += ndString.substring(0, ndString.indexOf(')')+1) + " {\n";
		
		if (checkContents(thenString)) {
			SliceVisitor inside = new SliceVisitor(varsByMethod, varsInSlice, fieldsToFind, varMethods, varNames, mName, then);
			then.accept(inside);
			done = inside.isDone();
			if (done) iString += inside.getInnerString();
		}
		
		if (iString.trim().length() > 0) oString += iString + "}\n";
		else oString += "\n} ";
		
		ASTNode els = node.getElseStatement();
		
		if (els != null && checkContents(els.toString())) {
			oString += "else ";
			SliceVisitor inside = new SliceVisitor(varsByMethod, varsInSlice, fieldsToFind, varMethods, varNames, mName, els);
			if (!els.toString().trim().substring(0, 2).equals("if")) {
				oString += "{\n";
			}
			inside.setDoIf(true);
			els.accept(inside);
			done = done && inside.isDone();
			if (done) iiString += inside.getInnerString();
			if (iiString.trim().length() > 0) oString = oString + iiString + "}\n";
		}
		
		if (iString.trim().length() > 0 || iiString.trim().length() > 0) visited += oString;

		return false;
	}
	
	public boolean visit(Assignment node) {
		if (!done) return false;
		if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

		if ((varsByMethod.containsKey(mName) && varsByMethod.get(mName).contains(node.getLeftHandSide().toString().trim()))
				|| (varsByMethod.containsKey("field") && varsByMethod.get("field").contains(node.getLeftHandSide().toString().trim()))) {
			//String prnt = node.getParent().toString();
			String ndString = node.toString();

			// Check if other vars exist in the declaration
			for (int j = 0; j < varNames.size(); j++) {
				if(ndString.contains(varNames.get(j))) {
					if (varMethods.get(j).equals(mName)
							&& (varsByMethod.containsKey(mName) && !varsByMethod.get(mName).contains(varNames.get(j)))) {
						varsInSlice.add(new String[]{varNames.get(j), varMethods.get(j)});
						done = false;
					} else if (varMethods.get(j).equals(mName)
							&& !varsByMethod.containsKey(mName)) {
						varsInSlice.add(new String[]{varNames.get(j), varMethods.get(j)});
						done = false;
					} else if (varMethods.get(j).equals("field")
							&& varsByMethod.containsKey("field")
							&& !fieldsToFind.contains(varNames.get(j))) {
						fieldsToFind.add(varNames.get(j));
						done = false;
					} else if (varMethods.get(j).equals("field")
							&& !varsByMethod.containsKey("field")) {
						fieldsToFind.add(varNames.get(j));
						done = false;
					}
				}
			}


			visited += node.getParent().toString();
		}

		return false;
	}

	public boolean visit(VariableDeclarationFragment node) {
		if (!done) return false;
		if (!(varsByMethod.containsKey(mName) || !fieldsToFind.isEmpty())) return false;

		if ((varsByMethod.containsKey(mName) && varsByMethod.get(mName).contains(node.getName().toString()))
				|| (varsByMethod.containsKey("field") && varsByMethod.get("field").contains(node.getName().toString()))) {
			String prnt = node.getParent().toString();
			String ndString = node.toString();

			// Check if other vars exist in the declaration
			for (int j = 0; j < varNames.size(); j++) {
				if(ndString.contains(varNames.get(j))) {
					if (varMethods.get(j).equals(mName)
							&& (varsByMethod.containsKey(mName) && !varsByMethod.get(mName).contains(varNames.get(j)))) {
						varsInSlice.add(new String[]{varNames.get(j), varMethods.get(j)});
						done = false;
					} else if (varMethods.get(j).equals(mName)
							&& !varsByMethod.containsKey(mName)) {
						varsInSlice.add(new String[]{varNames.get(j), varMethods.get(j)});
						done = false;
					} else if (varMethods.get(j).equals("field")
							&& varsByMethod.containsKey("field")
							&& !fieldsToFind.contains(varNames.get(j))) {
						fieldsToFind.add(varNames.get(j));
						done = false;
					} else if (varMethods.get(j).equals("field")
							&& !varsByMethod.containsKey("field")) {
						fieldsToFind.add(varNames.get(j));
						done = false;
					}
				}
			}


			visited += prnt;
		}

		return false;
	}
	
	private boolean checkContents (String contentString) {
		if (varsByMethod.containsKey(mName)) {
			for (int i = 0; i < varsByMethod.get(mName).size(); i++) {
				if (contentString.contains(varsByMethod.get(mName).get(i))) {
					return true;
				}
			}
		}

		if (varsByMethod.containsKey("field")) {
			for (int i = 0; i < varsByMethod.get("field").size(); i++) {
				if (contentString.contains(varsByMethod.get("field").get(i))) {
					return true;
				}
			}
		}
		
		return false;
	}

}
