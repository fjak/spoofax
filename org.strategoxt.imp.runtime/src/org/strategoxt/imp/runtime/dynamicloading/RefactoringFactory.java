package org.strategoxt.imp.runtime.dynamicloading;


import static org.spoofax.interpreter.core.Tools.termAt;
import static org.strategoxt.imp.runtime.dynamicloading.TermReader.collectTerms;
import static org.strategoxt.imp.runtime.dynamicloading.TermReader.cons;
import static org.strategoxt.imp.runtime.dynamicloading.TermReader.termContents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.KeywordRecognizer;
import org.strategoxt.imp.runtime.EditorState;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.imp.runtime.services.IStrategoRefactoringInput;
import org.strategoxt.imp.runtime.services.StrategoRefactoringBooleanInput;
import org.strategoxt.imp.runtime.services.StrategoRefactoringTextInput;
import org.strategoxt.imp.runtime.services.StrategoTextChangeCalculator;
import org.strategoxt.imp.runtime.services.IRefactoring;
import org.strategoxt.imp.runtime.services.IRefactoringMap;
import org.strategoxt.imp.runtime.services.RefactoringMap;
import org.strategoxt.imp.runtime.services.StrategoAnalysisQueueFactory;
import org.strategoxt.imp.runtime.services.StrategoObserver;
import org.strategoxt.imp.runtime.services.StrategoRefactoring;
import org.strategoxt.imp.runtime.services.StrategoRefactoringIdentifierInput;
import org.strategoxt.imp.runtime.services.StrategoRefactoringWizard;
import org.strategoxt.imp.runtime.services.SyntaxProperties;

/**
 * @author Maartje de Jonge
 */
public class RefactoringFactory extends AbstractServiceFactory<IRefactoringMap> {
	
	public RefactoringFactory() {
		super(IRefactoringMap.class, false); // not cached; depends on derived editor relation
	}

	@Override
	public IRefactoringMap create(Descriptor descriptor, SGLRParseController controller)
			throws BadDescriptorException {
		Set<IRefactoring> refactorings = collectRefactorings(descriptor, controller, controller.getEditor());
		setRefactoringActions(controller.getEditor(), refactorings);
		return new RefactoringMap(refactorings);
	}
	
	/**
	 * Eagerly initializes refactorings so that they can be triggered by their shortcuts.
	 */
	public static void eagerInit(Descriptor descriptor, IParseController controller, final EditorState editor) {
		Set<IRefactoring> refactorings = new HashSet<IRefactoring>();
		if (editor != null && controller instanceof SGLRParseController) {
			try {
				refactorings = collectRefactorings(descriptor, (SGLRParseController) controller, editor);
				setRefactoringActions(editor, refactorings);
			} catch (BadDescriptorException e) {
				Environment.logException("Could not eagerly initialize the refactoring service", e);
			} catch (RuntimeException e) {
				Environment.logException("Could not eagerly initialize the refactoring service", e);
			}
		}
	}

	private static void setRefactoringActions(final EditorState editor, Set<IRefactoring> refactorings) {
		for (final IRefactoring refactoring : refactorings) {
			IAction action = new Action(refactoring.getCaption()) {
				@Override
				public void run() {
					executeRefactoring(editor, refactoring);
				}
			};
			action.setActionDefinitionId(refactoring.getActionDefinitionId());
			editor.getEditor().getSite().getKeyBindingService().registerAction(action);
			refactoring.setAction(action);
		}
	}
	
	private static void executeRefactoring(EditorState editor, IRefactoring refactoring) {
		PlatformUI.getWorkbench().saveAllEditors(false);
		refactoring.prepareExecute(editor);
		StrategoRefactoringWizard wizard = new StrategoRefactoringWizard(
			(StrategoRefactoring) refactoring, 
			refactoring.getCaption()
		);
		RefactoringWizardOpenOperation operation= new RefactoringWizardOpenOperation(wizard);
		Shell shell = editor.getEditor().getSite().getShell();
		try {
			operation.run(shell, refactoring.getCaption());
		} catch (Exception e) {
			e.printStackTrace();
		}
		queueAnalysisAffectedFiles(refactoring, editor);
	}

	//Queue analysis for affected asts, 
	//dyn rules may be out of sync after re-analysis asts + abort refactoring)
	private static void queueAnalysisAffectedFiles(IRefactoring refactoring, EditorState editor) {		
		IProject project = editor.getProject().getRawProject();
		for (IPath projectRelativePath : refactoring.getRelativePathsOfAffectedFiles()) {
			StrategoAnalysisQueueFactory.getInstance().queueAnalysis(projectRelativePath, project, true);			
		}
	}

	private static Set<IRefactoring> collectRefactorings(Descriptor d, SGLRParseController controller, EditorState editor) throws BadDescriptorException {
		HashMap<IStrategoTerm, String> keybindings = getKeybindings(d);
		Set<IRefactoring> refactorings = new LinkedHashSet<IRefactoring>();
		StrategoObserver feedback = d.createService(StrategoObserver.class, controller);
		StrategoTextChangeCalculator textChangeCalculator = createTextChangeCalculator(d);
		for (IStrategoAppl aRefactoring : collectTerms(d.getDocument(), "Refactoring")) {
			IStrategoTerm[] semanticNodes = termAt(aRefactoring,0).getAllSubterms();
			String caption = termContents(termAt(aRefactoring, 1));
			String strategy = termContents(termAt(aRefactoring, 2));
			IStrategoList options = termAt(aRefactoring, 3);
			IStrategoTerm userInteractions = termAt(aRefactoring, 4);
			ArrayList<IStrategoRefactoringInput> inputFields = 
				getInputFields(userInteractions, editor);
			String actionDefinitionId = getActionDefinitionId(userInteractions, keybindings);
			//actionDefinitionId = "org.eclipse.jdt.ui.edit.text.java.rename.element";
			boolean cursor = false;
			boolean source = false;
			boolean meta = false;
			for (IStrategoTerm option : options.getAllSubterms()) {
				String type = cons(option);
				if (type.equals("Cursor")) {
					cursor = true;
				} else if (type.equals("Source")) {
					source = true;
				} else if (type.equals("Meta")) {
					meta = true;
				} else if (
						type.equals("OpenEditor") ||
						type.equals("RealTime") ||
						type.equals("Persistent")
					){
					Environment.logWarning("Unused builder annotation '"+ type + "' in '" + caption +"'");
				}
				else {
					throw new BadDescriptorException("Unknown builder annotation: " + type);
				}
			}
			if (!meta || d.isDynamicallyLoaded()){			
				refactorings.add(
					new StrategoRefactoring(
						feedback, 
						caption, 
						strategy,
						cursor, 
						source, 
						textChangeCalculator,
						semanticNodes,
						inputFields,
						actionDefinitionId
					)
				);
			}
		}
		return refactorings;
	}

	private static HashMap<IStrategoTerm, String> getKeybindings(Descriptor d) {
		HashMap<IStrategoTerm, String> keybindings = new HashMap<IStrategoTerm, String>();
		for (IStrategoAppl aBinding : collectTerms(d.getDocument(), "KeyBinding")) {
			IStrategoTerm key = termAt(aBinding, 0);
			String value = termContents(termAt(aBinding, 1));
			keybindings.put(key, value);
		}
		return keybindings;
	}

	private static StrategoTextChangeCalculator createTextChangeCalculator(Descriptor d)
			throws BadDescriptorException {
		String ppStrategy = getPPStrategy(d);
		String parenthesize = getParenthesizeStrategy(d);
		String overrideReconstruction = getOverrideReconstructionStrategy(d);
		String resugar = getResugarStrategy(d);
		StrategoTextChangeCalculator textChangeCalculator = new StrategoTextChangeCalculator(ppStrategy, parenthesize, overrideReconstruction, resugar);
		return textChangeCalculator;
	}

	public static String getPPStrategy(Descriptor d) throws BadDescriptorException {
		String ppStrategy = getHelperStrategyName(d, "PrettyPrint");
		if(ppStrategy == null)
			ppStrategy = ("pp-" + d.getLanguage().getName() + "-string").toLowerCase();
		return ppStrategy;
	}

	public static String getParenthesizeStrategy(Descriptor d) throws BadDescriptorException {
		String parenthesizeStrategy = getHelperStrategyName(d, "Parenthesize");
		if(parenthesizeStrategy == null)
			parenthesizeStrategy = ("parenthesize-" + d.getLanguage().getName());
		return parenthesizeStrategy;
	}

	public static String getOverrideReconstructionStrategy(Descriptor d) throws BadDescriptorException {
		String overrideReconstructionStrategy = getHelperStrategyName(d, "OverrideReconstruction");
		if(overrideReconstructionStrategy == null)
			overrideReconstructionStrategy = "fail";
		return overrideReconstructionStrategy;
	}

	public static String getResugarStrategy(Descriptor d) throws BadDescriptorException {
		String resugarStrategy = getHelperStrategyName(d, "Resugar");
		if(resugarStrategy == null)
			resugarStrategy = "fail";
		return resugarStrategy;
	}

	public static String getHelperStrategyName(Descriptor d, String strategyTermCons) {
		IStrategoAppl strategyTerm = TermReader.findTerm(d.getDocument(), strategyTermCons);
		String strategyName = null;
		if(strategyTerm != null)
			strategyName=termContents(termAt(strategyTerm, 0));
		return strategyName;
	}


	private static String getActionDefinitionId(IStrategoTerm userInteractions, HashMap<IStrategoTerm, String> keybindings) {
		IStrategoTerm keycombination = TermReader.findTerm(userInteractions, "KeyCombination");
		if(keycombination != null)
			return keybindings.get(keycombination);
		IStrategoTerm interactionId = TermReader.findTerm(userInteractions, "InteractionId");
		if(interactionId != null)
			return TermReader.termContents(interactionId);
		return null;
	}

	private static ArrayList<IStrategoRefactoringInput> getInputFields(
			IStrategoTerm userInteractions, EditorState editor) {
		ArrayList<IStrategoRefactoringInput> inputFields = new ArrayList<IStrategoRefactoringInput>();
		IStrategoTerm userInput = TermReader.findTerm(userInteractions, "UserInput");
		if(userInput != null){
			IStrategoTerm userInputList = userInput.getSubterm(0);
			for (int i = 0; i < userInputList.getSubtermCount(); i++) {
				IStrategoAppl input = TermReader.applAt(userInputList, i);
				tryAddIdentifierInput(editor, inputFields, input);
				tryAddTextInput(inputFields, input);
				tryAddBooleanInput(inputFields, input);
				//TODO other input types
				//TODO pattern
			}
		}
		return inputFields;
	}

	private static void tryAddIdentifierInput(EditorState editor,
			ArrayList<IStrategoRefactoringInput> inputFields, IStrategoAppl input) {
		assert editor != null;
		if(TermReader.hasConstructor(input, "IdInputField")){
			String label = termContents(termAt(input,0));
			String defaultValue = "";
			if(input.getSubtermCount() > 1)
				defaultValue = termContents(termAt(input,1)); //TODO: Strategy OR String
			StrategoRefactoringIdentifierInput idInput = 
				new StrategoRefactoringIdentifierInput(
					label, 
					defaultValue, 
					getIdPattern(editor), 
					getKeywordRecognizer(editor),
					getLanguageName(editor)
				);
			inputFields.add(idInput);
		}
	}

	private static void tryAddTextInput(ArrayList<IStrategoRefactoringInput> inputFields, IStrategoAppl input) {
		if(TermReader.hasConstructor(input, "TextInputField")){
			String label = termContents(termAt(input,0));
			String defaultValue = "";
			if(input.getSubtermCount() > 1)
				defaultValue = termContents(termAt(input,1)); //TODO: Strategy OR String
			StrategoRefactoringTextInput textInput = 
				new StrategoRefactoringTextInput(
					label, 
					defaultValue
				);
			inputFields.add(textInput);
		}
	}

	private static void tryAddBooleanInput(ArrayList<IStrategoRefactoringInput> inputFields, IStrategoAppl input) {
		if(TermReader.hasConstructor(input, "BooleanInputField")){
			String label = termContents(termAt(input,0));
			boolean defaultValue = false;
			if(input.getSubtermCount() > 1 && TermReader.findTerm(input, "TrueValue") != null)
				defaultValue = true; //TODO: Strategy OR String
			StrategoRefactoringBooleanInput booleanInput = 
				new StrategoRefactoringBooleanInput(
					label, 
					defaultValue
				);
			inputFields.add(booleanInput);
		}
	}

	private static String getLanguageName(EditorState editor) {
		try {
			return editor.getDescriptor().getLanguage().getName();
		} catch (Exception e) {
			e.printStackTrace();
			return "<MyLanguage>";
		}
	}

	private static Pattern getIdPattern(EditorState editor) {
		Descriptor descriptor = editor.getDescriptor();
		SyntaxProperties syntax = null;
		if (descriptor != null) {
			try {
				syntax = (SyntaxProperties) descriptor.createService(ILanguageSyntaxProperties.class, null);
			} catch (BadDescriptorException e) {
				Environment.logException("Could not read syntax properties", e);
				e.printStackTrace();
			}
		} 
		return syntax != null ? syntax.getIdentifierLexical() : null;
	}

	private static KeywordRecognizer getKeywordRecognizer(EditorState editor) {
		try {
			return editor.getParseController().getParser().getParseTable().getKeywordRecognizer();
		}
		catch (Exception e){
			Environment.logException("Could not fetch keyword recognizer", e);
			e.printStackTrace();
			return null;
		}
	}

}
