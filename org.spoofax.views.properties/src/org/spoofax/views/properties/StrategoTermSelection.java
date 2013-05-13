package org.spoofax.views.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class StrategoTermSelection implements IStructuredSelection {

	ITextSelection textSelection;
	List properties = new ArrayList();
	
	public StrategoTermSelection(ITextSelection textSelection, IStrategoTerm properties) {
		this.textSelection = textSelection;
		this.properties.add(new AdaptableStrategoTerm(properties));
	}
	
	public ITextSelection getTextSelection() {
		return textSelection;
	}
	
	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}
	
	@Override
	public Object getFirstElement() {
		if (!isEmpty()) {
			return properties.get(0);
		}
		return null;
	}

	@Override
	public Iterator iterator() {
		return properties.iterator();
	}

	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public Object[] toArray() {
		return properties.toArray();
	}

	@Override
	public List toList() {
		return properties;
	}
}
