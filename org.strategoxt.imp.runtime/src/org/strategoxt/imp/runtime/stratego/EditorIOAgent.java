package org.strategoxt.imp.runtime.stratego;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.spoofax.interpreter.library.LoggingIOAgent;
import org.strategoxt.imp.runtime.Debug;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;

/**
 * This class overrides the default IOAgent to support attached files in editor plugins,
 * and may redirect any disk reads to the Eclipse API. 
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class EditorIOAgent extends LoggingIOAgent {
	
	private Descriptor descriptor;
	
	public void setDescriptor(Descriptor descriptor) {
		this.descriptor = descriptor;
	}
	
	public Descriptor getDescriptor() {
		return descriptor;
	}
	
	@Override
	public InputStream openInputStream(String path, boolean isDefinitionFile)
			throws FileNotFoundException {
		
		if (isDefinitionFile && descriptor != null) {
			return openAttachedFile(path);
		} else {
			return super.openInputStream(path, isDefinitionFile);
		}
	}
	
	private InputStream openAttachedFile(String path) throws FileNotFoundException {
		try {
			return descriptor.openAttachment(path);
		} catch (FileNotFoundException e) {
			File localFile = new File(path);
			if (localFile.exists()) {
				Debug.log("Reading file form the current directory: ", path);  
				return new BufferedInputStream(new FileInputStream(localFile));
			} else {
				throw e;
			}
		}
	}
}