package com.dc.property.bytebuddy.advice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class BootConfiguration {


    private List<String> ignorePackages = Collections.emptyList();

    private boolean debugCheck = false;


    public List<ElementMatcher<? super TypeDescription>> ignoreMatchers(){
        return ignorePackages.stream()
                .map(ElementMatchers::nameStartsWith)
                .collect(Collectors.toList());
    }

}
