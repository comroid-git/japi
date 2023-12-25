package org.comroid.test.annotations;

import org.comroid.abstr.DataNode;
import org.comroid.annotations.Ignore;
import org.comroid.api.DataStructure;
import org.comroid.api.ValueBox;
import org.comroid.api.ValuePointer;
import org.comroid.test.Dummy;
import org.junit.Test;

import static org.comroid.annotations.Annotations.ignore;
import static org.junit.Assert.assertTrue;

public class AnnotationTest {
    @Test
    public void testIgnore() throws NoSuchMethodException {
        assertTrue("class based annotation not detected", ignore(ValuePointer.class, DataStructure.class));
        assertTrue("superclass based annotation not detected", ignore(ValueBox.class, DataStructure.class));
        assertTrue("method based annotation not detected", ignore(ValuePointer.class.getMethod("getHeldType"), DataStructure.class));
        assertTrue("ancestor method based annotation not detected", ignore(ValueBox.class.getMethod("getHeldType"), DataStructure.class));
        assertTrue("return type based annotation not detected", ignore(Dummy.class.getMethod("getTestObj1"), DataStructure.class));
    }
}
