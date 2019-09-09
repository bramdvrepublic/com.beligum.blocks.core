package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.resources.ifaces.Source;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface ValueModifier<T, E extends ValueModifierInput> {
     @JsonIgnore
     T getValue(E valueModifierInput);
     @JsonIgnore
     ValueModifierInput initValueModifierInput(Source source, Element element, OutputDocument htmlOutput);
     @JsonProperty
     String getName();
     String getInitialValue();
}
