/**
 * 
 */
package org.strategoxt.imp.runtime.services;

import static java.util.Collections.synchronizedMap;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.spoofax.interpreter.core.InterpreterErrorExit;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.InterpreterExit;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.imp.runtime.EditorState;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.stratego.adapter.IStrategoAstNode;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class CustomStrategyBuilder extends StrategoBuilder {
	
	private static final Map<String, String> initialValues =
		synchronizedMap(new HashMap<String, String>());
	
	public CustomStrategyBuilder(StrategoObserver observer, EditorState derivedFromEditor) {
		// TODO: reconsider source = true here?
		super(observer, "Apply custom rule...", null, true, true, false, true, false, derivedFromEditor);
	}
	
	@Override
	public void scheduleExecute(EditorState editor, IStrategoAstNode node, IFile errorReportFile,
			boolean isRebuild) {
		
		String builderRule = inputBuilderRule(editor);
		if (builderRule != null) {
			setBuilderRule(builderRule);
			super.scheduleExecute(editor, node, errorReportFile, isRebuild);
		}
	}
	
	private String inputBuilderRule(EditorState editor) {
		IInputValidator validator = 
			new IInputValidator() {
				public String isValid(String name) {
					return getObserver().getRuntime().lookupUncifiedSVar(name) == null
							? "No rule or strategy with this name"
							: null;
				}
			};
		
		synchronized (getObserver().getSyncRoot()) {
			InputDialog dialog = new InputDialog(null, "Apply custom rule", "Enter the name of the rewrite rule or strategy to apply", getInitialValue(editor), validator);
			if (dialog.open() == InputDialog.OK) {
				setInitialValue(editor, dialog.getValue());
				return dialog.getValue();
			} else {
				return null;
			}
		}
	}
	
	@Override
	protected IStrategoTerm invokeObserver(IStrategoAstNode node)
			throws UndefinedStrategyException, InterpreterErrorExit, InterpreterExit,
			InterpreterException {

		// Try invoke using (term)
		IStrategoTerm input = getDerivedFromEditor() == null
				? node.getTerm()
				: getObserver().implodeATerm(getObserver().getImplodableNode(node).getTerm());
		IStrategoTerm result = getObserver().invoke(getBuilderRule(), input, node.getResource());
		if (result != null) return addFileName(result, node.getResource());
		String[] trace1 = getObserver().getRuntime().getCompiledContext().getTrace();
		
		// Try invoke using (term, ast, ...) tuple
		result = super.invokeObserver(node);
		if (result != null) return result;
		String[] trace2 = getObserver().getRuntime().getCompiledContext().getTrace();
		
		// Report the previous stack trace if the new one is very short
		if (trace2.length == 1)
			getObserver().getRuntime().getCompiledContext().setTrace(trace1);

		return null;
	}
	
	private IStrategoTuple addFileName(IStrategoTerm result, IResource resource) {
		ITermFactory factory = getObserver().getRuntime().getFactory();
		IPath source = resource.getProjectRelativePath().removeFileExtension();
		String target = addCounter(source).addFileExtension("aterm").toPortableString();
		return factory.makeTuple(factory.makeString(target), result);
	}

	private IPath addCounter(IPath path) {
		if (getDerivedFromEditor() == null)
			return path;
		String counter = "2";
		if (path.getFileExtension() != null) {
			try {
				int prevCounter = Integer.parseInt(path.getFileExtension());
				path.removeFileExtension();
				counter = String.valueOf(prevCounter + 1);
			} catch (NumberFormatException e) {
				// Leave the counter at 2
			}
		}
		return path.addFileExtension(counter);
	}

	private void setInitialValue(EditorState editor, String value) {
		initialValues.put(editor.getLanguage().getName(), value);
	}

	private String getInitialValue(EditorState editor) {
		String language = editor.getLanguage().getName();
		String result = initialValues.get(language);
		if (result != null) return result;
		IBuilderMap allBuilders;
		try {
			allBuilders = editor.getDescriptor().createService(IBuilderMap.class, editor.getParseController());
			for (IBuilder builder : allBuilders.getAll()) {
				if (builder instanceof StrategoBuilder && ((StrategoBuilder) builder).getBuilderRule() != null)
					result = ((StrategoBuilder) builder).getBuilderRule();
			}
		} catch (BadDescriptorException e) {
			Environment.logException(e);
		}	
		
		return result == null ? "" : result;
	}
}
