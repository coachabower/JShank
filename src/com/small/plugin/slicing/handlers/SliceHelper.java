package com.small.plugin.slicing.handlers;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * SliceHelper provides functions to Stringify the building
 * blocks of a .java file.  This ensures that the program slice will compile.
 * 
 */
public class SliceHelper {
	
	/**
	 * Returns a String representation of the Package Declarations 
	 * of the .java file under slice.
	 * 
	 * All Original Package Declarations are retained in the program slice
	 * to ensure compilation.
	 * 
	 * @param cu - ICompilationUnit containing the source code of the file under slice
	 */
	protected static String stringifyPackageDecs (ICompilationUnit cu) {
		String packages = "";
		
		IPackageDeclaration[] packageDecs;
		try {
			packageDecs = cu.getPackageDeclarations();
			
			for (IPackageDeclaration element : packageDecs) {
				packages += element.getSource() + "\n";
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return packages;
	}
	
	/**
	 * Returns a String representation of the Import Declarations 
	 * of the .java file under slice.
	 * 
	 * All Original Import Declarations are retained in the program slice
	 * to ensure compilation.
	 * 
	 * @param cu - ICompilationUnit containing the source code of the file under slice
	 */
	protected static String stringifyImports (ICompilationUnit cu) {
		String imports = "";
		
		IImportDeclaration[] importDecs;
		try {
			importDecs = cu.getImports();
			
			for (IImportDeclaration element : importDecs) {
				imports += element.getSource() + "\n";
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return imports;
	}
	
	/**
	 * Returns a String representation of the Class Signature 
	 * of the .java file under slice.
	 * 
	 * @param cu - ICompilationUnit containing the source code of the file under slice
	 */
	protected static String stringifyClass (ICompilationUnit cu) {
		IJavaElement[] children;
		try {
			children = cu.getChildren();
			IType type = null;
			
			for (IJavaElement element : children) {
				int eleType = element.getElementType();
				if (eleType == IJavaElement.TYPE) {
					type = (IType)element;
				}
			}

			//Output Begins - basic imports and class name
			//String output = imports.getSource() + "\n";

			String classString = type.getSource();
			return classString.split("\\{")[0] + "{";
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

}
