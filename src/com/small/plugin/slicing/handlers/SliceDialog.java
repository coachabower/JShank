package com.small.plugin.slicing.handlers;

import java.util.Vector;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

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
public class SliceDialog {
	
	protected String getInput (Shell shell, Vector<String> typeStats) {
		ElementListSelectionDialog dialog = 
				new ElementListSelectionDialog(shell, new LabelProvider());
		dialog.setElements(typeStats.toArray());
		dialog.setTitle("Which variable should I slice?");

		if (dialog.open() != Window.OK) {
			return "";
		}
		return (String)dialog.getResult()[0]; 
	}
	
	protected void printSlice (Shell shell, String output) {
		/*
		MessageDialog dialogOutput = new MessageDialog(shell, "Your Slice", null,
			    output, MessageDialog.NONE, new String[] {"OK" }, 0);
		dialogOutput.open();
		*/
		
		SliceAreaDialog dialog = new SliceAreaDialog(shell, output);
		dialog.create();
		if (dialog.open() == Window.OK) {
		} 

	}
	
	
	private class SliceAreaDialog extends TitleAreaDialog {

		  private Text output;

		  private String outString;

		  public SliceAreaDialog(Shell parentShell, String outString) {
			  super(parentShell);
			  this.outString = outString;
		  }

		  @Override
		  public void create() {
		    super.create();
		    setTitle("Slice Box");
		    setMessage("Copy/Paste your Program Slice below", IMessageProvider.INFORMATION);
		  }

		  @Override
		  protected Control createDialogArea(Composite parent) {
		    Composite area = (Composite) super.createDialogArea(parent);
		    Composite container = new Composite(area, SWT.NONE);
		    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		    GridLayout layout = new GridLayout(1, false);
		    container.setLayout(layout);

		    createOutput(container);

		    return area;
		  }

		  private void createOutput(Composite container) {
		    //Label lbtFirstName = new Label(container, SWT.NONE);
		    //lbtFirstName.setText(outString);

		    GridData dataFirstName = new GridData();
		    dataFirstName.grabExcessHorizontalSpace = true;
		    dataFirstName.horizontalAlignment = GridData.FILL;

		    output = new Text(container, SWT.BORDER);
		    output.insert(outString);
		    output.setLayoutData(dataFirstName);
		  }



		  @Override
		  protected boolean isResizable() {
		    return true;
		  }

		  @Override
		  protected void okPressed() {
		    super.okPressed();
		  }

		} 

}
