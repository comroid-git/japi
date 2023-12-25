package org.comroid.test.annotations;

import org.comroid.annotations.Alias;
import org.comroid.api.DataStructure;
import org.comroid.api.ValueBox;
import org.comroid.api.ValuePointer;
import org.comroid.test.Dummy;
import org.junit.Test;

import java.util.List;

import static org.comroid.annotations.Annotations.*;
import static org.comroid.test.Dummy.*;
import static org.junit.Assert.*;

public class AnnotationTest {
    @Test
    public void testIgnoreAncestors() throws NoSuchMethodException {
        assertFalse("false positive ignore ancestor", ignoreAncestors(Dummy.class.getMethod("getFruit"), Alias.class));
        assertTrue("ignore ancestor not detected", ignoreAncestors(Apple.class.getMethod("getPrice"), Alias.class));
    }

    @Test
    public void testAlias() {
        assertArrayEquals("no alias detected", new Object[]{AliasFruit}, aliases(Fruit.class).toArray());
        assertEquals("not all aliases detected", 2, aliases(Apple.class).stream().filter(List.of(AliasApple, AliasFruit)::contains).count());
        assertArrayEquals("wrong ancestors for alias", new Object[]{AliasBanana}, aliases(Banana.class).toArray());
    }

    @Test
    public void testIgnore() throws NoSuchMethodException {
        assertTrue("class based annotation not detected", ignore(ValuePointer.class, DataStructure.class));
        assertTrue("superclass based annotation not detected", ignore(ValueBox.class, DataStructure.class));
        assertTrue("method based annotation not detected", ignore(ValuePointer.class.getMethod("getHeldType"), DataStructure.class));
        assertTrue("ancestor method based annotation not detected", ignore(ValueBox.class.getMethod("getHeldType"), DataStructure.class));
        assertTrue("return type based annotation not detected", ignore(Dummy.class.getMethod("getFruit"), DataStructure.class));
    }
}
