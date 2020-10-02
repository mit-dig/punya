package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.PunyaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.IOUtils;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory2;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.RuleReasoner;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DesignerComponent(version = PunyaVersion.REASONER_COMPONENT_VERSION,
    nonVisible = true,
    category = ComponentCategory.LINKEDDATA,
    iconName = "images/reasoner.png"
)
@SimpleObject
@UsesLibraries({"jena-reasoner.jar"})
public class Reasoner extends LinkedData<InfModel> {

  private static final String LOG_TAG = Reasoner.class.getSimpleName();
  private LinkedData<?> basemodel = null;
  private String rulesEngine = "";
  private String rulesFile = "";

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param form the container that this component will be placed in
   */
  public Reasoner(Form form) {
    super(form);
  }

  ///region Properties

  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT + ":com.google.appinventor.components.runtime.LinkedData"
  )
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void Model(LinkedData<? extends Model> model) {
    this.basemodel = model;
  }

  @SimpleProperty
  public LinkedData<?> Model() {
    return basemodel;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
      editorArgs = {"None", "RDFS", "OWL Micro", "OWL Mini", "OWL"},
      defaultValue = "None")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void RulesEngine(String rules) {
    this.rulesEngine = rules;
  }

  @SimpleProperty
  public String RulesEngine() {
    return "";
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET)
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void RulesFile(String rules) {
    this.rulesFile = rules;
  }

  @SimpleProperty
  public String RulesFile() {
    return rulesFile;
  }

  ///endregion
  ///region Methods

  @SimpleFunction
  public void Run() {
    if (this.basemodel == null) {
      form.dispatchErrorOccurredEvent(this, "Run", 0);
      return;
    }
    final String rulesFile = this.rulesFile;
    final String rulesEngine = this.rulesEngine;
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          com.hp.hpl.jena.reasoner.Reasoner reasoner;
          if ("RDFS".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getRDFSReasoner();
          } else if ("OWL Micro".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getOWLMicroReasoner();
          } else if ("OWL Mini".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getOWLMiniReasoner();
          } else if ("OWL".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getOWLReasoner();
          } else {
            reasoner = new FBRuleReasoner(new ArrayList<Rule>());
          }
          model = ModelFactory2.createInfModel(reasoner, basemodel.getModel());
          if (rulesFile != null && !rulesFile.equals("")) {
            if (reasoner instanceof FBRuleReasoner) {
              ((FBRuleReasoner) reasoner).addRules(loadRules(rulesFile));
            } else if (reasoner instanceof RuleReasoner) {
              ((RuleReasoner) reasoner).setRules(loadRules(rulesFile));
            }
          }
          model.prepare();
          form.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              ReasoningComplete();
            }
          });
        } catch (final Exception e) {
          form.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              ErrorOccurred(e.toString());
            }
          });
        }
      }
    });
  }

  @SimpleFunction
  public List<List<String>> GetStatements(Object subject, Object predicate, Object object) {
    List<List<String>> result = new ArrayList<>();
    Resource s = null;
    Property p = null;
    RDFNode o = null;
    if (subject != Boolean.FALSE) {
      s = ResourceFactory.createResource(subject.toString());
    }
    if (predicate != Boolean.FALSE) {
      if (predicate.toString().equals("a")) {
        p = RDF.type;
      } else {
        p = ResourceFactory.createProperty(predicate.toString());
      }
    }
    if (object != Boolean.FALSE) {
      String ostr = object.toString();
      if (ostr.startsWith("http:") || ostr.startsWith("https://") || ostr.startsWith("file://")) {
        o = ResourceFactory.createResource(ostr);
      } else {
        o = ResourceFactory.createPlainLiteral(ostr);
      }
    }
    for (StmtIterator it = model.listStatements(s, p, o); it.hasNext(); ) {
      Statement st = it.next();
      result.add(Arrays.asList(
          st.getSubject().toString(),
          st.getPredicate().toString(),
          st.getObject().toString()));
    }
    return result;
  }

  ///endregion
  ///region Events

  @SimpleEvent
  public void ReasoningComplete() {
    EventDispatcher.dispatchEvent(this, "ReasoningComplete");
  }

  @SimpleEvent
  public void ErrorOccurred(String message) {
    EventDispatcher.dispatchEvent(this, "ErrorOccurred", message);
  }

  ///endregion

  private List<Rule> loadRules(String filename) throws IOException {
    InputStream in = null;
    try {
      in = MediaUtil.openMedia(form, filename);
      return Rule.parseRules(IOUtils.readStream(in));
    } finally {
      IOUtils.closeQuietly(LOG_TAG, in);
    }
  }
}
