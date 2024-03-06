package org.comroid.test.util;

import org.comroid.api.data.seri.adp.JSON;
import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals("3 elements expected", 3, arr.size());

        for (var i = 0; i < 3; i++) {
            var obj = arr.get(i).asObject();

            Assert.assertEquals("ID mismatch at element " + i, i, obj.get("id").asInt());
            Assert.assertEquals("Name mismatch at element " + i, Names[i], obj.get("name").asString());
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
