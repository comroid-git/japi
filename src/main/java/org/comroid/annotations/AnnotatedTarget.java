package org.comroid.annotations;

import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.ext.Wrap;
import org.comroid.api.java.ReflectionHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotatedTarget {
    interface Extension {
        default Wrap<AnnotatedElement> element() {
            return Wrap.of(ReflectionHelper.fieldWithAnnotation(getClass(), AnnotatedTarget.class)
                            .stream()
                            .filter(fld -> AnnotatedElement.class.isAssignableFrom(fld.getType()))
                            .findAny()
                            .map(ThrowingFunction.sneaky(fld -> fld.get(this))))
                    .castRef();
        }
    }
}
