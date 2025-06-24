package org.example.util;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("newLineConverter")
public class NewLineConverter implements Converter<String> {

    @Override
    public String getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.replaceAll("<br/>", "\n").replaceAll("<br>", "\n");
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value.replaceAll("\n", "<br/>").replaceAll("\r\n", "<br/>");
    }
}