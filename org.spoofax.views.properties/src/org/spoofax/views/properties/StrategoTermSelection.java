package org.spoofax.views.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * @author oskarvanrest
 */
public class StrategoTermSelection implements IStructuredSelection {

	List properties = new ArrayList();
	ISelection actualSelection;
	
	public StrategoTermSelection(IStrategoTerm properties) {
		this(properties, null);
	}
	
	/**
	 * In some cases, an editor or view already provides a selection that does not contribute to the
	 * properties view, but is used for different applications. Textual editors, for example, provide
	 * an ITextSelection. By means of this constructor and method 'getActualSelection()', one can
	 * maintain and retrieve such 'actual' selections.
	 * TODO: may need to find a better name than 'actual selection'
	 * @param actualSelection
	 * @param properties
	 */
	public StrategoTermSelection(IStrategoTerm properties, ISelection actualSelection) {
		this.properties.add(new AdaptableStrategoTerm(properties));
		this.actualSelection = actualSelection;
	}
	
	public ISelection getActualSelection() {
		return actualSelection;
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
