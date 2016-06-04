/**
 * Author: Ryan Small
 * 
 * Developed as an assignment for Software Testing 2
 * @Florida Institute of Technology - Dr. Keith Gallagher
 */
package com.small.plugin.slicing.handlers;

import java.util.Vector;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
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
			sliceDriver(shell, cu);
		} else {
			//something went wrong
			MessageDialog.openInformation(shell, "Info",
					"Please select a Java source file");
		}
		return null;
	}
	
	/**
	 * This method drives the slicing commands.
	 * Call SliceDialog for input/output.
	 * Call SliceHelper for general structure (ensure code can be compiled).
	 * Call SliceParser to prepare Dialog for input, and to parse methods.
	 * 
	 * @param shell - Required to open the variable chooser dialog box
	 * @param cu - ICompilationUnit containing the source code of the file under slice
	 */
	private void sliceDriver (Shell shell, ICompilationUnit cu) {
		
		SliceParser parser = new SliceParser();
		SliceDialog dialog = new SliceDialog();
		
		parser.parseCU(cu);
		
		String result = dialog.getInput(shell, parser.getTypeStats());
		// If user closes the dialog without choosing a variable
		if (result.length() < 1) return;
		
		StringBuilder output = new StringBuilder();

		output.append(SliceHelper.stringifyPackageDecs(cu) + "\n");
		output.append(SliceHelper.stringifyImports(cu) + "\n");
		output.append(SliceHelper.stringifyClass(cu) + "\n");
		output.append(parser.parseMethodDriver(cu, result) + "\n");
		
		dialog.printSlice(shell, output.toString());
		
		resetSlicing(parser.getStatements(), parser.getMethodNames());
	}
	
	private void resetSlicing(Vector<String> statements, Vector<String> methodNames) {
		statements.clear();
		methodNames.clear();
	}

}
