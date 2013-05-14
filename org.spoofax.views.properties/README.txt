This plug-in provides functionality for feeding the properties view with a model 
that takes the form of a StrategoTerm. It can be used by editors or views by setting 
the selection provider to an instance of SelectionProvider and consequently setting 
selections to instances of StrategoTermSelection:

getSite().setSelectionProvider(new SelectionProvider());

ISelection selection = new StrategoTermSelection(properties);
getSite().getSelectionProvider().setSelection(selection);


A properties model is either a single property or a list of properties. Each property 
is a (key, value) pair, where the key is a StrategoString and the value is either a 
StrateoString or a list of (nested) properties.

For example, [ ("p1", "v1"), ("p2", [("p3", "v3"), ("p4", "v4")]) ] is represented in 
the properties view as follows:

"p1" "v1"
"p2"
  "p3" "v3"
  "p4" "v4"
  

https://github.com/oskarvanrest/spoofax-views-dummy-editor shows a working example.
Place the dummy editor plug-in together with this plug-in into your workspace,
run a new Eclipse application and create a new file 'test.xml', open it with the dummy
editor, type some text, open the properties view, select a piece of text in the editor
and observe what happens in the properties view. Note, the provided model is static 
and does not change based on the selection. The static model simply becomes visible 
upon selecting text.