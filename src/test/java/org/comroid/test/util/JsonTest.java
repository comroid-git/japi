package org.comroid.test.util;

import org.comroid.api.data.seri.adp.JSON;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonTest {
    public static final String[] Names = new String[]{"Logan_Mcclain", "Ayers_Rowe", "Lorene_William"};
    public static final @Language("json") String TestData = """
            [
                  {
                    "id": 0,
                    "name": "Logan_Mcclain"
                  },
                  {
                    "id": 1,
                    "name": "Ayers_Rowe"
                  },
                  {
                    "id": 2,
                    "name": "Lorene_William"
                  }
            ]
            """;

    @Test
    public void test() {
        var arr = JSON.Parser.parse(TestData).asArray();

        Assertions.assertEquals(3, arr.size(), "3 elements expected");

        for (var i = 0; i < 3; i++) {
            var obj = arr.get(i).asObject();

            Assertions.assertEquals(i, obj.get("id").asInt(), "ID mismatch at element " + i);
            Assertions.assertEquals(Names[i], obj.get("name").asString(), "Name mismatch at element " + i);
        }

        /*
        var testString = TestData
                .replace(" ", "")
                .replace("\n", "")
                .replace(":", ": ")
                .replace(",", ", ");
        Assert.assertEquals("toString mismatch", testString, arr.toString());
         */
    }
}
