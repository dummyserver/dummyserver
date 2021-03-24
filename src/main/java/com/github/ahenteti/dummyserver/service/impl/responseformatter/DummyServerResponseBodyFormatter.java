package com.github.ahenteti.dummyserver.service.impl.responseformatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.ahenteti.dummyserver.service.IDummyServerResponseBodyFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DummyServerResponseBodyFormatter implements IDummyServerResponseBodyFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyServerResponseBodyFormatter.class);

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private TemplateVariableConverterChainFactory templateVariableConverterChainFactory;

    private ITemplateVariableConverter templateVariableConverter;

    @PostConstruct
    public void init() {
        this.templateVariableConverter = templateVariableConverterChainFactory.create();
    }

    @Override
    public String format(String template) {
        try {
            Pattern pattern = Pattern.compile("\\{\\{([^\\s}]+)(.*?)}}");
            Matcher matcher = pattern.matcher(template);
            int lastIndex = 0;
            StringBuilder output = new StringBuilder();
            while (matcher.find()) {
                output.append(template, lastIndex, matcher.start());
                output.append(templateVariableConverter.convert(matcher.group(1), matcher.group(2)));
                lastIndex = matcher.end();
            }
            if (lastIndex < template.length()) {
                output.append(template, lastIndex, template.length());
            }
            return output.toString();
        } catch (Exception e) {
            LOGGER.error("error while formatting body. we return the template without formatting", e);
            return template;
        }
    }

    @Override
    public JsonNode format(JsonNode body) {
        try {
            String bodyString = format(body.toString());
            return mapper.readTree(bodyString);
        } catch (Exception e) {
            LOGGER.error("error while formatting body. we return the body without formatting", e);
            return body;
        }
    }

    private String format(String template, String regex, Function<Matcher, String> converter) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(template);
            int lastIndex = 0;
            StringBuilder output = new StringBuilder();
            while (matcher.find()) {
                output.append(template, lastIndex, matcher.start()).append(converter.apply(matcher));
                lastIndex = matcher.end();
            }
            if (lastIndex < template.length()) {
                output.append(template, lastIndex, template.length());
            }
            return output.toString();
        } catch (Exception e) {
            LOGGER.error("error while formatting body. we return the template without formatting", e);
            return template;
        }
    }

}
